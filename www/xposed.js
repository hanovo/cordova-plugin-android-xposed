"use strict";

module.exports = {
  // value must be an ArrayBuffer
  listen: function (success, error) {
    console.log("Initializing android xposed cordova plugin...");

    cordova.exec(success, error, "AndroidXPosedPluginEntry", "listen", []);
  }
};