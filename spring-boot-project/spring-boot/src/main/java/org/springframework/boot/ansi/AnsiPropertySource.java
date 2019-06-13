/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.ansi;

import java.util.*;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertyResolver} for {@link AnsiStyle}, {@link AnsiColor} and
 * {@link AnsiBackground} elements. Supports properties of the form
 * {@code AnsiStyle.BOLD}, {@code AnsiColor.RED} or {@code AnsiBackground.GREEN}. Also
 * supports a prefix of {@code Ansi.} which is an aggregation of everything (with
 * background colors prefixed {@code BG_}).
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
/*继承了spring框架  配置元 */
public class AnsiPropertySource extends PropertySource<AnsiElement> {

	/*  list  继承了  Iterable  这货可以开始 foreach 了 */
	private static final Iterable<MappedEnum<?>> MAPPED_ENUMS;

	/*static 我能理解  构造函数前 执行   下面这一大串  刷新了我的认知  跟本看不懂啊   */
	//fixme  要学习的 这块写的 号高深
	static {
		List<MappedEnum<?>> enums = new ArrayList<>();
		enums.add(new MappedEnum<>("AnsiStyle.", AnsiStyle.class));
		enums.add(new MappedEnum<>("AnsiColor.", AnsiColor.class));
		enums.add(new MappedEnum<>("AnsiBackground.", AnsiBackground.class));
		enums.add(new MappedEnum<>("Ansi.", AnsiStyle.class));
		enums.add(new MappedEnum<>("Ansi.", AnsiColor.class));
		enums.add(new MappedEnum<>("Ansi.BG_", AnsiBackground.class));
		MAPPED_ENUMS = Collections.unmodifiableList(enums);
	}

	private final boolean encode;

	/**
	 * Create a new {@link AnsiPropertySource} instance.
	 * @param name the name of the property source
	 * @param encode if the output should be encoded
	 */
	public AnsiPropertySource(String name, boolean encode) {
		super(name);
		this.encode = encode;
	}

	@Override
	/*能看懂一丢丢   把初始化的  那个enum  for循环  然后 严谨的校验了一下  再 根据是否需要编码 来去是否编码*/
	public Object getProperty(String name) {
		if (StringUtils.hasLength(name)) {
			for (MappedEnum<?> mappedEnum : MAPPED_ENUMS) {
				if (name.startsWith(mappedEnum.getPrefix())) {
					String enumName = name.substring(mappedEnum.getPrefix().length());
					for (Enum<?> ansiEnum : mappedEnum.getEnums()) {
						/*这里是把里面的set 集合中的 和外面的prefix 的 做equals*/
						if (ansiEnum.name().equals(enumName)) {
							if (this.encode) {
								return AnsiOutput.encode((AnsiElement) ansiEnum);
							}
							return ansiEnum;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Mapping between an enum and the pseudo property source.
	 */
	/*Enum  这货  、、、、   玄学 来了  enum
	*   和  enumset .allof */
	private static class MappedEnum<E extends Enum<E>> {

		private final String prefix;

		private final Set<E> enums;

		MappedEnum(String prefix, Class<E> enumType) {
			this.prefix = prefix;
			this.enums = EnumSet.allOf(enumType);

		}

		public String getPrefix() {
			return this.prefix;
		}

		public Set<E> getEnums() {
			return this.enums;
		}

	}

}
