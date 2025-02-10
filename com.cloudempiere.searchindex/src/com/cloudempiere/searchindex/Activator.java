/**
 * 
 */
package com.cloudempiere.searchindex;

import org.adempiere.base.Core;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.osgi.framework.BundleContext;

/**
 * @author developer
 *
 */
public class Activator extends Incremental2PackActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		Core.getMappedModelFactory().scan(context, "com.cloudempiere.searchindex.model");

		super.start(context);
	}
}
