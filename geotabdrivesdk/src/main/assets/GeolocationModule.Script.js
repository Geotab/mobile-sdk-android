(() => {
  const mod = window.geotabModules.geolocation;
  let nextId = 1000;

  /* type: id: {
      success: Function,
      error: Function || undefiend,
      options: PositionOptions || undefined,
      timerId: number || undefined;],
      timeoutFun: Function;
    } */
  mod.positionWatchers = {};

  /* type: { success: Function,
      error: Function || undefined,
      options: PositionOptions || undefined,
      timerId: number || undefined;
    } */
  mod.positionGetters = [];

  function __GeolocationPositionError(code, message) {
    this.name = 'GeolocationPositionError';
    this.code = code;
    this.message = message || '';
  }
  __GeolocationPositionError.prototype = Error.prototype;

  mod.getCurrentPosition = (success, error, options) => {
    if (typeof success !== 'function') {
      throw new Error('First argument must be a function');
    }
    mod.___startLocationService(options, async (err) => {
      let errorFunc = () => {};

      if (typeof error === 'function') {
        errorFunc = error;
      }
      if (err != null) {
        if (err.message && err.message.includes('PERMISSION_DENIED')) {
          try {
            await errorFunc(new __GeolocationPositionError(1, err.message));
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        } else if (
          err.message
          && err.message.includes('POSITION_UNAVAILABLE')
        ) {
          try {
            await errorFunc(new __GeolocationPositionError(2, err.message));
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        } else {
          try {
            await errorFunc(err);
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        }
        return;
      }
      const req = {
        success,
        error,
        options,
      };
      if (options != null && options.timeout != null && options.timeout > 0) {
        req.timerId = setTimeout(async () => {
          clearTimeout(req.timerId);
          req.timerId = undefined;
          const idx = mod.positionGetters.findIndex((itm) => itm === req);
          if (idx < 0) return;
          mod.positionGetters.splice(idx, 1);
          mod.___stopLocationServiceIfNeeded();
          if (typeof error === 'function') {
            try {
              await error(new __GeolocationPositionError(3, 'TIMEOUT'));
            } catch (caughtErr) {
              console.log(
                '>>>>> User provided function throws uncaught exception: ',
                caughtErr.message,
              );
            }
          }
        }, options.timeout);
      }

      mod.positionGetters.push(req);
    });
  };

  mod.watchPosition = (success, error, options) => {
    if (typeof success !== 'function') {
      throw new Error('First argument must be a function');
    }
    const id = nextId;
    nextId += 1;
    const req = {
      success,
      error,
      options,
      lastCode: null,
    };
    req.timeoutFun = async () => {
      req.timerId = setTimeout(req.timeoutFun, options.timeout);
      if (typeof error === 'function' && req.lastCode !== 3) {
        req.lastCode = 3;
        try {
          await error(new __GeolocationPositionError(3, 'TIMEOUT'));
        } catch (caughtErr) {
          console.log(
            '>>>>> User provided function throws uncaught exception: ',
            caughtErr.message,
          );
        }
      }
    };
    if (options != null && options.timeout != null && options.timeout > 0) {
      req.timerId = setTimeout(req.timeoutFun, options.timeout);
    }
    mod.positionWatchers[id] = req;
    mod.___startLocationService(options, async (err) => {
      let errorFunc = () => {};

      if (typeof error === 'function') {
        errorFunc = error;
      }
      if (err != null) {
        if (err.message && err.message.includes('PERMISSION_DENIED')) {
          if (req.lastCode !== 1) {
            try {
              await errorFunc(new __GeolocationPositionError(1, err.message));
            } catch (caughtErr) {
              console.log(
                '>>>>> User provided function throws uncaught exception: ',
                caughtErr.message,
              );
            }
          }
          req.lastCode = 1;
        } else if (
          err.message
          && err.message.includes('POSITION_UNAVAILABLE')
        ) {
          if (req.lastCode !== 2) {
            try {
              await errorFunc(new __GeolocationPositionError(2, err.message));
            } catch (caughtErr) {
              console.log(
                '>>>>> User provided function throws uncaught exception: ',
                caughtErr.message,
              );
            }
          }
          req.lastCode = 2;
        } else {
          if (req.lastCode !== -1) {
            try {
              await errorFunc(err);
            } catch (caughtErr) {
              console.log(
                '>>>>> User provided function throws uncaught exception: ',
                caughtErr.message,
              );
            }
          }
          req.lastCode = -1;
        }
      }
    });
    return id;
  };

  mod.clearWatch = (id) => {
    const req = mod.positionWatchers[id];
    if (req != null) {
      if (req.timerId != null) {
        clearTimeout(req.timerId);
      }
      delete mod.positionWatchers[id];
    }
    mod.___stopLocationServiceIfNeeded();
  };

  mod.___stopLocationServiceIfNeeded = () => {
    if (mod.positionGetters.length > 0) return;
    if (Object.keys(mod.positionWatchers).length > 0) return;
    mod.___stopLocationService(undefined, () => {});
  };

  mod.___native_getCurrentPosition = navigator.geolocation.getCurrentPosition;
  mod.___native_watchPosition = navigator.geolocation.watchPosition;
  mod.___native_clearWatch = navigator.geolocation.clearWatch;

  navigator.geolocation.getCurrentPosition = mod.getCurrentPosition;
  navigator.geolocation.watchPosition = mod.watchPosition;
  navigator.geolocation.clearWatch = mod.clearWatch;

  mod.___emitResultErrorForGetters = async (error) => {
    while (mod.positionGetters.length > 0) {
      const g = mod.positionGetters.splice(0, 1)[0];
      mod.___stopLocationServiceIfNeeded();
      if (g.timerId != null) {
        clearTimeout(g.timerId);
        g.timerId = undefined;
      }
      let errorFunc = () => {};
      if (typeof g.error === 'function') {
        errorFunc = g.error;
      }
      if (error && error.includes('PERMISSION_DENIED')) {
        try {
          errorFunc(new __GeolocationPositionError(1, error));
        } catch (caughtErr) {
          console.log(
            '>>>>> User provided function throws uncaught exception: ',
            caughtErr.message,
          );
        }
      } else if (error && error.includes('POSITION_UNAVAILABLE')) {
        try {
          errorFunc(new __GeolocationPositionError(2, error));
        } catch (caughtErr) {
          console.log(
            '>>>>> User provided function throws uncaught exception: ',
            caughtErr.message,
          );
        }
      } else if (error && error.includes('TIMEOUT')) {
        try {
          errorFunc(new __GeolocationPositionError(3, error));
        } catch (caughtErr) {
          console.log(
            '>>>>> User provided function throws uncaught exception: ',
            caughtErr.message,
          );
        }
      } else {
        try {
          errorFunc(error);
        } catch (caughtErr) {
          console.log(
            '>>>>> User provided function throws uncaught exception: ',
            caughtErr.message,
          );
        }
      }
    }
  };

  mod.___emitResultErrorForWatchers = async (error) => {
    Object.keys(mod.positionWatchers).forEach(async (id) => {
      const req = mod.positionWatchers[id];
      if (req == null) return;
      if (req.timerId != null) {
        clearTimeout(req.timerId);
        req.timerId = setTimeout(req.timeoutFun, req.options.timeout);
      }
      let errorFunc = () => {};
      if (typeof req.error === 'function') {
        errorFunc = req.error;
      }
      if (error && error.includes('PERMISSION_DENIED')) {
        if (req.lastCode !== 1) {
          try {
            await errorFunc(new __GeolocationPositionError(1, error));
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        }
        req.lastCode = 1;
      } else if (error && error.includes('POSITION_UNAVAILABLE')) {
        if (req.lastCode !== 2) {
          try {
            await errorFunc(new __GeolocationPositionError(2, error));
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        }
        req.lastCode = 2;
      } else if (error && error.includes('TIMEOUT')) {
        if (req.lastCode !== 3) {
          try {
            await errorFunc(new __GeolocationPositionError(3, error));
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        }
        req.lastCode = 3;
      } else {
        if (req.lastCode !== -1) {
          try {
            await errorFunc(error);
          } catch (caughtErr) {
            console.log(
              '>>>>> User provided function throws uncaught exception: ',
              caughtErr.message,
            );
          }
        }
        req.lastCode = -1;
      }
    });
  };

  mod.___emitResultPositionForGetters = async (position) => {
    while (mod.positionGetters.length > 0) {
      const g = mod.positionGetters.splice(0, 1)[0];
      mod.___stopLocationServiceIfNeeded();
      if (g.timerId != null) {
        clearTimeout(g.timerId);
        g.timerId = undefined;
      }
      try {
        g.success(position);
      } catch (caughtErr) {
        console.log(
          '>>>>> User provided function throws uncaught exception: ',
          caughtErr.message,
        );
      }
    }
  };

  mod.___emitResultPositionForWatchers = async (position) => {
    Object.keys(mod.positionWatchers).forEach(async (id) => {
      const req = mod.positionWatchers[id];
      if (req == null) return;
      if (req.timerId != null) {
        clearTimeout(req.timerId);
        req.timerId = setTimeout(req.timeoutFun, req.options.timeout);
      }
      try {
        await req.success(position);
      } catch (caughtErr) {
        console.log(
          '>>>>> User provided function throws uncaught exception: ',
          caughtErr.message,
        );
      }
      req.lastCode = null;
    });
  };

  window.addEventListener('geolocation.result', (evt) => {
    try {
      const result = evt != null ? evt.detail : undefined;
      const position = result != null ? result.position : undefined;
      const error = result != null ? result.error : undefined;
      if (error != null) {
        mod.___emitResultErrorForGetters(error);
        mod.___emitResultErrorForWatchers(error);
        return;
      }
      if (position == null) {
        mod.___emitResultErrorForGetters('POSITION_UNAVAILABLE');
        mod.___emitResultErrorForWatchers('POSITION_UNAVAILABLE');
        return;
      }

      mod.___emitResultPositionForGetters(position);
      mod.___emitResultPositionForWatchers(position);
    } catch (caughtErr) {
      mod.___emitResultErrorForGetters(caughtErr.message);
      mod.___emitResultErrorForWatchers(caughtErr.message);
    }
  });
})();
