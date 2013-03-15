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
 *
 * This code has been originally taken from JSON/XML-RPC Client
 * <http://code.google.com/p/json-xml-rpc/>.
 *
 * It has been modified to support only JSON-RPC
 *
 * JSON/XML-RPC Client <http://code.google.com/p/json-xml-rpc/>
 * Version: 0.8.0.2 (2007-12-06)
 * Copyright: 2007, Weston Ruter <http://weston.ruter.net/>
 * License: Dual licensed under MIT <http://creativecommons.org/licenses/MIT/>
 *          and GPL <http://creativecommons.org/licenses/GPL/2.0/> licenses.
 *
 * Original inspiration for the design of this implementation is from jsolait, from which
 * are taken the "ServiceProxy" name and the interface for synchronous method calls.
 */
var JsonRpc = {
    version:"1.0.0.1",
    requestCount: 0
};
JsonRpc.ServiceProxy = function (serviceUrl, options) {
    this.__serviceURL = serviceUrl;
    this.__isCrossSite = false;

    var urlParts = this.__serviceURL.match(/^(\w+:)\/\/([^\/]+?)(?::(\d+))?(?:$|\/)/);
    if (urlParts) {
        this.__isCrossSite = (
                location.protocol != urlParts[1] ||
                        document.domain != urlParts[2] ||
                        location.port != (urlParts[3] || "")
                );
    }

    if (this.__isCrossSite) {
        throw new Error("Cross site rpc not supported yet");
    }

    //Set other default options
    var providedMethodList;
    this.__isAsynchronous = true;
    this.__authUsername = null;
    this.__authPassword = null;
    this.__dateEncoding = 'ISO8601'; // ("@timestamp@" || "@ticks@") || "classHinting" || "ASP.NET"
    this.__decodeISO8601 = true; //JSON only

    //Get the provided options
    if (options instanceof Object) {
        if (options.asynchronous !== undefined) {
            this.__isAsynchronous = !!options.asynchronous;
        }
        if (options.user != undefined)
            this.__authUsername = options.user;
        if (options.password != undefined)
            this.__authPassword = options.password;
        if (options.dateEncoding != undefined)
            this.__dateEncoding = options.dateEncoding;
        if (options.decodeISO8601 != undefined)
            this.__decodeISO8601 = !!options.decodeISO8601;
        providedMethodList = options.methods;
    }

    // Obtain the list of methods made available by the server
    if (providedMethodList) {
        this.__methodList = providedMethodList;
    } else {
        var async = this.__isAsynchronous;
        this.__isAsynchronous = false;
        this.__methodList = this.__callMethod("system.listMethods", []);
        this.__isAsynchronous = async;
    }
    this.__methodList.push("system.listMethods");

    //Create local "wrapper" functions which reference the methods obtained above
    for (var methodName, i = 0; methodName = this.__methodList[i]; i++) {
        //Make available the received methods in the form of chained property lists (eg. "parent.child.methodName")
        var methodObject = this;
        var propChain = methodName.split(/\./);
        for (var j = 0; j + 1 < propChain.length; j++) {
            if (!methodObject[propChain[j]])
                methodObject[propChain[j]] = {};
            methodObject = methodObject[propChain[j]];
        }

        //Create a wrapper to this.__callMethod with this instance and this methodName bound
        var wrapper = (function(instance, methodName) {
            var call = {instance:instance, methodName:methodName}; //Pass parameters into closure
            return function() {
                if (call.instance.__isAsynchronous) {
                    if (arguments.length == 1 && arguments[0] instanceof Object) {
                        call.instance.__callMethod(call.methodName,
                                arguments[0].params,
                                arguments[0].onSuccess,
                                arguments[0].onException,
                                arguments[0].onComplete);
                    }
                    else {
                        call.instance.__callMethod(call.methodName,
                                arguments[0],
                                arguments[1],
                                arguments[2],
                                arguments[3]);
                    }
                    return undefined;
                }
                else return call.instance.__callMethod(call.methodName, JsonRpc.toArray(arguments));
            };
        })(this, methodName);
        methodObject[propChain[propChain.length - 1]] = wrapper;
    }
};

JsonRpc.setAsynchronous = function(serviceProxy, isAsynchronous) {
    serviceProxy.__isAsynchronous = !!isAsynchronous;
};


