# Jbox 工具集合
> **2.2.0**

---
## executor: [并发组件](https://github.com/feiqing/jbox/tree/master/src/main/java/com/github/jbox/executor)
| 组件 | 描述&内容 |
| :---- | :---- |
| `AsyncJobExecutor` | 批量任务并发执行&等待框架 |
| `ExecutorsManager` | ***`java.util.concurrent.Executors`线程池扩展:*** |
| | 分组`group`内线程池单例, 防止建线程池循环创建 |
| | 线程以`'${group}-${number}'`形式命名, 使线程栈查看更加清晰 |
| | 开放`newFixedMinMaxThreadPool()`方法, 提供比`Executors`更灵活, 比`ThreadPoolExecutor`更便捷的配置方式 |
| | 提供`com.github.jbox.executor.policy`线程拒绝策略, 在`Task-Queue`满时打印日志 |
| | 自动将`org.slf4j.MDC`数据copy到子线程context, 并在子线程执行结束时自动清理 |
| `ExecutorMonitor` | ***监控:打印线程池监控信息到`Logger:com.github.jbox.executor.ExecutorMonitor`下:*** |
| | Group相关:        'active count'、'core pool size'、'pool size'、'max pool size' |
| | TaskInvoke相关:   'success count'、'failure count'、'avg rt'、'avg tps' |
| | TaskQueue相关:    'queue size'、'remaining queue size' |
| | TopN阻塞Task相关:  'taskInfo()'、'task hash' |

---

## rpc: 基于akka、hessian两种实现的RPC框架
- 最佳示例
> 配置
```xml
<bean class="com.github.jbox.rpc.akka.RpcServer"/>
<bean class="com.github.jbox.rpc.akka.RpcClient">
    <property name="readTimeout" value="200"/>
</bean>
```

> client调用

