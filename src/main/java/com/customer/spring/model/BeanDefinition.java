package com.customer.spring.model;

/**
 * 对于所有Spring的bean 进行封装
 */
public class BeanDefinition {
    private String scope;
    private boolean isLazy;
    private Class type;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        this.isLazy = lazy;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }
}
