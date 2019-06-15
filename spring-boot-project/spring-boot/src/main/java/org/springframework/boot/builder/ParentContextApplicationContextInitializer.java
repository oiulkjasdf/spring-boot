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

package org.springframework.boot.builder;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

/**
 * {@link ApplicationContextInitializer} for setting the parent context. Also publishes
 * {@link ParentContextAvailableEvent} when the context is refreshed to signal to other
 * listeners that the context is available and has a parent.
 *
 * @author Dave Syer
 */

/*父类上下文 初始化  实现  spring core 的  application 初始化  泛型是 配置类 上下文  实现了 排序接口*/
public class ParentContextApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/*私有  最高级 排序*/
	private int order = Ordered.HIGHEST_PRECEDENCE;

	/*私有  不允许更改的  上下文父类*/
	private final ApplicationContext parent;
	/*构造*/
	public ParentContextApplicationContextInitializer(ApplicationContext parent) {
		this.parent = parent;
	}
	/*开闭原则*/
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	/*初始化*/
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (applicationContext != this.parent) {
			/*将默认父类 放进去*/
			applicationContext.setParent(this.parent);
			/*上面文监听器 事件 构造者 实例*/
			applicationContext.addApplicationListener(EventPublisher.INSTANCE);
		}
	}
	/*实现  app 监听器  上下文 刷新事件*/
	private static class EventPublisher implements ApplicationListener<ContextRefreshedEvent>, Ordered {

		private static final EventPublisher INSTANCE = new EventPublisher();

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {

			/*事件上下文*/
			ApplicationContext context = event.getApplicationContext();
			/*属于 配置上下文  并且 上下文 等于 事件源*/
			if (context instanceof ConfigurableApplicationContext && context == event.getSource()) {
				/*发表 上下文事件  */
				context.publishEvent(new ParentContextAvailableEvent((ConfigurableApplicationContext) context));
			}
		}

	}

	/**
	 * {@link ApplicationEvent} fired when a parent context is available.
	 */
	@SuppressWarnings("serial")
	/*父类上下文 事件  继承 app 事件*/
	public static class ParentContextAvailableEvent extends ApplicationEvent {
		/*构造*/
		public ParentContextAvailableEvent(ConfigurableApplicationContext applicationContext) {
			super(applicationContext);
		}
		/*获取上下文  */
		public ConfigurableApplicationContext getApplicationContext() {
			return (ConfigurableApplicationContext) getSource();
		}

	}

}
