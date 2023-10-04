(async (callerId) => {
  try {
    const getHosTeamClocksFunction = window?.webViewLayer?.getOpenCabService()?.getHosTeamClocks;

    if (!getHosTeamClocksFunction) {
      throw new Error('Loading...');
    }

    const availability = getHosTeamClocksFunction();
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
