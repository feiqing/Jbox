package com.github.jbox.rpc.hession;

import com.caucho.hessian.client.HessianProxyFactory;
import com.github.jbox.utils.IPv4;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 建议将RpcClient注册为一个SpringBean, 使用@Resource/@Autowired注入使用
 *
 * @author cunxiao
 * @since 2018-06-02 00:19
 */
@Slf4j(topic = "JboxRpcClient")
public class RpcClient implements ApplicationContextAware {

    private static final ConcurrentMap<String, Map<Class, Object>> ip2proxy = new ConcurrentHashMap<>();

    @Setter
    private int servPort = 80;

    @Setter
    private String servProto = "http";

    @Setter
    private String rpcPath = RpcServer.PATH;

    @Setter
    private long connectTimeout = 200;

    @Setter
    private long readTimeout = 200;

    private ApplicationContext applicationContext;

    private HessianProxyFactory factory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.factory = new HessianProxyFactory();
        this.factory.setConnectTimeout(this.connectTimeout);
        this.factory.setReadTimeout(this.readTimeout);
        this.factory.setOverloadEnabled(true);
        this.factory.setChunkedPost(true);
    }

    /**
     * Rpc最核心接口:
     * 示例: {@code Resp resp = rpcClient.proxy(serverIp, IServerService.class).method(req);}
     * 首先使用{@code proxy()}方法拿到服务端具体实现实例, 然后再请求具体方法.
     *
     * @param servIp: 服务端IP
     * @param api:    服务端服务接口(可以为接口, 也可以为具体实现类)
     * @return 服务端服务实例(RpcProxy)
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(String servIp, Class<T> api) {
        Object proxy = ip2proxy.getOrDefault(servIp, Collections.emptyMap()).get(api);
        if (proxy != null) {
            return (T) proxy;
        }

        RpcProcessor processor;
        if (servIp.equals(IPv4.getLocalIp())) {
            processor = new RpcProcessorImpl(applicationContext);
            log.info("hessian rpc client [{}] starting ...", servIp);
        } else {
            String url = String.format("%s://%s:%s%s", servProto, servIp, servPort, rpcPath);
            try {
                processor = (RpcProcessor) factory.create(RpcProcessor.class, url);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            log.info("hessian rpc client [{}] starting ...", url);
        }


        Enhancer en = new Enhancer();
        en.setSuperclass(api);
        en.setCallback(new RpcProxy(api, processor, servIp));
        proxy = en.create();

        ip2proxy.computeIfAbsent(servIp, (_k) -> new ConcurrentHashMap<>()).put(api, proxy);

        return (T) proxy;
    }
}

