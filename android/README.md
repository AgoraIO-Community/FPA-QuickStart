# 说明

本工程代码是为Android开发人员提供一份集成 agora 全链路加速 FPA示例，其中涉及到的相关代码使用只能说明建议使用，并不代表对应的具体业务一定需要如此操作，具体代码可以根据自己的业务场景而定。需要更多的帮助可访问官方文档。

## FPA 官方文档地址
[https://docs.agora.io/cn/global-accelerator/agora_ga_overview?platform=Android](https://docs.agora.io/cn/global-accelerator/agora_ga_overview?platform=Android)

# 编译
+ 依赖jitpack
```shell
    // root build.gradle 文件
    buildscript {
      repositories {
          maven { url "https://jitpack.io" }
      }
    }
```

+ 添加依赖
```shell
    // project build.gradle 文件
    implementation 'com.github.agorabuilder:fpa:0.1.12.0'
```
具体的版本依据实际情况而定

+ 制作配置文件
根据 assets 目录下的 `settings-example.json` 模版，制作一个自己对应的 `settings.json` 文件。    
代码中涉及到一些内部私有(例如 appId chainId 和 代理服务器)信息，所以示例代码使用如下代码隔离：
```java
    if (BuildConfig.is_agora_demo) {
        // business logic code
    }
```

# agora 人 

+ `settings.json` 文件可以向 @daijinguo 索要
+ 在 `local.properties` 文件中添加
```groovy
is_agora_demo=true
```
