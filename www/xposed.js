cordova.define("cordova-plugin-android-xposed.XPosedPluginEntry", function(require, exports, module) {
  "use strict";
  
  module.exports = {
    init: function (successCallback, errorCallback) {
      console.log("Init android xposed cordova plugin...");
  
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "init", []);
    },
  
    getHookableApps: function (successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "getHookableApps", []);
    },
  
    startApp: function (packageName, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "startApp", [packageName]);
    },
  
    stopApp: function (packageName, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "stopApp", [packageName]);
    },
  
    getLogs: function (packageName, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "getLogs", [packageName]);
    },
  
    getLoginUserInfo: function (packageName, successCallback, errorCallback) {
      cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "getLoginUserInfo", [packageName]);
    },
  };
  });
  