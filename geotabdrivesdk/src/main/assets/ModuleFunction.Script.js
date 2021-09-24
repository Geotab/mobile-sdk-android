window.{{geotabModules}}.{{moduleName}}.{{functionName}} = function(params, callback) {
    var nativeCallback = "{{callbackPrefix}}" + Math.random().toString(36).substring(2);
    window.{{geotabNativeCallbacks}}[nativeCallback] = async function (error, response) {
        try {
            await callback(error, response);
        } catch (err) {
            console.log(">>>>> User provided callback throws uncaught exception: ", err.message);
        }
        delete window.{{geotabNativeCallbacks}}[nativeCallback];
    };
    {{interfaceName}}.postMessage("{{moduleName}}", "{{functionName}}", JSON.stringify({result: params}), "{{geotabNativeCallbacks}}." + nativeCallback);
};
