(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    const violations = await api.mobile.user.getViolations();

    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(violations) },
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
