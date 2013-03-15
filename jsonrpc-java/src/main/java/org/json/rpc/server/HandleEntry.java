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

import org.json.rpc.commons.TypeChecker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class HandleEntry<T> {

    private final T handler;
    private final Map<String, String[]> signatures;
    private final Set<Method> methods;

    public HandleEntry(TypeChecker typeChecker, T handler, Class<T>... classes) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (classes.length == 0) {
            throw new IllegalArgumentException(
                    "at least one interface has to be mentioned");
        }

        this.handler = handler;

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        Set<Method> set = new HashSet<Method>();

        for (Class<?> clazz : classes) {
            typeChecker.isValidInterface(clazz, true);

            if (!clazz.isInterface()) {
                throw new IllegalArgumentException(
                        "class should be an interface : " + clazz);
            }

            for (Method m : clazz.getMethods()) {
                set.add(m);
                Class<?>[] params = m.getParameterTypes();

                List<String> list = map.get(m.getName());
                if (list == null) {
                    list = new ArrayList<String>();
                }
                StringBuffer buff = new StringBuffer(typeChecker.getTypeName(m
                        .getReturnType()));
                for (int i = 0; i < params.length; i++) {
                    buff.append(",").append(typeChecker.getTypeName(params[i]));
                }
                list.add(buff.toString());
                map.put(m.getName(), list);
            }

        }

        Map<String, String[]> signs = new TreeMap<String, String[]>();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String[] arr = new String[e.getValue().size()];
            signs.put(e.getKey(), e.getValue().toArray(arr));
        }

        this.methods = Collections.unmodifiableSet(set);
        this.signatures = Collections.unmodifiableMap(signs);
    }

    public T getHandler() {
        return handler;
    }

    public java.util.Map<String, String[]> getSignatures() {
        return signatures;
    }

    public java.util.Set<java.lang.reflect.Method> getMethods() {
        return methods;
    }
}
