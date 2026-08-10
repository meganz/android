# Workaround for androidx.webkit prefetch crash (AND-23954).
# The Google Mobile Ads SDK triggers a WebView URL prefetch flow that accesses
# androidx.webkit / Chromium support-lib boundary classes reflectively
# (BoundaryInterfaceReflectionUtil). R8 shrinking/obfuscation of these classes
# breaks the reflective lookup and causes a NullPointerException in
# PrefetchOperationCallbackAdapter on release/minified builds.
# See https://github.com/googleads/googleads-mobile-unity/issues/4207
-keep class androidx.webkit.** { *; }
-keep class org.chromium.support_lib_boundary.** { *; }
