(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    const dutyStatusLog = await api.mobile.dutyStatusLog.get();

    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(dutyStatusLog) },
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
