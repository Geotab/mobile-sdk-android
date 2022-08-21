import { INativeActionEvent, INativeNotify } from "./app/utils/nativeContainer";
import { DriverActionTypes, ILoginRequiredState } from "./app/store/mobile/types";

export interface FileInfo {
    name: string;
    isDir: boolean;
    modifiedDate: string;
    size?: number; // File size, if file is a directory, size is undefined
}

declare namespace geotabModules {
    /********
     * device module: provides constant information about the mobile device that is running on.
     */
    namespace device {
        // iOS or Android
        const platform: string;
        // manufacturer's name
        const manufacturer: string;
        // app Id, bundle ID, unique identifier of an app.
        const appId: string;
        // app name
        const appName: string;
        // app version
        const version: string;
        // Mobile SDK version
        const sdkVersion: string;
        // model name, for example iPhone7, Pixel etc.
        const model: string;
        // unique device identifier, transient. It could be changed anytime by user or policy.
        const uuid: string;
    }

    // local notification module
    namespace localNotification {
        /******
         * Retrieve all delivered and pending notifications.
         * @param callback:
         *  when retrieving success, a list of INativeNotify objects will be returned, if failed, err != null.
         *  result contains "Delivered" and "Pending" notifications.
         */
        function getAll(argument: undefined, callback: (err?: Error, result?: INativeNotify[]) => void);
        /******
         * Check if permission is granted for sending local notification
         * @param callback:
         *  result === true if permission is granted, otherwise not.
         * */
        function hasPermission(argument: undefined, callback: (err?: Error, result?: boolean) => void);

        /*******
         * Request local notification permission.
         * @param callback:
         *  If permission is granted, callback will be called with result equal true.
         *  If permission is denied, callback will be called with result equal to false.
         *  Any other errors, callback will be called with an err != null. err is of type Error.
         */
        function requestPermission(argument: undefined, callback: (err?: Error, result?: boolean) => void);

        /*******
         * Schedule a new local notification to be delivered to the user.
         * WARNING: Any change to INativeNotify must let Mobile SDK team know so that the SDK updates the support at the same time.
         * @param argument:
         *  The new notification to be delivered
         * @param callback:
         *  The result of delivery is returned by calling callback with result being true or false.
         *  If there's any error happened, for example, an incomplete argument or invalid format will result callback be called with err.
         */
        function schedule(argument: INativeNotify, callback: (err?: Error, result?: boolean) => void);

        /*******
         * Cancel a notification by id. The notification must not been closed or cancelled. i.e. it must be either a delivered or pending notification.
         * @param id: number
         *  ID of the notification
         * @param callback:
         *  If the notification not found, callback will be called with err. If cancelling operation is successfull, callback will be called with
         *  result being the original INativeNotify.
         */
        function cancel(id: number, callback: (err?: Error, result?: INativeNotify) => void);

        /*******
         * Listen for specific event, usually the actions like "click" events or a custom name associated with a INativeNotifyAction.
         * @param eventName:
         *  event name, "click", or a custom name associated with a INativeNotifyAction
         * @param callback:
         *  Event handler.
         */
        function on(eventName: string, callback: (notification: INativeNotify, event: INativeActionEvent) => void);

        /*******
         * remove a previously added notification event handler.
         * @param eventName
         *  The event name to remove from
         * @param eventListener
         *  The original event handler function. Note, this function must be the same function when compare by reference.
         *  If no such eventListener is found, the off function will remove all event handlers associated with the event name.
         */
        function off(eventName: string, eventListener: Function);
    }

    namespace battery {
        /*******
         * Whenever battery status is changed or charging status is changed. "batterystatus" will be sent to window object.
         * @param detail: { isPlugged: boolean; level: number; }
         *  isPlugged: boolean; if charging cable is plugged
         *  level: 0-100, battery level.
         */
        // event: "batterystatus";
    }

    namespace connectivity {
        /*******
         * Readonly connectivity state.
         */
        let state: {
            // whether online or offline
            online: boolean,
            // type of connection. values from: "UNKNOWN", "NONE", "ETHERNET", "WIFI", "CELL_2G", "CELL_3G", "CELL_4G", "CELL_5G", "CELL"
            type: string
        };

        /*******
         * Start watching connectivity changes
         * @param argument: undefined
         * @param callback:
         *  If starting the connectivity watch is successful or not, true or false will be returned on `result`.
         */
        function start(argument: undefined, callback: (err?: Error, result?: boolean) => void);

