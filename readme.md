## Spring 底层原理
### 前提概要
#### CGLIB

#### JDK 动态代理

#### 代理对象与实际对象的区别

### Spring bean 创建的生命周期[key steps]

（无参构造方法）推断构造器--》创建原始对象----》依赖注入（属性赋值）---》初始化[【初始化前】【初始化】【初始化后】(APO)] ---》(代理对象)--》bean 对象

1. 构造函数推断
  * Class 中存在多个构造器的场合，若明示存在无参构造函数，则默认使用。
  * 若存在多个有参的构造器，若没有明示指定，则启动时，因无法识别使用那个构造函数则程序无法实例化。使用注解的机制明示初始化时所使用的构造器【@autowired】

 ```
package com.spring.basic.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class UserService implements InitializingBean {

    private OrderService orderService;

    // 告知初始化时，所应用的构造器
    @Autowired
    public UserService(OrderService orderService) {
        this.orderService = orderService;
        System.out.println("orderService " + orderService);
    }
    
     public UserService(OrderService orderService,String userName) {
        this.orderService = orderService;
        System.out.println("orderService " + orderService);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
// 在Spring 中构造器中的参数进行初始化时，默认从容器中进行获取。
// 获取的方式，默认 by type 若同一个type 具有多个实例，则接着使用by name 的方式进行bean的获取
 ```
2. 依赖对象赋值
3. 初始化前--注解[@PostConstructor]
   ```
   The PostConstruct annotation is used on a method that needs to be
   executed after dependency injection is done to perform any
   initialization. This method MUST be invoked before the class is put into service
   ```
   核心原理：初始化bean对象前，遍历其method 判定其是否有注解PostConstructor.自动执行该注解所在方法的逻辑
4. 初始化-- 实现接口[InitializingBean]
   ```
   void afterPropertiesSet() throws Exception;
    // 核心：
   （InitializingBean）objectBean.afterPropertiesSet()
   ```
5. 初始化后 [AOP]
   是否进行AOP 的判断
   1. 启动加载时，扫描所有的aspect的bean 中的方法
   2. 对于有切点绑定的方法bean 则需要进行代理对象
   3. 存储代理方法

### Spring中的事务管理
1. 事务是是作用在代理对象时，事务才可生效
2. 事务无效场景
   * 同一个service 中两个带有事务注解方法调用（直接调用）
   * 初始化事务连接时，构建的连接对象必须是同一个【事务@Configuration】

### Spring bean 删除的生命周期


### 手写模拟Spring底层原理
#### Spring IOC
##### 前提-类加载器

#### IOC 流程概要
1. 定义注解 【@componentScan(path),@component】
   * componentScan 定义其扫描路路径
   * component 声明该类为Spring 托管的bean
2. 定义启动类
   * 启动类作为appicationContext的参数
   * 启动类上应用注解[@componentScan]标明所要扫描的path
3. classLoader 加载扫描路径下的文件
   * 判断类上是否有注解[@component]
   * 判定该类是否设定了Scope[prototype,singleton]
   * 构建beandefinition 对象
     ##### BeanDefinition
      关键点：BeanDefinition 用于对于spring 所管理bean特性的封装
      1. type -- 当前所扫描类型
      2. scope-- 当前bean的单列，多例
      3. isLazy -- 是否懒加载
 

4. 构建Map 用于作为IOC 容器
   * key --beanName [获取component 注解的value]
   * value --构建的beandefinition 对象

5. 构建单例池SingletonMap 



##### 从IOC 中获取Bean 对象
getBean() 流程
1. beanName 从beanDefinitionMap 中获取对应的beanDefinition
2. 判定beanDefinition 的scope 若为prototype 则创建bean 反之，从singletonMap 中获取

#### 依赖注入概要流程
1. 获取对象实例
2. 反射获取实例属性
3. 获取属性名称
4. 依据获取的属性名称，在IOC容器中调用getBean的方法，获取依赖bean.
   在获取bean时，有可能因为扫描顺序的问题，其依赖bean此时尚未被初始化到IOC 容器中，则此时需要创建bean,并将其put到容器中

#### 初始化流程
Interface InitializingBean中方法
```

```

#### BeanPostProcesser【AOP的实现基础】
1. 特性： 存在两个方法 
   * postProcessorBeforeInitialzation[初始化前]
   * postProcessorAfterInitialzation[初始化后]--AOP的实现
2. 创建List 缓存BeanPostProcesser
   
3. BeanNameWare --回调 【依赖注入之后，初始化之前】



  
  
