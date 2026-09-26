# Routes de navigation typées (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.apexus.storagelens.**$$serializer { *; }
-keepclassmembers class com.apexus.storagelens.** {
    *** Companion;
}
-keepclasseswithmembers class com.apexus.storagelens.** {
    kotlinx.serialization.KSerializer serializer(...);
}