![](https://img.alicdn.com/tfs/TB1abdVF4v1gK0jSZFFXXb0sXXa-1050-229.png)
> 调用方式: `client.proxy("${server-ip}", ${server-service}.class).${service-method}(${params})`

> akka额外提供asyncProxy高性能异步调用, 详见: `com.github.jbox.rpc.akka.RpcClient`

---

## job: 逻辑解耦框架
> 强制对复杂业务逻辑解耦, 保障业务流程清晰流畅, 每一个Task与整体任务在框架层面进行监控, 保障任务串正确执行

- 最佳示例
> 配置
```xml
<bean id="ipoTraceAspect" class="com.github.jbox.trace.TraceLauncher">
    <property name="tasks">
        <list>
            <bean class="com.github.jbox.trace.tasks.EagleEyeTask"/>
            <bean class="com.github.jbox.trace.tasks.ArgValidateTask"/>
            <bean class="com.github.jbox.trace.tasks.LogRootErrorTask"/>
            <bean class="com.github.jbox.trace.tasks.MethodInvokeTask"/>
        </list>
    </property>
</bean>
```
> 实现
![](https://img.alicdn.com/tfs/TB1qjBPFYr1gK0jSZFDXXb9yVXa-1009-754.png)

---

## mongo: MongoORM框架: MongoBatis
| 组件 | 描述 |
| :---- | :---- |
|  `SequenceDAO` | 分布式高性能SequenceID生成(已自动融合进`insert`、`upsert`等方法) |
| `TdmlTableFactory` | Mongo分表, 基于MDC context |
| `TdmlDbFactory` | Mongo分库, Proxy模式, 业务0侵入, 方便配置 |
| `MongoBatis` | ORM |
| | `insert`自动添加`gmt_create`、`gmt_modified`、`_id`(`long`)属性 |
| | `update`自动更新`gmt_modified`属性为当前系统时间 |
| | `find`、`update`、`remove`等以的`Map<String, Object>`作为**查询**、**更新**参数, 屏蔽Mongo特殊的语法 |
| | `findById`、分页`find`、`updateById`、`removeById`、`distinct`等helper方法 |

## oplog: MongoOplog日志解析&同步框架
- 详见: `com.github.jbox.oplog.OplogTailStarter`

---
## hbase: HBaseORM框架: HBaseBatis
- 详见: `com.github.jbox.hbase.HBaseBatis`


---

## trace: 业务层统一AOP框架
- 主控器: `com.github.jbox.trace.TraceLauncher`
- spi
    - 可执行节点: `com.github.jbox.trace.InvokerNode`
    - 节点环境  : `com.github.jbox.trace.NodeContext`
- 支持
    - 方法参数校验: `com.github.jbox.trace.nodes.ArgsValidateInvokerNode`
    - 运行耗时监控: `com.github.jbox.trace.nodes.ElapsedLogInvokerNode`
    - 运行出错日志: `com.github.jbox.trace.nodes.LogRootErrorInvokerNode`
    - 方法执行兜底: `com.github.jbox.trace.nodes.MethodInvokeInvokerNode`
    - 淘宝鹰眼监控: `com.github.jbox.trace.nodes.EagleEyeInvokerNode`
    - 淘宝哨兵限流: `com.github.jbox.trace.nodes.SentinelInvokerNode`
    - 统一日志打印: `com.github.jbox.trace.nodes.TlogInvokerNode`(目前支持xml-SpEL日志配置)
        
    | 默认占位符 | desc |
    | :------: | :-------- |
    | `start time`    | 方法执行开始时间 |
    | `invoke thread` | 方法执行线程 |
    | `rt`            | 方法执行耗时 |
    | `class name`    | 方法所属类名 |
    | `method name`   | 方法名 |
    | `args`          | 入参 |
    | `result`        | 返回值 |
    | `exception`     | 方法执行抛出异常 |
    | `server ip`     | 所属机器IP |
    | `trace id`      | EagleEye TraceId | 
    | `client name`   | 调用方name |
    | `client ip`     | 调用方IP |

---
## biz:商业工具

| class | 描述 |
| :---- | :---- |
| `com.github.jbox.biz.ResultDO` | 业务返回model统一封装 |
| `com.github.jbox.biz.PageResultDO` | (分页Result) |
| `com.github.jbox.biz.IErrorCode` | 业务错误结果统一封装 |
| `com.github.jbox.biz.BizException` | 业务异常统一封装 |

---
## flood:AB流量测试框架
- 详见: `com.github.jbox.flood.FloodABExperiment`

---
## http: 针对HttpClient `GET`/`POST`的统一请求/处理封装
- 详见: `com.github.jbox.http.HttpGetClient`
- 详见: `com.github.jbox.http.HttpPostClient`

---
## scheduler: 单机任务调度框架
- 详见: `com.github.jbox.scheduler.TaskScheduler`

---
## script: 运行时Script执行框架
- 详见: `com.github.jbox.script.ScriptExecutor`
- 支持:
    - JavaScript
    - Groovy
    - Python

---
## serializer: 统一序列化/反序列化接口
- 详见: `com.github.jbox.serializer.ISerializer`
- 默认支持:
    - fastjson: `com.github.jbox.serializer.support.FastJsonSerializer`
    - hessian2: `com.github.jbox.serializer.support.Hessian2Serializer`
    - kryo: `com.github.jbox.serializer.support.KryoSerializer`
    - jdk: `com.github.jbox.serializer.support.JdkSerializer`
    - jdk(with gzip): `com.github.jbox.serializer.support.JdkGzipSerializer`
   
---
## spring: Spring容器扩展
- Spring容器启动耗时监控: `com.github.jbox.spring.BeanInstantiationMonitor`
- SpringContext注入: `com.github.jbox.spring.AbstractApplicationContextAware`
- 懒初始化Bean支持: `com.github.jbox.spring.LazyInitializingBean`
- 非Spring托管Bean `@Resource`、`@Autowired`、`@Value`适配器: `com.github.jbox.spring.SpringAutowiredAdaptor`
- `@Value`注解动态配置支持: `com.github.jbox.spring.DynamicPropertySourcesPlaceholder`
    - 默认支持淘宝Diamond: `DiamondPropertySourcesPlaceholder`;
---
## stream: Stream并发多级利用
- 详见: `com.github.jbox.stream.StreamForker`

---
## utils- 通用工具
| utils | desc |
| :------: | :-------- |
| `JboxUtils` | 通用工具方法: `getFieldValue`、`getStackTrace`、`runWithNewMdcContext` |
| `AESUtils`   | AES加解密 |
| `AopTargetUtils`   | 获取AOP target |
| `BeanCopyUtil` | Bean属性对拷, 忽略属性类型 |
| `DateUtils` | 日期时间格式化/解析 |
| `HexStr` | 16进制与`byte`转换 |
| `IPv4` | IPv4地址获取 |
| `Mac` | Mac地址获取 |
| `MapBuilder` | Map构建器 |
| `JsonAppender` | append json string |
| `Collections3` | `Collections`扩展 |
| `Objects2` | `Objects`扩展 |
| `Performer` | 性能测试工具, 监控如 ***RT***、***QPS*** |
| `ProxyTargetUtils` | 获取Proxy target(仅支持Proxy实现包含`target`属性的情况) |
| `SizeOf` | 精确测量内存内Java对象大小(`-javaagent`) |
| `Tony` | 玩具工具 |

---
# todo
- 可持久化缓存
- Shell执行Service