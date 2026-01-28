# IntelliJ  OpenVADL plugin

Provides language support for the OpenVADL language in IntelliJ IDEA.

## Features
- Syntax highlighting
- Diagnostics

## Requirements

This plugin requires the OpenVADL LSP to be installed on your system. The `openvadl` command must be available in your PATH.

## Quirks

1) This is an early implementation and at the moment mostly targeted for OpenVADL developers.
2) You can only have a single instance of the lsp running on your machine.
3) Diagnostics are reported in a single line (further investigation needed).

## Build a release

```bash
./gradlew buildPlugin
```