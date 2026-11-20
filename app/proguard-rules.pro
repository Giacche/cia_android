# Reglas de R8 para el release del sandbox.
# Las librerías (OkHttp, Coil, Compose, DataStore, Room) traen sus propias reglas por consumidor.
# Acá solo va lo que depende de cómo usamos kotlinx.serialization.

# Se conservan los serializadores generados y los atributos que la reflexión de la serialización necesita.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Modelos de CIA: `serializer()` del companion de cada @Serializable.
-keep,includedescriptorclasses class com.ueno.cia.**$$serializer { *; }
-keepclassmembers class com.ueno.cia.** { *** Companion; }
-keepclasseswithmembers class com.ueno.cia.** { kotlinx.serialization.KSerializer serializer(...); }

# Navigation Compose con rutas tipadas y los argumentos JSON de CIA buscan las clases por su `serialName`
# (nombre completo) con reflexión. Si R8 las renombra o las elimina, la app falla al navegar con
# "Cannot find class with name ...". Por eso se conservan, con nombre y campos, todas las @Serializable de CIA y sus enums.
-keep @kotlinx.serialization.Serializable class com.ueno.cia.** { *; }
-keep enum com.ueno.cia.** { *; }

# Se conservan los números de línea para que los stack traces del release sean legibles tras el mapeo.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
