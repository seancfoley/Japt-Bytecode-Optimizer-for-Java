package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;

/**
 * @author sfoley
 *
 */
class RelatedNameCollectorCreator {

	private Map relatedNameCollectors = new HashMap();
	private NameHandler nameHandler;
	private JaptRepository classCollectorCreator;
		
	/**
	 * Constructor for RelatedClassCollectorCreator.
	 */
	public RelatedNameCollectorCreator(JaptRepository classCollectorCreator, NameHandler nameHandler) {
		this.nameHandler = nameHandler;
		this.classCollectorCreator = classCollectorCreator;
	}
	
	public RelatedNameCollector getRelatedNameCollector(BT_Class clazz) {
		RelatedNameCollector result = (RelatedNameCollector) relatedNameCollectors.get(clazz);
		if(result == null) {
			RelatedClassCollector classCollector = classCollectorCreator.getRelatedClassCollector(clazz);
			result = new RelatedNameCollector(nameHandler);
			classCollector.visitClasses(result);
		}
		return result;
	}

}
