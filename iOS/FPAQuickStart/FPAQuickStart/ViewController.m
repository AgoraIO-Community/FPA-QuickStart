
#import "ViewController.h"
#import <AgoraFpaProxyService/FpaProxyService.h>
#import "FpaHttpTester.h"
#import "TransparentProxyTester.h"

@interface ViewController ()

@property (nonatomic, strong) UIButton *uploadBtn;
@property (nonatomic, strong) UIButton *donwloadBtn;
@property (nonatomic, strong) UIButton *transparentProxyBtn;

@property (nonatomic, strong) FpaProxyServiceConfig *fpaConfig;
@property (nonatomic, strong) FpaChainInfo *transparentProxyChainInfo;
@property (nonatomic, strong) FpaHttpProxyChainConfig *httpProxyConfig;

@property (nonatomic, strong) NSMutableDictionary *testMap;
@property (nonatomic, assign) NSInteger connectionId;

@property (nonatomic, strong) NSDictionary *dataDict;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.connectionId = 0;
    self.testMap = [NSMutableDictionary dictionary];
    [self setupUI];
    if ([self parseData]) {
        [self setupFPA];
    }
}

- (void)setupFPA {
    [self configFPA];
    [self startFPA];
}

- (void)setupUI {
    UIButton *uploadBtn = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 160, 48)];
    uploadBtn.titleLabel.font = [UIFont systemFontOfSize:14];
    [uploadBtn setTitle:@"http/https upload" forState:UIControlStateNormal];
    uploadBtn.backgroundColor = [UIColor lightGrayColor];
    [uploadBtn addTarget:self action:@selector(testUpload) forControlEvents:UIControlEventTouchUpInside];
    uploadBtn.center = CGPointMake(CGRectGetMaxX(self.view.bounds)/2, CGRectGetMaxY(self.view.bounds)/2 + 20);
    [self.view addSubview:uploadBtn];
    self.uploadBtn = uploadBtn;
    
    UIButton *downloadBtn = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 160, 48)];
    downloadBtn.titleLabel.font = [UIFont systemFontOfSize:14];
    [downloadBtn setTitle:@"http/https download" forState:UIControlStateNormal];
    downloadBtn.backgroundColor = [UIColor lightGrayColor];
    [downloadBtn addTarget:self action:@selector(testDownload) forControlEvents:UIControlEventTouchUpInside];
    downloadBtn.center = CGPointMake(CGRectGetMaxX(self.view.bounds)/2, CGRectGetMaxY(self.view.bounds)/2 + 80);
    [self.view addSubview:downloadBtn];
    self.donwloadBtn = downloadBtn;
    
    UIButton *transportProxyBtn = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 160, 48)];
    transportProxyBtn.titleLabel.font = [UIFont systemFontOfSize:14];
    [transportProxyBtn setTitle:@"tcp transportProxy" forState:UIControlStateNormal];
    transportProxyBtn.backgroundColor = [UIColor lightGrayColor];
    [transportProxyBtn addTarget:self action:@selector(testTransportProxy) forControlEvents:UIControlEventTouchUpInside];
    transportProxyBtn.center = CGPointMake(CGRectGetMaxX(self.view.bounds)/2, CGRectGetMaxY(self.view.bounds)/2 + 140);
    [self.view addSubview:transportProxyBtn];
    self.transparentProxyBtn = transportProxyBtn;
}

- (void)testUpload {
    [[FpaHttpTester sharedHttpTester] createSessionManager:[[FpaProxyService sharedFpaProxyService] httpProxyPort]];
    NSString *uploadUrl = [self.dataDict valueForKey:@"test_upload_url"];
    NSString *uploadName = @"1MB.txt";
    NSString *filePath = [[NSBundle mainBundle] pathForResource:@"1MB.txt" ofType:nil];;
    [[FpaHttpTester sharedHttpTester] upload:uploadUrl
                                  uploadName:uploadName
                                    filePath:filePath
                           completionHandler:^(NSURLSessionDataTask *task, id _Nullable resp, NSError * _Nullable error) {
        if (!error) {
            NSLog(@"upload succeed!");
        }
    }];
}

- (void)testDownload {
    [[FpaHttpTester sharedHttpTester] createSessionManager:[[FpaProxyService sharedFpaProxyService] httpProxyPort]];
    NSString *downloadUrl = [self.dataDict valueForKey:@"test_download_url"];
    NSString *savePath = @"";
    [[FpaHttpTester sharedHttpTester] download:downloadUrl
                                        toPath:savePath
                             completionHandler:^(NSURLResponse *resp, NSURL * _Nullable url, NSError * _Nullable error) {
        if (!error) {
            NSLog(@"download succeed!");
        }
    }];
}

