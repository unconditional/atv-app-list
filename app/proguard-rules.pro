# App has no third-party dependencies; default R8 rules are sufficient.
# Keep line numbers for readable crash reports (no crash reporter yet, but cheap).
-keepattributes SourceFile,LineNumberTable

# Entry points reached via the framework (manifest components) are kept automatically.

# Suppress noise for framework-injected callbacks we do not use.
-dontwarn org.jetbrains.annotations.**