JsonRpc.ServiceProxy.prototype.__callMethod = function(methodName, params, successHandler, exceptionHandler, completeHandler) {
    JsonRpc.requestCount++;

    //Verify that successHandler, exceptionHandler, and completeHandler are functions
    if (this.__isAsynchronous) {
        if (successHandler && typeof successHandler != 'function')
            throw Error('The asynchronous onSuccess handler callback function you provided is invalid; the value you provided (' + successHandler.toString() + ') is of type "' + typeof(successHandler) + '".');
        if (exceptionHandler && typeof exceptionHandler != 'function')
            throw Error('The asynchronous onException handler callback function you provided is invalid; the value you provided (' + exceptionHandler.toString() + ') is of type "' + typeof(exceptionHandler) + '".');
        if (completeHandler && typeof completeHandler != 'function')
            throw Error('The asynchronous onComplete handler callback function you provided is invalid; the value you provided (' + completeHandler.toString() + ') is of type "' + typeof(completeHandler) + '".');
    }

    try {
        //Assign the provided callback function to the response lookup table
        if (this.__isAsynchronous) {
            JsonRpc.pendingRequests[String(JsonRpc.requestCount)] = {
                //method:methodName,
                onSuccess:successHandler,
                onException:exceptionHandler,
                onComplete:completeHandler
            };
        }

        //Obtain and verify the parameters
        if (params && (!(params instanceof Object) || params instanceof Date)) //JSON-RPC 1.1 allows params to be a hash not just an array
            throw Error('When making asynchronous calls, the parameters for the method must be passed as an array (or a hash); the value you supplied (' + String(params) + ') is of type "' + typeof(params) + '".');

        //Prepare the XML-RPC request
        var request,postData;
        request = {
            version:"2.0",
            method:methodName,
            id:JsonRpc.requestCount
        };
        if (params)
            request.params = params;
        postData = this.__toJSON(request);


        //XMLHttpRequest chosen (over Ajax.Request) because it propogates uncaught exceptions
        var xhr;
        if (window.XMLHttpRequest)
            xhr = new XMLHttpRequest();
        else if (window.ActiveXObject) {
            try {
                xhr = new ActiveXObject('Msxml2.XMLHTTP');
            } catch(err) {
                xhr = new ActiveXObject('Microsoft.XMLHTTP');
            }
        }
        xhr.open('POST', this.__serviceURL, this.__isAsynchronous, this.__authUsername, this.__authPassword);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.setRequestHeader('Accept', 'application/json');

        if (this.__isAsynchronous) {
            //Send the request
            xhr.send(postData);

            //Handle the response
            var instance = this;
            var requestInfo = {id:JsonRpc.requestCount}; //for XML-RPC since the 'request' object cannot contain request ID
            xhr.onreadystatechange = function() {
                if (xhr.readyState == 4) {
                    //XML-RPC
                    var response = instance.__evalJSON(xhr.responseText, instance.__isResponseSanitized);
                    if (!response.id)
                        response.id = requestInfo.id;
                    instance.__doCallback(response);
                }
            };

            return undefined;
        } else {
            //Send the request
            xhr.send(postData);
            var response;
            response = this.__evalJSON(xhr.responseText, this.__isResponseSanitized);

            //Note that this error must be caught with a try/catch block instead of by passing a onException callback
            if (response.error)
                throw Error('Unable to call "' + methodName + '". Server responsed with error (code ' + response.error.code + '): ' + response.error.message);

            this.__upgradeValuesFromJSON(response);
            return response.result;
        }

    } catch(err) {
        //err.locationCode = PRE-REQUEST Cleint
        var isCaught = false;
        if (exceptionHandler)
            isCaught = exceptionHandler(err); //add error location
        if (completeHandler)
            completeHandler();

        if (!isCaught)
            throw err;
    }
};


//This acts as a lookup table for the response callback to execute the user-defined
//   callbacks and to clean up after a request
JsonRpc.pendingRequests = {};

//Ad hoc cross-site callback functions keyed by request ID; when a cross-site request
//   is made, a function is created
JsonRpc.callbacks = {};

