package xyz.herz.ep.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 跨模块静态取 Spring Bean 的工具。
 * <p>当 erupt 通过反射 new 一个非 Spring 托管的对象(比如实体内的 static Proxy 类)时,
 * 可通过 {@link #getBean(Class)} 或 {@link #getBean(String)} 获得业务 Bean。
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext CTX;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CTX = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (CTX == null) throw new IllegalStateException("SpringContextHolder 尚未被 Spring 初始化," +
                "请确认启动类能扫描到 xyz.herz.ep.common.spring 包");
        return CTX.getBean(clazz);
    }

    public static Object getBean(String name) {
        if (CTX == null) throw new IllegalStateException("SpringContextHolder 尚未被 Spring 初始化");
        return CTX.getBean(name);
    }
}
