package com.nosoop.json;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for VDF-JSON conversion.
 */
public class VDFTest extends TestCase {

    /**
     * Create the test case.
     *
     * @param testName name of the test case
     */
    public VDFTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(VDFTest.class);
    }

    /**
     * Testing
     */
    public void testApp() {
        assertTrue(true);
    }
}
