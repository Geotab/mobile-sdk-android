(async (callerId) => {
  try {
    const userNames = window.webViewLayer.getApiUserNames();
    if (userNames == null || userNames.length === 0) {
      throw new Error('No users');
    }
    const api = window.webViewLayer.getApi(userNames[0]);
    const users = await api.mobile.user.get({{getAllUsers}});
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(users) },
      () => {}
    );
  } catch (err) {
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, error: err.message },
      () => {}
    );
    throw err;
  }
})('{{callerId}}');
