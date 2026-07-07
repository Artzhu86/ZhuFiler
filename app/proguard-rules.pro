-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

-keep class io.github.rosemoe.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-keep class org.joni.** { *; }
-keep class jregex.** { *; }

-keep class io.getstream.photoview.** { *; }

-keepclassmembers class androidx.appcompat.widget.Toolbar {
    android.widget.TextView mTitleTextView;
}

# 7-Zip-JBinding uses JNI + reflection to load and call the native library.
-keep class net.sf.sevenzipjbinding.** { *; }
-dontwarn net.sf.sevenzipjbinding.**

-dontwarn kotlin.Cloneable$DefaultImpls
