package org.apache.ctakes.ytex.umls.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

/**
 * this test only works if MRCONSO is in the database (not the case for default
 * test settings). In case MRCONSO is not there, catch exception and ignore.
 * 
 * @author vgarla
 * 
 */
public class UMLSDaoTest {
	private static final Logger log = Logger.getLogger(UMLSDaoTest.class);
	UMLSDao umlsDao = null;

	@Before
	public void setUp() throws Exception {
		ApplicationContext appCtx = (ApplicationContext) ContextSingletonBeanFactoryLocator
				.getInstance(
						"classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml")
				.useBeanFactory("kernelApplicationContext").getFactory();
		umlsDao = appCtx.getBean(UMLSDao.class);
	}

	@Test
	public void testGetAllAuiStr() {
		try {
			List<Object[]> auis = umlsDao.getAllAuiStr("");
			Assert.assertNotNull(auis);
			log.debug("testGetAllAuiStr()" + auis.size());
		} catch (Exception e) {
			log.warn(
					"sql exception - mrconso probably doesn't exist, check error",
					e);
		}
	}

}
