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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controls which otherwise dropped wikitext keeps its text in {@link WikiTextParser#getPlainText(PlainTextOptions)}.
 * <p>
 * By default templates, image captions and gallery captions are dropped. Installations whose
 * wiki keeps prose in templates (notes, warnings, how-to boxes) or captions can opt in to
 * indexing that text through the {@code keep_template_text} and {@code keep_captions} handler
 * parameters.
 * </p>
 */
public final class PlainTextOptions {

    /** Drops all templates and captions. */
    public static final PlainTextOptions DEFAULT = new PlainTextOptions(Set.of(), false);

    private static final String TEMPLATE_NAMESPACE = "template:";

    private final Set<String> keptTemplates;

    private final boolean keepCaptions;

    private PlainTextOptions(final Set<String> keptTemplates, final boolean keepCaptions) {
        this.keptTemplates = keptTemplates;
        this.keepCaptions = keepCaptions;
    }

    /**
     * Creates options from the handler parameter values.
     *
     * @param keepTemplateText comma-separated names of templates whose argument text is kept,
     *        such as {@code Note,Warning}; null or blank keeps none
     * @param keepCaptions whether image and gallery captions are kept
     * @return the options
     */
    public static PlainTextOptions of(final String keepTemplateText, final boolean keepCaptions) {
        final Set<String> templates = keepTemplateText == null ? Set.of()
                : Arrays.stream(keepTemplateText.split(","))
                        .map(PlainTextOptions::normalizeTemplateName)
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        return new PlainTextOptions(templates, keepCaptions);
    }

    /**
     * Returns whether the argument text of the named template is kept.
     *
     * @param templateName the template name as written in the wikitext
     * @return true if the template's argument text is kept
     */
    public boolean keepsTemplate(final String templateName) {
        return !keptTemplates.isEmpty() && keptTemplates.contains(normalizeTemplateName(templateName));
    }

    /**
     * Returns whether image and gallery captions are kept.
     *
     * @return true if captions are kept
     */
    public boolean keepsCaptions() {
        return keepCaptions;
    }

    /**
     * Normalizes a template name the way MediaWiki resolves it: the {@code Template:} prefix is
     * optional, underscores and runs of spaces are one space, and the first letter is
     * case-insensitive.
     *
     * @param name the template name
     * @return the normalized name
     */
    static String normalizeTemplateName(final String name) {
        String n = name.replace('_', ' ').trim().replaceAll("\\s+", " ");
        if (n.toLowerCase(Locale.ROOT).startsWith(TEMPLATE_NAMESPACE)) {
            n = n.substring(TEMPLATE_NAMESPACE.length()).trim();
        }
        if (n.isEmpty()) {
            return n;
        }
        final int first = n.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toUpperCase(first)).append(n.substring(Character.charCount(first))).toString();
    }
}
