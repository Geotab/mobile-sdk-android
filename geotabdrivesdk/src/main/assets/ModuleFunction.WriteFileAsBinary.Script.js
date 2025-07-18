window.{{geotabModules}}.{{moduleName}}.{{functionName}} = (params, callback) => {
  if (params.data instanceof ArrayBuffer !== true) {
    throw new Error('data must be ArrayBuffer');
  }
  const options = {
    path: params.path,
    data: [].slice.call(new Uint8Array(params.data)),
    offset: params.offset
  };
  const nativeCallback = `{{callbackPrefix}}${Math.random().toString(36).substring(2)}`;
  window.{{geotabNativeCallbacks}}[nativeCallback] = async (error, response) => {
    try {
      await callback(error, response);
    } catch (err) {
      console.log(
        '>>>>> User provided callback throws uncaught exception: ',
        err.message
      );
    }
    delete window.{{geotabNativeCallbacks}}[nativeCallback];
  };
  // eslint-disable-next-line no-undef
  {{interfaceName}}.postMessage(
    '{{moduleName}}',
    '{{functionName}}',
    JSON.stringify({ result: options }),
    `{{geotabNativeCallbacks}}.${nativeCallback}`
  );
};
