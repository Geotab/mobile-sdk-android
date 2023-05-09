(async (callerId) => {
  try {
    const userNames = window.webViewLayer.getApiUserNames();
    if (userNames == null || userNames.length === 0) {
      throw new Error('No users');
    }
    const api = window.webViewLayer.getApi(userNames[0]);
    await api.mobile.user.setDriverSeat('{{driverId}}');
    const user = await api.mobile.user.get(false);
    if (user[0].id !== '{{driverId}}') {
      throw new Error('Driver not set');
    }
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(user) },
      () => {},
    );
  } catch (err) {
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, error: err.message },
      () => {},
    );
    throw err;
  }
})('{{callerId}}');
