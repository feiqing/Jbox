package com.github.jbox.rpc;

import com.github.jbox.rpc.proto.RpcProcessor;
import com.github.jbox.rpc.proto.RpcProcessorImpl;
import com.github.jbox.utils.IPv4;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.caucho.HessianServiceExporter;

/**
 * @author cunxiao
 * @since 2018-06-01 23:33
 **/
@Slf4j
@Configuration
public class RpcServer {

    static final String PATH = "/hessianRpcProvider";

    @Setter
    private boolean logParams = true;

    @Setter
    private boolean logRetObj = true;

    @Setter
    private boolean logMdcCtx = true;

    @Bean(PATH)
    public HessianServiceExporter hessianRpcProvider(ApplicationContext applicationContext) {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(new RpcProcessorImpl(applicationContext, logParams, logRetObj, logMdcCtx));
        exporter.setServiceInterface(RpcProcessor.class);
        log.info("hessian rpc server [{}] starting...", IPv4.getLocalIp());

        return exporter;
    }
}
