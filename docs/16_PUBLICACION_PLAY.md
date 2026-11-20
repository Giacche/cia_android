# Publicación en Google Play: ciclo de vida y estado del proyecto

Este documento describe el ciclo completo y marca **qué está hecho en el repo y qué depende de la cuenta de Play Console** (que no está configurada).

## Ciclo de vida

```
Código → CI verde → Build firmado (AAB) → Track interno → Cerrado (QA) → Abierto → Producción por etapas → Monitoreo
```

| Etapa | Qué pasa | Estado en este repo |
|---|---|---|
| **1. Versionado** | `versionCode` entero creciente (Play rechaza repetir uno) y `versionName` legible | ✅ Por propiedades: `-PCIA_VERSION_CODE=7 -PCIA_VERSION_NAME=1.2.0`. Sin ellas, 1 y `0.1.0` |
| **2. Firma** | El AAB se firma con la clave de subida (*upload key*) | ✅ Por propiedades o variables de entorno `CIA_STORE_FILE`, `CIA_STORE_PASSWORD`, `CIA_KEY_ALIAS`, `CIA_KEY_PASSWORD`. Sin ellas se genera sin firmar. Los `.jks` están en `.gitignore` |
| **3. Optimización** | R8 reduce y ofusca el código; se reducen recursos | ✅ `isMinifyEnabled` e `isShrinkResources` activos, con reglas en `app/proguard-rules.pro` |
| **4. Formato** | Play exige **Android App Bundle (AAB)**, no APK | ✅ `./gradlew :app:bundleRelease` |
| **5. Subida** | A un track de prueba en Play Console | ❌ Requiere la app creada en Play Console y credenciales |
| **6. Revisión y despliegue por etapas** | Porcentaje creciente de usuarios; se puede pausar | ❌ Se hace en Play Console |
| **7. Monitoreo** | Android vitals (ANR, crashes), reportes, reseñas | ❌ Depende de la cuenta y de un crash reporting |

## Comandos

```bash
# Debug (sin firmar, para probar)
./gradlew :app:assembleDebug

# Release firmado: APK y AAB
./gradlew :app:assembleRelease :app:bundleRelease \
  -PCIA_STORE_FILE=/ruta/upload.jks -PCIA_STORE_PASSWORD=... \
  -PCIA_KEY_ALIAS=upload -PCIA_KEY_PASSWORD=... \
  -PCIA_VERSION_CODE=12 -PCIA_VERSION_NAME=1.3.0
# Salida: app/build/outputs/bundle/release/app-release.aab
```

En CI las credenciales van como **secretos** del repositorio, nunca en el código. Para pasar el keystore, se guarda en Base64 como secreto y se reconstruye en el job.

## Lo que hay que saber de R8

R8 achica y ofusca, pero **puede romper lo que se resuelve por reflexión**. En este proyecto ocurrió de verdad:

- **Síntoma:** el release se cerraba al abrir con `IllegalArgumentException: Cannot find class with name "com.ueno.cia.core.model.ChargeType"`.
- **Causa:** la navegación con rutas tipadas busca las clases por su nombre completo; R8 renombró un enum.
- **Arreglo:** reglas `-keep` para las clases `@Serializable` de CIA y sus enums.
- **Por qué importa:** ningún test unitario lo detecta, porque corren sin R8. **Hay que probar el release minificado en un dispositivo** antes de publicar.

Los stack traces del release salen ofuscados. Se conservan los números de línea y el archivo `mapping.txt` (en `app/build/outputs/mapping/release/`) se sube a Play Console o al crash reporting para desofuscarlos. **Guardar el `mapping.txt` de cada versión publicada.**

## Qué falta para publicar de verdad

1. **App en Play Console** con su `applicationId` definitivo (el actual, `com.ueno.cia.sandbox`, es del sandbox).
2. **Play App Signing:** Google guarda la clave final; nosotros firmamos con la clave de subida.
3. **Ficha de la tienda:** descripción, capturas, política de privacidad y formulario de **seguridad de los datos**.
4. **`targetSdk` vigente:** Play exige apuntar a una versión reciente; hoy es 36.
5. **Pipeline de publicación:** un job que suba el AAB al track interno (Gradle Play Publisher o Fastlane).
6. **Crash reporting y vitals:** el proyecto no los tiene integrados.
7. **Pruebas previas al lanzamiento:** la consola corre el AAB en dispositivos reales; hay que revisar su informe.

## Backups

`android:allowBackup="false"` está activo. Es lo correcto mientras haya datos cifrados con el Keystore: la clave no viaja con el backup y los datos restaurados serían ilegibles.
