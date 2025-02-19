/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.framework;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.debug.FrameworkDebugOptions;
import org.eclipse.osgi.internal.framework.legacy.PackageAdminImpl;
import org.eclipse.osgi.internal.framework.legacy.StartLevelImpl;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.internal.url.EquinoxFactoryManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.storage.BundleLocalizationImpl;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.storage.url.BundleURLConverter;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condition.Condition;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class activates the System Bundle.
 */
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.osgi.service.condition.Condition'", uses = Condition.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.eclipse.osgi.service.environment.EnvironmentInfo'", uses = EnvironmentInfo.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.osgi.service.packageadmin.PackageAdmin'", uses = PackageAdmin.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.osgi.service.startlevel.StartLevel'", uses = StartLevel.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.osgi.service.permissionadmin.PermissionAdmin'", uses = PermissionAdmin.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.osgi.service.condpermadmin.ConditionalPermissionAdmin'", uses = ConditionalPermissionAdmin.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='oorg.osgi.service.resolver.Resolver'", uses = Resolver.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.eclipse.osgi.service.debug.DebugOptions'", uses = DebugOptions.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.urlconversion.URLConverter'",
		"protocol:List<String>='" + BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL + ","
				+ BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL + "'" }, uses = URLConverter.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>='org.eclipse.osgi.service.localization.BundleLocalization'", uses = BundleLocalization.class)

// Currently not a Provid-Capability may add one for each of the 2 reg. Services.
//@Capability(namespace = SERVICE_NAMESPACE, attribute = {
//		"objectClass:List<String>='org.eclipse.osgi.service.debug.DebugOptionsListener'",
//		DebugOptions.LISTENER_SYMBOLICNAME+":List<String>='"+EquinoxContainer.NAME"'"		
//}, uses = DebugOptionsListener.class)

@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.datalocation.Location'",
		"type='" + EquinoxLocations.PROP_USER_AREA+ "'" }, uses = Location.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.datalocation.Location'",
		"type='" + EquinoxLocations.PROP_INSTANCE_AREA+ "'" }, uses = Location.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.datalocation.Location'",
		"type='" + EquinoxLocations.PROP_CONFIG_AREA+ "'" }, uses = Location.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.datalocation.Location'",
		"type='" + EquinoxLocations.PROP_INSTALL_AREA+ "'" }, uses = Location.class)
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
		"objectClass:List<String>='org.eclipse.osgi.service.datalocation.Location'",
		"type='" + EquinoxLocations.PROP_HOME_LOCATION_AREA+ "'" }, uses = Location.class)

