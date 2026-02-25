# TombstoneProtos - protobuf classes use reflection for toString() and field access.
# R8 must not obfuscate/remove getter methods (e.g. getHumanReadable) used by GeneratedMessageV3.
# Include all inner classes: Cause, Cause$Builder, etc.
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos { *; }
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos$* { *; }
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos$*$* { *; }
