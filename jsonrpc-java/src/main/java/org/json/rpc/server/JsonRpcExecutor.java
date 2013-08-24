/*
 * Copyright (C) 2011 ritwik.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.json.rpc.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.rpc.commons.GsonTypeChecker;
import org.json.rpc.commons.JsonRpcErrorCodes;
import org.json.rpc.commons.JsonRpcException;
import org.json.rpc.commons.JsonRpcRemoteException;
import org.json.rpc.commons.RpcIntroSpection;
import org.json.rpc.commons.TypeChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class JsonRpcExecutor implements RpcIntroSpection {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcExecutor.class);

    private static final Pattern METHOD_PATTERN = Pattern
            .compile("([_a-zA-Z][_a-zA-Z0-9]*)\\.([_a-zA-Z][_a-zA-Z0-9]*)");

    private final Map<String, HandleEntry<?>> handlers;

    private final TypeChecker typeChecker;
    private volatile boolean locked;
    
    private final Gson gson;

    public JsonRpcExecutor() {
        this(new GsonTypeChecker(), new Gson());
    }

    public JsonRpcExecutor(Gson gson) {
        this(new GsonTypeChecker(), gson);
    }

    public JsonRpcExecutor(TypeChecker typeChecker) {
    	this(typeChecker, new Gson());
    }

    @SuppressWarnings("unchecked")
    public JsonRpcExecutor(TypeChecker typeChecker, Gson gson) {
        this.typeChecker = typeChecker;
		this.gson = gson;
        this.handlers = new HashMap<String, HandleEntry<?>>();
        addHandler("system", this, RpcIntroSpection.class);
    }

    public boolean isLocked() {
        return locked;
    }

    public <T> void addHandler(String name, T handler, Class<T>... classes) {
        if (locked) {
            throw new JsonRpcException("executor has been locked, can't add more handlers");
        }

        synchronized (handlers) {
            HandleEntry<T> handleEntry = new HandleEntry<T>(typeChecker, handler, classes);
            if (this.handlers.containsKey(name)) {
                throw new IllegalArgumentException("handler already exists");
            }
            this.handlers.put(name, handleEntry);
        }
    }

    public void execute(JsonRpcServerTransport transport) {
        if (!locked) {
            synchronized (handlers) {
                locked = true;
            }
            LOG.info("locking executor to avoid modification");
        }

        String methodName = null;
        JsonArray params = null;

        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");

        String errorMessage = null;
        Integer errorCode = null;
        String errorData = null;

        JsonObject req = null;
        try {
            String requestData = transport.readRequest();
            LOG.debug("JSON-RPC >>  {}", requestData);
            JsonParser parser = new JsonParser();
            req = (JsonObject) parser.parse(new StringReader(requestData));
        } catch (Throwable t) {
            errorCode = JsonRpcErrorCodes.PARSE_ERROR_CODE;
            errorMessage = "unable to parse json-rpc request";
            errorData = getStackTrace(t);

            LOG.warn(errorMessage, t);

            sendError(transport, resp, errorCode, errorMessage, errorData);
            return;
        }


        try {
            assert req != null;
            resp.add("id", req.get("id"));

            methodName = req.getAsJsonPrimitive("method").getAsString();
            params = (JsonArray) req.get("params");
            if (params == null) {
                params = new JsonArray();
            }
        } catch (Throwable t) {
            errorCode = JsonRpcErrorCodes.INVALID_REQUEST_ERROR_CODE;
            errorMessage = "unable to read request";
            errorData = getStackTrace(t);


            LOG.warn(errorMessage, t);
            sendError(transport, resp, errorCode, errorMessage, errorData);
            return;
        }

        try {
            JsonElement result = executeMethod(methodName, params);
            resp.add("result", result);
        } catch (Throwable t) {
            LOG.warn("exception occured while executing : " + methodName, t);
            if (t instanceof JsonRpcRemoteException) {
                sendError(transport, resp, (JsonRpcRemoteException) t);
                return;
            }
            errorCode = JsonRpcErrorCodes.getServerError(1);
            errorMessage = t.getMessage();
            errorData = getStackTrace(t);
            sendError(transport, resp, errorCode, errorMessage, errorData);
            return;
        }

        try {
            String responseData = resp.toString();
            LOG.debug("JSON-RPC result <<  {}", responseData);
            transport.writeResponse(responseData);
        } catch (Exception e) {
            LOG.warn("unable to write response : " + resp, e);
        }
    }

    private void sendError(JsonRpcServerTransport transport, JsonObject resp, JsonRpcRemoteException e) {
        sendError(transport, resp, e.getCode(), e.getMessage(), e.getData());
    }

    private void sendError(JsonRpcServerTransport transport, JsonObject resp, Integer code, String message, String data) {
        JsonObject error = new JsonObject();
        if (code != null) {
            error.addProperty("code", code);
        }

        if (message != null) {
            error.addProperty("message", message);
        }

        if (data != null) {
            error.addProperty("data", data);
        }

        resp.add("error", error);
        resp.remove("result");
        String responseData = resp.toString();

        LOG.debug("JSON-RPC error <<  {}", responseData);
        try {
            transport.writeResponse(responseData);
        } catch (Exception e) {
            LOG.error("unable to write error response : " + responseData, e);
        }
    }

    private String getStackTrace(Throwable t) {
        StringWriter str = new StringWriter();
        PrintWriter w = new PrintWriter(str);
        t.printStackTrace(w);
        w.close();
        return str.toString();
    }

    private JsonElement executeMethod(String methodName, JsonArray params) throws Throwable {
        try {
            Matcher mat = METHOD_PATTERN.matcher(methodName);
            if (!mat.find()) {
                throw new JsonRpcRemoteException(JsonRpcErrorCodes.INVALID_REQUEST_ERROR_CODE, "invalid method name", null);
            }

            String handleName = mat.group(1);
            methodName = mat.group(2);

            HandleEntry<?> handleEntry = handlers.get(handleName);
            if (handleEntry == null) {
                throw new JsonRpcRemoteException(JsonRpcErrorCodes.METHOD_NOT_FOUND_ERROR_CODE, "no such method exists", null);
            }

            Method executableMethod = null;
            for (Method m : handleEntry.getMethods()) {
                if (!m.getName().equals(methodName)) {
                    continue;
                }

                if (canExecute(m, params)) {
                    executableMethod = m;
                    break;
                }
            }

            if (executableMethod == null) {
                throw new JsonRpcRemoteException(JsonRpcErrorCodes.METHOD_NOT_FOUND_ERROR_CODE, "no such method exists", null);
            }

            Object result = executableMethod.invoke(
                    handleEntry.getHandler(), getParameters(executableMethod, params));

            return gson.toJsonTree(result);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }
            if (t instanceof JsonRpcRemoteException) {
                throw (JsonRpcRemoteException) t;
            }
            throw new JsonRpcRemoteException(JsonRpcErrorCodes.getServerError(0), t.getMessage(), getStackTrace(t));
        }
    }

    public boolean canExecute(Method method, JsonArray params) {
        if (method.getParameterTypes().length != params.size()) {
            return false;
        }

        return true;
    }

    public Object[] getParameters(Method method, JsonArray params) {
        List<Object> list = new ArrayList<Object>();
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            JsonElement p = params.get(i);
            Object o = gson.fromJson(p.toString(), types[i]);
            list.add(o);
        }
        return list.toArray();
    }

    public String[] listMethods() {
        Set<String> methods = new TreeSet<String>();
        for (String name : this.handlers.keySet()) {
            HandleEntry<?> handleEntry = this.handlers.get(name);
            for (String method : handleEntry.getSignatures().keySet()) {
                methods.add(name + "." + method);
            }
        }
        String[] arr = new String[methods.size()];
        return methods.toArray(arr);
    }

    public String[] methodSignature(String method) {
        if (method == null) {
            throw new NullPointerException("method");
        }

        Matcher mat = METHOD_PATTERN.matcher(method);
        if (!mat.find()) {
            throw new IllegalArgumentException("invalid method name");
        }

        String handleName = mat.group(1);
        String methodName = mat.group(2);

        Set<String> signatures = new TreeSet<String>();

        HandleEntry<?> handleEntry = handlers.get(handleName);
        if (handleEntry == null) {
            throw new IllegalArgumentException("no such method exists");
        }

        for (Method m : handleEntry.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }

            String[] sign = handleEntry.getSignatures().get(m.getName());

            StringBuffer buff = new StringBuffer(sign[0]);
            for (int i = 1; i < sign.length; i++) {
                buff.append(",").append(sign[i]);
            }

            signatures.add(buff.toString());
        }

        if (signatures.size() == 0) {
            throw new IllegalArgumentException("no such method exists");
        }

        String[] arr = new String[signatures.size()];
        return signatures.toArray(arr);
    }


}