@Header(name=Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class SystemBundleActivator implements BundleActivator {
	private EquinoxFactoryManager urlFactoryManager;
	private List<ServiceRegistration<?>> registrations = new ArrayList<>(10);
	private SecurityManager setSecurityManagner;

	@SuppressWarnings("deprecation")
	@Override
	public void start(BundleContext bc) throws Exception {
		registrations.clear();
		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();
		EquinoxContainer equinoxContainer = bundle.getEquinoxContainer();

		equinoxContainer.systemStart(bc);

		EquinoxConfiguration configuration = bundle.getEquinoxContainer().getConfiguration();
		installSecurityManager(configuration);
		equinoxContainer.getLogServices().start(bc);

		urlFactoryManager = new EquinoxFactoryManager(equinoxContainer);
		urlFactoryManager.installHandlerFactories(bc);

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) configuration.getDebugOptions();
		dbgOptions.start(bc);
		Hashtable<String, Object> props = new Hashtable<>(7);
		props.clear();

		props.put(Condition.CONDITION_ID, Condition.CONDITION_ID_TRUE);
		register(bc, Condition.class, Condition.INSTANCE, false, props);

		registerLocations(bc, equinoxContainer.getLocations());
		register(bc, EnvironmentInfo.class, equinoxContainer.getConfiguration(), null);
		PackageAdmin packageAdmin = new PackageAdminImpl(equinoxContainer,
				equinoxContainer.getStorage().getModuleContainer().getFrameworkWiring());
		register(bc, PackageAdmin.class, packageAdmin, null);
		StartLevel startLevel = new StartLevelImpl(
				equinoxContainer.getStorage().getModuleContainer().getFrameworkStartLevel());
		register(bc, StartLevel.class, startLevel, null);

		SecurityAdmin sa = equinoxContainer.getStorage().getSecurityAdmin();
		register(bc, PermissionAdmin.class, sa, null);
		register(bc, ConditionalPermissionAdmin.class, sa, null);


		props.clear();
		props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		register(bc, Resolver.class, new ResolverImpl(new Logger(0), null), false, props);

		register(bc, DebugOptions.class, dbgOptions, null);

		ClassLoader tccl = equinoxContainer.getContextFinder();
		if (tccl != null) {
			props.clear();
			props.put("equinox.classloader.type", "contextClassLoader"); //$NON-NLS-1$ //$NON-NLS-2$
			register(bc, ClassLoader.class, tccl, props);
		}

		props.clear();
		props.put("protocol", new String[] {BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL, BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL}); //$NON-NLS-1$
		register(bc, URLConverter.class, new BundleURLConverter(), props);

		register(bc, BundleLocalization.class, new BundleLocalizationImpl(), null);

		boolean setTccl = "true".equals(bundle.getEquinoxContainer().getConfiguration().getConfiguration("eclipse.parsers.setTCCL", "true")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			register(bc, "javax.xml.parsers.SAXParserFactory", new XMLParsingServiceFactory(true, setTccl), false, null); //$NON-NLS-1$
			register(bc, "javax.xml.parsers.DocumentBuilderFactory", new XMLParsingServiceFactory(false, setTccl), false, null); //$NON-NLS-1$
		} catch (NoClassDefFoundError e) {
			// ignore; on a platform with no javax.xml (Java 8 SE compact1 profile)
		}
		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().startExtensionActivators(bc);

		// Add an options listener; we already read the options on initialization.
		// Here we are just allowing the options to change
		props.clear();
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, EquinoxContainer.NAME);
		register(bc, DebugOptionsListener.class, bundle.getEquinoxContainer().getConfiguration().getDebug(), props);
		register(bc, DebugOptionsListener.class, bundle.getModule().getContainer(), props);
	}

	private void installSecurityManager(EquinoxConfiguration configuration) throws BundleException {
		String frameworkSecurityProp = configuration.getConfiguration(Constants.FRAMEWORK_SECURITY);

		if (System.getSecurityManager() != null) {
			if (Constants.FRAMEWORK_SECURITY_OSGI.equals(frameworkSecurityProp)) {
				throw new BundleException("Cannot specify the \"" + Constants.FRAMEWORK_SECURITY //$NON-NLS-1$
						+ "\" configuration property when a security manager is already installed."); //$NON-NLS-1$
			}
			// otherwise, never do anything if there is an existing security manager
			return;
		}

		String javaSecurityProp = configuration.getConfiguration(EquinoxConfiguration.PROP_EQUINOX_SECURITY,
				configuration.getProperty("java.security.manager")); //$NON-NLS-1$

		SecurityManager toInstall = null;
		if (Constants.FRAMEWORK_SECURITY_OSGI.equals(frameworkSecurityProp)) {
			toInstall = new EquinoxSecurityManager();
		} else if (javaSecurityProp != null) {
			switch (javaSecurityProp) {
			case "disallow": //$NON-NLS-1$
			case "allow": //$NON-NLS-1$
				// in both cases someone set the java.security.manager property but
				// not the osgi specific security properties, just ignore
				break;
			case "": //$NON-NLS-1$
			case "default": //$NON-NLS-1$
				toInstall = new SecurityManager(); // use the default one from java
				break;
			default:
				// try to use a specific classname
				try {
					Class<?> clazz = Class.forName(javaSecurityProp);
					toInstall = (SecurityManager) clazz.getConstructor().newInstance();
				} catch (Throwable t) {
					throw new BundleException("Failed to create security manager", t); //$NON-NLS-1$
				}
				break;
			}
		}

		if (configuration.getDebug().DEBUG_SECURITY)
			Debug.println("Setting SecurityManager to: " + toInstall); //$NON-NLS-1$
		try {
			if (toInstall != null) {
				System.setSecurityManager(toInstall);
			}
		} catch (UnsupportedOperationException e) {
			throw new UnsupportedOperationException(
					"Setting the security manager is not allowed. The java.security.manager=allow java property must be set.", //$NON-NLS-1$
					e);
		}
		setSecurityManagner = toInstall;
	}

	private void registerLocations(BundleContext bc, EquinoxLocations equinoxLocations) {
		Dictionary<String, Object> locationProperties = new Hashtable<>(1);
		Location location = equinoxLocations.getUserLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_USER_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getInstanceLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_INSTANCE_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getConfigurationLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_CONFIG_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
		location = equinoxLocations.getInstallLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_INSTALL_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}

		location = equinoxLocations.getEclipseHomeLocation();
		if (location != null) {
			locationProperties.put("type", EquinoxLocations.PROP_HOME_LOCATION_AREA); //$NON-NLS-1$
			register(bc, Location.class, location, locationProperties);
		}
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();

		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().stopExtensionActivators(bc);

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) bundle.getEquinoxContainer().getConfiguration().getDebugOptions();
		dbgOptions.stop(bc);

		urlFactoryManager.uninstallHandlerFactories();

		// unregister services
		for (ServiceRegistration<?> registration : registrations)
			registration.unregister();
		registrations.clear();
		bundle.getEquinoxContainer().getLogServices().stop(bc);
		unintallSecurityManager();
		bundle.getEquinoxContainer().systemStop(bc);
	}

	private void unintallSecurityManager() {
		if (setSecurityManagner != null && System.getSecurityManager() == setSecurityManagner)
			System.setSecurityManager(null);
		setSecurityManagner = null;
	}

	private void register(BundleContext context, Class<?> serviceClass, Object service, Dictionary<String, Object> properties) {
		register(context, serviceClass.getName(), service, true, properties);
	}

	private void register(BundleContext context, Class<?> serviceClass, Object service, boolean setRanking, Dictionary<String, Object> properties) {
		register(context, serviceClass.getName(), service, setRanking, properties);
	}

	private void register(BundleContext context, String serviceClass, Object service, boolean setRanking, Dictionary<String, Object> properties) {
		if (properties == null)
			properties = new Hashtable<>();
		if (setRanking) {
			properties.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		}
		properties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		registrations.add(context.registerService(serviceClass, service, properties));
	}
}
