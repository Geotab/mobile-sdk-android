(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    const availability = await api.mobile.user.getAvailability();

    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(availability) },
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
