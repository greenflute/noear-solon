package org.noear.solon.scheduling.simple.integration;

import org.noear.solon.Utils;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.scheduling.ScheduledException;

import java.lang.reflect.Method;

/**
 * 方法运行器（支持非单例）
 *
 * @author noear
 * @since 1.6
 */
public class MethodRunnable implements Runnable {
    private BeanWrap target;
    private Method method;

    public MethodRunnable(BeanWrap target, Method method) {
        this.target = target;
        this.method = method;
    }

    @Override
    public void run() {
        try {
            method.invoke(target.raw());
        } catch (Throwable e) {
            e = Utils.throwableUnwrap(e);
            throw new ScheduledException(e);
        }
    }
}
