# fess-ds-wikipedia (ram-electronic fork)

A fork of [`codelibs/fess-ds-wikipedia`](https://github.com/codelibs/fess-ds-wikipedia)
(forked at its `fess-ds-wikipedia-15.8.0` tag, since merged with upstream
`main` up to `9817f6d`), patched to fix a
wikitext-to-plaintext extraction gap in `source=xml` crawls (MediaWiki's
`dumpBackup.php`-style export, including official Wikimedia dumps like
`*-pages-articles.xml.bz2`).

## Why this fork exists

Upstream's `WikiTextParser.getPlainText()` is a regex-only pass ported from
the old `elasticsearch-river-wikipedia` plugin. It strips `<ref>` tags,
`{{templates}}`, and most `[[links]]`, but never touches section headers
(`== Heading ==`) or list markers (`*`, `#`, `:`, `;`) — those pass straight
into the indexed `content`/`digest` fields unstripped, showing up as raw
markup in search results. This isn't specific to any one wiki's dump; it
affects every `source=xml` crawl, official Wikimedia dumps included, since
they all go through the same code path.

## What's patched

`src/main/java/org/codelibs/fess/ds/wikipedia/support/WikiTextParser.java`,
`getPlainText()`, no longer uses regexes. It parses the page with
[Sweble](https://github.com/rzo1/sweble-wikitext) (`swc-engine`), a real
MediaWiki wikitext parser, and renders the resulting syntax tree to plain text
with `SwebleTextConverter` (adapted from Sweble's example `TextConverter`):

- Keeps the text of section headings, list items, links (`[[Link|Text]]` →
  `Text`, `[[dog]]s` → `dogs`), labeled external links
  (`[http://example.com label]` → `label`) and tables (one line per row).
- Keeps the text of templates' argument values (parsed as wikitext, without
  parameter names) and of image and gallery captions, since text that is
  dropped can't be found. Both can be filtered out per installation, see below.
  A template on its own line (an infobox, a maintenance tag) becomes its own
  line; one inside a sentence continues it.
- Drops `<ref>` tags in all forms, HTML comments, image file names and
  options, categories, interlanguage links, magic words (`__TOC__`), bare URLs
  and HTML tags.
- Handles tag extensions explicitly: `<pre>`/`<source>`/`<syntaxhighlight>`
  bodies are kept as literal text, `<poem>`/`<indicator>`/`<langconvert>`
  bodies are parsed as wikitext (a poem keeps one line per line), gallery
  captions are kept like image captions, and every other extension
  (`<imagemap>`, `<inputbox>`, `<math>`, `<templatestyles>`, …) is dropped, so
  its syntax never reaches the index.
- Keeps apostrophes inside words (`Einstein's`), which the regex chain
  stripped along with `'''bold'''`/`''italic''` markup, and decodes entities
  to the characters MediaWiki displays (`&ndash;` → `–`, `&lt;tag&gt;` →
  `<tag>`).

Templates aren't expanded, since their definitions aren't in the dump: a kept
template contributes its argument text, not what MediaWiki would render. So
`{{birth date|1900|1|1}}` is indexed as "1900 1 1", and a template's own fixed
text (a warning box's heading, say) isn't indexed. Parsing is roughly 5–7× slower than the regex chain (about
100–150 ms for a large article such as *Germany*). If Sweble throws on a page,
the old regex stripping (`getPlainTextByRegex()`) is used as a fallback and a
warning is logged.

See `SweblePlainTextTest.java` for the test cases, including regression checks
on real articles (`src/test/resources/wikitext/`, CC BY-SA 4.0 Wikipedia
revisions attributed in that folder's `README.md`): no markup may survive, and
prose from every part of the article must.

### Filtering template text and captions

Everything the converter drops is also unsearchable: Fess indexes and
displays the same plain text (`content`/`digest`), and the raw wikitext isn't
indexed. So by default the text of every template and every image and gallery
caption is kept: on many wikis, especially internal ones, templates carry real
prose (notes, warnings, how-to boxes), and captions describe the page.

Some templates mostly add noise, such as citation details, date-format tags
or infobox file names. Three optional handler parameters, set in the crawl
config next to `url` and `source`, filter that out for `source=xml` crawls:

```
drop_templates=Cite web,Cite news,Use dmy dates
keep_templates=
drop_captions=false
```

- **`drop_templates`**: comma-separated names of templates whose text is
  dropped, also when nested inside another template. `*` drops all templates.
- **`keep_templates`**: templates whose text is kept even though
  `drop_templates` matches them. Together with `drop_templates=*` this is an
  allowlist: `drop_templates=*` and `keep_templates=Note,Warning` index only
  those two.
- **`drop_captions`**: `true` drops image and gallery captions.

For example, `{{Note|Restart Apache after changing the config.|title=Careful}}`
is indexed as "Restart Apache after changing the config. Careful" unless
`Note` is dropped. Template names match the way MediaWiki resolves them: the
first letter is case-insensitive, `_` equals a space, and a `Template:` prefix
is optional.

Footnote templates such as `{{efn|…}}` keep their text where the footnote
marker is, inside the sentence. Word searches find it, but a phrase search
across that spot won't match; `drop_templates=Efn,Refn,Sfn` avoids that.

These parameters don't affect `source=cirrus`, whose dumps already carry
MediaWiki's own rendered text. Existing pages pick up a change on the next
crawl, since every crawl re-reads the whole dump.

## Everything else

Unmodified from upstream — same crawl config format, same field mapping,
same `WikipediaDataStore` handler name. The fork builds against Fess 15.8.0
even though upstream `main` has moved its parent to `15.9.0-SNAPSHOT`. See
[`codelibs/fess-ds-wikipedia`](https://github.com/codelibs/fess-ds-wikipedia)
for general usage.

## Requirements

- Java 21+, Maven 3.x
- Fess 15.8.0 (pinned via `pom.xml`'s `<parent>` version). This plugin's own
  version (`pom.xml`'s top-level `<version>`, and its release tags) is
  independent semver, not tied to Fess's version — same reasoning as
  [`ram-electronic/fess-ds-trello`](https://github.com/ram-electronic/fess-ds-trello).
- Sweble and its dependencies are bundled into the plugin jar with
  `maven-shade-plugin` (about 3.8 MB), so installing the single jar is still
  enough. `commons-lang3`, `commons-io`, `slf4j` and `log4j` are not bundled;
  Fess provides them at runtime.

## Testing

`mvn test` runs all tests. Surefire's `failIfNoTests` (from upstream) fails
the build if none are discovered, so a test method missing `@Test` can't
silently turn the suite into a no-op again.

## Releases

`.github/workflows/release.yml` builds the jar and attaches it to a GitHub
Release on every `v*` tag push, signed with a
[GitHub Artifact Attestation](https://docs.github.com/en/actions/security-guides/using-artifact-attestations-to-establish-provenance-for-builds):

```
gh attestation verify fess-ds-wikipedia-<version>.jar --repo ram-electronic/fess-ds-wikipedia
```
