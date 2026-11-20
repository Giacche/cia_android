# Persistencia local y seguridad de datos

Todo vive en el módulo **`:storage`** (Android library). `core` solo define los **contratos** (`SecureStorage`, `FeatureFlags`), así que sigue siendo Kotlin puro y las features no dependen de Room ni de DataStore.

## Qué se guarda y dónde

| Dato | Tecnología | Por qué |
|---|---|---|
| Preferencias y feature flags locales | **DataStore** (Preferences) | Asíncrono, transaccional y sin los problemas de `SharedPreferences` en el hilo principal |
| Respuestas del BFF para mostrar sin conexión | **Room** (`JsonCache`) | Consultas y vencimiento por tiempo; guarda JSON crudo para no migrar la base cada vez que cambia un modelo |
| Secretos (tokens, claves de sesión) | **Android Keystore + AES-256-GCM** sobre DataStore | La clave no sale del hardware seguro y el texto plano nunca toca el disco |

Regla: **nada sensible en Room ni en DataStore sin cifrar.** Los secretos van siempre por `SecureStorage`.

## Cifrado con Android Keystore

```
SecureStorage (core) ← EncryptedSecureStorage ── AesGcmCipher ── AndroidKeystoreKeyProvider
                              │                        │                  │
                           DataStore             IV nuevo por         clave AES-256 que
                       (valor cifrado en        cada cifrado         vive en el Keystore
                         Base64, "secure_*")
```

- **`AndroidKeystoreKeyProvider`:** crea o recupera una clave AES-256 (GCM, sin padding). La app nunca ve el material de la clave; solo pide cifrar o descifrar.
- **`AesGcmCipher`:** cifrado autenticado. El resultado es `IV (12 bytes) || cifrado + etiqueta`. El IV es nuevo en cada operación; **reutilizar un IV con la misma clave rompe GCM**. No depende de Android: recibe la clave como función, y en los tests de JVM se usa una clave de software.
- **`EncryptedSecureStorage`:** guarda los valores cifrados en DataStore con el prefijo `secure_`.

### Si no se puede descifrar

Un valor que falla al descifrar (clave perdida o dato alterado) se **trata como inexistente y se borra**. Es preferible pedir iniciar sesión de nuevo que fallar o devolver basura. Pasa, por ejemplo, al restaurar un backup en otro dispositivo, porque la clave del Keystore **no viaja** con el backup.

## Pendiente del lado de la app host

- **Backups:** excluir el archivo de DataStore de `android:allowBackup` / reglas de extracción de datos. Si el valor cifrado se restaura sin su clave, queda inservible.
- **Cierre de sesión:** llamar a `SecureStorage.clear()` y, si corresponde, `AndroidKeystoreKeyProvider.delete()`.
- **Autenticación biométrica:** la clave actual no exige biometría. Para datos que la requieran hay que agregar `setUserAuthenticationRequired(true)` y un `BiometricPrompt`; no está implementado.
- **Room sin cifrar:** la base no está cifrada. Para cifrarla haría falta SQLCipher.

## Qué se prueba

| Qué | Dónde | Cómo |
|---|---|---|
| Cifrado AES-GCM (ida y vuelta, IV distinto, dato alterado, clave distinta) | JVM | `AesGcmCipherTest` |
| Almacén cifrado (el valor en disco no es el original, recuperación ante clave perdida o dato corrupto) | JVM | `EncryptedSecureStorageTest` con un DataStore real sobre un archivo temporal |
| Caché con vencimiento | JVM | `JsonCacheTest` con un DAO falso y reloj controlado |
| Flags en DataStore | JVM | `DataStoreFeatureFlagsTest` |
| Clave real en el Android Keystore y base Room | Dispositivo | `androidTest` de `:storage` (ver abajo) |
