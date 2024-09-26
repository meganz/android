# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclassmembers
-verbose
-dontpreverify

##########
# Enums #
#########
-keepclassmembers enum * {
    *;
}

##############
# Parcelable #
##############
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

#################
# Serializable #
################
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#########################
# Google Play Services #
########################
-keep class com.google.android.gms.* {  *; }
-dontwarn com.google.android.gms.**
-dontnote **ILicensingService
-dontnote com.google.android.gms.**
-dontwarn com.google.android.gms.ads.**

########################
# Firebase Crashlytics #
########################
-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable, Signature
-keep public class * extends java.lang.Exception
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

-keep class org.apache.http.** { *; }
-keepclassmembers class org.apache.http.** {*;}
-dontwarn org.apache.**

-keep class android.net.http.** { *; }
-keepclassmembers class android.net.http.** {*;}
-dontwarn android.net.**

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##############
# Coroutines #
##############
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation


##################
# WebRTC Library #
#################
-keep class org.webrtc.**  { *; }
-dontwarn org.webrtc.Dav1dDecoder


#####################
# MEGA SDK Bindings #
#####################
-keep class nz.mega.sdk.** { *; }


#####################
# SQLCipher Library #
#####################
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

#####################
# Protobuf Library #
#####################
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

#####################
# Local Lifecycle Owner for version 2.8.2 #
#####################
-if public class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static *** getLocalLifecycleOwner();
}
-keep public class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static *** getLocalLifecycleOwner();
}

#####################
# Keep Fragment classes #
#####################
-keep class * extends androidx.fragment.app.Fragment {
    # Keep the public no-argument constructor while allowing other methods to be optimized.
    <init>();
}

#####################
# LiveEventBus remove this rule when removing LiveEventBus
# ExternalLiveData.java getFieldObservers and callMethodPutIfAbsent
#####################
-keep class androidx.arch.core.internal.SafeIterableMap { *; }

-keep public class org.slf4j.** { *; }
-keep public class ch.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.mail.internet.** { *; }
-dontwarn javax.mail.**

#####################
# BannerViewPager #
#####################
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.viewpager2.widget.** { *; }

#####################
# Matcher #
#####################
-keep class org.hamcrest.** { *; }
-dontwarn org.hamcrest.**

#####################
# Google Ads #
#####################
-keep class com.google.android.gms.internal.ads.** { *; }
-keep public class com.google.android.gms.ads.** {
    public *;
}

-keep public class com.google.ads.** {
    public *;
}

# For mediation
-keepattributes *Annotation*

# Other required classes for Google Play Services
# Read more at http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
   public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
   @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
   public static final ** CREATOR;
}
