# IntelliJ OpenVADL plugin

Provides language support for the OpenVADL language in IntelliJ IDEA.

## Features
- Syntax highlighting
- Diagnostics

## Requirements

This plugin requires the OpenVADL LSP to be installed on your system.

## Quirks

1) This is an early implementation and at the moment mostly targeted for OpenVADL developers.
2) You can only have a single instance of the lsp running on your machine.

## Build a release

```bash
./gradlew buildPlugin
```