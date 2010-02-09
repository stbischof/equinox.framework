/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.osgi.tests.debugoptions;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.framework.debug.FrameworkDebugTraceEntry;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DebugOptionsTestCase extends CoreTest {
	public static Test suite() {
		return new TestSuite(DebugOptionsTestCase.class);
	}

	DebugOptions debugOptions;
	ServiceReference ref;
	Dictionary props = null;
	TestDebugOptionsListener listener = null;
	ServiceRegistration reg = null;
	private final static String TRACE_ELEMENT_DELIMITER = "|"; //$NON-NLS-1$ // this value needs to match EclipseDebugTrace#TRACE_ELEMENT_DELIMITER
	private final static String TRACE_ELEMENT_DELIMITER_ENCODED = "&#124;"; //$NON-NLS-1$ // this value needs to match EclipseDebugTrace#TRACE_ELEMENT_DELIMITER_ENCODED
	private final static SimpleDateFormat TRACE_FILE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
	private final static String LINE_SEPARATOR;
	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	private final static String TAB_CHARACTER = "\t"; //$NON-NLS-1$

	protected void setUp() throws Exception {
		ref = OSGiTestsActivator.getContext().getServiceReference(DebugOptions.class.getName());
		assertNotNull("DebugOptions service is not available", ref); //$NON-NLS-1$
		debugOptions = (DebugOptions) OSGiTestsActivator.getContext().getService(ref);
		assertNotNull("DebugOptions service is not available", debugOptions); //$NON-NLS-1$
		props = new Hashtable();
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, getName());
		listener = new TestDebugOptionsListener();
		reg = OSGiTestsActivator.getContext().registerService(DebugOptionsListener.class.getName(), listener, props);
	}

	protected void tearDown() throws Exception {
		if (debugOptions == null)
			return;
		debugOptions.setDebugEnabled(false);
		debugOptions = null;
		OSGiTestsActivator.getContext().ungetService(ref);
		if (reg != null)
			reg.unregister();
	}

	public void testRegistration01() {
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
	}

	/**
	 * Test that a new {@link FrameworkDebugTraceEntry} object created without a trace class
	 * has 'org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase' as the class name and
	 * 'testTracingEntry01' as the method name that it determined as the caller of it.
	 * 
	 * This test mimics the tracing framework to ensure that the correct class name and method name
	 * are returned and written to the trace file.
	 */
	public void testTracingEntry01() {

		String bundleName = OSGiTestsActivator.getContext().getBundle().getSymbolicName();
		String optionPath = "/debug"; //$NON-NLS-1$
		String message = "Test message"; //$NON-NLS-1$
		FrameworkDebugTraceEntry traceEntry = new FrameworkDebugTraceEntry(bundleName, optionPath, message, null);
		String correctClassName = "org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase"; //$NON-NLS-1$
		String correctMethodName = "testTracingEntry01"; //$NON-NLS-1$
		assertEquals("The class calling the trace API does not match the expected value.", correctClassName, traceEntry.getClassName()); //$NON-NLS-1$  
		assertEquals("The method calling the trace API does not match the expected value.", correctMethodName, traceEntry.getMethodName()); //$NON-NLS-1$
	}

	/**
	 * Test that a new {@link FrameworkDebugTraceEntry} object created with a trace class
	 * of 'org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase' has the correct class name and
	 * method name of the caller.
	 * 
	 * This test mimics the tracing framework to ensure that the correct class name and method name
	 * are returned and written to the trace file.
	 */
	public void testTracingEntry02() {

		String correctClassName = Runner1.class.getName();
		String correctMethodName = "run"; //$NON-NLS-1$
		FrameworkDebugTraceEntry traceEntry = new Runner1().run();
		assertEquals("The class calling the trace API does not match the expected value.", correctClassName, traceEntry.getClassName()); //$NON-NLS-1$  
		assertEquals("The method calling the trace API does not match the expected value.", correctMethodName, traceEntry.getMethodName()); //$NON-NLS-1$
	}

	static class Runner1 {
		public FrameworkDebugTraceEntry run() {
			return new Runner2().run();
		}
	}

	static class Runner2 {
		public FrameworkDebugTraceEntry run() {
			String bundleName = OSGiTestsActivator.getContext().getBundle().getSymbolicName();
			String optionPath = "/debug"; //$NON-NLS-1$
			String message = "Test message"; //$NON-NLS-1$
			String tracingClass = this.getClass().getName();
			return new FrameworkDebugTraceEntry(bundleName, optionPath, message, tracingClass);
		}
	}

	public void testDyanmicEnablement01() {
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		listener.clear();
		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
	}

	public void testDyanmicEnablement02() {
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		listener.clear();
		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNotNull("Should find bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
	}

	public void testDyanmicEnablement03() {
		listener.clear();
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		TestDebugOptionsListener anotherListener = new TestDebugOptionsListener();
		Dictionary anotherProps = new Hashtable();
		anotherProps.put(DebugOptions.LISTENER_SYMBOLICNAME, "anotherListener"); //$NON-NLS-1$
		ServiceRegistration anotherReg = OSGiTestsActivator.getContext().registerService(DebugOptionsListener.class.getName(), anotherListener, anotherProps);
		assertTrue("Not called", anotherListener.gotCalled()); //$NON-NLS-1$

		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$

		listener.clear();
		anotherListener.clear();

		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Should not call wrong listener", anotherListener.gotCalled()); //$NON-NLS-1$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
		listener.clear();
		anotherListener.clear();
		debugOptions.setOption("anotherListener/test", "blah"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Listener should not have been called", listener.gotCalled()); //$NON-NLS-1$
		assertTrue("Another listener should have been called", anotherListener.gotCalled()); //$NON-NLS-1$

		listener.clear();
		anotherListener.clear();
		anotherProps.put(DebugOptions.LISTENER_SYMBOLICNAME, getName());
		anotherReg.setProperties(anotherProps);
		debugOptions.setOption(getName() + "/debug", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertTrue("Another listener did not get called", anotherListener.gotCalled()); //$NON-NLS-1$

		anotherReg.unregister();
	}

	public void testDyanmicEnablement04() {
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		listener.clear();
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$

		listener.clear();
		checkValues.put(getName() + "/debug", null); //$NON-NLS-1$ 
		listener.setCheckValues(checkValues);
		debugOptions.setDebugEnabled(false);
		assertFalse("Debug is enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$

		listener.clear();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$

	}

	public void testSetOptionsWhenDisabled() {

		// enable tracing initially.
		if (!debugOptions.isDebugEnabled()) {
			debugOptions.setDebugEnabled(true);
		}
		// create a new key to add.
		String testKey = getName() + "/debug/disableCheck";
		// set its value to 'ok'
		debugOptions.setOption(testKey, "ok");
		// make sure the right value is added
		String actualValue = debugOptions.getOption(testKey, "not ok");
		assertEquals("The correct option value was not returned from the debug option: " + testKey, "ok", actualValue);
		// disable tracing
		debugOptions.setDebugEnabled(false);
		// check for the value
		actualValue = debugOptions.getOption(testKey, "not ok");
		assertEquals("The 'default' value supplied was not returned when tracing is disabled.", "not ok", actualValue);
		// try setting the value to "ok" (this should be a no-op and return the 'default' value)
		debugOptions.setOption(testKey, "ok");
		actualValue = debugOptions.getOption(testKey, "not ok");
		assertEquals("The 'default' value supplied was not returned when tracing is disabled.", "not ok", actualValue);
		// remove the option and check for the value (it should still exist after re-enabling tracing)
		debugOptions.removeOption(testKey);
		// re-enable tracing
		debugOptions.setDebugEnabled(true);
		// check that the value is still the initial "ok"
		actualValue = debugOptions.getOption(testKey, "not ok");
		assertEquals("The value after re-enabling tracing is invalid.", "ok", actualValue);
	}

	public void testStringValues() {

		if (!debugOptions.isDebugEnabled()) {
			debugOptions.setDebugEnabled(true);
		}
		// create a new key to add.
		String testKey = getName() + "/debug/stringValue";
		// set its value to 'test'
		debugOptions.setOption(testKey, "test");
		// check to make sure the value returned is correct
		String actualValue = debugOptions.getOption(testKey, "default");
		assertEquals("The correct option value was not returned from the debug option: " + testKey, "test", actualValue);
		// remove the option and check for the value (it should not exist so the default value should be returned)
		debugOptions.removeOption(testKey);
		actualValue = debugOptions.getOption(testKey, "default");
		assertEquals("The 'default' value supplied was not returned when the key does not exist in the DebugOptions.", "default", actualValue);
	}

	public void testIntegerValues() {

		if (!debugOptions.isDebugEnabled()) {
			debugOptions.setDebugEnabled(true);
		}
		// create a new key to add.
		String testKey = getName() + "/debug/intValue";
		// set its value to 42.
		debugOptions.setOption(testKey, "42");
		// check to make sure the value returned is correct
		int actualValue = debugOptions.getIntegerOption(testKey, 0);
		assertEquals("The correct option value was not returned from the debug option: " + testKey, 42, actualValue);
		// set the value of this key so that a NumberFormatException will occur
		debugOptions.setOption(testKey, "test");
		actualValue = debugOptions.getIntegerOption(testKey, 0);
		assertEquals("The 'default' value supplied was not returned when a NumberFormatException occurs.", 0, actualValue);
		// remove the option and check for the value (it should not exist so the default value should be returned)
		debugOptions.removeOption(testKey);
		actualValue = debugOptions.getIntegerOption(testKey, 0);
		assertEquals("The 'default' value supplied was not returned when the key does not exist in the DebugOptions.", 0, actualValue);
	}

	public void testBooleanValues() {
		if (!debugOptions.isDebugEnabled()) {
			debugOptions.setDebugEnabled(true);
		}
		String testKey = getName() + "/debug";
		boolean testValue = debugOptions.getBooleanOption(testKey, false);
		assertFalse(testKey + " is true", testValue);
		debugOptions.setOption(testKey, "false");
		testValue = debugOptions.getBooleanOption(testKey, true);
		assertFalse(testKey + " is true", testValue);

		debugOptions.setOption(testKey, "true");
		testValue = debugOptions.getBooleanOption(testKey, false);
		assertTrue(testKey + " is false", testValue);
		testValue = debugOptions.getBooleanOption(testKey, true);
		assertTrue(testKey + " is false", testValue);
	}

	private TestDebugTrace createDebugTrace(final File traceFile) {

		TestDebugTrace debugTrace = null;
		if (!debugOptions.isDebugEnabled()) {
			/** Setup the DebugOptions */
			debugOptions.setDebugEnabled(true);
			assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		}
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		debugOptions.setFile(traceFile);
		DebugTrace wrapped = debugOptions.newDebugTrace(getName(), TestDebugTrace.class);
		debugTrace = new TestDebugTrace(wrapped);
		return debugTrace;
	}

	/**
	 * test DebugTrace.trace(option, message);
	*/
	public void testTraceFile01() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.trace("/debug", "testing 1"); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.trace("/notset", "testing 2"); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.trace("/debug", "testing 3"); //$NON-NLS-1$ //$NON-NLS-2$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.trace(option, message)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile01", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("trace message is incorrect", "testing 1", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile01", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("trace message is incorrect", "testing 3", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.trace(option, message, Throwable)
	 */
	public void testTraceFile02() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		final String exceptionMessage1 = "An error 1"; //$NON-NLS-1$
		final String exceptionMessage2 = "An error 2"; //$NON-NLS-1$
		final String exceptionMessage3 = "An error 3"; //$NON-NLS-1$
		try {
			debugTrace.trace("/debug", "testing 1", new Exception(exceptionMessage1)); //$NON-NLS-1$ //$NON-NLS-2$ 
			debugTrace.trace("/notset", "testing 2", new Exception(exceptionMessage2)); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.trace("/debug", "testing 3", new Exception(exceptionMessage3)); //$NON-NLS-1$ //$NON-NLS-2$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.trace(option, message, Throwable)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}

		final StringBuffer expectedThrowableText1 = new StringBuffer("java.lang.Exception: "); //$NON-NLS-1$
		expectedThrowableText1.append(exceptionMessage1);
		expectedThrowableText1.append(DebugOptionsTestCase.LINE_SEPARATOR);
		expectedThrowableText1.append(DebugOptionsTestCase.TAB_CHARACTER);
		expectedThrowableText1.append("at org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase.testTraceFile02(DebugOptionsTestCase.java:"); //$NON-NLS-1$		

		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile02", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("trace message is incorrect", "testing 1", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("throwable text should not be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		if (!traceOutput[0].getThrowableText().startsWith(expectedThrowableText1.toString())) {
			final StringBuffer errorMessage = new StringBuffer("The expected throwable text does not start with the actual throwable text."); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("Expected"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("--------"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(expectedThrowableText1.toString());
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("Actual"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("--------"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(traceOutput[0].getThrowableText());
			fail(errorMessage.toString());
		}
		assertNotNull("throwable should not be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$

		final StringBuffer expectedThrowableText2 = new StringBuffer("java.lang.Exception: "); //$NON-NLS-1$
		expectedThrowableText2.append(exceptionMessage3);
		expectedThrowableText2.append(DebugOptionsTestCase.LINE_SEPARATOR);
		expectedThrowableText2.append(DebugOptionsTestCase.TAB_CHARACTER);
		expectedThrowableText2.append("at org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase.testTraceFile02(DebugOptionsTestCase.java:"); //$NON-NLS-1$		

		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile02", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("trace message is incorrect", "testing 3", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("throwable text should not be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		if (!traceOutput[1].getThrowableText().startsWith(expectedThrowableText2.toString())) {
			final StringBuffer errorMessage = new StringBuffer("The expected throwable text does not start with the actual throwable text."); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("Expected"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("--------"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(expectedThrowableText2.toString());
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("Actual"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append("--------"); //$NON-NLS-1$
			errorMessage.append(DebugOptionsTestCase.LINE_SEPARATOR);
			errorMessage.append(traceOutput[1].getThrowableText());
			fail(errorMessage.toString());
		}
		assertNotNull("throwable should not be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceDumpStack(option)
	*/
	public void testTraceFile03() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceDumpStack("/debug"); //$NON-NLS-1$
			debugTrace.traceDumpStack("/notset"); //$NON-NLS-1$
			debugTrace.traceDumpStack("/debug"); //$NON-NLS-1$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceDumpStack(option)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile03", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Trace message is not a stack dump", traceOutput[0].getMessage().startsWith("Thread Stack dump: ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile03", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Trace message is not a stack dump", traceOutput[1].getMessage().startsWith("Thread Stack dump: ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceEntry(option)
	*/
	public void testTraceFile04() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceEntry("/debug"); //$NON-NLS-1$
			debugTrace.traceEntry("/notset"); //$NON-NLS-1$
			debugTrace.traceEntry("/debug"); //$NON-NLS-1$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceEntry(option)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile04", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with no parameters", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile04", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with no parameters", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceEntry(option, methodArgument)
	 */
	public void testTraceFile05() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceEntry("/debug", new String("arg1")); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.traceEntry("/notset", new String("arg2")); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.traceEntry("/debug", new String("arg3")); //$NON-NLS-1$ //$NON-NLS-2$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceEntry(option, methodArgument)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile05", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with parameters: (arg1)", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile05", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with parameters: (arg3)", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceEntry(option, methodArgument[])
	 */
	public void testTraceFile06() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceEntry("/debug", new String[] {"arg1", "arg2"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			debugTrace.traceEntry("/notset", new String[] {"arg3", "arg4"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			debugTrace.traceEntry("/debug", new String[] {"arg5", "arg6"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceEntry(option, methodArgument[])' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile06", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with parameters: (arg1 arg2)", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile06", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Entering method with parameters: (arg5 arg6)", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceExit(option)
	*/
	public void testTraceFile07() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceExit("/debug"); //$NON-NLS-1$
			debugTrace.traceExit("/notset"); //$NON-NLS-1$
			debugTrace.traceExit("/debug"); //$NON-NLS-1$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceExit(option)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile07", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Exiting method with a void return", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile07", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Exiting method with a void return", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * test DebugTrace.traceExit(option, result)
	*/
	public void testTraceFile08() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.traceExit("/debug", new String("returnValue1")); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.traceExit("/notset", new String("returnValue2")); //$NON-NLS-1$ //$NON-NLS-2$
			debugTrace.traceExit("/debug", new String("returnValue3")); //$NON-NLS-1$ //$NON-NLS-2$
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.traceExit(option, result)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[0].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[0].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[0].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[0].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile08", traceOutput[0].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Exiting method with result: returnValue1", traceOutput[0].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[0].getThrowableText()); //$NON-NLS-1$
		assertEquals("Wrong number of trace entries for trace without an exception", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Thread name is incorrect", Thread.currentThread().getName(), traceOutput[1].getThreadName()); //$NON-NLS-1$
		assertEquals("Bundle name is incorrect", getName(), traceOutput[1].getBundleSymbolicName()); //$NON-NLS-1$
		assertEquals("option-path value is incorrect", "/debug", traceOutput[1].getOptionPath()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("class name value is incorrect", DebugOptionsTestCase.class.getName(), traceOutput[1].getClassName()); //$NON-NLS-1$
		assertEquals("method name value is incorrect", "testTraceFile08", traceOutput[1].getMethodName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "Exiting method with result: returnValue3", traceOutput[1].getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("throwable should be null", traceOutput[1].getThrowableText()); //$NON-NLS-1$
		// delete the trace file
		traceFile.delete();
	}

	/**
	 * tests DebugTrace.trace(option, message) where the 'option' and 'message contain a '|' character (the delimiter).
	*/
	public void testTraceFile09() {

		final File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		TestDebugTrace debugTrace = this.createDebugTrace(traceFile);
		debugOptions.setOption(getName() + "/debug|path", "true");
		TraceEntry[] traceOutput = null;
		try {
			debugTrace.trace("/debug|path", "A message with a | character.");
			debugTrace.trace("/debug|path", "|A message with | multiple || characters.|");
			traceOutput = readTraceFile(traceFile); // Note: this call will also delete the trace file
		} catch (InvalidTraceEntry invalidEx) {
			fail("Failed 'DebugTrace.trace(option, message)' test as an invalid trace entry was found.  Actual Value: '" + invalidEx.getActualValue() + "'.", invalidEx); //$NON-NLS-1$ //$NON-NLS-2$
		}
		assertEquals("Wrong number of entries", 2, traceOutput.length);
		String optionPath = decodeString(traceOutput[0].getOptionPath());
		String message = decodeString(traceOutput[0].getMessage());
		assertEquals("option-path value is incorrect", "/debug|path", optionPath); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "A message with a | character.", message); //$NON-NLS-1$ //$NON-NLS-2$
		optionPath = decodeString(traceOutput[1].getOptionPath());
		message = decodeString(traceOutput[1].getMessage());
		assertEquals("option-path value is incorrect", "/debug|path", optionPath); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Trace message is not correct", "|A message with | multiple || characters.|", message); //$NON-NLS-1$ //$NON-NLS-2$
		// delete the trace file
		traceFile.delete();
	}

	private static String decodeString(final String inputString) {
		if (inputString == null || inputString.indexOf(TRACE_ELEMENT_DELIMITER_ENCODED) < 0)
			return inputString;
		final StringBuffer tempBuffer = new StringBuffer(inputString);
		int currentIndex = tempBuffer.indexOf(TRACE_ELEMENT_DELIMITER_ENCODED);
		while (currentIndex >= 0) {
			tempBuffer.replace(currentIndex, currentIndex + TRACE_ELEMENT_DELIMITER_ENCODED.length(), TRACE_ELEMENT_DELIMITER);
			currentIndex = tempBuffer.indexOf(TRACE_ELEMENT_DELIMITER_ENCODED);
		}
		return tempBuffer.toString();
	}

	private TraceEntry[] readTraceFile(File traceFile) throws InvalidTraceEntry {

		Reader traceReader = null;
		List traceEntries = new ArrayList();
		try {
			traceReader = new BufferedReader(new InputStreamReader(new FileInputStream(traceFile), "UTF-8")); //$NON-NLS-1$
			TraceEntry entry = null;
			while ((entry = this.readMessage(traceReader)) != null) {
				traceEntries.add(entry);
			}
		} catch (IOException ex) {
			fail("Failed to read trace file", ex); //$NON-NLS-1$
		} finally {
			if (traceReader != null) {
				try {
					traceReader.close();
				} catch (Exception ex) {
					// do nothing
				}
			}
		}
		return (TraceEntry[]) traceEntries.toArray(new TraceEntry[traceEntries.size()]);
	}

	private TraceEntry readMessage(final Reader traceReader) throws IOException, InvalidTraceEntry {

		TraceEntry result = null;
		int input = traceReader.read();
		while (input != -1) {
			char inputChar = (char) input;
			if (inputChar == '#') {
				while ((inputChar != '\n') && (inputChar != '\r')) {
					// do nothing
					inputChar = (char) traceReader.read();
				}
			}
			if (inputChar == DebugOptionsTestCase.TRACE_ELEMENT_DELIMITER.charAt(0)) {
				// first entry - thread name
				final String threadName = this.readEntry(traceReader);
				if ((threadName == null) || (threadName.length() == 0)) {
					throw new InvalidTraceEntry("The thread name in a trace entry is null or empty", threadName); //$NON-NLS-1$
				}
				// read the next character
				final String date = this.readEntry(traceReader);
				if ((date == null) || (date.length() == 0)) {
					throw new InvalidTraceEntry("The timestamp in a trace entry is null or empty", date); //$NON-NLS-1$
				}
				long timestamp = 0;
				try {
					timestamp = DebugOptionsTestCase.TRACE_FILE_DATE_FORMATTER.parse(date).getTime();
				} catch (ParseException parseEx) {
					throw new InvalidTraceEntry("The date in a trace entry '" + date + "' could not be parsed.", date); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// third entry - bundle symbolic name
				final String symbolicName = this.readEntry(traceReader);
				if ((symbolicName == null) || (symbolicName.length() == 0)) {
					throw new InvalidTraceEntry("The bundle symbolic name in a trace entry is null or empty", symbolicName); //$NON-NLS-1$
				}
				// fourth entry - option path
				final String optionPath = this.readEntry(traceReader);
				if ((optionPath == null) || (optionPath.length() == 0)) {
					throw new InvalidTraceEntry("The option-path in a trace entry is null or empty", optionPath); //$NON-NLS-1$
				}
				// fifth entry - class name
				final String className = this.readEntry(traceReader);
				if ((className == null) || (className.length() == 0)) {
					throw new InvalidTraceEntry("The class name in a trace entry is null or empty", className); //$NON-NLS-1$
				}
				// sixth entry - method name
				final String methodName = this.readEntry(traceReader);
				if ((methodName == null) || (methodName.length() == 0)) {
					throw new InvalidTraceEntry("The method name in a trace entry is null or empty", methodName); //$NON-NLS-1$
				}
				// seventh entry - line number
				final String lineNumberString = this.readEntry(traceReader);
				if ((lineNumberString == null) || (lineNumberString.length() == 0)) {
					throw new InvalidTraceEntry("The line number in a trace entry is null or empty", lineNumberString); //$NON-NLS-1$
				}
				final int lineNumber = Integer.valueOf(lineNumberString).intValue();
				// eighth entry - message
				final String message = this.readEntry(traceReader);
				// read in the next character. if it is \r or \n then the throwable was not supplied
				traceReader.mark(1);
				inputChar = (char) traceReader.read();
				String throwable = null;
				if (inputChar != '\n' && inputChar != '\r') {
					traceReader.reset();
					// ninth entry (optional) - throwable
					throwable = this.readEntry(traceReader);
					if ((throwable == null) || (throwable.length() == 0)) {
						throw new InvalidTraceEntry("The throwable in a trace entry is null or empty", throwable); //$NON-NLS-1$
					}
				}
				// create the entry
				result = new TraceEntry(threadName, timestamp, symbolicName, optionPath, className, methodName, lineNumber, message, throwable);
				break; // no point in reading any more information since the TraceEntry is created
			}
			// read the next character
			input = traceReader.read();
		}
		return result;
	}

	private String readEntry(final Reader traceReader) throws IOException {

		char inputChar = (char) traceReader.read();
		StringBuffer buffer = new StringBuffer();
		while (inputChar != DebugOptionsTestCase.TRACE_ELEMENT_DELIMITER.charAt(0)) {
			inputChar = (char) traceReader.read();
			if (inputChar != DebugOptionsTestCase.TRACE_ELEMENT_DELIMITER.charAt(0)) {
				buffer.append(inputChar);
			}
		}
		return buffer.toString().trim();
	}

	static public class TraceEntry {
		/** If a bundles symbolic name is not specified then the default value of /debug can be used */
		public final static String DEFAULT_OPTION_PATH = "/debug"; //$NON-NLS-1$

		/**
		 * The name of the thread executing the code
		 */
		private final String threadName;

		/**
		 * The date and time when the trace occurred.
		 * 
		 */
		private final long timestamp;

		/**
		 * The trace option-path
		 */
		private final String optionPath;

		/**
		 * The symbolic name of the bundle being traced
		 */
		private final String bundleSymbolicName;

		/**
		 * The class being traced
		 */
		private final String className;

		/**
		 * The method being traced
		 */
		private String methodName = null;

		/**
		 * The line number
		 */
		private final int lineNumber;

		/**
		 * The trace message
		 */
		private final String message;

		/**
		 * The trace exception
		 */
		private final String throwableText;

		public TraceEntry(final String traceThreadName, final long traceTimestamp, final String traceBundleName, final String traceOptionPath, final String traceClassName, final String traceMethodName, final int traceLineNumber, final String traceMessage) {

			this.threadName = traceThreadName;
			this.bundleSymbolicName = traceBundleName;
			this.optionPath = traceOptionPath;
			this.className = traceClassName;
			this.methodName = traceMethodName;
			this.lineNumber = traceLineNumber;
			this.message = traceMessage;
			this.timestamp = traceTimestamp;
			this.throwableText = null;
		}

		public TraceEntry(final String traceThreadName, final long traceTimestamp, final String traceBundleName, final String traceOptionPath, final String traceClassName, final String traceMethodName, final int traceLineNumber, final String traceMessage, final String traceThrowable) {

			this.threadName = traceThreadName;
			this.bundleSymbolicName = traceBundleName;
			this.optionPath = traceOptionPath;
			this.className = traceClassName;
			this.methodName = traceMethodName;
			this.lineNumber = traceLineNumber;
			this.message = traceMessage;
			this.timestamp = traceTimestamp;
			this.throwableText = traceThrowable;
		}

		public final String getThreadName() {

			return threadName;
		}

		public final long getTimestamp() {

			return timestamp;
		}

		public final String getBundleSymbolicName() {

			return bundleSymbolicName;
		}

		public final String getMessage() {

			return message;
		}

		public final String getThrowableText() {

			return throwableText;
		}

		public final String getClassName() {

			return className;
		}

		public final String getMethodName() {

			return methodName;
		}

		public final String getOptionPath() {

			return optionPath;
		}

		public final int getLineNumber() {

			return lineNumber;
		}
	}

	static class InvalidTraceEntry extends Exception {

		private static final long serialVersionUID = -63837787081750344L;

		public InvalidTraceEntry(String message, String actual) {
			super(message);
			this.actualValue = actual;
		}

		public final String getActualValue() {
			return this.actualValue;
		}

		String actualValue;
		String expectedValue;
	}

	static class TestDebugTrace implements DebugTrace {
		private final DebugTrace wrapped;

		public TestDebugTrace(DebugTrace wrapped) {
			this.wrapped = wrapped;
		}

		public void trace(String option, String message) {
			wrapped.trace(option, message);
		}

		public void trace(String option, String message, Throwable error) {
			wrapped.trace(option, message, error);
		}

		public void traceDumpStack(String option) {
			wrapped.traceDumpStack(option);
		}

		public void traceEntry(String option) {
			wrapped.traceEntry(option);
		}

		public void traceEntry(String option, Object methodArgument) {
			wrapped.traceEntry(option, methodArgument);
		}

		public void traceEntry(String option, Object[] methodArguments) {
			wrapped.traceEntry(option, methodArguments);
		}

		public void traceExit(String option) {
			wrapped.traceExit(option);
		}

		public void traceExit(String option, Object result) {
			wrapped.traceExit(option, result);
		}
	}

	class TestDebugOptionsListener implements DebugOptionsListener {
		boolean called = false;
		String incorrectValue;
		Map checkValues;

		public void optionsChanged(DebugOptions options) {
			called = true;
			if (checkValues == null)
				return;
			for (Iterator entries = checkValues.entrySet().iterator(); entries.hasNext();) {
				Map.Entry entry = (Entry) entries.next();
				String debugValue = options.getOption((String) entry.getKey());
				String error = "Value is incorrect for key: " + entry.getKey() + " " + debugValue; //$NON-NLS-1$//$NON-NLS-2$
				if (debugValue == null) {
					if (entry.getValue() != null) {
						incorrectValue = error;
						return;
					}
					continue;
				}
				if (!debugValue.equals(entry.getValue())) {
					incorrectValue = error;
					return;
				}
			}
		}

		public boolean gotCalled() {
			return called;
		}

		public void clear() {
			called = false;
			checkValues = null;
			incorrectValue = null;
		}

		public void setCheckValues(Map checkValues) {
			this.checkValues = checkValues;
		}

		public String getIncorrectValue() {
			return incorrectValue;
		}
	}
}