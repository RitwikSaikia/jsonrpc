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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GsonTypeCheckerTest {

    private GsonTypeChecker typeChecker;

    @BeforeTest
    public void setup() {
        typeChecker = new GsonTypeChecker();
    }

    @DataProvider
    public Object[][] validTypes() {
        return new Object[][]{
                {void.class}, //
                {byte.class}, //
                {char.class}, //
                {int.class}, //
                {short.class}, //
                {long.class}, //
                {float.class}, //
                {double.class}, //
                {boolean.class}, //
                {Void.class}, //
                {String.class}, //
                {Byte.class}, //
                {Integer.class}, //
                {Short.class}, //
                {Long.class}, //
                {Float.class}, //
                {Double.class}, //
                {Boolean.class}, //
                {Character.class}, //
                {Date.class}, //

                {byte[].class}, //
                {char[].class}, //
                {int[].class}, //
                {short[].class}, //
                {long[].class}, //
                {float[].class}, //
                {double[].class}, //
                {boolean[].class}, //
                {String[].class}, //
                {Byte[].class}, //
                {Integer[].class}, //
                {Short[].class}, //
                {Long[].class}, //
                {Float[].class}, //
                {Double[].class}, //
                {Boolean[].class}, //
                {Character[].class}, //
                {Date[].class}, //


                {ValidInnerClass.class}, //
                {TooManyInnerClasses.class}, //
                {ZeroArgConstructorClass.class}, //

                {ValidInnerClass[].class}, //
                {TooManyInnerClasses[].class}, //
                {ZeroArgConstructorClass[].class}, //
        };
    }

    static class TooManyInnerClasses {
        A a;
        int b;

        static class A {
            C c;
            double d;

            static class C {
                String e;
            }
        }
    }

    static class ValidInnerClass {
        static ParameterizedClass<Integer> a;
        transient CyclicClass b;

        int c;
    }

    static class ZeroArgConstructorClass {
        public ZeroArgConstructorClass() {
        }

        public ZeroArgConstructorClass(String a) {
        }
    }

    static class ParameterizedClass<T> {
        String a;
        double b;
    }

    static class CyclicClass {
        CyclicClass a;
        int b;
        boolean c;
    }

    static class FinalFields {
        final int n = 0;
    }

    static class ParameterizedCollections {
        Set<Integer> a;
        List<Byte> b;
        Map<String, Object> d;
    }

    static interface InterfaceClass {
    }

    static abstract class AbstractClass {
    }

    static class NonZeroArgConstructorClass {
        public NonZeroArgConstructorClass(String c) {
        }

        public NonZeroArgConstructorClass(int a, double b) {
        }
    }


    @Test(dataProvider = "validTypes")
    public void testValidType(Class<?> clazz) {
        assertTrue(typeChecker.isValidType(clazz));
    }

    @Test(dataProvider = "validTypes")
    public void testValidTypeWithException(Class<?> clazz) {
        assertTrue(typeChecker.isValidType(clazz, true));
    }

    @DataProvider
    public Object[][] invalidTypes() {
        return new Object[][]{
                {ParameterizedClass.class}, //
                {CyclicClass.class}, //
                {FinalFields.class}, //
                {ParameterizedCollections.class}, //
                {InterfaceClass.class}, //
                {AbstractClass.class}, //
                {NonZeroArgConstructorClass.class}, //

                {ParameterizedClass[].class}, //
                {CyclicClass[].class}, //
                {FinalFields[].class}, //
                {ParameterizedCollections[].class}, //
                {InterfaceClass[].class}, //
                {AbstractClass[].class}, //
                {NonZeroArgConstructorClass[].class}, //

                {new Object() {
                }.getClass()}, //  Anonymous class
        };
    }


    @Test(dataProvider = "invalidTypes")
    public void testInvalidType(Class<?> clazz) {
        assertFalse(typeChecker.isValidType(clazz));
    }

    @Test(dataProvider = "invalidTypes", expectedExceptions = {IllegalArgumentException.class})
    public void testInvalidTypeWithException(Class<?> clazz) {
        assertFalse(typeChecker.isValidType(clazz, true));
    }

    @DataProvider
    public Object[][] typeNames() {
        return new Object[][]{
                {void.class, "void"}, //
                {Void.class, "void"}, //
                {boolean.class, "boolean"}, //
                {Boolean.class, "boolean"}, //
                {double.class, "double"}, //
                {Double.class, "double"}, //
                {float.class, "double"}, //
                {Float.class, "double"}, //
                {byte.class, "int"}, //
                {Byte.class, "int"}, //
                {char.class, "int"}, //
                {Character.class, "int"}, //
                {int.class, "int"}, //
                {Integer.class, "int"}, //
                {short.class, "int"}, //
                {Short.class, "int"}, //
                {long.class, "int"}, //
                {Long.class, "int"}, //
                {String.class, "string"}, //

                {boolean[].class, "array"}, //
                {Boolean[].class, "array"}, //
                {double[].class, "array"}, //
                {Double[].class, "array"}, //
                {float[].class, "array"}, //
                {Float[].class, "array"}, //
                {byte[].class, "array"}, //
                {Byte[].class, "array"}, //
                {char[].class, "array"}, //
                {Character[].class, "array"}, //
                {int[].class, "array"}, //
                {Integer[].class, "array"}, //
                {short[].class, "array"}, //
                {Short[].class, "array"}, //
                {long[].class, "array"}, //
                {Long[].class, "array"}, //
                {String[].class, "array"}, //
                {FinalFields[].class, "array"}, //

                {Set.class, "struct"}, //
                {CyclicClass.class, "struct"}, //
                {new Object() {
                }.getClass(), "struct"}, //
        };
    }

    @Test(dataProvider = "typeNames")
    public void testGetTypeName(Class<?> clazz, String name) {
        assertEquals(typeChecker.getTypeName(clazz), name);
    }


}