//Called by asychronous calls when their responses have loaded
JsonRpc.ServiceProxy.prototype.__doCallback = function(response) {
    if (typeof response != 'object')
        throw Error('The server did not respond with a response object.');
    if (!response.id)
        throw Error('The server did not respond with the required response id for asynchronous calls.');

    if (!JsonRpc.pendingRequests[response.id])
        throw Error('Fatal error with RPC code: no ID "' + response.id + '" found in pendingRequests.');

    //Remove the SCRIPT element from the DOM tree for cross-site (JSON-in-Script) requests
    if (JsonRpc.pendingRequests[response.id].scriptElement) {
        var script = JsonRpc.pendingRequests[response.id].scriptElement;
        script.parentNode.removeChild(script);
    }
    //Remove the ad hoc cross-site callback function
    if (JsonRpc.callbacks[response.id])
        delete JsonRpc.callbacks['r' + response.id];

    var uncaughtExceptions = [];

    //Handle errors returned by the server
    if (response.error !== undefined) {
        var err = new Error(response.error.message);
        err.code = response.error.code;
        //err.locationCode = SERVER
        if (JsonRpc.pendingRequests[response.id].onException) {
            try {
                if (!JsonRpc.pendingRequests[response.id].onException(err))
                    uncaughtExceptions.push(err);
            }
            catch(err2) { //If the onException handler also fails
                uncaughtExceptions.push(err);
                uncaughtExceptions.push(err2);
            }
        }
        else uncaughtExceptions.push(err);
    }

    //Process the valid result
    else if (response.result !== undefined) {
        //iterate over all values and substitute date strings with Date objects
        //Note that response.result is not passed because the values contained
        //  need to be modified by reference, and the only way to do so is
        //  but accessing an object's properties. Thus an extra level of
        //  abstraction allows for accessing all of the results members by reference.
        this.__upgradeValuesFromJSON(response);

        if (JsonRpc.pendingRequests[response.id].onSuccess) {
            try {
                JsonRpc.pendingRequests[response.id].onSuccess(response.result);
            }
                //If the onSuccess callback itself fails, then call the onException handler as above
            catch(err) {
                //err3.locationCode = CLIENT;
                if (JsonRpc.pendingRequests[response.id].onException) {
                    try {
                        if (!JsonRpc.pendingRequests[response.id].onException(err))
                            uncaughtExceptions.push(err);
                    }
                    catch(err2) { //If the onException handler also fails
                        uncaughtExceptions.push(err);
                        uncaughtExceptions.push(err2);
                    }
                }
                else uncaughtExceptions.push(err);
            }
        }
    }

    //Call the onComplete handler
    try {
        if (JsonRpc.pendingRequests[response.id].onComplete)
            JsonRpc.pendingRequests[response.id].onComplete(response);
    }
    catch(err) { //If the onComplete handler fails
        //err3.locationCode = CLIENT;
        if (JsonRpc.pendingRequests[response.id].onException) {
            try {
                if (!JsonRpc.pendingRequests[response.id].onException(err))
                    uncaughtExceptions.push(err);
            }
            catch(err2) { //If the onException handler also fails
                uncaughtExceptions.push(err);
                uncaughtExceptions.push(err2);
            }
        }
        else uncaughtExceptions.push(err);
    }

    delete JsonRpc.pendingRequests[response.id];

    //Merge any exception raised by onComplete into the previous one(s) and throw it
    if (uncaughtExceptions.length) {
        var code;
        var message = 'There ' + (uncaughtExceptions.length == 1 ?
                'was 1 uncaught exception' :
                'were ' + uncaughtExceptions.length + ' uncaught exceptions') + ': ';
        for (var i = 0; i < uncaughtExceptions.length; i++) {
            if (i)
                message += "; ";
            message += uncaughtExceptions[i].message;
            if (uncaughtExceptions[i].code)
                code = uncaughtExceptions[i].code;
        }
        var err = new Error(message);
        err.code = code;
        throw err;
    }
};


/*******************************************************************************************
 * JSON-RPC Specific Functions
 ******************************************************************************************/
