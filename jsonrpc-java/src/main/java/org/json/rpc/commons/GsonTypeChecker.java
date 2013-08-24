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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GsonTypeChecker extends TypeChecker {

    @Override
    public boolean isValidType(Class<?> clazz, boolean throwException) {
        return isValidType(clazz, throwException, null);
    }

    private boolean isValidType(Class<?> clazz, boolean throwException, Set<Class<?>> visited) {
        if (clazz.isPrimitive()) {
            return true;
        }

        if (Boolean.class == clazz) {
            return true;
        }

        if (Number.class.isAssignableFrom(clazz)) {
            return true;
        }

        if (String.class == clazz) {
            return true;
        }

        if (Character.class == clazz) {
            return true;
        }

        if (Date.class == clazz) {
            return true;
        }

        if (clazz.isArray()) {
            return this.isValidType(clazz.getComponentType(), throwException, visited);
        }

        /**
         * False cases
         */

        if (clazz.isAnonymousClass()) {
            if (throwException) {
                throw new IllegalArgumentException("anonymous class not allowed : " + clazz);
            }
            return false;
        }

        if (Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers())) {
            if (throwException) {
                throw new IllegalArgumentException("abstract class or interface not allowed : " + clazz);
            }
            return false;
        }

        if (clazz.getTypeParameters().length > 0) {
            if (throwException) {
                throw new IllegalArgumentException("parametrized classes not allowed : " + clazz);
            }
            return false;
        }

        boolean zeroArgConstructor = (clazz.getConstructors().length == 0);
        for (Constructor c : clazz.getConstructors()) {
            if (c.getParameterTypes().length == 0) {
                zeroArgConstructor = true;
                break;
            }
        }

        if (!zeroArgConstructor) {
            if (throwException) {
                throw new IllegalArgumentException("no zero-arg constructor found : " + clazz);
            }
            return false;
        }

        // avoid cyclic references
        // Issue #6: Be more lenient and allow more types,
        //  let the developer handle StackOverFlowError
        // in case of cycles
        visited = (visited == null ? new HashSet<Class<?>>() : visited);
        if (visited.contains(clazz)) {
            return true;
        }
        visited.add(clazz);

        // Check for fields because Gson uses fields
        for (Field f : clazz.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m)) {
                continue;
            }

            if (Modifier.isFinal(m)) {
                if (throwException) {
                    throw new IllegalArgumentException("final field found : " + f);
                }
                return false;
            }

            boolean result = false;
            try {
                result = isValidType(f.getType(), throwException, visited);
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid field found : " + f);
                    }
                    return false;
                }
            } catch (RuntimeException e) {
                if (!result) {
                    if (throwException) {
                        throw new IllegalArgumentException("invalid field found : " + f, e);
                    }
                    return false;
                }
            }
        }


        return true;
    }

    @Override
    public String getTypeName(Class<?> clazz) {
        if (clazz == void.class || clazz == Void.class) {
            return void.class.getName();
        }

        if (clazz == boolean.class || Boolean.class == clazz) {
            return boolean.class.getName();
        }

        if (clazz == double.class || clazz == float.class
                || Double.class == clazz || Float.class == clazz) {
            return double.class.getName();
        }

        if (clazz == byte.class || clazz == char.class || clazz == int.class || clazz == short.class
                || clazz == long.class || clazz == Character.class || Number.class.isAssignableFrom(clazz)) {
            return int.class.getName();
        }

        if (clazz == String.class) {
            return "string";
        }

        if (clazz.isArray()) {
            return "array";
        }

        return "struct";
    }
}
