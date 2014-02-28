vdf-json-java
=============

Class to convert Valve Data Format / Source Engine KeyValue format files to JSON format.
All values are either strings or nested KeyValues.

Supported are nested KeyValues, parsing out C++ comments, converting a set of KeyValues ```"0" ... "n"``` to a JSONArray.
Currently unsupported are imports and resolving numbers to boolean / integer values, but otherwise the conversion's pretty good.

The parsing is lazy; you can feed it a partial file and you'll still get some JSON data.  Might not want that, but oh well.

[Single source file for it](https://github.com/nosoop/vdf-json-java/blob/master/src/main/java/com/nosoop/json/VDF.java).

Depends on the ```org.json``` library.

Released under the MIT license.

Sample conversion results
-------------------------
```
// Snippet of Original JSON
// Source: http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v0001/?format=json
{"apilist": {"interfaces": [
    {
        "methods": [{
            "name": "GetServerVersion",
            "httpmethod": "GET",
            "parameters": [],
            "version": 1
        }],
        "name": "IGCVersion_205790"
    },
    {
        "methods": [{
            "name": "GetServerVersion",
            "httpmethod": "GET",
            "parameters": [],
            "version": 1
        }],
        "name": "IGCVersion_247040"
    }]
}}

// Snippet of converted VDF data with array conversion enabled.
// Source: http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v0001/?format=vdf
{"apilist": {"interfaces": [
    {
        "methods": [{
            "name": "GetServerVersion",
            "httpmethod": "GET",
            "parameters": [],
            "version": "1"
        }],
        "name": "IGCVersion_205790"
    },
    {
        "methods": [{
            "name": "GetServerVersion",
            "httpmethod": "GET",
            "parameters": [],
            "version": "1"
        }],
        "name": "IGCVersion_247040"
    }]
}}
```
