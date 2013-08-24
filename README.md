# JsonRpc

[![Build Status](https://travis-ci.org/RitwikSaikia/jsonrpc.png?branch=master)](https://travis-ci.org/RitwikSaikia/jsonrpc)

JSON-RPC is a Java library implementing a very light weight client/server functionality of JSON-RPC protocol.
Server/Client API is designed in such a way that, you don't have to worry about the details of the protocol.
Just register the implementation classes in the server side, and create remote proxy objects at the client sides on the fly.

The API works well in **Android/Google App Engine/Javascript** applications.
<br /><br /><br /><br /><br />

# Usage

## Defining Interfaces

Lets pickup an example of a Calculator service. 

The client side interface will be
```java
public interface Calculator {

   double add(double x, double y);

   double multiply(double x, double y);

}
```

The server side implementation will be
```java
public class SimpleCalculatorImpl implements Calculator {
   
   public double add(double x, double y) {
      return x + y;
   }

   public double multiply(double x, double y) {
      return x * y;
   }

}
```

## Hosting the service

### Binding Service Implementation

Once the service is ready, it needs to be bound to the JSON-RPC Server to make it available.
```java
private JsonRpcExecutor bind() {
   JsonRpcExecutor executor = new JsonRpcExecutor();

   Calculator calcImpl = new SimpleCalculatorImpl();
   executor.addHandler("calc", calcImpl, Calculator.class); 

   // add more services here

   return executor;
}
```

### Hosting with a Servlet
```
public class JsonRpcServlet extends HttpServlet {

    private final JsonRpcExecutor executor;

    public JsonRpcServlet() {
        executor = bind();
    }

    private JsonRpcExecutor bind() {
        JsonRpcExecutor executor = new JsonRpcExecutor();

        Calculator calcImpl = new SimpleCalculatorImpl();
        executor.addHandler("calc", calcImpl, Calculator.class);
        // add more services here

        return executor;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        executor.execute(new JsonRpcServletTransport(req, resp));
    }

}
```

## Calling the service

### Call from Java Client

```java
// where the servlet is hosted
String url = "http://127.0.0.1:8080/jsonrpc"; 

HttpJsonRpcClientTransport transport = new HttpJsonRpcClientTransport(new URL(url));

JsonRpcInvoker invoker = new JsonRpcInvoker();
Calculator calc = invoker.get(transport, "calc", Calculator.class);

double result = calc.add(1.2, 7.5);
```

**Exception Handling**
* In case of remote exception it throws a JsonRpcRemoteException
  * invalid request
  * unable to parse json response
  * unknown method
  * any custom exception thrown by the application (SimpleCalculator) will appear with a full stack trace at the client side.
* In case of local exception it throws a JsonRpcClientException
  * unable to reach service end point
  * unable to parse json response

### Call from JavaScript Client

```javascript
<script src="jsonrpc.js"></script>
<script>
var jsonService = new JsonRpc.ServiceProxy("/jsonrpc", {
            asynchronous: true,
            methods: ['calc.add', 'calc.multiply']
        });

// access it asynchronously
JsonRpc.setAsynchronous(jsonService, true);
jsonService.calc.add({
    params:[1.2, 7.5],
    onSuccess: function(result) {
        alert("result is " + result);
    },
    onException: function(e) {
        alert("Unable to compute because: " + e);
        return true;
    }
});

// access it synchronously
JsonRpc.setAsynchronous(jsonService, false);
try {
    var result = jsonService.calc.add(1.2, 7.5);
    alert("result is " + result);
} catch (e) {
    alert("Unable to compute because: " + e);
}
</script>
```

**Exception Handling**
In case of remote exception it throws an _Error_

## Dependencies
* [Gson 1.4](http://code.google.com/p/google-gson/)
* [SLF4J 1.5.8](http://www.slf4j.org/)

### Maven
```xml
<dependencies>
   <dependency>
      <groupId>org.json.rpc</groupId>
      <artifactId>jsonrpc</artifactId>
      <version>1.1</version>
   </dependency>
</dependencies>

<repositories>
   <repository>
      <id>ritwik-mvn-repo</id>
      <url>http://ritwik.net/mvn/releases/</url>
   </repository>
</repositories>
```
Note: classifier = server/client can be used to include only specific implementations.

## Logging
JSON-RPC uses SLF4J for logging, so you will have to include specific implementation based on your requirement

### No Logging
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-nop</artifactId>
    <version>1.5.8</version>
</dependency>
```

### Log4j
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
    <version>1.5.8</version>
</dependency>

<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.16</version>
</dependency>
```

### Android
```xml
<dependency>
    <groupId>org.json.rpc</groupId>
    <artifactId>jsonrpc</artifactId>
    <version>1.0</version>
    <classifier>client</classifier>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-android</artifactId>
    <version>1.6.1-RC1</version>
</dependency>
```
