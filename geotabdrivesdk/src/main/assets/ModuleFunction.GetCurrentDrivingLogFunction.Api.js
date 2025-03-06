(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    const currentDrivingLogs = await api.mobile.dutyStatusLog.getCurrentDrivingLog();

    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(currentDrivingLogs) },
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
