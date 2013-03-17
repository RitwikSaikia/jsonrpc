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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.rpc.commons.JsonRpcRemoteException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

public class JsonRpcInvokerTest {

    private JsonRpcInvoker invoker;

    @BeforeTest
    public void setup() {
        invoker = new JsonRpcInvoker();
    }

    @Test
    public void testErrorWithPrimitiveData() {
        final String errorMessage = "some error message";
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");
        resp.addProperty("error", errorMessage);

        TestInterface handle = invoker.get(getTransport(resp), "someHandler", TestInterface.class);

        try {
            handle.call(1);
            fail("should throw exception");
        } catch (JsonRpcRemoteException e) {
            assertNull(e.getCode());
            assertEquals(e.getMsg(), errorMessage);
            assertNull(e.getData());
        }
    }

    @Test // issue number is from google code,
    // upgrade to Gson 2.2.2 was breaking it, as
    // previous version was not able to detect the
    // last '}' brace as parse error
    public void testIssue0002() throws Exception {
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");
        JsonObject error = new JsonObject();
        error.addProperty("code", -32002);
        error.addProperty("message", "service.invalid-parameters");
        error.add("data", new JsonParser().parse("{\"email\":[\"'email' is no valid email address in the basic format local-part@hostname\"]}"));
        resp.add("error", error);

        TestInterface handle = invoker.get(getTransport(resp), "someHandler", TestInterface.class);

        try {
            handle.call(1);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            fail("issue 0002 is not resolved");
        } catch (Exception e) {
            // ignore
        }
    }

    @DataProvider
    public Object[][] testErrorData() {
        return new Object[][]{
                {null, null, null}, //
                {123, null, null}, //
                {null, "some message", null}, //
                {null, null, "some data"}, //
                {null, "some message", "some data"}, //
                {123, null, "some data"}, //
                {123, "some message", null}, //
                {123, "some message", "some data"}, //
        };
    }

    @Test(dataProvider = "testErrorData")
    public void testError(Integer errorCode, String errorMessage, String errorData) {
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");

        JsonObject error = new JsonObject();
        if (errorCode != null) {
            error.addProperty("code", errorCode);
        }
        if (errorMessage != null) {
            error.addProperty("message", errorMessage);
        }
        if (errorData != null) {
            error.addProperty("data", errorData);
        }

        resp.add("error", error);


        TestInterface handle = invoker.get(getTransport(resp), "someHandler", TestInterface.class);

        try {
            handle.call(1);
            fail("should throw exception");
        } catch (JsonRpcRemoteException e) {
            if (errorCode == null) {
                assertNull(errorCode);
            } else {
                assertEquals(e.getCode(), errorCode);
            }
            if (errorMessage == null) {
                assertNull(e.getMsg());
            } else {
                assertEquals(e.getMsg(), errorMessage);
            }
            if (errorData == null) {
                assertNull(e.getData());
            } else {
                assertEquals(e.getData(), errorData);
            }
        }
    }

    @Test
    public void testErrorArray() {
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");

        JsonArray error = new JsonArray();
        resp.add("error", error);

        TestInterface handle = invoker.get(getTransport(resp), "someHandler", TestInterface.class);

        try {
            handle.call(1);
            fail("should throw exception");
        } catch (JsonRpcRemoteException e) {
            assertNull(e.getCode());
            assertNull(e.getData());
        }
    }


    @Test
    public void testResultVoid() {
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");

        TestInterface handle = invoker.get(getTransport(resp), "someHandler", TestInterface.class);
        handle.call();
    }


    static interface TestInterface {
        boolean call(int arg);

        void call();
    }

    static JsonRpcClientTransport getTransport(final JsonElement resp) {
        return new JsonRpcClientTransport() {
            public String call(String requestData) throws Exception {
                return resp.toString();
            }
        };
    }
}
