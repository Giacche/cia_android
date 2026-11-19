# Migración de Smart Charge Microapp (CIA) a Android nativo

Guía para portar la microapp Flutter `smart_charge_microapp` ("Cobros in App", squad `ChargeInApp`) a Android nativo. Es el equivalente Android de las guías de `cia_ios/CIA/docs/` y reutiliza sus decisiones de arquitectura: la lógica, los contratos del BFF, los mocks y el inventario de pantallas son los mismos.

> Estado: propuesta. Las decisiones marcadas como **[Decidir]** hay que cerrarlas con el equipo y con la app host antes de la fase 0.

---

## 1. Tecnología elegida y por qué

| Tema | Elección | Por qué |
|------|----------|---------|
| Lenguaje | **Kotlin** | Es el lenguaje oficial de Android. Tiene coroutines, null-safety y data classes inmutables, y se parece mucho a Dart y a Swift. |
| UI | **Jetpack Compose** (Material 3 como base, design system propio encima) | Es declarativa como Flutter: `@Composable` ≈ `Widget`, `State` ≈ `setState`/Riverpod. Se porta casi 1:1 desde los widgets `Gu*` y es el camino oficial de Google. Con Views/XML duplicaríamos el código y el modelo mental sería el opuesto al de Flutter. |
| Arquitectura | **MVVM + flujo unidireccional de datos** (UI → ViewModel → UseCase → Repository) | Es la arquitectura recomendada por Google y mapea directo a `Screen → Notifier → UseCase → Repository` de Flutter y a `ViewController → ViewModel → UseCase → Repository` de iOS. |
| Estado | **`StateFlow<UiState>`** en el `ViewModel` + `SharedFlow`/`Channel` para eventos de navegación | Equivale a `AsyncValue` de Riverpod y a `BehaviorRelay`/`Signal` de iOS. `UiState` es una `sealed interface` (`Loading`, `Error`, `Data`). |
| Asincronía | **Kotlin Coroutines + Flow** | Es el estándar actual. RxJava queda como legado en Android, así que no se replica RxSwift. Las `suspend fun` equivalen a `Future`/`async`. |
| Inyección | **Hilt** (alternativa: Koin) | Validación en compilación, integración con `ViewModel` y `Navigation`, y es el estándar de Google. Koin es más simple pero valida en runtime. **[Decidir]** según lo que use la app host. |
| Red | **Retrofit + OkHttp** + **kotlinx.serialization** | Retrofit y OkHttp son el estándar. Un `Interceptor` de OkHttp reemplaza al adapter de headers multitenant (`CIAHTTPClient`). kotlinx.serialization es multiplataforma y no usa reflexión, y reemplaza a `json_serializable`/`Codable`. Si el host ya expone su propio cliente, se envuelve en un `HttpClient` propio como en iOS. |
| Navegación | **Navigation Compose** (rutas tipadas con `@Serializable`) | Equivale a `go_router`/`CIARouter`. Soporta deeplinks y argumentos tipados. |
| Imágenes / íconos | **Coil** para remotas. Los SVG del design system se convierten a **VectorDrawable / `ImageVector`** | Los íconos e ilustraciones ya se extrajeron como SVG para iOS y se reutilizan como fuente. |
| Build | **Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`)** | Versiones centralizadas, equivalente a `pubspec.yaml` + `melos`. |
| Tests | **JUnit + MockK + Turbine + `kotlinx-coroutines-test`**. UI con `compose-ui-test`. Snapshots con **Paparazzi** o **Roborazzi** | MockK es el `mocktail` de Kotlin. Turbine testea `Flow`. Los snapshots no necesitan emulador. |
| Calidad | **Detekt + ktlint (Spotless)**, warnings como error en CI | Equivale a `very_good_analysis` + `dart format`. |
| Analytics / logs | Mismo contrato con el host (Amplitude) detrás de una interfaz `Analytics` + `Timber` para logs | Igual que `Analytics`/`CIALogger` en iOS. |
| l10n | `strings.xml` (`values/` y `values-es/`) | Se generan desde `l10n/app_es.arb`. |
| Min SDK | **26** (propuesta) | Cubre prácticamente todo el parque activo y evita desugaring pesado. **[Decidir]** con el host. |

### Alternativas descartadas
- **Views/XML + RxJava**: espejo del stack iOS, pero es tecnología en retirada y el doble de código para el mismo resultado.
- **Kotlin Multiplatform + Compose Multiplatform**: reutilizaría dominio y red entre Android e iOS. Lo reabriría solo si se quisiera una sola base en el futuro. Hoy iOS ya está hecho en Swift, así que no compensa.
- **Seguir con Flutter embebido**: es la alternativa si el objetivo no fuera específicamente "nativo".

---

## 2. Equivalencias Flutter ↔ iOS ↔ Android

| Concepto | Flutter | iOS (CIA) | Android |
|----------|---------|-----------|---------|
| Lenguaje | Dart 3.9 | Swift 5 | Kotlin |
| UI | Widgets `Gu*` | UIKit programático + DS propio | Compose + DS propio |
| Patrón | Screen → Notifier → UseCase → Repository | VC → ViewModel → UseCase → Repository | Screen → ViewModel → UseCase → Repository |
| Estado | Riverpod `AsyncValue` | `BehaviorRelay` → `Driver` | `StateFlow<UiState>` |
| Eventos de navegación | `BaseCoordinator` | `Signal<Destination>` | `SharedFlow<Destination>` / `Channel` |
| Navegación | `go_router` | `CIARouter` + `CIARoute` | Navigation Compose + rutas `@Serializable` |
| Red | `IMultitenantHttpClient` | `HTTPClient` → `Single` | Interfaz `HttpClient` → `suspend` (o Retrofit) |
| Modelos | `json_serializable` | `Codable` | `@Serializable` data classes |
| Asincronía | `Future` / `Timer` | `Single`, `Observable.interval` | `suspend`, `Flow`, `delay`/`ticker` |
| DI | Providers | Inyección manual | Hilt |
| Mocks de desarrollo | `MockConfig` | `MockHTTPClient` | `MockInterceptor` (OkHttp) con los mismos JSON |
| Tests | `flutter_test` + `mocktail` | XCTest + RxTest | JUnit + MockK + Turbine |
| Lint | `very_good_analysis` | SwiftLint | Detekt + ktlint |

---

## 3. Estructura del proyecto (multi-módulo)

Se repite la decisión D1 de iOS: **la lógica vive en un módulo reutilizable** (`cia-kit`) que la app host puede embeber, y la app `app` es solo un **sandbox** con mocks.

```
cia-android/
├── app/                      # Sandbox: Application, MainActivity, pantalla "Mocks", selector de tenant
├── cia-kit/                  # Módulo público: CIAModule (punto de entrada) + CIAEntryFlow
├── core/
│   ├── network/              # HttpClient, interceptor multitenant, manejo de errores, MockInterceptor
│   ├── model/                # Modelos compartidos (@Serializable) y envelope `data`
│   ├── analytics/            # Analytics, CIALogger
│   └── common/               # Result/UiState, formatters (moneda, fechas), deeplink resolver
├── designsystem/             # Tokens, tipografía, componentes Compose (botones, skeleton, banner, bottom sheet…)
├── feature/
│   ├── home/  qr/  inputamount/  validatelimit/  link/  congrats/
│   ├── movements/  detail/  onboarding/  profile/  products/  addons/
│   ├── subscriptions/  uposrequest/  benefits/  paymentselector/
│   └── taptophone/           # Solo Android
├── build-logic/              # Convention plugins (config común de módulos)
└── gradle/libs.versions.toml
```

Cada `feature/x` tiene el mismo esqueleto:

```
feature/x/
├── data/        # XRepositoryImpl, DTOs, mappers
├── domain/      # XRepository (interfaz), UseCases, modelos de dominio
└── ui/          # XScreen (Composable), XViewModel, XUiState, XDestination
```

Regla de dependencias: `feature → core / designsystem`, nunca `feature → feature`. La navegación entre features pasa por rutas, no por referencias directas.

---

## 4. Buenas prácticas

### Arquitectura y estado
1. **Flujo unidireccional**: la UI emite eventos hacia el `ViewModel` y observa un único `UiState`. La UI no tiene lógica de negocio.
2. `UiState` es una `sealed interface` inmutable (`Loading` / `Error(retry)` / `Data`). Todas las pantallas BFF-driven manejan los tres estados, igual que en Flutter.
3. Los `ViewModel` **no conocen** `Context`, `Activity` ni Compose. Si necesitan strings, exponen un `@StringRes` o un modelo.
4. Colectar con `collectAsStateWithLifecycle()` y no con `collectAsState()`, para respetar el ciclo de vida.
5. Eventos de una sola vez (navegar, toast) por `Channel`/`SharedFlow`. No se modelan como parte del `UiState`.
6. El estado de flujos multi-pantalla (equivale a `OnboardingSession`/`MovementsSession` en iOS) vive en un `ViewModel` con scope al `NavGraph`, o en un `@Singleton` solo si es realmente global.

### Compose
7. Composables **sin estado** (`state hoisting`): reciben estado y lambdas, no ViewModels. Solo el composable `Screen` de la ruta conoce el `ViewModel`.
8. Cada componente del design system tiene `@Preview` y, si es posible, un test de snapshot.
9. Evitar recomposiciones innecesarias: modelos estables o `@Immutable`, `key` en `LazyColumn`, `remember`/`derivedStateOf` con criterio.
10. Nada de colores, tamaños o fuentes hardcodeados: todo sale de los tokens (`MaterialTheme` extendido con los tokens ueno).
11. Accesibilidad desde el inicio: `contentDescription`, `semantics`, tamaño táctil mínimo de 48dp, soporte de `fontScale` y de TalkBack.

### Coroutines
12. Inyectar los `Dispatcher` (no usar `Dispatchers.IO` directo) para poder testear.
13. `viewModelScope` en ViewModels. Nunca `GlobalScope`.
14. Propagar la cancelación: no capturar `CancellationException`.
15. El timer/polling del QR se modela como `Flow` con `delay`, atado al ciclo de vida.

### Red, modelos y errores
16. DTOs (`@Serializable`) separados de los modelos de dominio, con mappers explícitos.
17. Un único `Interceptor` agrega los headers multitenant desde `TenantContext`.
18. Los errores de red se traducen a un tipo propio (`CIAError`) en el repositorio. La UI nunca ve excepciones de OkHttp.
19. Reportar errores al `EventsRepository` igual que en Flutter/iOS (`reportingError`).
20. Ningún secreto ni token en logs. `Timber` en debug, árbol vacío o filtrado en release.

### Calidad y proceso
21. Detekt + ktlint obligatorios en CI. Warnings del compilador como errores.
22. Tests de **ViewModel** y **UseCase** para toda feature (fakes a mano o MockK + Turbine). Los fixtures JSON son los mismos de `microapp/assets/mocks`.
23. Commits con Conventional Commits (misma regex de `commitlint.yaml`, hook `commit-msg`).
24. Cada fase termina con las pantallas comparadas con Flutter en el sandbox, tests en verde y escenarios de mocks cargados.
25. R8/ProGuard activos en release. Reglas de `keep` para kotlinx.serialization y Retrofit.
26. Documentar los desvíos de Flutter en el plan (como el de uPOS en iOS) para que no se pierdan.

---

## 5. Tap to Phone (solo Android)

Tap to Phone es **Android-only** y no se migró a iOS. Hoy vive como plugin Flutter en `plugin/android/src/main/kotlin/com/itti/ueno_app/smart_charge_nfc_plugin/SmartChargeNfcPlugin.kt`, con un test en `src/test/kotlin/...`.

- El plugin ya está en Kotlin, así que el trabajo es **sacarle la capa de `MethodChannel`** y exponer su lógica como una clase/`UseCase` en `feature/taptophone`.
- Hay que revisar el manifest del plugin para los permisos y features de NFC (`android.permission.NFC`, `uses-feature`).
- **El emulador no soporta NFC.** Esta feature solo se prueba en un dispositivo físico.
- Antes de portar, leer el plugin completo y confirmar qué SDK de pagos/lector usa. **[Decidir]** si el módulo depende de un SDK de terceros.

---

## 6. Plan de migración propuesto

Mismo orden que iOS, para poder comparar avance entre plataformas.

| Fase | Contenido |
|------|-----------|
| **0 · Fundaciones** | Proyecto multi-módulo, version catalog, convention plugins, Hilt, `core/network` (+ interceptor multitenant), `TenantContext`, Analytics/Logger, navegación base, **design system** (tokens, tipografía Inter/Montserrat, componentes), sandbox con `MockInterceptor` y selector de tenant. |
| **1 · Cobro QR** | Home mínima → InputAmount → ValidateLimit → QRGenerate → Congrats. |
| **2 · Home, historial y link** | Home completa, Movements + filtros, Detail, Link (pre-confirm + generate + Mis links), PaymentSelector. |
| **3 · Onboarding y perfil** | Entry point, onboarding unificado, legacy, Profile, UpdateField, TaxInfo, ValidateRUC. |
| **4 · Productos y add-ons** | ProductsOnboarding, add-ons, SubscriptionsShipping, uPOS, Benefits. |
| **5 · Android-only e integración** | **Tap to Phone**, integración con el host real, multitenant en caliente, analytics contra Amplitude, accesibilidad, rollout. |

Para portar cada pantalla:
1. Leer el código Flutter (`lib/src/features/<feature>`) y la referencia visual de `cia_ios/CIA/docs/13_REFERENCIA_VISUAL.md`.
2. Crear `UiState`, `ViewModel`, `UseCase` y `Repository` con sus tests.
3. Construir el `Screen` en Compose usando el design system.
4. Agregar el escenario de mocks y un test de UI del flujo.

Features existentes en `lib/src/features/`: `add_ons`, `benefits`, `congrats`, `cpf`, `detail`, `home`, `input_amount`, `link`, `movements`, `onboarding_legacy`, `onboarding_unf`, `payment_selector`, `products_onboarding`, `profile`, `qr`, `subscriptions`, `tap_to_phone`, `update_field`, `upos_request`, `validate_limit`, `validate_ruc`. Hay que confirmar si `cpf` aplica a Android, porque no figura en el inventario de iOS.

---

## 7. Cómo compilar y correr en un emulador

### 7.1 Requisitos

| Herramienta | Notas |
|-------------|-------|
| **Android Studio** (última estable) | Incluye el SDK Manager y el AVD Manager. |
| **JDK 17** | Es el que usa el Gradle de Android actual. Ya está instalado (`openjdk@17`). |
| **Android SDK** | En `~/Library/Android/sdk`. Necesita `platform-tools`, `emulator`, una `platforms;android-XX` y una `system-images`. |

Variables de entorno (en `~/.zshrc`):

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin"
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```

