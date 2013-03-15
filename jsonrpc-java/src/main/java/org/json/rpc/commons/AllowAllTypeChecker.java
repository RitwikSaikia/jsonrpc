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

import java.lang.reflect.Method;

public class AllowAllTypeChecker extends TypeChecker {

    public boolean isValidType(Class<?> clazz) {
        return true;
    }

    public boolean isValidType(Class<?> clazz, boolean throwException) {
        return true;
    }

    public String getTypeName(Class<?> clazz) {
        return clazz.getName();
    }

    public boolean isValidMethod(Method method) {
        return true;
    }

    public boolean isValidInterface(Class<?> clazz) {
        return true;
    }

    public boolean isValidMethod(Method method, boolean throwException) {
        return true;
    }

    public boolean isValidInterface(Class<?> clazz, boolean throwException) {
        return true;
    }
}
