(async (callerId) => {
  try {
    const openCabService = window.webViewLayer.getOpenCabService();
    const availability = openCabService.getHosTeamClocks();
    window.geotabModules.{{moduleName}}.{{functionName}}(
      { callerId, result: JSON.stringify(availability) },
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
