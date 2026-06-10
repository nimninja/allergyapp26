# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ingredientchecker.app.data.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# SnakeYAML
-dontwarn org.yaml.snakeyaml.**
