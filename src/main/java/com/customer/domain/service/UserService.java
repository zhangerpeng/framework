package com.customer.domain.service;

import com.customer.spring.annotation.Autowired;
import com.customer.spring.annotation.Component;
import com.customer.spring.model.BeanNameAware;

@Component
public class UserService implements UserServiceI, BeanNameAware {

    private String beanName;

    @Autowired
    private OrderService orderService;

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void test() {
        System.out.println("we create a bean with name"+beanName);
    }
}
