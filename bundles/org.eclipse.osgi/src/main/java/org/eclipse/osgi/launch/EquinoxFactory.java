/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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
package org.eclipse.osgi.launch;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.Map;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * The framework factory implementation for the Equinox framework.
 * @since 3.5
 */
@ServiceProvider(value = FrameworkFactory.class, attribute = "service.vendor:String='eclipse.org'", resolution = Resolution.OPTIONAL)
@ServiceProvider(value = ConnectFrameworkFactory.class, attribute = "service.vendor:String='eclipse.org'", resolution = Resolution.OPTIONAL)
public class EquinoxFactory implements FrameworkFactory, ConnectFrameworkFactory {

	@Override
	public Framework newFramework(Map<String, String> configuration) {
		return newFramework(configuration, null);
	}

	@Override
	public Framework newFramework(Map<String, String> configuration, ModuleConnector moduleConnector) {
		return new Equinox(configuration, moduleConnector);
	}
}
