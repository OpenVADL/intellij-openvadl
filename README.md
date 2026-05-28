# IntelliJ OpenVADL plugin

Provides language support for the OpenVADL language in IntelliJ IDEA.

## Features
- Syntax highlighting (via TextMate grammar)
- Diagnostics (via LSP)

## Requirements

This plugin requires:
- The OpenVADL LSP to be installed on your system

## Quirks

1) This is an early implementation and at the moment mostly targeted for OpenVADL developers.
2) Syntax highlighting is provided by the TextMate grammar, not the LSP

## Build a release

```bash
./gradlew buildPlugin
```