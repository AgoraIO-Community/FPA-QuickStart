
#import <Foundation/Foundation.h>

typedef void (^TransparentProxyDownloadCallback)(NSError * _Nullable error, NSInteger requestId);

NS_ASSUME_NONNULL_BEGIN

@interface TransparentProxyTester : NSObject

- (void)connectWithPort:(NSInteger )port
                   name:(NSString *)name
              requestId:(NSInteger)requestId
               callback:(TransparentProxyDownloadCallback)callback;

@end

NS_ASSUME_NONNULL_END
