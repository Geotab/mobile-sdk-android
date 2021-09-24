window.{{geotabModules}}.{{moduleName}}.{{functionName}} = function(params, callback) {
    if (params.data instanceof ArrayBuffer !== true) {
        throw new Error("data must be ArrayBuffer");
    }
    var options = {
        path: params.path,
        data: [].slice.call(new Uint8Array(params.data)),
        offset: params.offset
    };
    var nativeCallback = "{{callbackPrefix}}" + Math.random().toString(36).substring(2);
    window.{{geotabNativeCallbacks}}[nativeCallback] = async function (error, response) {
        try {
            await callback(error, response);
        } catch (err) {
            console.log(">>>>> User provided callback throws uncaught exception: ", err.message);
        }
        delete window.{{geotabNativeCallbacks}}[nativeCallback];
    };
    {{interfaceName}}.postMessage("{{moduleName}}", "{{functionName}}", JSON.stringify({result: options}), "{{geotabNativeCallbacks}}." + nativeCallback);
};

