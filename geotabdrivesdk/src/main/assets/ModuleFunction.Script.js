window.{{geotabModules}}.{{moduleName}}.{{functionName}} = (
  params,
  callback
) => {
  const nativeCallback = `{{callbackPrefix}}${Math.random()
    .toString(36)
    .substring(2)}`;
  window.{{geotabNativeCallbacks}}[nativeCallback] = async (
    error,
    response
  ) => {
    try {
      await callback(error, response);
    } catch (err) {
      console.log(
        '>>>>> User provided callback throws uncaught exception: ',
        err.message
      );
    } finally {
      delete window.{{geotabNativeCallbacks}}[nativeCallback];
    }
  };

  try {
    // eslint-disable-next-line no-undef
    {{interfaceName}}.postMessage(
      '{{moduleName}}',
      '{{functionName}}',
      JSON.stringify({ result: params }),
      `{{geotabNativeCallbacks}}.${nativeCallback}`
    );
  } catch (err) {
    console.log(
      '>>>>> Unexpected exception in JavascriptInterface callback: ',
      err.message
    );
    delete window.{{geotabNativeCallbacks}}[nativeCallback];
  }
};
