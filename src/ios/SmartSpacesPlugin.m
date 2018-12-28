
#import "SmartSpacesBeacon.h"
#import "SmartSpacesPlugin.h"

@implementation SmartSpacesPlugin

- (void)pluginInitialize
{
    self.deviceid = [[NSUUID UUID] UUIDString];
    
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    self.peripheralManager = [[CBPeripheralManager alloc] init];
}

- (void)registerForBeacons:(CDVInvokedUrlCommand*)command
{
    [self _handleCallSafely:^CDVPluginResult *(CDVInvokedUrlCommand *command) {
        NSString* url = [command.arguments objectAtIndex:0];
        url = @"http://172.27.1.227:4567/";
        [self setBaseURL:url];

        if (url != nil && [url length] > 0) {
            NSLog(@"Server URL: %@", url);

            [self getBeaconListAndRegister];
        }

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [pluginResult setKeepCallbackAsBool:YES];
        return pluginResult;
    } :command];
}

- (void)disableBeaconDetection:(CDVInvokedUrlCommand*)command
{
    [self _handleCallSafely:^CDVPluginResult *(CDVInvokedUrlCommand *command) {
        NSLog(@"disableBeaconDetection");
        // TODO
        for (CLRegion *region in _locationManager.monitoredRegions) {
            [_locationManager stopMonitoringForRegion: region];
        }

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [pluginResult setKeepCallbackAsBool:YES];
        return pluginResult;
    } :command];
}



- (void)locationManager:(CLLocationManager *)manager didDetermineState:(CLRegionState)state forRegion:(CLRegion *)region {
    NSLog(@"didDetermineState: %d for region: %@", (int)state, region);

    CLBeaconRegion *beaconRegion = (CLBeaconRegion*)region;
    if (state == CLRegionStateInside) {
        [self sendData:[NSString stringWithFormat:@"deviceid=%@&uuid=%@&major=%d&minor=%d", [self deviceid], [beaconRegion proximityUUID], [[beaconRegion major] shortValue], [[beaconRegion minor] shortValue]] toApi:@"enter"];
    } else {
        [self sendData:[NSString stringWithFormat:@"deviceid=%@&uuid=%@&major=%d&minor=%d", [self deviceid], [beaconRegion proximityUUID], [[beaconRegion major] shortValue], [[beaconRegion minor] shortValue]] toApi:@"exit"];
    }
}

-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region {
    NSLog(@"didEnterRegion: %@", region);
    
    CLBeaconRegion *beaconRegion = (CLBeaconRegion*)region;
    [self sendData:[NSString stringWithFormat:@"deviceid=%@&uuid=%@&major=%d&minor=%d", [self deviceid], [beaconRegion proximityUUID], [[beaconRegion major] shortValue], [[beaconRegion minor] shortValue]] toApi:@"enter"];
}

-(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region {
    NSLog(@"didExitRegion: %@", region);
    
    CLBeaconRegion *beaconRegion = (CLBeaconRegion*)region;
    [self sendData:[NSString stringWithFormat:@"deviceid=%@&uuid=%@&major=%d&minor=%d", [self deviceid], [beaconRegion proximityUUID], [[beaconRegion major] shortValue], [[beaconRegion minor] shortValue]] toApi:@"exit"];
}

- (void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region {
    NSLog(@"didStartMonitoringForRegion: %@", region);
}

- (void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(CLRegion *)region withError:(NSError *)error {
    NSLog(@"monitoringDidFailForRegion: %@ reason %@", region, error);
}



- (void)getBeaconListAndRegister {
    NSError *error;
    NSString *url_string = [NSString stringWithFormat: @"%@regions", [self baseURL]];
    NSData *data = [NSData dataWithContentsOfURL: [NSURL URLWithString:url_string] options:NSDataReadingUncached error:&error];
    if (error != nil) {
        NSLog(@"Data error: %@", error);
        return;
    }
    NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:&error];
    if (error != nil) {
        NSLog(@"JSON error: %@", error);
        return;
    }
    
    for (int i = 0; i < [json count]; i++) {
        NSMutableDictionary *obj = [json objectAtIndex:i];
        
        NSString *name = [obj objectForKey:@"name"];
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString: [obj objectForKey:@"uuid"]];
        NSNumber *major = [obj objectForKey:@"major"];
        NSNumber *minor = [obj objectForKey:@"minor"];
        
        
        CLBeaconRegion *beacon = [[CLBeaconRegion alloc] initWithProximityUUID:uuid major:[major shortValue] minor:[minor shortValue] identifier:name];
        
        [_locationManager startMonitoringForRegion:beacon];
    }
    NSLog(@"Registered %d beacons", (int)[json count]);
}

-(void) sendData:(NSString*)post_data toApi:(NSString*)apiName {
    
    NSData *postData = [post_data dataUsingEncoding:NSASCIIStringEncoding allowLossyConversion:YES];
    NSString *postLength = [NSString stringWithFormat:@"%lu", (unsigned long)[postData length]];
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setURL:[NSURL URLWithString: [NSString stringWithFormat:@"%@%@", [self baseURL], apiName] ]];
    [request setHTTPMethod:@"POST"];
    [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:postData];
    
    NSURLSession *session = [NSURLSession sharedSession];
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data,
                                                                                          NSURLResponse * _Nullable response,
                                                                                          NSError * _Nullable error) {
        // Ignore silently
    }];
    [task resume];
}

- (void) _handleCallSafely: (CDVPluginCommandHandler) unsafeHandler : (CDVInvokedUrlCommand*) command  {
    [self _handleCallSafely:unsafeHandler :command :true];
}

- (void) _handleCallSafely: (CDVPluginCommandHandler) unsafeHandler : (CDVInvokedUrlCommand*) command : (BOOL) runInBackground :(NSString*) callbackId {
    if (runInBackground) {
        [self.commandDelegate runInBackground:^{
            @try {
                [self.commandDelegate sendPluginResult:unsafeHandler(command) callbackId:callbackId];
            }
            @catch (NSException * exception) {
                [self _handleExceptionOfCommand:command :exception];
            }
        }];
    } else {
        @try {
            [self.commandDelegate sendPluginResult:unsafeHandler(command) callbackId:callbackId];
        }
        @catch (NSException * exception) {
            [self _handleExceptionOfCommand:command :exception];
        }
    }
}

- (void) _handleCallSafely: (CDVPluginCommandHandler) unsafeHandler : (CDVInvokedUrlCommand*) command : (BOOL) runInBackground {
    [self _handleCallSafely:unsafeHandler :command :true :command.callbackId];
    
}

- (void) _handleExceptionOfCommand: (CDVInvokedUrlCommand*) command : (NSException*) exception {
    NSLog(@"Uncaught exception in command %@: %@", command.methodName, exception.description);
    NSLog(@"Stack trace: %@", [exception callStackSymbols]);
    
    // When calling without a request (LocationManagerDelegate callbacks) from the client side the command can be null.
    if (command == nil) {
        return;
    }
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.description];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (BOOL)isBluetoothEnabled {
    return _peripheralManager.state == CBPeripheralManagerStatePoweredOn;
}

@end
