# fess-ds-wikipedia (ram-electronic fork)

A fork of [`codelibs/fess-ds-wikipedia`](https://github.com/codelibs/fess-ds-wikipedia)
(forked at its `fess-ds-wikipedia-15.8.0` tag), patched to fix a
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
`getPlainText()`:

- Strips section headers (`== Heading ==`, any level), keeping the heading
  text but dropping the `=` markup.
- Strips leading bullet/numbered/definition list markers (`*`, `#`, `:`,
  `;`) at the start of a line.
- Strips HTML comments (`<!-- ... -->`).
- Handles `<ref>` tags that carry attributes (`<ref name="...">`) or are
  self-closing (`<ref .../>`) — upstream only matched the bare
  `<ref>...</ref>` form.
- Correctly resolves piped links (`[[Link|Text]]` → `Text`, including
  multi-pipe forms like `[[File:x.jpg|thumb|Caption]]` → `Caption`) instead
  of the fragile single-word-only regex upstream used.

See `WikiTextParserTest.java` for the added test cases.

## Everything else

Unmodified from upstream — same crawl config format, same field mapping,
same `WikipediaDataStore` handler name. See
[`codelibs/fess-ds-wikipedia`](https://github.com/codelibs/fess-ds-wikipedia)
for general usage.

## Requirements

- Java 21+, Maven 3.x
- Fess 15.8.0 (pinned via `pom.xml`'s `<parent>` version). This plugin's own
  version (`pom.xml`'s top-level `<version>`, and its release tags) is
  independent semver, not tied to Fess's version — same reasoning as
  [`ram-electronic/fess-ds-trello`](https://github.com/ram-electronic/fess-ds-trello).

## Releases

`.github/workflows/release.yml` builds the jar and attaches it to a GitHub
Release on every `v*` tag push, signed with a
[GitHub Artifact Attestation](https://docs.github.com/en/actions/security-guides/using-artifact-attestations-to-establish-provenance-for-builds):

```
gh attestation verify fess-ds-wikipedia-<version>.jar --repo ram-electronic/fess-ds-wikipedia
```
