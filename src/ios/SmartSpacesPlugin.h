
#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <CoreLocation/CoreLocation.h>
#import <CoreBluetooth/CoreBluetooth.h>

typedef CDVPluginResult* (^CDVPluginCommandHandler)(CDVInvokedUrlCommand*);

@interface SmartSpacesPlugin : CDVPlugin<CLLocationManagerDelegate> {

}

@property NSString *baseURL;
@property NSString *deviceid;

@property (retain) CLLocationManager *locationManager;
@property (retain) CBPeripheralManager *peripheralManager;

- (void)registerForBeacons:(CDVInvokedUrlCommand*)command;
- (void)disableBeaconDetection:(CDVInvokedUrlCommand*)command;

@end
