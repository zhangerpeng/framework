package com.customer;

import com.customer.domain.service.UserService;
import com.customer.domain.service.UserServiceI;
import com.customer.spring.ApplicationContext;
import com.customer.spring.config.AppConfig;

public class CoustomerSpringTest {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext(AppConfig.class);
        UserServiceI userService = (UserServiceI) applicationContext.getBean("userService");
        userService.test();

    }
}
