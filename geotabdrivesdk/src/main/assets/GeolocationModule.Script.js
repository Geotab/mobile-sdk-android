//
//  Module.Geolocation.Script.js
//  GeotabDriveSDK
//
//  Created by Yunfeng Liu on 2020-08-20.
//

(function () {
    var mod = window.geotabModules.geolocation;
    var nextId = 1000;
    mod.positionWatchers = {}; // type: id: { success: Function, error: Function || undefiend, options: PositionOptions || undefined, timerId: number || undefined; timeoutFun: Function;  }
    mod.positionGetters = []; // type: { success: Function, error: Function || undefined, options: PositionOptions || undefined; timerId: number || undefined; }

    function __GeolocationPositionError(code, message) {
        this.name = "GeolocationPositionError";
        this.code = code;
        this.message = message || "";
    }
    __GeolocationPositionError.prototype = Error.prototype;

    mod.getCurrentPosition = function (success, error, options) {
        if (typeof success != "function") {
            throw new Error("First argument must be a function");
        }
        mod.___startLocationService(options, async function (err) {
            var errorFunc = function () { };

            if (typeof error == "function") {
                errorFunc = error;
            }
            if (err != null) {
                if (err.message && err.message.includes("PERMISSION_DENIED")) {
                    try {
                        await errorFunc(new __GeolocationPositionError(1, err.message));
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                } else if (err.message && err.message.includes("POSITION_UNAVAILABLE")) {
                    try {
                        await errorFunc(new __GeolocationPositionError(2, err.message));
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                } else {
                    try {
                        await errorFunc(err);
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                }
                return;
            }
            var req = {
                success: success,
                error: error,
                options: options
            };
            if (options != null && options.timeout != null && options.timeout > 0) {
                req.timerId = setTimeout(async function () {
                    clearTimeout(req.timerId);
                    req.timerId = undefined;
                    var idx = mod.positionGetters.findIndex(itm => itm === req);
                    if (idx < 0) return;
                    mod.positionGetters.splice(idx, 1);
                    mod.___stopLocationServiceIfNeeded();
                    if (typeof error == "function") {
                        try {
                            await error(new __GeolocationPositionError(3, "TIMEOUT"));
                        } catch(err) {
                            console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                        }
                    }
                }, options.timeout);
            }

            mod.positionGetters.push(req);
        });

    };

    mod.watchPosition = function (success, error, options) {
        if (typeof success != "function") {
            throw new Error("First argument must be a function");
        }
        var id = nextId++;
        var req = {
            success: success,
            error: error,
            options: options,
            lastCode: null,
        };
        req.timeoutFun = async function () {
            req.timerId = setTimeout(req.timeoutFun, options.timeout);
            if (typeof error == "function" && req.lastCode != 3) {
                req.lastCode = 3;
                try {
                    await error(new __GeolocationPositionError(3, "TIMEOUT"));
                } catch(err) {
                    console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                }
            }
        }
        if (options != null && options.timeout != null && options.timeout > 0) {
            req.timerId = setTimeout(req.timeoutFun, options.timeout);
        }
        mod.positionWatchers[id] = req;
        mod.___startLocationService(options, async function (err) {
            var errorFunc = function () { };

            if (typeof error == "function") {
                errorFunc = error;
            }
            if (err != null) {
                if (err.message && err.message.includes("PERMISSION_DENIED")) {
                    if (req.lastCode != 1) {
                        try {
                            await errorFunc(new __GeolocationPositionError(1, err.message));
                        } catch(err) {
                            console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                        }
                    }
                    req.lastCode = 1;
                } else if (err.message && err.message.includes("POSITION_UNAVAILABLE")) {
                    if (req.lastCode != 2) {
                        try {
                            await errorFunc(new __GeolocationPositionError(2, err.message));
                        } catch(err) {
                            console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                        }
                    }
                    req.lastCode = 2;
                } else {
                    if (req.lastCode != -1) {
                        try {
                            await errorFunc(err);
                        } catch(err) {
                            console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                        }
                    }
                    req.lastCode = -1;
                }
                return;
            }
        });
        return id;
    };

    mod.clearWatch = function (id) {
        var req = mod.positionWatchers[id];
        if (req != null) {
            if (req.timerId != null) {
                clearTimeout(req.timerId);
            }
            delete mod.positionWatchers[id];
        }
        mod.___stopLocationServiceIfNeeded();
    };

    mod.___stopLocationServiceIfNeeded = function () {
        if (mod.positionGetters.length > 0) return;
        if (Object.keys(mod.positionWatchers).length > 0) return;
        mod.___stopLocationService(undefined, function (err) { });
    };

    mod.___native_getCurrentPosition = navigator.geolocation.getCurrentPosition;
    mod.___native_watchPosition = navigator.geolocation.watchPosition;
    mod.___native_clearWatch = navigator.geolocation.clearWatch;

    navigator.geolocation.getCurrentPosition = mod.getCurrentPosition;
    navigator.geolocation.watchPosition = mod.watchPosition;
    navigator.geolocation.clearWatch = mod.clearWatch;


    mod.___emitResultErrorForGetters = async function (error) {
        while (mod.positionGetters.length > 0) {
            var g = mod.positionGetters.splice(0, 1)[0];
            mod.___stopLocationServiceIfNeeded();
            if (g.timerId != null) {
                clearTimeout(g.timerId);
                g.timerId = undefined;
            }
            var errorFunc = function () { };
            if (typeof g.error == "function") {
                errorFunc = g.error;
            }
            if (error && error.includes("PERMISSION_DENIED")) {
                try {
                    await errorFunc(new __GeolocationPositionError(1, error));
                } catch(err) {
                    console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                }
            } else if (error && error.includes("POSITION_UNAVAILABLE")) {
                try {
                    await errorFunc(new __GeolocationPositionError(2, error));
                } catch(err) {
                    console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                }
            } else if (error && error.includes("TIMEOUT")) {
                try {
                    await errorFunc(new __GeolocationPositionError(3, error));
                } catch(err) {
                    console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                }
            } else {
                try {
                    await errorFunc(error);
                } catch(err) {
                    console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                }
            }
        }
    };

    mod.___emitResultErrorForWatchers = async function (error) {
        var ids = Object.keys(mod.positionWatchers);
        for (var id of ids) {
            var req = mod.positionWatchers[id];
            if (req == null) continue;
            if (req.timerId != null) {
                clearTimeout(req.timerId);
                req.timerId = setTimeout(req.timeoutFun, req.options.timeout);
            }
            var errorFunc = function () { };
            if (typeof req.error == "function") {
                errorFunc = req.error;
            }
            if (error && error.includes("PERMISSION_DENIED")) {
                if (req.lastCode != 1) {
                    try {
                        await errorFunc(new __GeolocationPositionError(1, error));
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                }
                req.lastCode = 1;
            } else if (error && error.includes("POSITION_UNAVAILABLE")) {
                if (req.lastCode != 2) {
                    try {
                        await errorFunc(new __GeolocationPositionError(2, error));
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                }
                req.lastCode = 2;
            } else if (error && error.includes("TIMEOUT")) {
                if (req.lastCode != 3) {
                    try {
                        await errorFunc(new __GeolocationPositionError(3, error));
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                }
                req.lastCode = 3;
            } else {
                if (req.lastCode != -1) {
                    try {
                        await errorFunc(error);
                    } catch(err) {
                        console.log(">>>>> User provided function throws uncaught exception: ", err.message)
                    }
                }
                req.lastCode = -1;
            }
        }
    };

    mod.___emitResultPositionForGetters = async function (position) {
        while (mod.positionGetters.length > 0) {
            var g = (mod.positionGetters.splice(0, 1))[0];
            mod.___stopLocationServiceIfNeeded();
            if (g.timerId != null) {
                clearTimeout(g.timerId);
                g.timerId = undefined;
            }
            try {
                await g.success(position);
            } catch(err) {
                console.log(">>>>> User provided function throws uncaught exception: ", err.message)
            }
        }
    };

    mod.___emitResultPositionForWatchers = async function (position) {
        var ids = Object.keys(mod.positionWatchers);
        for (var id of ids) {
            var req = mod.positionWatchers[id];
            if (req == null) continue;
            if (req.timerId != null) {
                clearTimeout(req.timerId);
                req.timerId = setTimeout(req.timeoutFun, req.options.timeout);
            }
            try {
                await req.success(position);
            } catch(err) {
                console.log(">>>>> User provided function throws uncaught exception: ", err.message)
            }
            req.lastCode = null;
        }
    };

    window.addEventListener("geolocation.result", function (evt) {
        try {
            var result = evt != null ? evt.detail : undefined;
            var position = result != null ? result.position : undefined;
            var error = result != null ? result.error : undefined;
            if (error != null) {
                mod.___emitResultErrorForGetters(error);
                mod.___emitResultErrorForWatchers(error);
                return;
            }
            if (position == null) {
                mod.___emitResultErrorForGetters("POSITION_UNAVAILABLE");
                mod.___emitResultErrorForWatchers("POSITION_UNAVAILABLE");
                return;
            }

            mod.___emitResultPositionForGetters(position);
            mod.___emitResultPositionForWatchers(position);
        } catch (err) {
            mod.___emitResultErrorForGetters(err.message);
            mod.___emitResultErrorForWatchers(err.message);
        }
    });
})();
