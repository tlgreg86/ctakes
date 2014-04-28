package org.apache.ctakes.ytex.kernel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

public class KernelContextHolder {
	static ApplicationContext kernelApplicationContext = null;
	static {
		String beanRefContext = "classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml";
		kernelApplicationContext = (ApplicationContext) ContextSingletonBeanFactoryLocator
				.getInstance(beanRefContext).useBeanFactory(
						"kernelApplicationContext").getFactory();
	}

	public static ApplicationContext getApplicationContext() {
		return kernelApplicationContext;
	}

}
