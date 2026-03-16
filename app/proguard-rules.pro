# Conservar información de línea para stack traces legibles en producción
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Modelos de datos (usados por CsvStorage vía nombre de campo)
-keep class com.controltemperatura.model.** { *; }

# BroadcastReceivers registrados en el Manifest
-keep class com.controltemperatura.AlarmReceiver { *; }
-keep class com.controltemperatura.BootReceiver  { *; }

# Vistas personalizadas referenciadas desde XML
-keep class com.controltemperatura.TempChartView { *; }

# Material Components y AndroidX — ya cubiertos por el archivo base,
# pero se explicitan por claridad
-dontwarn com.google.android.material.**
-dontwarn androidx.**
