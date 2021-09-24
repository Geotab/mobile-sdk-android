    window.{{geotabModules}}.{{moduleName}}.{{functionName}} = function (eventName, callback) {
        if (typeof eventName != "string" || eventName == "") {
            throw new Error("eventName should be a string type and non-empty");
        }
        const mod = window.{{geotabModules}}.{{moduleName}};
        if (mod.onListeners == null) {
            mod.onListeners = {};
        }
        if (mod.onListeners[eventName] == null) {
            mod.onListeners[eventName] = [];
        }
        const index = mod.onListeners[eventName].indexOf(callback);
        if (index < 0) {
            // Web Drive has bug that tries to off event callbacks that are same function but not exact the same function fun.bind(this) != fun.bind(this)
            // when we couldnt find such callback, we see such off event as unregister all callbacks
            delete mod.onListeners[eventName];

        } else {
            mod.onListeners[eventName].splice(index, 1);
            if (mod.onListeners[eventName].length == 0) {
                delete mod.onListeners[eventName];
            }
        }

        if (mod.___offCallback == null) {
            mod.___offCallback = function (error, response) {
            };
        }
        {{interfaceName}}.postMessage("{{moduleName}}", "{{functionName}}", JSON.stringify({result: Object.keys(mod.onListeners)}), "window.{{geotabModules}}.{{moduleName}}.___offCallback");

    };
