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

public final class JsonRpcRemoteException extends JsonRpcException {

    private final Integer code;
    private final String msg;
    private final String data;

    public JsonRpcRemoteException(String msg) {
        super(msg);
        this.code = null;
        this.msg = msg;
        this.data = null;
    }

    public JsonRpcRemoteException(Integer code, String msg, String data) {
        super(format(code, msg, data));
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getData() {
        return data;
    }

    private static String format(Integer code, String message, String data) {
        StringBuilder str = new StringBuilder();
        str.append("jsonrpc error");
        if (code != null) {
            str.append("[").append(code).append("]");
        }
        str.append(" : ");
        if (message != null) {
            str.append(message);
        }
        if (data != null) {
            str.append("\n");
            str.append("Caused by " + data);
        }
        return str.toString();
    }

}
