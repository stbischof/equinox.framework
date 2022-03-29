/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.internal.serviceregistry;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.osgi.framework.*;

/**
 * This class represents the use of a service by a bundle. One is created for each
 * service acquired by a bundle.
 *
 * <p>
 * This class manages a prototype service factory.
 *
 * @ThreadSafe
 */
public class PrototypeServiceFactoryUse<S> extends ServiceFactoryUse<S> {
	/** Service objects returned by PrototypeServiceFactory.getService() and their use count. */
	/* @GuardedBy("this") */
	private final Map<S, AtomicInteger> serviceObjects;

	/**
	 * Constructs a service use encapsulating the service object.
	 *
	 * @param   context bundle getting the service
	 * @param   registration ServiceRegistration of the service
	 */
	PrototypeServiceFactoryUse(BundleContextImpl context, ServiceRegistrationImpl<S> registration) {
		super(context, registration);
		this.serviceObjects = new IdentityHashMap<>();
	}

	/**
	 * Create a new service object for the service.
	 *
	 * <p>
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("this") */
	@Override
	S newServiceObject() {
		assert Thread.holdsLock(this);
		if (debug.DEBUG_SERVICES) {
			Debug.println("getServiceObject[factory=" + registration.getBundle() + "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		final S service = factoryGetService();
		if (service == null) {
			return null;
		}
		AtomicInteger useCount = serviceObjects.get(service);
		if (useCount == null) {
			serviceObjects.put(service, new AtomicInteger(1));
		} else {
			if (useCount.getAndIncrement() == Integer.MAX_VALUE) {
				useCount.getAndDecrement();
				throw new ServiceException(Msg.SERVICE_USE_OVERFLOW);
			}
		}
		return service;
	}

	/**
	 * Release a service object for the service.
	 *
	 * @param service The service object to release.
	 * @return true if the service was released; otherwise false.
	 * @throws IllegalArgumentException If the specified service was not
	 *         provided by this object.
	 */
	/* @GuardedBy("this") */
	@Override
	boolean releaseServiceObject(final S service) {
		assert Thread.holdsLock(this);
		if ((service == null) || !serviceObjects.containsKey(service)) {
			throw new IllegalArgumentException(Msg.SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION);
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println("ungetService[factory=" + registration.getBundle() + "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		AtomicInteger useCount = serviceObjects.get(service);
		if (useCount.decrementAndGet() < 1) {
			serviceObjects.remove(service);
			factoryUngetService(service);
		}
		return true;
	}

	/**
	 * Release all uses of the service and reset the use count to zero.
	 *
	 * <ol>
	 * <li>The bundle's use count for this service is set to zero.
	 * <li>The {@link PrototypeServiceFactory#ungetService(Bundle, ServiceRegistration, Object)} method
	 * is called to release the service object for the bundle.
	 * </ol>
	 */
	/* @GuardedBy("this") */
	@Override
	void release() {
		super.release();
		for (S service : serviceObjects.keySet()) {
			if (debug.DEBUG_SERVICES) {
				Debug.println("releaseService[factory=" + registration.getBundle() + "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			factoryUngetService(service);
		}
		serviceObjects.clear();
	}

	/**
	 * Is this service use using any services?
	 *
	 * @return true if no services are being used and this service use can be discarded.
	 */
	/* @GuardedBy("this") */
	@Override
	boolean isEmpty() {
		return super.isEmpty() && serviceObjects.isEmpty();
	}
}
