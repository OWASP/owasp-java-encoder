/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.owasp.encoder.tag;

/**
 * Simple tests for the ForJavaScriptTag.
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class ForJavaScriptTagTest extends EncodingTagTest {

    public ForJavaScriptTagTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of doTag method, of class ForJavaScriptTag.
     * This is a very simple test that doesn't fully
     * exercise/test the encoder - only that the
     * tag itself works.
     * @throws Exception is thrown if the tag fails.
     */
    public void testDoTag() throws Exception {
        System.out.println("doTag");
        ForJavaScriptTag instance = new ForJavaScriptTag();
        String value = "\0'\"";
        String expected = "\\x00\\x27\\x22";
        instance.setJspContext(_pageContext);
        instance.setValue(value);
        instance.doTag();
        String results = _response.getContentAsString();
        assertEquals(expected,results);
    }
}
