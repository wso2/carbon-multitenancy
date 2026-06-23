/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.tenant.mgt.ui.utils;

import junit.framework.TestCase;

/**
 * Unit tests for TenantMgtUtil.
 */
public class TenantMgtUtilTest extends TestCase {

    /** The confirmed XSS payload from Issue #17475 reproduction. */
    private static final String XSS_DOUBLE_QUOTE_PAYLOAD =
            "\" onfocus=\"alert(document.domain)\" autofocus x=\"";

    public void testRemoveHtmlElementsDoesNotEncodeDoubleQuote() {
        String result = TenantMgtUtil.removeHtmlElements(XSS_DOUBLE_QUOTE_PAYLOAD);
        // Double-quote is preserved unchanged — this IS the vulnerability path.
        // This assertion documents the limitation; do not use removeHtmlElements()
        // when rendering data into HTML value="" attributes.
        assertTrue(
                "removeHtmlElements() intentionally does not encode double-quotes; " +
                "any caller rendering output into an HTML attribute must use " +
                "Encode.forHtml() instead (see Issue #17475)",
                result.contains("\""));
    }

    public void testRemoveHtmlElementsDoesNotEncodeSingleQuote() {
        String singleQuotePayload = "' onmouseover='alert(1)' x='";
        String result = TenantMgtUtil.removeHtmlElements(singleQuotePayload);
        assertTrue(
                "removeHtmlElements() intentionally does not encode single-quotes; " +
                "callers rendering into HTML attribute context must use Encode.forHtml()",
                result.contains("'"));
    }

    /**
     * Verifies that removeHtmlElements() encodes opening angle brackets (existing behaviour).
     * This is the contract the method was designed to fulfil.
     */
    public void testRemoveHtmlElementsEncodesOpeningAngleBracket() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                TenantMgtUtil.removeHtmlElements("<script>alert(1)</script>"));
    }

    /**
     * Verifies that removeHtmlElements() handles null input without throwing.
     */
    public void testRemoveHtmlElementsHandlesNull() {
        assertNull(
                "removeHtmlElements(null) must return null, not throw",
                TenantMgtUtil.removeHtmlElements(null));
    }

    /**
     * Verifies that removeHtmlElements() returns an empty string unchanged.
     */
    public void testRemoveHtmlElementsHandlesEmptyString() {
        assertEquals("", TenantMgtUtil.removeHtmlElements(""));
    }

    /**
     * Verifies that removeHtmlElements() leaves safe alphanumeric input unchanged.
     */
    public void testRemoveHtmlElementsLeavesSafeInputUnchanged() {
        String safe = "tenant.example.com";
        assertEquals(safe, TenantMgtUtil.removeHtmlElements(safe));
    }

    public void testRemoveHtmlElementsMixedPayload() {
        String mixed = "<img src=x onerror=alert(1)>\" onfocus=\"alert(2)\"";
        String result = TenantMgtUtil.removeHtmlElements(mixed);
        // Angle brackets are encoded
        assertFalse("< must be encoded", result.contains("<"));
        assertFalse("> must be encoded", result.contains(">"));
        // But double-quote is NOT encoded — this is the dangerous residue
        assertTrue("\" is NOT encoded by removeHtmlElements() — callers must use " +
                "Encode.forHtml() for HTML attribute contexts", result.contains("\""));
    }
}
