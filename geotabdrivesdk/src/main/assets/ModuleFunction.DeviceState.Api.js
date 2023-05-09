(async (callerId) => {
  try {
    const userNames = window.webViewLayer.getApiUserNames();
    if (userNames == null || userNames.length === 0) {
      throw new Error('No users');
    }
    // eslint-disable-next-line global-require, import/no-unresolved
    const device = require('utils/addinHelper').getExtendedAppState(
      window.driveApp.manager.store.getState().mobile,
    );
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(device) },
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
