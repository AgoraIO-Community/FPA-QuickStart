

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface FpaHttpTester : NSObject

typedef void(^FPAPluginUploadCallback)(NSURLSessionDataTask * _Nullable, id _Nullable, NSError * _Nullable);
typedef void(^FPAPluginDownloadCallback)(NSURLResponse *_Nullable, NSURL * _Nullable, NSError *_Nullable);

+ (FpaHttpTester * _Nonnull)sharedHttpTester;

- (void)createSessionManager:(NSInteger)port;

- (void)upload:(NSString *)uploadURL
    uploadName:(NSString *)uploadName
      filePath:(NSString *)filePath
completionHandler:(FPAPluginUploadCallback)completionHandler;

- (void)download:(NSString *)downloadURL
          toPath:(NSString *)toPath
completionHandler:(FPAPluginDownloadCallback)completionHandler;

@end

NS_ASSUME_NONNULL_END
