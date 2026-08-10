# TombstoneProtos - protobuf classes use reflection for toString() and field access.
# R8 must not obfuscate/remove getter methods (e.g. getHumanReadable) used by GeneratedMessageV3.
# Include all inner classes: Cause, Cause$Builder, etc.
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos { *; }
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos$* { *; }
-keep class mega.privacy.android.data.model.protobuf.TombstoneProtos$*$* { *; }

# Gson-persisted domain models: use keep rules instead of @SerializedName to avoid upgrade
# issues when JSON keys change. Keeping these classes prevents obfuscation so Gson uses
# property names consistently across app versions.
-keep class mega.privacy.android.domain.entity.chat.messages.reactions.Reaction { *; }
-keep class mega.privacy.android.domain.entity.user.UserCredentials { *; }
-keep class mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation { *; }