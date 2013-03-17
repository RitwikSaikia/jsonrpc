package org.json.rpc.server;

import org.json.rpc.client.JsonRpcClientTransport;
import org.json.rpc.client.JsonRpcInvoker;
import org.json.rpc.commons.JsonRpcRemoteException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author ritwik
 */
public class JsonRpcExecutorTest {

    private JsonRpcExecutor executor;

    @BeforeMethod
    public void setupMethod() {
        executor = new JsonRpcExecutor();
    }

    @Test //Issue #6: Cyclic Reference if Custom Type is used more than once
    public void testIssue006_withDistinctEndPoint() throws Exception {
        CyclicReferenceBugImpl impl = new CyclicReferenceBugImpl() {

            public CyclicResult getResult() {
                CyclicA a = new CyclicA();
                a.value = 1;
                CyclicA b = new CyclicA();
                b.value = 2;
                CyclicResult result = new CyclicResult();
                result.a = a;
                result.b = b;
                return result;
            }
        };
        testCycle(impl, 1, 2, null, null);
    }

    @Test //Issue #6: Cyclic Reference if Custom Type is used more than once
    public void testIssue006_withSingleEndPoint() throws Exception {
        CyclicReferenceBugImpl impl = new CyclicReferenceBugImpl() {
            public CyclicResult getResult() {
                CyclicA a = new CyclicA();
                a.value = 1;
                CyclicA b = new CyclicA();
                b.value = 2;
                CyclicA c = new CyclicA();
                c.value = 3;
                CyclicResult result = new CyclicResult();
                result.a = a;
                result.b = b;
                result.a.ref = c;
                result.b.ref = c;
                return result;
            }
        };
        testCycle(impl, 1, 2, 3, 3);
    }

    @Test //Issue #6: Cyclic Reference if Custom Type is used more than once
    public void testIssue006_withCycle() throws Exception {
        CyclicReferenceBugImpl impl = new CyclicReferenceBugImpl() {
            public CyclicResult getResult() {
                CyclicA a = new CyclicA();
                a.value = 1;
                CyclicA b = new CyclicA();
                b.value = 2;
                CyclicResult result = new CyclicResult();
                result.a = a;
                result.b = b;
                result.a.ref = b;
                result.b.ref = a;
                return result;
            }
        };
        try {
            testCycle(impl, 1, 2, 2, 1);
            fail("should have thrown " + JsonRpcRemoteException.class.getName() + " with cause = " + StackOverflowError.class.getName());
        } catch (JsonRpcRemoteException e) {
            assertTrue(e.getMsg().indexOf("Caused by java.lang.StackOverflowError") > 0);
            // Be lenient and let the developer, handle this error
        }
    }

    private void testCycle(CyclicReferenceBugImpl impl, int a, int b, Integer refA, Integer refB) {
        executor.addHandler("impl", impl, CyclicReferenceBug.class);

        JsonRpcInvoker invoker = new JsonRpcInvoker();
        CyclicReferenceBug bug = invoker.get(new JsonRpcClientTransport() {
            public String call(String requestData) throws Exception {
                final String request = requestData;
                final StringBuilder response = new StringBuilder();
                executor.execute(new JsonRpcServerTransport() {
                    public String readRequest() throws Exception {
                        return request;
                    }

                    public void writeResponse(String responseData) throws Exception {
                        response.append(responseData);
                    }
                });
                return response.toString();
            }
        }, "impl", CyclicReferenceBug.class);

        CyclicResult result = bug.getResult();
        assertNotNull(result);
        assertNotNull(result.a);
        assertNotNull(result.b);
        assertEquals(result.a.value, a);
        assertEquals(result.b.value, b);
        if (refA == null) {
            assertNull(result.a.ref);
        } else {
            assertEquals(result.a.ref.value, refA.intValue());
        }
        if (refB == null) {
            assertNull(result.b.ref);
        } else {
            assertEquals(result.b.ref.value, refB.intValue());
        }
    }


}
