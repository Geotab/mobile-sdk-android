(async function (callerId) {
    try {
        var userName = "{{userName}}";

        if (userName == null || userName == '') {
            throw new Error("No users");
        }

        var api = window.webViewLayer.getApi(userName);
        var violations = await api.mobile.user.getViolations();

        window.geotabModules.{{moduleName}}.{{functionName}}({callerId: callerId, result: JSON.stringify(violations)}, (error, res) => {});
    } catch(err) {
        window.geotabModules.{{moduleName}}.{{functionName}}({callerId: callerId, error: err.message}, (error, res) => {});
        throw err;
    }
})("{{callerId}}");