> En este equipo `adb` y `sdkmanager` ya están en el `PATH`, pero `emulator` no. Con la línea de arriba queda resuelto.

### 7.2 Crear un emulador (AVD)

Desde Android Studio: **Device Manager → Create Device** (por ejemplo Pixel 8, API 34 o 35, imagen "Google APIs").

Por terminal:

```bash
uname -m   # arm64 → usar arm64-v8a ; x86_64 → usar x86_64

sdkmanager "platforms;android-35" "system-images;android-35;google_apis;arm64-v8a"
avdmanager create avd -n cia_pixel8 -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_8
emulator -list-avds
```

En este equipo ya hay imágenes `google_apis` y las plataformas `android-34`, `android-35`, `android-36` y `android-37.0`. También hay un AVD llamado `lostpaws_test`, que se puede reutilizar. Conviene crear uno propio para CIA.

### 7.3 Compilar e instalar

```bash
# 1. Levantar el emulador
emulator -avd cia_pixel8 &
adb wait-for-device

# 2. Compilar e instalar el sandbox (debug)
./gradlew :app:installDebug

# 3. Abrir la app
adb shell am start -n <applicationId>/.MainActivity
```

Desde Android Studio: elegir el módulo `app`, el emulador como destino y presionar **Run ▶**.

Otros comandos útiles:

