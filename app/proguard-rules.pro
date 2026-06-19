# Kotlin
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.Metadata { *; }

# Hilt
-keepclasseswithmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }

# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep class com.google.gson.** { *; }
-keep class com.calculocorridas.data.network.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# Billing
-keep class com.android.billingclient.** { *; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# App domain models
-keep class com.calculocorridas.domain.entities.** { *; }
-keep class com.calculocorridas.data.billing.** { *; }

# Prevent stripping accessibility service
-keep class com.calculocorridas.services.** { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
