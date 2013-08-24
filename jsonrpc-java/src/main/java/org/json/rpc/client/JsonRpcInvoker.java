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

package org.json.rpc.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.rpc.commons.GsonTypeChecker;
import org.json.rpc.commons.JsonRpcClientException;
import org.json.rpc.commons.JsonRpcRemoteException;
import org.json.rpc.commons.TypeChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;

public final class JsonRpcInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcInvoker.class);

    private final Random rand = new Random();

    private final TypeChecker typeChecker;

    private final Gson gson;

    public JsonRpcInvoker() {
        this(new GsonTypeChecker(), new Gson());
    }

    public JsonRpcInvoker(Gson gson) {
        this(new GsonTypeChecker(), gson);
    }

    public JsonRpcInvoker(TypeChecker typeChecker) {
        this(typeChecker, new Gson());
    }

    public JsonRpcInvoker(TypeChecker typeChecker, Gson gson) {
        this.typeChecker = typeChecker;
		this.gson = gson;
    }

    public <T> T get(final JsonRpcClientTransport transport, final String handle, final Class<T>... classes) {
        for (Class<T> clazz : classes) {
            typeChecker.isValidInterface(clazz);
        }
        return (T) Proxy.newProxyInstance(JsonRpcInvoker.class.getClassLoader(), classes, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return JsonRpcInvoker.this.invoke(handle, transport, method, args);
            }
        });
    }

    private Object invoke(String handleName,
                          JsonRpcClientTransport transport, Method method,
                          Object[] args) throws Throwable {
        int id = rand.nextInt(Integer.MAX_VALUE);
        String methodName = handleName + "." + method.getName();

        JsonObject req = new JsonObject();
        req.addProperty("id", id);
        req.addProperty("method", methodName);

        JsonArray params = new JsonArray();
        if (args != null) {
            for (Object o : args) {
                params.add(gson.toJsonTree(o));
            }
        }
        req.add("params", params);

        String requestData = req.toString();
        LOG.debug("JSON-RPC >>  {}", requestData);
        String responseData;
        try {
            responseData = transport.call(requestData);
        } catch (Exception e) {
            throw new JsonRpcClientException("unable to get data from transport", e);
        }
        LOG.debug("JSON-RPC <<  {}", responseData);

        JsonParser parser = new JsonParser();
        JsonObject resp = (JsonObject) parser.parse(new StringReader(responseData));

        JsonElement result = resp.get("result");
        JsonElement error = resp.get("error");

        if (error != null && !error.isJsonNull()) {
            if (error.isJsonPrimitive()) {
                throw new JsonRpcRemoteException(error.getAsString());
            } else if (error.isJsonObject()) {
                JsonObject o = error.getAsJsonObject();
                Integer code = (o.has("code") ? o.get("code").getAsInt() : null);
                String message = (o.has("message") ? o.get("message").getAsString() : null);
                String data = (o.has("data") ? (o.get("data") instanceof JsonObject ? o.get("data").toString() : o.get("data").getAsString()) : null);
                throw new JsonRpcRemoteException(code, message, data);
            } else {
                throw new JsonRpcRemoteException("unknown error, data = " + error.toString());
            }
        }

        if (method.getReturnType() == void.class) {
            return null;
        }

        return gson.fromJson(result.toString(), method.getReturnType());
    }
}
