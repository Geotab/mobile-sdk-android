(async function (callerId) {
    try {
        var userName = "{{userName}}";

        if (userName == null || userName == '') {
            throw new Error("No users");
        }

        var api = window.webViewLayer.getApi(userName);
        var hos = await api.mobile.user.getHosRuleSet();

        window.geotabModules.{{moduleName}}.{{functionName}}({callerId: callerId, result: JSON.stringify(hos)}, (error, res) => {});
    } catch(err) {
        window.geotabModules.{{moduleName}}.{{functionName}}({callerId: callerId, error: err.message}, (error, res) => {});
        throw err;
    }
})("{{callerId}}");