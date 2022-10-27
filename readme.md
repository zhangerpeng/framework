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
package com.customer.spring.model;

public interface InitializingBean {
    void afterPropertiesSet();
}

```

#### BeanPostProcesser【AOP的实现基础】
1. 特性： 存在两个方法 
   * postProcessorBeforeInitialzation[初始化前]
   * postProcessorAfterInitialzation[初始化后]--AOP的实现

```
package com.customer.spring.model;

public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean,String beanName){
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean,String beanName) {
        return bean;
    }
}

```
2. 创建List 缓存BeanPostProcesser
   

3. BeanNameWare --回调 【依赖注入之后，初始化之前】

```
package com.customer.spring.model;

public interface BeanNameAware {
    void setBeanName(String name);
}

```

Core code
```
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

```



### Spring bean 的生命周期源码解析
#### Bean的生成过程
##### Scan过程
1. 核心类[ClassPathBeanDefinitionScanner]
```
/**
	 * Perform a scan within the specified base packages,
	 * returning the registered bean definitions.
	 * <p>This method does <i>not</i> register an annotation config processor
	 * but rather leaves this up to the caller.
	 * @param basePackages the packages to check for annotated classes
	 * @return set of beans registered if any for tooling registration purposes (never {@code null})
	 */
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		for (String basePackage : basePackages) {
         // 指定package 下的所有的候选bean 对象，封装称为BeanDefinition对象
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
         // 遍历Beandefinition 对象
			for (BeanDefinition candidate : candidates) {
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
            // 获取beanName
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				if (candidate instanceof AbstractBeanDefinition) {
               // 赋值默认值
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
            // Check the given candidate's bean name, determining whether the corresponding bean definition needs to be registered or conflicts with an existing definition
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}
```

2. BeanName 构建[AnnotationBeanNameGenerator]

```
@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		if (definition instanceof AnnotatedBeanDefinition) {
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			if (StringUtils.hasText(beanName)) {
				// Explicit bean name found.
				return beanName;
			}
		}
		// Fallback: generate a unique default bean name.
		return buildDefaultBeanName(definition, registry);
	}

```

3. 加快扫描启动机制[ClassPathScanningCandidateComponentProvider]
```
/**
	 * Scan the class path for candidate components.
	 * @param basePackage the package to check for annotated classes
	 * @return a corresponding Set of autodetected bean definitions
	 */
	public Set<BeanDefinition> findCandidateComponents(String basePackage) {=
      // 使用索引机制，若在resource 目录下存在文件spring.components文件，可在该文件中明示要注入bean的路径
      // key --类 value --对应的注解
		if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
			return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
		}
		else {
			return scanCandidateComponents(basePackage);
		}
	}

```
扫描索引的实现



4. Spring bean 的register[DefaultListableBeanFactory]

```
@Override
	public void preInstantiateSingletons() throws BeansException {
		if (logger.isTraceEnabled()) {
			logger.trace("Pre-instantiating singletons in " + this);
		}

		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

		// Trigger initialization of all non-lazy singleton beans...
		for (String beanName : beanNames) {
         // 场景：若存在beandefination的父类为abstract, 在子类的beandefinition的属性合并父类
         // 作用对于部分属性进行统一封装
         //<bean id = 'user' class =com.model.User' abstract='true' scope='prototype'>
         //<bean id='userService' class='com.service.UserService' parent='user' scopr='singleton'>
			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
			if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            // 判断是否是Factorybean,若想获取factorybean本身的bean，则 applciationContext 中使用 getBean("&factoryBeanName")
            // 若想获取FactoryBean 中的getObject方法中的bean 则getBean("factoryBeanname")
				if (isFactoryBean(beanName)) {
               // 将当前的factoryBean 放到singletonObjects
					Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
               // 若实现了SmartFactoryBean 则存储getBean的实例作为其单例池
					if (bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isEagerInit()) {
						getBean(beanName);
					}
				}
				else {
					getBean(beanName);
				}
			}
		}

		// Trigger post-initialization callback for all applicable beans...
		for (String beanName : beanNames) {
         // 获取单例对象
			Object singletonInstance = getSingleton(beanName);
         // 生命周期是否实现了接口 SmartInitializingSingleton[所有的单例bean实例化后才会触发] 
			if (singletonInstance instanceof SmartInitializingSingleton smartSingleton) {
				StartupStep smartInitialize = this.getApplicationStartup().start("spring.beans.smart-initialize")
						.tag("beanName", beanName);
				smartSingleton.afterSingletonsInstantiated();
				smartInitialize.end();
			}
		}
	}

```



  
  
