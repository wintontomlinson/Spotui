# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep enough attributes for reflection, serialization and crash readability.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*

# ---------------------------------------------------------------------------
# WebView JS bridges (reflection via @JavascriptInterface)
# ---------------------------------------------------------------------------
# The YouTube cipher/potoken/sabr solvers and the Spotify web-player bridge
# (SpotuiBridge) expose methods to JavaScript running inside a WebView. R8
# must never rename or strip these or the JS -> native callbacks break at
# runtime (playback stream resolution + Widevine probe depend on them).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# Keep the bridge classes themselves (including anonymous bridge objects that
# only ever get called from JS by name).
-keep class com.metrolist.music.utils.cipher.** { *; }
-keep class com.metrolist.music.utils.potoken.** { *; }
-keep class com.metrolist.music.utils.sabr.** { *; }
-keep class com.music.spotui.di.SpotifyWebPlayer** { *; }

# ---------------------------------------------------------------------------
# kotlinx.serialization (@Serializable models in :innertube and :spotify)
# ---------------------------------------------------------------------------
# The vendored stream libraries decode YouTube/Spotify JSON responses with
# kotlinx.serialization. The plugin generates a synthetic $$serializer plus a
# Companion that exposes it reflectively; both must survive R8.
-keepclassmembers class **$$serializer {
    *** descriptor;
}
-keepclasseswithmembers,allowshrinking class * {
    @kotlinx.serialization.Serializable <fields>;
}
-if @kotlinx.serialization.Serializable class **
-keep,includedescriptorclasses class <1>$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the generated serializers and @Serializable model classes outright for
# the vendored stream modules (safest — these are reached only via reflection
# through the serialization framework).
-keep,includedescriptorclasses class com.metrolist.innertube.**$$serializer { *; }
-keep,includedescriptorclasses class com.metrolist.spotify.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.metrolist.innertube.** { *; }
-keep @kotlinx.serialization.Serializable class com.metrolist.spotify.** { *; }
# Serialization runtime references.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ---------------------------------------------------------------------------
# Vendored stream libraries (:innertube / :spotify / com.metrolist.*)
# ---------------------------------------------------------------------------
# These modules carry request/response model classes, enums and stream helpers
# (cipher / potoken / sabr) that are reached reflectively through the
# serialization framework and the WebView bridges. Keep them wholesale — the
# size win here is R8 on the app + Compose + resource shrinking, not stripping
# the stream resolver (correctness beats a few KB).
-keep class com.metrolist.innertube.** { *; }
-keep class com.metrolist.spotify.** { *; }
-keep class com.metrolist.music.** { *; }

# ---------------------------------------------------------------------------
# Enums — keep values()/valueOf() (used reflectively by serialization & prefs)
# ---------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# Media3 / ExoPlayer
# ---------------------------------------------------------------------------
# Media3 instantiates renderers/extractors/decoders and the audio effects
# (Equalizer / LoudnessEnhancer) via reflection and manifest-declared
# services. Keep the whole surface to avoid breaking playback.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---------------------------------------------------------------------------
# Glide (image loading)
# ---------------------------------------------------------------------------
# No @GlideModule is declared in this app, but Glide still looks up registry
# components and the generated API reflectively; keep its runtime + any
# generated module should one appear.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ---------------------------------------------------------------------------
# Hilt / Dagger (generated components reached reflectively)
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclasseswithmembers class * {
    @dagger.hilt.** <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-dontwarn dagger.hilt.**

# ---------------------------------------------------------------------------
# Jetpack Compose runtime
# ---------------------------------------------------------------------------
# The Compose Gradle plugin already ships consumer rules, but keep the runtime
# defensively; stripping it can break @Composable reflection in release.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Ktor + OkHttp + Timber + NewPipe + Brotli (transitive networking stack)
# ---------------------------------------------------------------------------
# The vendored modules talk HTTP through Ktor (OkHttp engine). These rely on
# reflection/optional transitive classes; keep them quiet and intact.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class org.schabi.newpipe.** { *; }
-dontwarn org.schabi.newpipe.**
-dontwarn org.brotli.**

# NewPipe bundles the Rhino (org.mozilla.javascript) JS engine to run the
# YouTube cipher/n-transform. Rhino references desktop-only java.beans.* and
# javax.script.* classes that don't exist on Android; they are only touched by
# an optional JSON converter / script-engine factory we never use, so silence
# the missing-class checks instead of failing R8.
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-keep class org.mozilla.javascript.** { *; }

# ---------------------------------------------------------------------------
# ML Kit (on-device translate + language id)
# ---------------------------------------------------------------------------
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------------------
# Kotlin metadata / coroutines
# ---------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
