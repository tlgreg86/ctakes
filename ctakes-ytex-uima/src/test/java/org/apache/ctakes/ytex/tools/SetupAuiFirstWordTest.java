package org.apache.ctakes.ytex.tools;

import org.junit.Assert;
import org.junit.Test;

public class SetupAuiFirstWordTest {

	@Test
	public void test() throws Exception {
		SetupAuiFirstWord fw = new SetupAuiFirstWord();
		System.out.println(fw.tokenizeStr("1tok", "heart"));
		Assert.assertTrue(fw.tokenizeStr("2tok", "heart attack").getFword().equals("heart"));
	}

}
