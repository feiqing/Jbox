package com.github.jbox.rpc.hession;

import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.caucho.HessianServiceExporter;

/**
 * @author cunxiao
 * @since 2018-06-01 23:33
 **/
@Configuration
@Slf4j(topic = "JboxRpcServer")
public class RpcServer {

    static final String PATH = "/hessianRpcProvider";

    @Bean(PATH)
    public HessianServiceExporter hessianRpcProvider(ApplicationContext applicationContext) {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(new RpcProcessorImpl(applicationContext));
        exporter.setServiceInterface(RpcProcessor.class);
        log.info("hessian rpc server [{}] starting...", IPv4.getLocalIp());

        return exporter;
    }
}
