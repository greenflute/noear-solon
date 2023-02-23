package org.noear.solon.aspect;

import org.noear.solon.Utils;
import org.noear.solon.core.AopContext;
import org.noear.solon.aspect.asm.AsmProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Bean 调用处理
 *
 * @author noear
 * @since 1.5
 * */
public class BeanInvocationHandler implements InvocationHandler {
    private Object target;
    private Object proxy;
    private InvocationHandler handler;
    private final AopContext context;

    /**
     * @since 1.6
     */
    public BeanInvocationHandler(AopContext context, Object bean, InvocationHandler handler) {
        this(context, bean.getClass(), bean, handler);
    }

    /**
     * @since 1.6
     * @since 2.2
     */
    public BeanInvocationHandler(AopContext context, Class<?> clazz, Object target, InvocationHandler handler) {
        this.context = context;
        this.target = target;
        this.handler = handler;

        //支持APT (支持 Graalvm Native  打包)
        String proxyClassName = clazz.getName() + "$$SolonProxy";
        this.proxy = Utils.newInstance(context.getClassLoader(), proxyClassName);

        if (this.proxy == null) {
            //支持ASM（兼容旧的包，不支持 Graalvm Native  打包）
            this.proxy = AsmProxy.newProxyInstance(context, this, clazz);
        }
    }

    public Object getProxy() {
        return proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (handler == null) {
            method.setAccessible(true);

            Object result = context.methodGet(method).invokeByAspect(target, args);

            return result;
        } else {
            return handler.invoke(target, method, args);
        }
    }
}
