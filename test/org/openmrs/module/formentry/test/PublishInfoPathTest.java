package org.openmrs.module.formentry.test;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.PublishInfoPath;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import com.sun.tools.javac.code.Attribute.Array;

public class PublishInfoPathTest extends BaseModuleContextSensitiveTest {

	static FileFilter NO_DOT_FILES;

	/**
	 * Directory containing original files used for testing. These
	 * files are copied into the ACTUAL_UNIT_TEST_DIR before each
	 * test.
	 * 
	 * This directory MUST NOT BE ALTERED BY THE TESTS.
	 */
	static File ORIGINAL_UNIT_TEST_DIR;
	
	/**
	 * Directory containing files as expected as a result
	 * of testing.
	 * 
	 * These file MUST NOT BE ALTERED BY THE TESTS.
	 */
	static File EXPECTED_UNIT_TEST_DIR;
	
	/**
	 * Directory containing files modified or generated by the
	 * unit tests.
	 */
	static File ACTUAL_UNIT_TEST_DIR;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ORIGINAL_UNIT_TEST_DIR = findDirNamed("original_unit_test_dir");
		EXPECTED_UNIT_TEST_DIR = findDirNamed("expected_unit_test_dir");
		ACTUAL_UNIT_TEST_DIR = findDirNamed("actual_unit_test_dir");
		
		NO_DOT_FILES = new FileFilter() {

			public boolean accept(File pathname) {
				return !pathname.getName().startsWith(".");
			}
			
		};
	}
	
	private static File findDirNamed(String dirname) {
		URL urlForDir = ClassLoader.getSystemResource(dirname);
		String fullPathToDir = urlForDir.getFile();
		return new File(fullPathToDir);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		FileUtils.deleteDirectory(ACTUAL_UNIT_TEST_DIR);
	}
	
	@Before
	public void setupBeforeEachTest() throws IOException {
		FileUtils.deleteDirectory(ACTUAL_UNIT_TEST_DIR);
		FileTestUtils.filteredCopy(ORIGINAL_UNIT_TEST_DIR, ACTUAL_UNIT_TEST_DIR, NO_DOT_FILES);
	}
	
	@Test
	public void shouldMatchQuotedConceptNameHl7Messages() {
		final String CONCEPT_ID = "1107";
		final String CONCEPT_TEXT = "NONE";
		final String HL7_TO_BE_UPDATED = "\"" + CONCEPT_ID + "^" + CONCEPT_TEXT + "^99DCT\"";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	assertTrue(m.find());
    	assertEquals(CONCEPT_ID, m.group(2));
    	assertEquals(CONCEPT_TEXT, m.group(3));
	}
	
	@Test
	public void shouldMatchHl7MessagesWithSpacesInName() {
		final String HL7_TO_BE_UPDATED = "\"2124^2RHZ / 4RH^99DCT\"";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	assertTrue(m.find());
	}
	
	/**
	 * The XSL update should match HL7 formatted messages with 
	 * concept values.
	 * 
	 */
	@Test
	public void shouldMatchWithinRadioButtonSelectionWhenUpdatingXsl() {
		final String HL7_TO_BE_UPDATED = "<div><input class=\"xdBehavior_Boolean\" title=\"\" type=\"radio\" name=\"{generate-id(obs/actual_treatment/current_antitb_holder/tb_schema/value)}\" xd:xctname=\"OptionButton\" xd:CtrlId=\"CTRL1895\" tabIndex=\"0\" xd:binding=\"obs/actual_treatment/current_antitb_holder/tb_schema/value\" xd:boundProp=\"xd:value\" xd:onValue=\"2124^2RHZ / 4RH^99DCT\">";
		final String EXPECTED_MATCH_CONCEPT_ID = "2124";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	assertTrue(m.find());
    	
    	assertEquals(EXPECTED_MATCH_CONCEPT_ID, m.group(2));
	}

	/**
	 * During rebuilding of the XSN, a regex pattern is used to 
	 * identify HL7 messages with concepts. A concept-name component
	 * is appended to the messagem, using the default locale.  
	 * 
	 * It should not match against HL7 messages which already have
	 * a concept-name.
	 */
	@Test
	public void shouldIgnoreExistingConceptNamesWhenUpdatingXsl() {
		final String HL7_WITH_CONCEPT_NAME = "\"2124^2RHZ / 4RH^99DCT^2292^2RHZ / 4RH^99DCT\"";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_WITH_CONCEPT_NAME);
    	
    	assertFalse(m.find());
	}
	
	/**
	 * Because the HL7 messages may appear inside of quotes, 
	 * often the HTML entity for the quote is used to delimit
	 * the HL7 message. The regex should match against that
	 * correctly.
	 */
	@Test
	public void shouldMatchHtmlQuoteEntityDelimiters() {
		final String HL7_TO_BE_UPDATED = "&quot;2124^2RHZ / 4RH^99DCT&quot;";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	assertTrue(m.find());
    	assertEquals("&quot;", m.group(1));
    	assertEquals("&quot;", m.group(m.groupCount()));
	}
	
	/**
	 * The regex should match against an HL7 message delimited
	 * with angle brackets. 
	 */
	@Test
	public void shouldMatchAngleBracketDelimiters() {
		final String HL7_TO_BE_UPDATED = ">2124^2RHZ / 4RH^99DCT<";

    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	assertTrue(m.find());
    	assertEquals(">", m.group(1));
    	assertEquals("<", m.group(m.groupCount()));
	}
	
	@Test
	public void shouldFindAllHL7MessagesInALine() {
		final String HL7_TO_BE_UPDATED =  "<input class=\"xdBehavior_Boolean\" title=\"\" type=\"radio\" name=\"{generate-id(obs/actual_treatment/current_antitb_holder/tb_schema/value)}\" xd:xctname=\"OptionButton\" xd:CtrlId=\"CTRL1895\" tabIndex=\"0\" xd:binding=\"obs/actual_treatment/current_antitb_holder/tb_schema/value\" xd:boundProp=\"xd:value\" xd:onValue=\"2124^2RHZ / 4RH^99DCT\"><xsl:attribute name=\"xd:value\"><xsl:value-of select=\"obs/actual_treatment/current_antitb_holder/tb_schema/value\"/></xsl:attribute><xsl:if test=\"obs/actual_treatment/current_antitb_holder/tb_schema/value=&quot;2124^2RHZ / 4RH^99DCT&quot;\">";
    	Matcher m = PublishInfoPath.hl7ConceptNamePattern.matcher(HL7_TO_BE_UPDATED);
    	
    	// should find two...
    	assertTrue(m.find());
    	assertTrue(m.find());
    	assertFalse(m.find());
		
	}
	
}