- (void)testTransportProxy {
    self.connectionId++;
    TransparentProxyTester *tester = [[TransparentProxyTester alloc] init];
    NSInteger port = [[FpaProxyService sharedFpaProxyService] getTransparentProxyPortWithChainInfo:self.transparentProxyChainInfo];
    if (port <= 0) {
        return;
    }
    NSString *resource = [self.dataDict valueForKey:@"transparentProxy_test_resource"];
    [tester connectWithPort:port name:resource requestId:self.connectionId callback:^(NSError * _Nonnull error, NSInteger requestId) {
        if (error) {
            NSLog(@"download failed.");
        } else {
            NSLog(@"download succeed");
        }
        [self.testMap removeObjectForKey:@(requestId).stringValue];
    }];
    [self.testMap setValue:tester forKey:@(self.connectionId).stringValue];
}

- (BOOL)parseData {
    NSData *data = [NSData dataWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"settings-example.json" ofType:nil]];
    if (data) {
        NSError *error = nil;
        NSDictionary *result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableLeaves error:&error];
        if (error) {
            return NO;
        }
        self.dataDict = result;
        NSAssert(![[self.dataDict valueForKey:@"app_id"] isEqualToString:@"You App Id"], @"Make sure your parameters in settings-example.json are configured correctly, If you need help, please contact us");
        return YES;
    } else {
        return NO;
    }
}

- (void)configFPA {
    // config
    NSString *docDir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    NSString *logFile = [NSString stringWithFormat:@"%@/fpa.log",docDir];
    
    FpaProxyServiceConfig *config = [[FpaProxyServiceConfig alloc] init];
    config.appId = [self.dataDict valueForKey:@"app_id"];
    config.token = [self.dataDict valueForKey:@"app_token"];
    config.logLevel = FpaLogLevelInfo;
    config.logFilePath = logFile;
    config.fileSize = 1024;
    self.fpaConfig = config;
    
    // http proxy config
    FpaHttpProxyChainConfig *httpConfig = [[FpaHttpProxyChainConfig alloc] init];
    NSMutableArray *array = [NSMutableArray array];
    NSArray *dataArray = [[self.dataDict valueForKey:@"http_chain_info"] valueForKey:@"chain_info"];
    for (NSDictionary *dict in dataArray) {
        NSInteger chainId = [[dict valueForKey:@"chain_id"] integerValue];
        NSString *domain = [dict valueForKey:@"domain"];
        NSInteger port = [[dict valueForKey:@"port"] integerValue];
        BOOL enableFallback = [[dict valueForKey:@"fallback"] boolValue];
        FpaChainInfo *info = [FpaChainInfo fpaChainInfoWithChainId:chainId address:domain port:port enableFallback:enableFallback];
        [array addObject:info];
    }
    httpConfig.chainArray = [array copy];
    httpConfig.fallbackWhenNoChainAvailable = [[[self.dataDict valueForKey:@"http_chain_info"] valueForKey:@"fallback"] boolValue];
    self.httpProxyConfig = httpConfig;
    
    // transparentProxy
    NSDictionary *dict = [self.dataDict valueForKey:@"transparentProxy_chain_info"];
    NSInteger chainId = [[dict valueForKey:@"chain_id"] integerValue];
    NSString *domain = [dict valueForKey:@"domain"];
    NSInteger port = [[dict valueForKey:@"port"] integerValue];
    self.transparentProxyChainInfo = [FpaChainInfo fpaChainInfoWithChainId:chainId address:domain port:port enableFallback:YES];
}

- (void)startFPA {
    // start
    [[FpaProxyService sharedFpaProxyService] startWithConfig:self.fpaConfig];
    [[FpaProxyService sharedFpaProxyService] setupDelegate:(id<FpaProxyServiceDelegate>)self];
    [[FpaProxyService sharedFpaProxyService] setOrUpdateHttpProxyChainConfig:self.httpProxyConfig];
}

- (void)diagnosisInfo {
    FpaProxyServiceDiagnosisInfo *info = [[FpaProxyService sharedFpaProxyService] diagnosisInfo];
}

- (void)stopFPA {
    [[FpaProxyService sharedFpaProxyService] stop];
}

- (void)reset {
    [self stopFPA];
    [self startFPA];
}

- (void)onAccelerationSuccess:(FpaProxyServiceConnectionInfo * _Nonnull)connectionInfo {
    NSLog(@"fpa accelerationSuccess");
}

- (void)onConnected:(FpaProxyServiceConnectionInfo * _Nonnull)connectionInfo {
    NSLog(@"fpa connected");
}

- (void)onDisconnectedAndFallback:(FpaProxyServiceConnectionInfo * _Nonnull)connectionInfo reason:(FpaFailedReason)reason {
    NSLog(@"fpa disconnected");
}

- (void)onConnectionFailed:(FpaProxyServiceConnectionInfo * _Nonnull)connectionInfo reason:(FpaFailedReason)reason {
    NSLog(@"fpa connection failed");
}


@end