```bash
./gradlew :app:assembleDebug            # genera el APK en app/build/outputs/apk/debug/
./gradlew test                          # tests unitarios de todos los módulos
./gradlew :app:connectedDebugAndroidTest  # tests instrumentados (requiere emulador)
./gradlew detekt spotlessCheck          # calidad
./gradlew clean
adb logcat | grep -i cia                # logs
```

### 7.4 Notas del emulador
- **NFC no está disponible**: Tap to Phone requiere un dispositivo físico (`adb devices` con depuración USB activa).
- Para apuntar a un BFF local desde el emulador, usar `10.0.2.2` en lugar de `localhost`.
- El sandbox arranca con `MockInterceptor`, así que no necesita red. Debe tener un equivalente a `-cia-autostart` de iOS, por ejemplo un `Intent extra` o una `BuildConfig`.
- Si el emulador va lento, activar aceleración por hardware. En Apple Silicon usar imágenes `arm64-v8a`.
- Problemas típicos: `JAVA_HOME` apuntando a otra versión, licencias sin aceptar (`sdkmanager --licenses`) y falta de espacio en disco.

---

## 8. Decisiones abiertas

| # | Decisión | Recomendación |
|---|----------|---------------|
| A1 | **Modo de integración**: ¿CIA se embebe en una superapp Android o es una app independiente? | Construir `cia-kit` como módulo con `CIAModule` desde el día 1, y dejar `app` como sandbox. Sirve para los dos escenarios (igual que D1 en iOS). |
| A2 | Hilt o Koin | Hilt, salvo que el host ya use Koin. |
| A3 | `minSdk` | 26, a confirmar con el host. |
| A4 | Cliente de red: ¿se usa el del host o Retrofit propio? | Envolver el del host detrás de `HttpClient`, con Retrofit como implementación del sandbox. |
| A5 | Tests de snapshot | Paparazzi o Roborazzi para el design system y pantallas BFF-driven. |
| A6 | SDK de Tap to Phone | Revisar el plugin actual antes de portar. |
| A7 | Features pendientes del BFF (equivalentes a D10 y D11 de iOS) | Pedir el contrato de subscriptions shipping y confirmar el id de "pendiente de liquidación". |
| A8 | Doble mantenimiento | Congelar features nuevas en Flutter por fase, o portarlas a las tres plataformas a la vez. Registrar en cada PR si afecta a Android e iOS. |
