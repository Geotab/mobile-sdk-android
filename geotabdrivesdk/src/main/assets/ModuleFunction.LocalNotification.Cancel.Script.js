window.{{geotabModules}}.{{moduleName}}.{{functionName}} = function (id, callback) {

    const mod = window.{{geotabModules}}.{{moduleName}};

    var nativeCallback = "{{callbackPrefix}}" + Math.random().toString(36).substring(2);

    window.{{geotabNativeCallbacks}}[nativeCallback] = async function (error, notification) {
        if (error != null) {
            try {
                await callback(error, undefined);
            } catch (err) {
                console.log(">>>>> User provided callback throws uncaught exception: ", err.message);
            }
            return;
        }
        const actions = notification.actions;
        if (actions != null && actions.length > 0) {
            actions.forEach(action => {
                mod.{{off}}(action.id, (err, result) => {});
            });
        }
        try {
            await callback(error, notification);
        } catch (err) {
            console.log(">>>>> User provided callback throws uncaught exception: ", err.message);
        }
        delete window.{{geotabNativeCallbacks}}[nativeCallback];
    };

    {{interfaceName}}.postMessage("{{moduleName}}", "{{functionName}}", JSON.stringify({result: id}), "{{geotabNativeCallbacks}}." + nativeCallback);
};
