# Release code shrinking and obfuscation.
# Android/Compose/Media3 libraries supply their own consumer ProGuard rules.
# Keep stack trace line numbers for crash diagnosis without retaining source names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
