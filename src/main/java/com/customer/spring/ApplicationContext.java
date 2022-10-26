package com.customer.spring;


import com.customer.spring.annotation.Autowired;
import com.customer.spring.annotation.Component;
import com.customer.spring.annotation.ComponentScan;
import com.customer.spring.annotation.Scope;
import com.customer.spring.model.BeanDefinition;
import com.customer.spring.model.BeanNameAware;
import com.customer.spring.model.BeanPostProcessor;
import com.customer.spring.model.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {

    private Class appConfig;

    private Map<String, Object> singletonObjects = new HashMap<>();
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ApplicationContext(Class appConfig) {
        this.appConfig = appConfig;
        scan(appConfig);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);

            }
        }

    }

    /**
     * 构建BeanDefinitionMap
     *
     * @param appConfig
     */
    private void scan(Class appConfig) {

        if (appConfig.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = (ComponentScan) appConfig.getAnnotation(ComponentScan.class);
            String path = componentScan.value();
            String classLoadPath = path.replace(".", "/");
            ClassLoader classLoader = ApplicationContext.class.getClassLoader();
            URL resources = classLoader.getResource(classLoadPath);
            File file = new File(resources.getFile());

            if (file.isDirectory()) {
                for (File fe : file.listFiles()) {
                    String absolutePath = fe.getAbsolutePath();
                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    try {
                        Class<?> clazz = classLoader.loadClass(absolutePath.replace("/", "."));
                        if (clazz.isAnnotationPresent(Component.class)) {
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }

                            Component componentAnnotation = clazz.getAnnotation(Component.class);

                            String beanName = componentAnnotation.value();
                            if (beanName.isEmpty()) {
                                beanName = Introspector.decapitalize(clazz.getSimpleName());
                            }

                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                String value = scopeAnnotation.value();
                                beanDefinition.setScope(value);
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        }


    }

    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new RuntimeException();
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if ("singleton".equals(beanDefinition.getScope())) {
            Object singletonBean = singletonObjects.get(beanName);
            if (null == singletonBean) {
                singletonBean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, singletonBean);
            }
            return singletonBean;
        } else {
            Object prototype = createBean(beanName, beanDefinition);
            return prototype;
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        Object instance = null;
        try {
            /**
             * bean 的生命周期
             * 1. 构造函数的推断 clazz 获取所有的构造函数。若构造函数存在多个，判断其上有注解，则表明其为默认的构造器
             * 2.依赖注入，获取属性，对属性进行赋值
             * 3. 初始化
             *    3.1 初始化前 判断当前类是否实现了接口postBeanProcess
             *    3.2 初始化
             *    3.3 初始化后
             */
            // 创建原始对象
            instance = clazz.getConstructor().newInstance();
            // 依赖注入
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }

            if (instance instanceof BeanNameAware) {
                ( (BeanNameAware) instance ).setBeanName(beanName);
            }
//            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
//                // 调用其方法进行初始化之前的操作
//                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
//            }


            if (instance instanceof InitializingBean) {
                // 调用其方法，进行初始化
                ( (InitializingBean) instance ).afterPropertiesSet();
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                // 初始化后，一般可用于生成代理对象
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return instance;
    }
}
