# R8 rules for the `remote` build type (minify + resource shrink).
# Goal: shrink the 73 MB unminified dex for fast remote (Tailscale/DERP) installs,
# while keeping reflection-driven bits (kotlinx.serialization, Ktor) intact.

# --- kotlinx.serialization (FatSecret JSON via Ktor) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep generated serializers and serializer() accessors for our own classes.
-keepclassmembers class com.mettyoung.fitbro.** {
    *** Companion;
}
-keepclasseswithmembers class com.mettyoung.fitbro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mettyoung.fitbro.**$$serializer { *; }

# Serializable data classes (defensive: keep names + members).
-keep @kotlinx.serialization.Serializable class com.mettyoung.fitbro.** { *; }

# --- Ktor / coroutines (mostly covered by consumer rules; keep defensive) ---
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-dontwarn io.ktor.**

# --- Keep crash-readable line numbers for a debuggable remote build ---
-keepattributes SourceFile,LineNumberTable
