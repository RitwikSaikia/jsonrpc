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
import java.lang.reflect.Modifier;

public abstract class TypeChecker {

    public boolean isValidType(Class<?> clazz) {
        return isValidType(clazz, false);
    }

    public abstract boolean isValidType(Class<?> clazz, boolean throwException);

    public abstract String getTypeName(Class<?> clazz);

    public boolean isValidMethod(Method method) {
        return isValidMethod(method, false);
    }

    public boolean isValidInterface(Class<?> clazz) {
        return isValidInterface(clazz, false);
    }

    public boolean isValidMethod(Method method, boolean throwException) {
        Class<?> returnType = method.getReturnType();
        boolean result = false;
        try {
            result = isValidType(returnType, throwException);
            if (!result) {
                if (throwException) {
                    throw new IllegalArgumentException("invalid return type : " + returnType);
                }
                return false;
            }
        } catch (RuntimeException e) {
            if (!result) {
                if (throwException) {
                    throw new IllegalArgumentException("invalid return type : " + returnType, e);
                }
                return false;
            }
        }

        for (Class<?> paramType : method.getParameterTypes()) {
            result = false;
            try {
                result = isValidType(paramType, throwException);
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid parameter type : " + paramType);
                    }
                    return false;
                }
            } catch (RuntimeException e) {
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid parameter type : " + paramType, e);
                    }
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isValidInterface(Class<?> clazz, boolean throwException) {
        if (!clazz.isInterface()) {
            if (throwException) {
                throw new IllegalArgumentException("not an interface : " + clazz);
            }
            return false;
        }

        for (Method method : clazz.getDeclaredMethods()) {
            int m = method.getModifiers();
            if (Modifier.isStatic(m)) {
                continue;
            }

            boolean result = false;
            try {
                result = isValidMethod(method, throwException);
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid method : " + method);
                    }
                    return false;
                }
            } catch (RuntimeException e) {
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid method : " + method, e);
                    }
                    return false;
                }
            }
        }

        return true;
    }

}
