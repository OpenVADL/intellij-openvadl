# IntelliJ  OpenVADL plugin

Provides language support for the OpenVADL language in IntelliJ IDEA.

## Quirks

1) This is an early implementation and at the moment mostly targeted for OpenVADL developers. As such it currently uses 
a custom (pre-release) version of the openvadl-lsp.
2) The lsp is currently the JVM version and not the native build. 
3) You can only have a single instance of the lsp running on your machine.
4) Diagnostics are reported in a single line (further investigation needed).

## Build a release

```bash
./gradlew buildPlugin
```

## Updating the LSP

We update the underlying LSP quite frequently, so there is an automated script you can use.
```bash
uv run update_lsp.py
```