        /*******
         * Stop watching connectivity changes
         * @param argument: undefined
         * @param callback:
         *  If stopping the connectivity watch is successful or not, true or false will be returned on `result`.
         */
        function stop(argument: undefined, callback: (err?: Error, result?: boolean) => void);
    }

    namespace user {
        /*******
         * Notify Mobile SDK the driver action necessary event
         * @param argument: { isDriverActionNecessary: boolean, driverActionType: DriverActionTypes }
         * @param callback:
         *  callback will be called with `err` if the argument format is incorrect. Otherwise, err is null and result is undefined.
         */
        function driverActionNecessary(argument: { isDriverActionNecessary: boolean, driverActionType: DriverActionTypes }, callback: (err?: Error, result?: undefined) => void);

        /*******
         * Notify Mobile SDK the page navigation event
         * @param argument: string
         *  the current path of view.
         * @param callback:
         *  callback will be called with `err` if the argument format is incorrect. Otherwise, err is null and result is undefined.
         */
        function pageNavigation(argument: string, callback: (err?: Error, result?: undefined) => void);

        /*******
         * Notify Mobile SDK the login Required event
         * @param argument: ILoginRequiredState
         *  the current login required state.
         * @param callback:
         *  callback will be called with `err` if the argument format is incorrect. Otherwise, err is null and result is undefined.
         */
        function loginRequired(argument: ILoginRequiredState, callback: (err?: Error, result?: undefined) => void);
    }

    namespace speech {

        /*******
         * Invoke the text to speech to speak the `text` at the given `rate` and `language`
         * @param argument:
         *  text: The text to be spoken
         *  rate: 0.1-10; `1` is normal speed. Default value is 1.0.
         *  lang: Specifies the BCP-47 language tag that represents the voice. Default value is en-US.
         * @param callback:
         *  callback will be called with `err` if the argument format is incorrect. Otherwise, err is null and result is undefined.
         */
        function nativeSpeak(argument: { text: string, rate: number, lang: string }, callback: (err?: Error, result?: undefined) => void);
    }

    namespace screen {
        /*******
         * Keep the screen awake.
         * @param argument: boolean
         *  set to true if want to keep the screen awake, otherwise false.
         * @param callback
         *  callback will be called with `err` if the argument format is incorrect.
         *  Otherwise, err is null and result is true of false. True means keep awake, false means do not keep awake
         */
        function keepAwake(argument: boolean, callback: (err?: Error, result?: boolean) => void);
    }

    namespace app {
        /*******
         * Readonly property, indicating if the app is in the background. true if app is in the background, false otherwise.
         */
        let background: boolean;
        /******
         * keep alive status object. If Keep alive failed, keepAlive.error will indicate the reason as string.
         */
        let keepAlive: { error?: string };

        /*******
         * window event. Fires whenever app goes into background or switch back to foreground.
         * @param detail: boolean
         *  detail equal to true if app is switch back to background, false if app switched back to foreground
         */
        // event: "app.background"

        /*******
         * Keep alive event, fires when Mobile SDK fails to enable keep alive in the background
         * @param detail: { error?: string }
         *  error: If Keep alive failed, keepAlive.error will indicate the reason as string.
         */
        // event: "app.background.keepalive"

        /*******
         * Notify Mobile SDK the last server address
         * @param server: string
         *  Server address from drive
         * @param callback:
         *  callback will be called with `err` if the server address it not a valid format. Otherwise, err is null and result is undefined.
         */
        function updateLastServer(server: string, callback: (err?: Error, result?: undefined) => void);

    }

    namespace browser {
        /*******
         * Open an URL either in the app or externally
         * @param argument: { url: string; target?: string; features?: string; }
         *  url: must be a valid URL string.
         *  target: Optional. could be one of the following values:
         *      _blank: Open the URL in external browser app.
         *      _self, _parent, _top, iab: Open the URL inside the current app.
         *      <other values>: will be treated as openning the URL using external browser.
         *  features: optional, not implemented at this time. https://developer.mozilla.org/en-US/docs/Web/API/Window/open
         * @param callback
         *  callback will be called with `err` if the argument format is incorrect.
         *  If success, result will be the URL being opened.
         */
        function openBrowserWindow(argument: { url: string; target?: string; features?: string; }, callback: (err?: Error, result?: string) => void);
    }

