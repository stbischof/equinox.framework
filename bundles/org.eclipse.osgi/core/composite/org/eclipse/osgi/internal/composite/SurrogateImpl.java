package org.eclipse.osgi.internal.composite;

import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.module.ResolverBundle;
import org.eclipse.osgi.service.internal.composite.CompositeModule;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.SurrogateBundle;

public class SurrogateImpl extends CompositeBase implements SurrogateBundle {

	public SurrogateImpl(BundleData bundledata, org.eclipse.osgi.framework.internal.core.Framework framework) throws BundleException {
		super(bundledata, framework);
	}

	protected Framework findCompanionFramework(org.eclipse.osgi.framework.internal.core.Framework thisFramework, BundleData thisData) throws BundleException {
		// just get the property which was set when creating the child framework
		return (Framework) FrameworkProperties.getProperties().get(PROP_PARENTFRAMEWORK);
	}

	public BundleContext getCompositeBundleContext() {
		Bundle composite = getCompanionBundle();
		return composite == null ? null : composite.getBundleContext();
	}

	protected boolean isSurrogate() {
		return true;
	}

	public boolean giveExports(ExportPackageDescription[] matchingExports) {
		if (matchingExports == null) {
			// set the surrogate to disabled to prevent resolution this go around
			CompositeHelper.setDisabled(true, getBundleDescription());
			// refresh the composite bundle (in the parent framework) asynchronously and enable it
			// should only do this if the composite is not in the process of refreshing this 
			// surrogate bundle
			if (refreshing.get() == null)
				((CompositeModule) getCompanionBundle()).refreshContent(false);
			return true;
		}
		return validExports(matchingExports);
	}

	private boolean validExports(ExportPackageDescription[] matchingExports) {
		// make sure each matching exports matches the export signature of the composite
		CompositeModule composite = (CompositeModule) getCompanionBundle();
		BundleDescription childDesc = composite.getCompositeDescription();
		ExportPackageDescription[] childExports = childDesc.getExportPackages();
		for (int i = 0; i < matchingExports.length; i++) {
			for (int j = 0; j < childExports.length; j++) {
				if (matchingExports[i].getName().equals(childExports[j].getName())) {
					if (!validateExport(matchingExports[i], childExports[j]))
						return false;
					continue;
				}
			}
		}
		return true;
	}

	private boolean validateExport(ExportPackageDescription matchingExport, ExportPackageDescription childExport) {
		Version matchingVersion = matchingExport.getVersion();
		Version childVersion = childExport.getVersion();
		if (!childVersion.equals(Version.emptyVersion) && !matchingVersion.equals(childVersion))
			return false;
		if (!ResolverBundle.equivalentMaps(childExport.getAttributes(), matchingExport.getAttributes(), false))
			return false;
		if (!ResolverBundle.equivalentMaps(childExport.getDirectives(), matchingExport.getDirectives(), false))
			return false;
		return true;
	}

	protected void startHook() {
		((CompositeModule) getCompanionBundle()).started(this);
	}

	protected void stopHook() {
		((CompositeModule) getCompanionBundle()).stopped(this);
	}
}
