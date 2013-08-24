package org.json.rpc.server;

public class CyclicReferenceBugImpl implements CyclicReferenceBug {

    public CyclicResult getResult() {
        return new CyclicResult();
    }
}

interface CyclicReferenceBug {
    CyclicResult getResult();
}

class CyclicResult {
    CyclicA a;
    CyclicA b;

    public CyclicResult() {
    }
}

class CyclicA {
    int value;
    CyclicA ref;

    public CyclicA() {
    }
}