JsonRpc.ServiceProxy.prototype.__toJSON = function(value) {
    switch (typeof value) {
        case 'number':
            return isFinite(value) ? value.toString() : 'null';
        case 'boolean':
            return value.toString();
        case 'string':
            //Taken from Ext JSON.js
            var specialChars = {
                "\b": '\\b',
                "\t": '\\t',
                "\n": '\\n',
                "\f": '\\f',
                "\r": '\\r',
                '"' : '\\"',
                "\\": '\\\\',
                "/" : '\/'
            };
            return '"' + value.replace(/([\x00-\x1f\\"])/g, function(a, b) {
                var c = specialChars[b];
                if (c)
                    return c;
                c = b.charCodeAt();
                //return "\\u00" + Math.floor(c / 16).toString(16) + (c % 16).toString(16);
                return '\\u00' + JsonRpc.zeroPad(c.toString(16));
            }) + '"';
        case 'object':
            if (value === null)
                return 'null';
            else if (value instanceof Array) {
                var json = ['['];  //Ext's JSON.js reminds me that Array.join is faster than += in MSIE
                for (var i = 0; i < value.length; i++) {
                    if (i)
                        json.push(',');
                    json.push(this.__toJSON(value[i]));
                }
                json.push(']');
                return json.join('');
            }
            else if (value instanceof Date) {
                switch (this.__dateEncoding) {
                    case 'classHinting': //{"__jsonclass__":["constructor", [param1,...]], "prop1": ...}
                        return '{"__jsonclass__":["Date",[' + value.valueOf() + ']]}';
                    case '@timestamp@':
                    case '@ticks@':
                        return '"@' + value.valueOf() + '@"';
                    case 'ASP.NET':
                        return '"\\/Date(' + value.valueOf() + ')\\/"';
                    default:
                        return '"' + JsonRpc.dateToISO8601(value) + '"';
                }
            }
            else if (value instanceof Number || value instanceof String || value instanceof Boolean)
                return this.__toJSON(value.valueOf());
            else {
                var useHasOwn = {}.hasOwnProperty ? true : false; //From Ext's JSON.js
                var json = ['{'];
                for (var key in value) {
                    if (!useHasOwn || value.hasOwnProperty(key)) {
                        if (json.length > 1)
                            json.push(',');
                        json.push(this.__toJSON(key) + ':' + this.__toJSON(value[key]));
                    }
                }
                json.push('}');
                return json.join('');
            }
        //case 'undefined':
        //case 'function':
        //case 'unknown':
        //default:
    }
    throw new TypeError('Unable to convert the value of type "' + typeof(value) + '" to JSON.'); //(' + String(value) + ')
};

JsonRpc.isJSON = function(string) { //from Prototype String.isJSON()
    var testStr = string.replace(/\\./g, '@').replace(/"[^"\\\n\r]*"/g, '');
    return (/^[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]*$/).test(testStr);
};

JsonRpc.ServiceProxy.prototype.__evalJSON = function(json, sanitize) { //from Prototype String.evalJSON()
    //Remove security comment delimiters
    json = json.replace(/^\/\*-secure-([\s\S]*)\*\/\s*$/, "$1");
    var err;
    try {
        if (!sanitize || JsonRpc.isJSON(json))
            return eval('(' + json + ')');
    }
    catch(e) {
        err = e;
    }
    throw new SyntaxError('Badly formed JSON string: ' + json + " ... " + (err ? err.message : ''));
};

//This function iterates over the properties of the passed object and converts them
//   into more appropriate data types, i.e. ISO8601 strings are converted to Date objects.
JsonRpc.ServiceProxy.prototype.__upgradeValuesFromJSON = function(obj) {
    var matches, useHasOwn = {}.hasOwnProperty ? true : false;
    for (var key in obj) {
        if (!useHasOwn || obj.hasOwnProperty(key)) {
            //Parse date strings
            if (typeof obj[key] == 'string') {
                //ISO8601
                if (this.__decodeISO8601 && (matches = obj[key].match(/^(?:(\d\d\d\d)-(\d\d)(?:-(\d\d)(?:T(\d\d)(?::(\d\d)(?::(\d\d)(?:\.(\d+))?)?)?)?)?)$/))) {
                    obj[key] = new Date(0);
                    if (matches[1]) obj[key].setUTCFullYear(parseInt(matches[1]));
                    if (matches[2]) obj[key].setUTCMonth(parseInt(matches[2] - 1));
                    if (matches[3]) obj[key].setUTCDate(parseInt(matches[3]));
                    if (matches[4]) obj[key].setUTCHours(parseInt(matches[4]));
                    if (matches[5]) obj[key].setUTCMinutes(parseInt(matches[5]));
                    if (matches[6]) obj[key].setUTCMilliseconds(parseInt(matches[6]));
                }
                //@timestamp@ / @ticks@
                else if (matches = obj[key].match(/^@(\d+)@$/)) {
                    obj[key] = new Date(parseInt(matches[1]))
                }
                //ASP.NET
                else if (matches = obj[key].match(/^\/Date\((\d+)\)\/$/)) {
                    obj[key] = new Date(parseInt(matches[1]))
                }
            }
            else if (obj[key] instanceof Object) {

                //JSON 1.0 Class Hinting: {"__jsonclass__":["constructor", [param1,...]], "prop1": ...}
                if (obj[key].__jsonclass__ instanceof Array) {
                    //console.info('good1');
                    if (obj[key].__jsonclass__[0] == 'Date') {
                        //console.info('good2');
                        if (obj[key].__jsonclass__[1] instanceof Array && obj[key].__jsonclass__[1][0])
                            obj[key] = new Date(obj[key].__jsonclass__[1][0]);
                        else
                            obj[key] = new Date();
                    }
                }
                else this.__upgradeValuesFromJSON(obj[key]);
            }
        }
    }
};


/*******************************************************************************************
 * Other helper functions
 ******************************************************************************************/

//Takes an array or hash and coverts it into a query string, converting dates to ISO8601
//   and throwing an exception if nested hashes or nested arrays appear.
JsonRpc.toQueryString = function(params) {
    if (!(params instanceof Object || params instanceof Array) || params instanceof Date)
        throw Error('You must supply either an array or object type to convert into a query string. You supplied: ' + params.constructor);

    var str = '';
    var useHasOwn = {}.hasOwnProperty ? true : false;

    for (var key in params) {
        if (useHasOwn && params.hasOwnProperty(key)) {
            //Process an array
            if (params[key] instanceof Array) {
                for (var i = 0; i < params[key].length; i++) {
                    if (str)
                        str += '&';
                    str += encodeURIComponent(key) + "=";
                    if (params[key][i] instanceof Date)
                        str += encodeURIComponent(JsonRpc.dateToISO8601(params[key][i]));
                    else if (params[key][i] instanceof Object)
                        throw Error('Unable to pass nested arrays nor objects as parameters while in making a cross-site request. The object in question has this constructor: ' + params[key][i].constructor);
                    else str += encodeURIComponent(String(params[key][i]));
                }
            }
            else {
                if (str)
                    str += '&';
                str += encodeURIComponent(key) + "=";
                if (params[key] instanceof Date)
                    str += encodeURIComponent(JsonRpc.dateToISO8601(params[key]));
                else if (params[key] instanceof Object)
                    throw Error('Unable to pass objects as parameters while in making a cross-site request. The object in question has this constructor: ' + params[key].constructor);
                else str += encodeURIComponent(String(params[key]));
            }
        }
    }
    return str;
};

//Converts an iterateable value into an array; similar to Prototype's $A function
JsonRpc.toArray = function(value) {
    //if(value && value.length){
    if (value instanceof Array)
        return value;
    var array = [];
    for (var i = 0; i < value.length; i++)
        array.push(value[i]);
    return array;
    //}
    //throw Error("Unable to convert to an array the value: " + String(value));
};

//Returns an ISO8601 string *in UTC* for the provided date (Prototype's Date.toJSON() returns localtime)
JsonRpc.dateToISO8601 = function(date) {
    //var jsonDate = date.toJSON();
    //return jsonDate.substring(1, jsonDate.length-1); //strip double quotes

    return date.getUTCFullYear() + '-' +
            JsonRpc.zeroPad(date.getUTCMonth() + 1) + '-' +
            JsonRpc.zeroPad(date.getUTCDate()) + 'T' +
            JsonRpc.zeroPad(date.getUTCHours()) + ':' +
            JsonRpc.zeroPad(date.getUTCMinutes()) + ':' +
            JsonRpc.zeroPad(date.getUTCSeconds()) + '.' +
        //Prototype's Date.toJSON() method does not include milliseconds
            JsonRpc.zeroPad(date.getUTCMilliseconds(), 3);
};

JsonRpc.zeroPad = function(value, width) {
    if (!width)
        width = 2;
    value = (value == undefined ? '' : String(value))
    while (value.length < width)
        value = '0' + value;
    return value;
};
