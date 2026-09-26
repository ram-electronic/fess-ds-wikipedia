/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.wikipedia.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PlainTextOptionsTest {

    @Test
    public void defaultKeepsNothing() {
        assertFalse(PlainTextOptions.DEFAULT.keepsTemplate("Note"));
        assertFalse(PlainTextOptions.DEFAULT.keepsCaptions());
    }

    @Test
    public void templateNamesMatchLikeMediaWiki() {
        final PlainTextOptions options = PlainTextOptions.of(" note , Template:How_to  step,", true);
        // first letter case-insensitive, "_" is a space, the Template: prefix is optional
        assertTrue(options.keepsTemplate("Note"));
        assertTrue(options.keepsTemplate("note"));
        assertTrue(options.keepsTemplate("How to step"));
        assertTrue(options.keepsTemplate("how_to_step"));
        assertTrue(options.keepsTemplate("Template:How to  step"));
        // the rest of the name stays case-sensitive, as in MediaWiki
        assertFalse(options.keepsTemplate("NOTE"));
        assertFalse(options.keepsTemplate("Warning"));
        assertTrue(options.keepsCaptions());
    }

    @Test
    public void blankOrMissingListKeepsNoTemplate() {
        assertFalse(PlainTextOptions.of(null, false).keepsTemplate("Note"));
        assertFalse(PlainTextOptions.of(" , ", false).keepsTemplate(""));
    }

    @Test
    public void normalizeTemplateName() {
        assertEquals("Ünicode name", PlainTextOptions.normalizeTemplateName("  template:ünicode__name "));
        assertEquals("", PlainTextOptions.normalizeTemplateName("Template:"));
    }
}
