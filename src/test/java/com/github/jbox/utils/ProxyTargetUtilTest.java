package com.github.jbox.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import com.github.jbox.caces.service.HelloWorldService;
import com.github.jbox.caces.service.impl.HelloWorldServiceImpl;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ClassUtils;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/31 19:04:00.
 */
public class ProxyTargetUtilTest {

    @Test
    public void testCGLIB() {

        HelloWorldServiceImpl impl = new HelloWorldServiceImpl();
        // 1. 创建Enhancer对象
        Enhancer enhancer = new Enhancer();

        // 2. cglib创建代理, 对目标对象创建子对象
        enhancer.setSuperclass(impl.getClass());

        // 3. 传入回调接口, 对目标增强
        enhancer.setCallback(new MethodInterceptor() {

            private Object target = impl;

            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy)
                throws Throwable {
                return "ss";
            }
        });

        Object o = enhancer.create();

        System.out.println(ClassUtils.isCglibProxy(o));
        Object cglibProxyTarget = ProxyTargetUtils.getProxyTarget(o);

        Assert.assertTrue(cglibProxyTarget == impl);
    }

    @Test
    public void testJdkProxy() {
        Object _target = new Object();

        Object jdkProxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[] {HelloWorldService.class},
            new InvocationHandler() {
                private Object target = _target;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return null;
                }
            });

        Assert.assertTrue(ProxyTargetUtils.getJdkProxyTarget(jdkProxy) == _target);
    }

    @Test
    public void testCGLibProxy() {
        String _target = "nihao";

        Object proxy = net.sf.cglib.proxy.Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[] {HelloWorldService.class}, new net.sf.cglib.proxy.InvocationHandler() {
                String target = _target;

                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    return null;
                }
            });

        Assert.assertTrue(Objects.equals(_target, ProxyTargetUtils.getProxyTarget(proxy)));

    }
}