    namespace camera {
        /*******
         * capture an image from camera.
         * @param argument: undefined | null  | { fileName?: string; size?: { width: number; height: number; } }
         *  Either undefined | null or passing with an option of type { fileName?: string; size?: { width: number; height: number; } }.
         *      where `fileName` is optional. fileName is without extension.
         *      `size` is optional, and is given, the original image will be resized to the specificed dimension.
         * @param callback:
         *  If success, result will be the drvfs filesystem url pointing the the file.
         *  Otherwise err will be given.
         */
        function captureImage(argument: undefined | null | { fileName?: string; size?: { width: number; height: number; } }, callback: (err?: Error, result?: string) => void);
    }

    namespace photoLibrary {
        /*******
         * pick an image from photo library.
         * @param argument: undefined | null  | { fileName?: string; size?: { width: number; height: number; } }
         *  Either undefined | null or passing with an option of type { fileName?: string; size?: { width: number; height: number; } }.
         *      where `fileName` is optional. fileName is without extension.
         *      `size` is optional, and is given, the original image will be resized to the specificed dimension.
         * @param callback:
         *  If success, result will be the drvfs filesystem url pointing the the file.
         *  Otherwise err will be given.
         */
        function pickImage(argument: undefined | null | { fileName?: string; size?: { width: number; height: number; } }, callback: (err?: Error, result?: string) => void);
    }

    namespace fileSystem {
        /********
         * Read file as Binary
         * @param argument:
         *  path: drvfs path of the file
         *  offset: Optional, offset to read from. If offset is not given, its default is 0.
         *  size: Optional, size to read. If not provided, entire file size will be used.
         * @param callback:
         *  If there's an error reading the file, err will be given.
         *  Otherwise, ArrayBuffer will be given as result.
         */
        function readFileAsBinary(argument: { path: string; offset?: number; size?: number; }, callback: (err?: Error, result?: ArrayBuffer) => void);

        /********
         * Read file as text
         * @param argument:
         *  path: drvfs path of the file
         * @param callback:
         *  If there's an error reading the file, err will be given.
         *  Otherwise, entire file content will be given as result.
         */
        function readFileAsText(argument: { path: string; }, callback: (err?: Error, result?: string) => void);

        /********
         * Write file as Binary
         * @param argument:
         *  path: drvfs path of the file
         *  data: ArrayBuffer. data to be written.
         *  offset: Optional, offset to write to. If not given, it will be the end of the current file content.
         * @param callback
         *  If success, returns result as file size after write.
         */
        function writeFileAsBinary(argument: { path: string; data: ArrayBuffer; offset?: number; }, callback: (err?: Error, result?: number) => void);

        /********
         * Write file as text
         * @param argument:
         *  path: drvfs path of the file
         *  data: ArrayBuffer. data to be written.
         *  offset: Optional, offset to write to. If not given, it will be the end of the current file content.
         * @param callback
         *  If success, returns result as file size after write.
         */
        function writeFileAsText(argument: { path: string; data: string; offset?: number; }, callback: (err?: Error, result?: number) => void);

        /********
         * Delete a file
         * @param argument: string
         *  drvfs path of the file
         * @param callback:
         *  result: undefined. Any other errors will be responded in the err argument of the callback.
         */
        function deleteFile(argument: string, callback: (err?: Error, result?: undefined) => void);

        /********
         * List files in a directory
         * @param argument: string
         *  drvfs path of the file
         * @param callback:
         *  result: an array of FileInfo objects will be returned. Any other errors will be responded in the err argument of the callback.
         */
        function list(argument: string, callback: (err?: Error, result?: FileInfo[]) => void);

        /********
         * Get File Info
         * @param argument: string
         *  drvfs path of the file
         * @param callback:
         *  result: a FileInfo object if success. Any errors will be responded in the err argument of the callback.
         */
        function getFileInfo(argument: string, callback: (err?: Error, result?: FileInfo) => void);

        /********
         * Delete an empty folder. it will fail if the folder is not empty.
         * @param argument: string
         *  drvfs path of the folder
         * @param callback:
         *  result: undefined. Any other errors will be responded in the err argument of the callback.
         */
        function deleteFolder(argument: string, callback: (err?: Error, result?: undefined) => void);

