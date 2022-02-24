
#import "TransparentProxyTester.h"
#import "GCDAsyncSocket.h"

@interface TransparentProxyTester ()<GCDAsyncSocketDelegate>

@property (nonatomic, strong) NSMutableData *data;
@property (nonatomic, copy) NSString *writeString;
@property (strong, nonatomic) GCDAsyncSocket *clientSocket;
@property (strong, nonatomic) TransparentProxyDownloadCallback callback;
@property (nonatomic, assign) NSInteger requestId;

@end

@implementation TransparentProxyTester

- (void)connectWithPort:(NSInteger )port
                   name:(NSString *)name
              requestId:(NSInteger)requestId
               callback:(TransparentProxyDownloadCallback)callback {
    self.clientSocket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_global_queue(0, 0)];
    NSError *error;
    [self.clientSocket connectToHost:@"127.0.0.1" onPort:port viaInterface:nil withTimeout:-1 error:&error];
    self.writeString = [NSString stringWithFormat:@"GET /%@? HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n",name];
    self.callback = callback;
    self.requestId = requestId;
}

- (void)socket:(GCDAsyncSocket *)sock didConnectToHost:(NSString *)host port:(uint16_t)port {
    NSLog(@"didConnectToHost");
//    NSString *string = @"GET /100m.zip? HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n";
    NSData *data =[self.writeString dataUsingEncoding:NSUTF8StringEncoding];
    [self.clientSocket writeData:data withTimeout:10 tag:0];
    [self.clientSocket readDataWithTimeout:-1 tag:0];
}

- (void)socket:(GCDAsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag {
//    NSString *text = [[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
//    NSLog(@"data.length = %ld",data.length);
    [self.clientSocket readDataWithTimeout:1 tag:0];
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err {
    if (self.callback) {
        self.callback(err.code == 4 ? nil : err, self.requestId);
    }
    NSLog(@"socketDidDisconnect");
    self.clientSocket.delegate = nil;
    self.clientSocket = nil;
}

- (void)socketDidCloseReadStream:(GCDAsyncSocket *)sock {
    if (self.callback) {
        self.callback(nil,self.requestId);
    }
}

@end
