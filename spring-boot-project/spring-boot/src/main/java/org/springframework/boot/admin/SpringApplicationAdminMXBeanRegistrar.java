/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.admin;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

/**
 * Register a {@link SpringApplicationAdminMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.3.0
 */


/*
 * @Description admin注册者 实现 了 可以set 上下文 ， 啥啥监听器 设置环境  初始化 bean   可销毁bean
 * @author suxxin
 * @date 2019/6/12
 * @param
 * @return
 */
public class SpringApplicationAdminMXBeanRegistrar implements ApplicationContextAware, GenericApplicationListener,
		EnvironmentAware, InitializingBean, DisposableBean {

	/*log*/
	private static final Log logger = LogFactory.getLog(SpringApplicationAdmin.class);
	/*配置上下文*/
	private ConfigurableApplicationContext applicationContext;
	/*环境*/
	private Environment environment = new StandardEnvironment();
	/*obj名字*/
	private final ObjectName objectName;
	/*初始化还没准备好*/
	private boolean ready = false;
	/*默认不是web*/
	private boolean embeddedWebApplication = false;
	/*注册名 */
	public SpringApplicationAdminMXBeanRegistrar(String name) throws MalformedObjectNameException {
		this.objectName = new ObjectName(name);
	}

	@Override
	/*设置上下文  由ApplicationContextAware 实现来得方法*/
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		/*断言 属于ConfigurableApplicationContext 子类  否则抛异常*/
		Assert.state(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		/*上下文配置为 ConfigurableApplicationContext*/
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	@Override
	/*设置环境  由EnvironmentAware  实现而来*/
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	/*是否支持事件类型  由 继承GenericApplicationListener  实现而来*/
	/*fixme  看不懂了*/
	public boolean supportsEventType(ResolvableType eventType) {
		Class<?> type = eventType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationReadyEvent.class.isAssignableFrom(type)
				|| WebServerInitializedEvent.class.isAssignableFrom(type);
	}

	@Override
	/*接口有default 方法 继承GenericApplicationListener  实现而来 不用重写  */
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	/*event 事件 */
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationReadyEvent) {
			onApplicationReadyEvent((ApplicationReadyEvent) event);
		}
		if (event instanceof WebServerInitializedEvent) {
			onWebServerInitializedEvent((WebServerInitializedEvent) event);
		}
	}

	@Override
	/*排序      int HIGHEST_PRECEDENCE = -2147483648;  int最小值 */
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/*判断地址相同 则准备好了？*/
	void onApplicationReadyEvent(ApplicationReadyEvent event) {
		if (this.applicationContext.equals(event.getApplicationContext())) {
			this.ready = true;
		}
	}
	/*判断地址相同 则准备好了？*/
	void onWebServerInitializedEvent(WebServerInitializedEvent event) {
		if (this.applicationContext.equals(event.getApplicationContext())) {
			this.embeddedWebApplication = true;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		/*jdk 的 。。。   要看下*/
		/*fixme*/
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		/*注册 admin obj name 为name*/
		server.registerMBean(new SpringApplicationAdmin(), this.objectName);
		if (logger.isDebugEnabled()) {
			logger.debug("Application Admin MBean registered with name '" + this.objectName + "'");
		}
	}

	@Override
	/*销毁*/
	public void destroy() throws Exception {
		/*取消注册*/
		ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
	}

	/*同包下 上面那个接口 */
	private class SpringApplicationAdmin implements SpringApplicationAdminMXBean {

		@Override
		/*返回是否准备好*/
		public boolean isReady() {
			return SpringApplicationAdminMXBeanRegistrar.this.ready;
		}

		@Override
		/*是否是web环境*/
		public boolean isEmbeddedWebApplication() {
			return SpringApplicationAdminMXBeanRegistrar.this.embeddedWebApplication;
		}

		@Override
		/*获取配置信息*/
		public String getProperty(String key) {
			return SpringApplicationAdminMXBeanRegistrar.this.environment.getProperty(key);
		}

		@Override
		/*关机*/
		public void shutdown() {
			logger.info("Application shutdown requested.");
			/*关闭*/
			SpringApplicationAdminMXBeanRegistrar.this.applicationContext.close();
		}

	}

}
