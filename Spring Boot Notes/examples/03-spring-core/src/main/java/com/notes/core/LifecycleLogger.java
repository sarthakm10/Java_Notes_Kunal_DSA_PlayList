package com.notes.core;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/** Spring calls this for EVERY bean it creates: the hook that @Transactional, AOP etc. are built on. */
@Component
public class LifecycleLogger implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().getPackageName().equals("com.notes.core")) {
            System.out.println("  [bpp] ready: " + beanName);
        }
        return bean;           // could return a proxy (wrapper) instead
    }
}
