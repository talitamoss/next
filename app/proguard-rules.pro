# File location: app/proguard-rules.pro

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.domain.app.**$$serializer { *; }
-keepclassmembers class com.domain.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.domain.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Briar P2P
-keep class org.briarproject.** { *; }
-keep class org.torproject.** { *; }
-dontwarn org.briarproject.**
-dontwarn org.torproject.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# OkHttp (used by Briar)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Bouncy Castle (used by Briar)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Java Crypto
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep data classes
-keep class com.domain.app.network.protocol.** { *; }
-keep class com.domain.app.core.data.** { *; }

# Reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
