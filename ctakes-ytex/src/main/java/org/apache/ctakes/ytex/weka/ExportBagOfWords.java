package org.apache.ctakes.ytex.weka;

import java.io.IOException;

import org.apache.ctakes.ytex.kernel.BagOfWordsExporter;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;


/**
 * Export bag of words using the queries specified in the given property/xml
 * file. Delegate to BagOfWordsExporter
 * 
 * @author vijay
 * 
 */
public class ExportBagOfWords {

	public static void main(String args[]) throws IOException {
		BagOfWordsExporter exporter = (BagOfWordsExporter) KernelContextHolder
				.getApplicationContext().getBean("wekaBagOfWordsExporter");
		exporter.exportBagOfWords(args[0]);
	}
}
