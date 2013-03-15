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

package org.json.rpc.commons;

public final class JsonRpcErrorCodes {

    public static final int PARSE_ERROR_CODE = -32700;
    public static final int INVALID_REQUEST_ERROR_CODE = -32600;
    public static final int METHOD_NOT_FOUND_ERROR_CODE = -32601;
    public static final int INVALID_PARAMS_ERROR_CODE = -32602;
    public static final int INTERNAL_ERROR_CODE = -32603;

    private static final int SERVER_ERROR_START = -32000;


    /**
     * Server error range : (-32099..-32000)
     */

    public static int getServerError(int n) {
        return SERVER_ERROR_START - n;
    }

    private JsonRpcErrorCodes() {
        throw new AssertionError();
    }

}
