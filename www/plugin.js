var exec = require("cordova/exec");

var SmartSpacesPlugin = {
	registerForBeacons: function(serverURL, cb) {
		exec(cb, null, "SmartSpacesPlugin", "registerForBeacons", [serverURL]);
	},
	disableBeaconDetection: function(cb) {
		exec(cb, null, "SmartSpacesPlugin", "disableBeaconDetection", []);
	}
};

module.exports.SmartSpacesPlugin = SmartSpacesPlugin;