        /********
         * Move File
         * @param argument:
         *  srcPath: drvfs path of the source file. Directories cannot be specified.
         *  destPath: drvfs path of the destination file. Directories cannot be specified.
         *  overwrite: Optional. Should the destination file be overwritten if it already exists. Defaults to false (do not overwrite).
         * @param callback
         *  result: undefined. Any other errors will be responded in the err argument of the callback.
         */
        function moveFile(argument: { srcPath: string; destPath: string; overwrite?: number; }, callback: (err?: Error, result?: undefined) => void);
    }

    namespace motion {
        enum MotionActivityType {
            Unknown = 0,
            Stationary,
            Walking,
            Running,
            Biking,
            Driving
        }
        /********
         * start monitoring "geotab.motion" event.
         * @param argument: undefined
         * @param callback:
         *  err: Error, if permission denied, or motion sensor is not available, callback will be called with an error.
         *  result: undefined, if success, otherwise callback will be called with the corresponding motion type.
         *
         * To listen for the event:
         * window.addEventListener("geotab.motion", (param) => {
         *     console.log("geotab.motion event: ", param.detail); // detail is of type MotionActivityType
         * });
         */
        function startMonitoringMotionActivity(argument: undefined, callback: (err?: Error, result?: undefined) => void);

        /********
         * stop monitoring "geotab.motion" event. Will always success even if startMonitoringMotionActivity() was not called, or called with error.
         * @param argument: undefined
         * @param callback:
         *  err: Error, will always be undefined.
         *  result: undefined.
         */
        function stopMonitoringMotionActivity(argument: undefined, callback: (err?: Error, result?: undefined) => void);

        /*******
         * Motion event
         * @param detail: MotionActivityType
         *
         * To listen for the event:
         * window.addEventListener("geotab.motion", (param) => {
         *     console.log("geotab.motion event: ", param.detail); // detail is of type MotionActivityType
         * });
         */
        // event: "geotab.motion";
    }

    namespace ioxble {
        function start(argument: string, callback: (err?: Error, result?: undefined) => void);
        function stop(argument: undefined, callback: (err?: Error, result?: undefined) => void);

        /*******
         * ioxble.godevicedata event
         * @param detail: GoDeviceData
         *
         * To listen for the event:
         * window.addEventListener("ioxble.godevicedata", (param) => {
         *     console.log("ioxble.godevicedata event: ", param.detail); // detail is of type GoDeviceData
         * });
         */
        // event: "ioxble.godevicedata";

        /*******
         * ioxble.error event, fires only after a successfully start() and there's an error happened.
         * ioxble.error will not be fired if ioxble is not started successfully.
         *
         * During an start() session, error could happens.
         * Examples like losing connection, user invoking permission, user disabling Bluetooth device on phone.
         *
         * @param detail: Error
         *
         *
         * To listen for the iox ble error event:
         * window.addEventListener("ioxble.error", (param) => {
         *     console.log("ioxble.error event: ", param.detail); // detail is of type Error
         * });
         */
        // event: "ioxble.error";
    }

    namespace sso {
        /**********
         * initiatiate a SAML Login request. Please note if a SAML login request hasn't completed(callback function wasn't called),
         * and a new SAML Login is initiated, the previous request will be terminated first with callback(error).
         * @param argument: { samlLoginUrl: string }. Where samlLoginUrl is a URL of the thirdparty's website.
         * @param callback: (err?: Error, result?: string) => void
         *      - err: Error. An error will be sent if anything other than success happened. This includes can't load the samlLoginUrl, user cancel's the SAML login, terminated due to a new SAML login is requested etc.
         *      - result: string. The `JSON.stringify(sessionStorage.getItem('geotab_sso_credentials'))` value from the sso.html page. That means, Drive is responsible for deserializing the result and checking if the result is null/undefined or has a value.
         */
        function samlLogin(argument: { samlLoginUrl: string }, callback: (err?: Error, result?: string) => void);
    }

    namespace appearance {
        enum AppearanceType {
            Unknown = 0,
            Light,
            Dark
        }
        /*******
         * Readonly property, indicating the current appearance type of the device.
         */
        let appearanceType: AppearanceType;

        /*******
         * window event. Fires whenever the devices appearance type changes
         * @param detail: { "appearanceType": AppearanceType }
         */
        // event: "geotab.appearance"
    }
}
