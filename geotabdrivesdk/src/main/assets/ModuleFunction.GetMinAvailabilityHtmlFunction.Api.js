(async (callerId) => {
  try {
    const userName = '{{userName}}';

    if (userName == null || userName === '') {
      throw new Error('No users');
    }

    const api = window.webViewLayer.getApi(userName);
    const availabilityHtml = await api.mobile.user.getMinAvailabilityHtml();

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
