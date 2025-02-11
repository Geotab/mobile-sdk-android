(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    // legacy html set to false to retrieve the new availability html
    const availabilityHtml = await api.mobile.user.getMinAvailabilityHtml(false);

    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: availabilityHtml },
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
