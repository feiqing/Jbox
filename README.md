# Jbox 工具集合

## biz-业务对象包
- `com.github.jbox.biz.ResultDO`: HTTP/RPC接口Result统一封装
- `com.github.jbox.biz.BizException`、`com.github.jbox.biz.IErrorCode`: 业务异常封装

## executor-线程池扩展
- `ExecutorsManager`: 线程池管理
- `ExecutorsMonitor`: 线程池监控
1. 优势:
    - 每个分组`group`中的线程默认都是单例的, 防止出现在方法内部循环创建线程池的错误写法;
    - 线程以`'${group}-${number}'`形式命名, 使在查看线程栈时更加清晰;
    - 开放`newFixedMinMaxThreadPool()`方法, 提供比`Executors`更灵活, 比`ThreadPoolExecutor`更便捷的配置方式;
    - 提供`com.github.jbox.executor.policy`线程拒绝策略, 在`RunnableQueue`满时打印日志;
    - 自动将`org.slf4j.MDC`数据copy到子线程context, 并在子线程执行结束时自动清理;
    - 添加`ExecutorMonitor`监控: 将线程池监控日志打印到`'executor-monitor'`这个`Logger`下, 打印内容包含:
        - a) 线程组信息: 'group', 'pool count', 'active count', 'core pool count', 'max pool count'
        - b) 线程组执行信息: 'success', 'failure', 'rt', 'tps'
        - c) RunnableQueue信息: 'queues:被阻塞在Queue内任务数量', 'remains:Queue尚余空间'
        - d) 被阻塞的任务detail信息: 'taskInfo()', 实例id
2. 封装`java.lang.Runnable`为`AsyncRunnable`, 描述信息详见`AsyncRunnable`;
3. 封装`java.util.concurrent.Callable`为`AsyncCallable`, 描述信息详见`AsyncCallable`;
4. 可通过`setSyncInvoke()`方法将提交到线程池内的task在主线程中同步执行, 便于debug业务逻辑或其他场景, 详见`SyncInvokeExecutorService`,
目前暂时不支持`ScheduledExecutorService`实现;
5. 如果将`ExecutorManager`注册为SpringBean, 会在应用关闭时自动将线程池关闭掉, 防止线程池未关导致应用下线不成功的bug;

---
## flood:AB流量测试框架
- 详见: `com.github.jbox.flood.FloodABExperiment`

---
## hbase: 基于HBase(HbaseTemplate)的ORM框架
- 详见: `com.github.jbox.hbase.HBaseBatis`

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
    - hession2: `com.github.jbox.serializer.support.Hession2Serializer`
    - kryo: `com.github.jbox.serializer.support.KryoSerializer`
    - jdk: `com.github.jbox.serializer.support.JdkSerializer`
    - jdk(with gzip): `com.github.jbox.serializer.support.JdkGzipSerializer`
   
---
## spring: Spring容器扩展
- 容器启动耗时监控: `com.github.jbox.spring.BeanInstantiationMonitor`
- SpringContext注入: `com.github.jbox.spring.AbstractApplicationContextAware`
- 懒初始化Bean支持: `com.github.jbox.spring.LazyInitializingBean`
- 非Spring托管Bean `@Resource`、`@Autowired`、`@Value`适配器: `com.github.jbox.spring.SpringAutowiredAdaptor`
- `@Value`注解动态配置支持: `com.github.jbox.spring.DynamicPropertySourcesPlaceholder`
    - 默认支持淘宝Diamond: `DiamondPropertySourcesPlaceholder`;
---
### stream: Stream并发多级利用
- 详见: `com.github.jbox.stream.StreamForker`

---
### trace: 业务层统一AOP框架
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
### utils- 通用工具

| utils | desc |
| :------: | :-------- |
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
| `JboxUtils` | 通用工具集: `getFieldValue`、`getAbstractMethod`、`getImplMethod`、`getStackTrace`、`getServerIp`、`trimPrefixAndSuffix`、`getSimplifiedMethodName` |