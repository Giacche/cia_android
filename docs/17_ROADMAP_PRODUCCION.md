# Roadmap hacia producción

Qué está hecho, qué se verificó y cómo, y qué falta. Pensado como una lista honesta: **un ✅ significa que se probó**, no que se escribió.

## Estado

| Área | Estado | Evidencia |
|---|---|---|
| Arquitectura (MVVM, flujo unidireccional) | ✅ | 10 features migradas, `docs/10_PLAN_DE_MIGRACION.md` |
| Compose + Coroutines/Flow | ✅ | `StateFlow`, `Channel`, tests con Turbine |
| Modularización | ✅ | 12 módulos con reglas de dependencia |
| Tests unitarios | ✅ | **305 tests, 0 fallos** en 12 módulos |
| **MockK** | ✅ | `CIAHttpClientTest` (10) y `CompositeAnalyticsTrackingTest` (3) |
| **Detekt + ktlint** | ✅ | Pasan en todos los módulos, sin baseline (`12_CALIDAD_Y_CI.md`) |
| **CI (GitHub Actions)** | 🟡 | Workflow escrito (`quality`, `test`, `build`); **no se corrió en GitHub** |
| **Persistencia** (DataStore, Room) | ✅ | `:storage`: 19 tests de JVM (cifrado, almacén, caché, flags) y 8 de dispositivo (Room y Keystore reales) |
| **KeyStore y datos sensibles** | ✅ | AES-256-GCM con clave del Keystore: 11 tests de JVM del cifrado y del almacén, y 4 de dispositivo con el Keystore real |
| **Feature flags y A/B** | ✅ | `FeatureFlags`, `Experiment`, reparto probado con 10.000 usuarios |
| Analítica con varios destinos | ✅ | `CompositeAnalyticsTracking`; adaptadores de Firebase/Amplitude **no incluidos** |
| **Release firmado con R8** | ✅ | APK y AAB generados y firmados con un keystore de prueba; el release se instaló y recorrió Home, movimientos, QR y la pantalla de monto en el emulador, sin crashes (después de corregir una regla de R8) |
| UI tests y accesibilidad | ✅ | `SandboxUiTest`: 4 tests en el emulador (Android 16), con verificación de que todo control clickeable tenga etiqueta |
| **Estrategia de ramas** | ✅ | `13_TRUNK_BASED.md` |

## Pendiente

| Prioridad | Tarea | Por qué no está hecha |
|---|---|---|
| Alta | **Correr el CI en GitHub** y arreglar lo que falle | Hay que subir el repo; el workflow nunca corrió |
| Alta | **Tests instrumentados en CI** (emulador con `reactivecircus/android-emulator-runner`) | Hoy se corren solo en local |
| Alta | **Adaptadores de Firebase y Amplitude** | Necesitan `google-services.json` y claves de API |
| Media | **Crash reporting** (Crashlytics u otro) conectado a `EventsReporting` | Depende de la cuenta |
| Media | **Hilt o Dagger en la app host** | Ver nota abajo |
| Media | **Profiling**: Macrobenchmark y Baseline Profile del Home | Necesita un dispositivo físico para números confiables |
| Media | **Publicación**: app en Play Console y pipeline de subida | Depende de la cuenta (`16_PUBLICACION_PLAY.md`) |
| Media | **Biometría** para claves del Keystore sensibles | No hay un dato que lo requiera todavía |
| Baja | **SonarQube** y cobertura (Kover o JaCoCo) | Depende del servidor del equipo |
| Baja | **Room cifrada** (SQLCipher) | Solo si se guardan datos sensibles en la base |
| Baja | **Migraciones de Room** con `MigrationTestHelper` | Está en la versión 1; hace falta al cambiar el esquema |

### Nota sobre Hilt y Dagger

La inyección es **manual a propósito**: `CIAContainer` arma todo en un único archivo, es trivial de leer y se traduce igual a iOS. Hilt tiene sentido en la **app host**, que es la que construye `CIADependencies`, y no dentro de los módulos de feature (que son una librería que el host monta). Si se agrega, va en `:app` y entrega las dependencias a `CIAModule`. No se hizo porque no aporta a la librería y requiere ajustar versiones de KSP y del plugin con este Kotlin.

## Cómo verificar todo

```bash
./gradlew ktlintCheck detekt                      # estilo y calidad
./gradlew testDebugUnitTest :core:test            # 305 tests unitarios
./gradlew :storage:connectedDebugAndroidTest \
          :app:connectedDebugAndroidTest          # requiere un emulador o dispositivo
./gradlew :app:assembleRelease :app:bundleRelease # con las propiedades de firma
```
