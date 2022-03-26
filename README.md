# Hibiki-Twitter-Anouncer

> 一个Twitter转发器 from [原版仓库地址](https://github.com/7ddn/HibikiTwitterAnnouncer)

## 改动
根据实际需求，主要做了如下改动
* 仅转发原创图片内容（过滤纯文本、转推）
* 通过谷歌翻译API实现自动翻译

## 下载
可以从[releases](https://github.com/PokersKun/HibikiTwitterAnnouncer/releases)里下载

## 配置
这个版本的config比较简陋，配置文件位于.\config\HibikiTwitterAnnouncer\config.yml

第一次运行时插件会以默认值自动创建config

```yaml
# API
# Twitter/Google的各项API
APIs: 
  usersBy: 'https://api.twitter.com/2/users/by'
  recent: 'https://api.twitter.com/2/tweets/search/recent?query='
  showSingle: 'https://api.twitter.com/1.1/statuses/show.json?'
  translate: 'https://translation.googleapis.com/language/translate/v2'
  
# Twitter API Token/Google Translation API Key
# 在Twitter/Google注册应用之后会得到的Token/API Key，如果不填入该项会返回401错误
# Twitter建议使用bearerToken
Tokens: 
  bearerToken: 
  apiKey: 
    
# 拆分长文本开关
# 尽在很少的场景会用到
# 如果bot是新注册的可能会被限制发送100个字符以上的文本，此时请打开此开关，bot会将更长的信息拆分为100个字符的字信息发送
ifNeedToSplit: true
```

## 食用方法

暂时只支持部分自然语言解析的命令，Command形式的命令在写了，但是跑不起来qwq

### 帮助
```
帮助
```
查看帮助

### 查询
```
查询<twitterID>的<number>条推文
```
获取&lt;number>条来自@&lt;twitterID>的最新推文,其中&lt;number>为阿拉伯数字,&lt;twitterID>为只包含英文/数字/下划线的标准TwitterID
```
查询关于<object>的<number>条推文
```
获取包含&lt;object>关键字的&lt;number>条推文，其中&lt;object>为任意UTF-8标准字符,&lt;number>为阿拉伯数字

### 订阅
```
添加订阅
```
打开自动转发开关
````
取消订阅
````
关闭自动转发开关
````
关注<username>
````
添加对@&lt;username>的新推文的自动转发，需要先添加订阅才能使用这个功能
````
取消关注<username>
````
取消对@&lt;username>的新推文的自动转发
````
添加@<username>的过滤器:包含<keyword>
````
只推送包含&lt;keyword>的来自@&lt;username>的过滤器
````
添加@<username>的过滤器:不包含<keyword>
````
不推送包含&lt;keyword>的来自@&lt;username>的过滤器
