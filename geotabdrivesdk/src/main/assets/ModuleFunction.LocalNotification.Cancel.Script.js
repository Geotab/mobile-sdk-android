window.{{geotabModules}}.{{moduleName}}.{{functionName}} = (id, callback) => {
  const mod = window.{{geotabModules}}.{{moduleName}};

  const nativeCallback = `{{callbackPrefix}}${Math.random().toString(36).substring(2)}`;

  window.{{geotabNativeCallbacks}}[nativeCallback] = async (error, notification) => {
    if (error != null) {
      try {
        await callback(error, undefined);
      } catch (err) {
        console.log(
          '>>>>> User provided callback throws uncaught exception: ',
          err.message,
        );
      }
      return;
    }
    const { actions } = notification;
    if (actions != null && actions.length > 0) {
      actions.forEach((action) => {
        mod.{{off}}(action.id, () => {});
      });
    }
    try {
      await callback(error, notification);
    } catch (err) {
      console.log(
        '>>>>> User provided callback throws uncaught exception: ',
        err.message,
      );
    }
    delete window.{{geotabNativeCallbacks}}[nativeCallback];
  };
  // eslint-disable-next-line no-undef
  {{interfaceName}}.postMessage(
    '{{moduleName}}',
    '{{functionName}}',
    JSON.stringify({ result: id }),
    `{{geotabNativeCallbacks}}.${nativeCallback}`,
  );
};
