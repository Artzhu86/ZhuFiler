-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

-keep class io.github.rosemoe.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-keep class org.joni.** { *; }
-keep class jregex.** { *; }

-keep class com.github.chrisbanes.photoview.** { *; }

-dontwarn kotlin.Cloneable$DefaultImpls
