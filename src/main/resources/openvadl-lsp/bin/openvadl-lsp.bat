@echo off
set DIR="%~dp0"
set JAVA_EXEC="%DIR:"=%\java"



pushd %DIR% & %JAVA_EXEC% %CDS_JVM_OPTS% -Dslf4j.internal.verbosity=WARN -p "%~dp0/../app" -m openvadl.lsp/vadl.lsp.Main  %* & popd
