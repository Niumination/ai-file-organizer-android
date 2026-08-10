# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature

# App classes (Kotlin serialization models included)
-keep class com.arena.aifileorganizer.** { *; }

# Kotlinx serialization
-keepclassmembers class com.arena.aifileorganizer.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.arena.aifileorganizer.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# PDFBox-Android
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**

# ML Kit / Firebase components carry their own consumer rules
-dontwarn com.google.mlkit.**
