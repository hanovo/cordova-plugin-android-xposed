"use strict";

module.exports = {
  init: function (successCallback, errorCallback) {
    console.log("Init android xposed cordova plugin...");

    cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "init", []);
  },

  getHookableApps: function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "getHookableApps", []);
  },

  hookApp: function (packageName, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "XPosedPluginEntry", "hookApp", [packageName]);
  },
};