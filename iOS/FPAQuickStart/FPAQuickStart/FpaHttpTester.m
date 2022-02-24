
#import "FpaHttpTester.h"
#import <AFNetworking/AFNetworking.h>

@interface FpaHttpTester ()

@property (nonatomic, strong) AFHTTPSessionManager *manager;
@property (nonatomic, assign) NSInteger proxyPort;

@end

@implementation FpaHttpTester

+ (FpaHttpTester * _Nonnull)sharedHttpTester {
    static FpaHttpTester *_sharedClient = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedClient = [[FpaHttpTester alloc] init];
    });
    return _sharedClient;
}

- (void)createSessionManager:(NSInteger)port {
    if (port == self.proxyPort) {
        return;
    }
    AFHTTPSessionManager *manager = [[AFHTTPSessionManager alloc] initWithSessionConfiguration:[self configurationWithFPALocalServer:(int)port]];
    [manager.requestSerializer willChangeValueForKey:@"timeoutInterval"];
    manager.requestSerializer.timeoutInterval = 10.f;
    [manager.requestSerializer didChangeValueForKey:@"timeoutInterval"];
    [manager.requestSerializer setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    manager.responseSerializer.acceptableContentTypes = [NSSet setWithObjects:@"application/json", @"text/json", @"text/javascript", @"text/html", @"text/plain",nil];
    self.manager = manager;
    self.proxyPort = port;
}

- (void)upload:(NSString *)uploadURL
    uploadName:(NSString *)uploadName
      filePath:(NSString *)filePath
completionHandler:(FPAPluginUploadCallback)completionHandler {
    NSLog(@"upload from filePath = %@",filePath);
    BOOL isfileEx = [[NSFileManager defaultManager] fileExistsAtPath:filePath];
    NSLog(@"isfileEx = %d",isfileEx);
    [self.manager POST:uploadURL parameters:nil headers:nil constructingBodyWithBlock:^(id<AFMultipartFormData>  _Nonnull formData) {
           [formData appendPartWithFileURL:[NSURL fileURLWithPath:filePath] name:@"file" fileName:uploadName mimeType:@"text/plain" error:nil];
    } progress:^(NSProgress * _Nonnull uploadProgress) {
       NSLog(@"upload progress session = %.2f",uploadProgress.fractionCompleted * 100);
    } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
       if (completionHandler) {
           completionHandler(task,responseObject,nil);
       }
    } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
       if (completionHandler) {
           completionHandler(task,nil,error);
       }
    }];
}

- (void)download:(NSString *)downloadURL toPath:(NSString *)toPath completionHandler:(FPAPluginDownloadCallback)completionHandler {
    NSURL *URL = [NSURL URLWithString:downloadURL];
    NSURLRequest *request = [NSURLRequest requestWithURL:URL];
    NSURLSessionDownloadTask *downloadTask = [self.manager downloadTaskWithRequest:request progress:^(NSProgress * _Nonnull downloadProgress) {
        NSLog(@"%.2f",downloadProgress.fractionCompleted * 100);
    }  destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        NSURL *documentsDirectoryURL = [[NSFileManager defaultManager] URLForDirectory:NSDocumentDirectory inDomain:NSUserDomainMask appropriateForURL:nil create:NO error:nil];
        return [documentsDirectoryURL URLByAppendingPathComponent:[response suggestedFilename]];
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        NSLog(@"download to filePath = %@",filePath.path);
        if (response) {
            if (completionHandler) {
                completionHandler(response,filePath,error);
            }
        }
    }];
    [downloadTask resume];
}

- (NSURLSessionConfiguration *)configurationWithFPALocalServer:(int)port {
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    configuration.connectionProxyDictionary = @{
        (id)kCFNetworkProxiesHTTPEnable:@YES,
        (id)kCFNetworkProxiesHTTPProxy:@"127.0.0.1",
        (id)kCFNetworkProxiesHTTPPort:@(port),
        @"HTTPSEnable":@YES,
        @"HTTPSProxy":@"127.0.0.1",
        @"HTTPSPort":@(port),
    };
    return configuration;
}

@end
