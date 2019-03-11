package com.github.jbox.rpc;

import com.caucho.hessian.client.HessianProxyFactory;
import com.github.jbox.rpc.proto.RpcProcessor;
import com.github.jbox.rpc.proto.RpcProcessorImpl;
import com.github.jbox.rpc.proto.RpcProxy;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.IPv4;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 建议将RpcClient注册为一个SpringBean, 使用@Resource/@Autowired注入使用
 *
 * @author cunxiao
 * @since 2018-06-02 00:19
 */
@Slf4j
public class RpcClient implements ApplicationContextAware, InitializingBean {

    private static final ConcurrentMap<String, RpcProcessor> ip2processor = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Map<Class, Object>> ip2proxy = new ConcurrentHashMap<>();

    @Getter
    private List<String> servs;

    public void setServs(List<String> servs) {
        this.servs = servs;
    }

    public void setServs(String servs) {
        this.setServs(Splitter.on(",").trimResults().omitEmptyStrings().splitToList(servs));
    }

    @Setter
    private int servPort = 80;

    @Setter
    private String servProto = "http";

    @Setter
    private long connectTimeout = 200;

    @Setter
    private long readTimeout = 200;

    @Setter
    private boolean logParams = true;

    @Setter
    private boolean logRetObj = true;

    @Setter
    private boolean logMdcCtx = true;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Preconditions.checkState(Collections3.isNotEmpty(this.servs));

        HessianProxyFactory factory = new HessianProxyFactory();
        factory.setConnectTimeout(this.connectTimeout);
        factory.setReadTimeout(this.readTimeout);
        factory.setOverloadEnabled(true);
        factory.setChunkedPost(true);

        for (String servIp : this.servs) {
            if (servIp.equals(IPv4.getLocalIp())) {
                RpcProcessor processor = new RpcProcessorImpl(applicationContext, logParams, logRetObj, logMdcCtx);
                ip2processor.put(servIp, processor);
                log.info("hessian rpc client [{}] starting ...", servIp);
            } else {
                String url = String.format("%s://%s:%s%s", servProto, servIp, servPort, RpcServer.PATH);
                RpcProcessor processor = (RpcProcessor) factory.create(RpcProcessor.class, url);
                ip2processor.put(servIp, processor);
                log.info("hessian rpc client [{}] starting ...", url);
            }
        }
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

        RpcProcessor processor = ip2processor.get(servIp);
        Preconditions.checkState(processor != null, String.format("servIp:[%s] not exist from servs:%s", servIp, servs));

        Enhancer en = new Enhancer();
        en.setSuperclass(api);
        en.setCallback(new RpcProxy(api, processor, servIp, logParams, logRetObj, logMdcCtx));
        proxy = en.create();

        ip2proxy.computeIfAbsent(servIp, (_k) -> new ConcurrentHashMap<>()).put(api, proxy);

        return (T) proxy;
    }
}

