# CIA Android

Migración nativa a **Android (Kotlin + Jetpack Compose)** de **Smart Charge Microapp** ("Cobros in App", squad `ChargeInApp`), hoy implementada en Flutter (`smart_charge_microapp`) y ya migrada a iOS (`cia_ios/CIA`).

La meta es la **paridad con Flutter e iOS**: mismas pantallas, mismos contratos del BFF (los mismos JSON que se usan para mockear las respuestas) y la misma arquitectura (Screen → ViewModel → UseCase → Repository), con las herramientas nativas de Android.

> Este repo es la **librería embebible** (`cia-kit`) más un **sandbox** (`app`) que hace de app host para desarrollar y probar. Todavía no hay integración con la superapp real.

---

## Contenido

1. [Estado de la migración](#1-estado-de-la-migración)
2. [Stack y por qué](#2-stack-y-por-qué)
3. [Arquitectura](#3-arquitectura)
4. [Módulos y reglas de dependencia](#4-módulos-y-reglas-de-dependencia)
5. [Cómo correrlo](#5-cómo-correrlo)
6. [El sandbox: Testing App y mocks](#6-el-sandbox-testing-app-y-mocks)
7. [Tests](#7-tests)
8. [Equivalencias Flutter ↔ iOS ↔ Android](#8-equivalencias-flutter--ios--android)
9. [Cómo migrar una pantalla nueva](#9-cómo-migrar-una-pantalla-nueva)
10. [Design system](#10-design-system)
11. [Decisiones y desvíos respecto de la guía original](#11-decisiones-y-desvíos-respecto-de-la-guía-original)
12. [Lecciones aprendidas y gotchas](#12-lecciones-aprendidas-y-gotchas)
13. [Pendientes conocidos](#13-pendientes-conocidos)
14. [Documentación](#14-documentación)

---

## 1. Estado de la migración

Las fases son las mismas que en iOS (`cia_ios/CIA/docs/10_PLAN_DE_MIGRACION.md`) para poder comparar el avance entre plataformas. El detalle vive en [docs/10_PLAN_DE_MIGRACION.md](docs/10_PLAN_DE_MIGRACION.md).

| Fase | Contenido | Estado |
|------|-----------|--------|
| **0 · Fundaciones** | Proyecto multi-módulo, core (red, tenant, analytics, rutas), design system, sandbox con mocks | Hecho |
| **Home** | Layout dinámico, header de comercio y de sucursales, alertas, opciones (+ modal), banners, total de ventas, últimos cobros, reintento por sección, pull to refresh | Hecho |
| **1 · Cobro QR** | ValidateLimit, InputAmount, CPF, QRGenerate (polling, timeout, cambio de proveedor, compartir), QRTimeout, Congrats, Receipt | Hecho |
| **2 · Historial y link** | Movimientos + filtros + pestañas, Detalle, Link (pre-confirmación, generación, "Mis links"), PaymentSelector (entry point, selector, países / QR internacional) | Hecho |
| **3 · Onboarding y perfil** | Onboarding unificado y legacy, Profile, ProfileName, UpdateField, TaxInfo, ValidateRUC | Pendiente |
| **4 · Productos y add-ons** | ProductsOnboarding, Add-ons, SubscriptionsShipping, uPOS, Benefits | Pendiente |
| **5 · Android-only e integración** | **Tap to Phone** (solo Android), host real, multitenant en caliente, analytics reales, accesibilidad, rollout | Pendiente |

Las rutas de las fases pendientes existen en `CIARoute` y hoy llevan a una pantalla placeholder **"Próximamente"** que muestra la ruta y sus argumentos (sirve para verificar la navegación antes de migrar la pantalla).

---

## 2. Stack y por qué

| Tema | Elección | Por qué |
|------|----------|---------|
| Lenguaje | **Kotlin 2.3** | Lenguaje oficial de Android; null-safety, data classes inmutables y coroutines. |
| UI | **Jetpack Compose** (Material 3 como base, design system propio encima) | Declarativo como Flutter: los widgets `Gu*` se portan casi 1:1. |
| Patrón | **MVVM + flujo unidireccional** (`Screen → ViewModel → UseCase → Repository`) | Es el patrón recomendado por Google y mapea directo a Flutter (`Screen → Notifier → UseCase → Repository`) e iOS. |
| Estado | **`StateFlow<UiState>`** + `Channel` para eventos de navegación | Equivale a `AsyncValue` de Riverpod y a `BehaviorRelay`/`Signal` de iOS. |
| Asincronía | **Coroutines + Flow** | Es el estándar actual; RxJava es legado en Android. |
| Red | **`HttpClient` propio** (interfaz) + implementación sobre **OkHttp** | Igual que iOS: el host provee el cliente y CIA solo agrega headers y decodifica. |
| Modelos | **kotlinx.serialization** | Sin reflexión; equivale a `json_serializable` / `Codable`. |
| Navegación | **Navigation Compose** con rutas tipadas (`@Serializable`) | Soporta argumentos tipados y scope de `ViewModel` por entrada. |
| Imágenes / íconos | **Coil 3** (+ `coil-svg`) | Los íconos e ilustraciones del design system son los mismos SVG de iOS. |
| Montos | **`BigDecimal`** | Sin pérdida de precisión; formato fijo de guaraníes (`Gs. 85.000`). |
| Tests | **JUnit + kotlin.test + coroutines-test** | ViewModels y repositorios testeables en la JVM, sin emulador. |
| Build | **Gradle 8.14 + AGP 8.13 + version catalog** | Versiones centralizadas en `gradle/libs.versions.toml`. |

Parámetros: `compileSdk 36`, `minSdk 26`, `targetSdk 36` (app), JDK 17 como bytecode objetivo. Paquete base `com.ueno.cia`; `applicationId` del sandbox `com.ueno.cia.sandbox` (provisorio).

---

## 3. Arquitectura

### Flujo de una pantalla

```
Composable (Screen)  ──eventos──▶  ViewModel  ──suspend──▶  UseCase (interfaz)  ──▶  Repository (impl)  ──▶  CIAHttpClient
        ▲                              │                                                                        │
        └────── StateFlow<UiState> ────┘                                                              HttpClient (del host)
                         Channel<Destination>  ──▶  host de navegación (cia-kit)
```

- **La UI no tiene lógica de negocio.** El `Screen` recibe estado y lambdas (`state hoisting`); solo la función `…Route` conoce el `ViewModel`. Así las pantallas se previsualizan y testean sin ViewModel.
- **Un único `UiState`** por pantalla, normalmente una `sealed interface` (`Loading` / `Loaded` / `Failed`). Cada sección de la Home tiene su propio `Loadable`.
- **Navegación como eventos**, no como estado: el ViewModel emite un `Destination` por un `Channel` y el host decide cómo navegar. Los features **no se conocen entre sí**; piden rutas (`CIARoute`) y `cia-kit` las resuelve.
- **Corrutinas cancelables:** los `ViewModel` usan `viewModelScope`; al salir de la pantalla se corta todo (polling del QR, cargas en vuelo). Los `catch` nunca tragan `CancellationException`.

### Red y multitenant

- `HttpClient` (interfaz en `core`) es lo que el **host provee**. `CIAHttpClient` es el único lugar donde se agregan los headers por tenant (`x-branch-id`, `x-origin`), se traducen los errores HTTP a `APIError` y se decodifica el envelope `{ "data": ... }` del BFF.
- `TenantContext` guarda el tenant activo (Individuos / Empresas) y la sucursal elegida; es seguro entre hilos.
- Los errores de repositorio se reportan con `reportingError(...)` (equivale al de Flutter); hoy el reportador es un no-op en el sandbox.
- Los cuerpos de request usan un `Json` con `explicitNulls = true`, porque Flutter manda `documentNumber: null` explícito.

### Inyección de dependencias

Contenedor manual (`CIAContainer` en `cia-kit`) y fábricas de `ViewModel` en el grafo de navegación. **No se usa Hilt**: `cia-kit` es un módulo embebible y no debe imponerle un framework de DI a la app host (ver [decisiones](#11-decisiones-y-desvíos-respecto-de-la-guía-original)).

### Punto de entrada para la app host

```kotlin
val cia = CIAModule(
    CIADependencies(
        httpClient = hostHttpClient,          // contrato HttpClient
        tenantContext = TenantContext(Tenant.INDIVIDUOS),
        analyticsTracking = hostAnalytics,    // opcional
        events = hostEvents,                  // opcional
        connectivity = hostConnectivity,      // opcional (por defecto: siempre conectado)
    ),
)

cia.Content(host = object : CIAHost {
    override fun onExit() { /* el usuario salió de CIA */ }
    override fun onOpenHelpCenter(deeplink: String) { /* abrir el Centro de ayuda */ }
}, startRoute = CIARoute.Home)
```

### Rutas con argumentos complejos

Navigation Compose solo pasa primitivos y enums. Las rutas con modelos del BFF (`Movements`, `Congrats`, `InputAmount`, `Receipt`…) los viajan como **JSON** dentro del argumento mediante `jsonNavType<T>()` (en `cia-kit`). Los montos van como texto (`BigDecimalSerializer`) para no perder precisión.

---

## 4. Módulos y reglas de dependencia

```
app                    Sandbox: Testing App, MockHttpClient, catálogo de mocks, pantalla "Mocks"
cia-kit                CIAModule (punto de entrada), CIANavHost (grafo), PlatformActions, placeholders
core                   Kotlin puro (JVM): red, tenant, analytics, flags y experimentos, contrato de almacén seguro, modelos compartidos, CIARoute
storage                Android: DataStore (preferencias y flags), Room (caché con vencimiento) y Android Keystore (secretos cifrados)
designsystem           Tokens, tipografía, componentes Compose, íconos e ilustraciones SVG
feature/
  home                 Home modular (una sección por archivo)
  validatelimit        Loader + ruteo según la respuesta (tope → CPF → monto)
  inputamount          Monto con separador de miles, mínimos/máximos, comisión, descripción del link
  cpf                  Documento del cliente (QR Pix)
  qr                   Generar QR (polling, timeout, proveedor, compartir) y QR vencido
  congrats             Pantalla de resultado genérica (semántica / neutra / error)
  detail               Detalle del cobro y comprobante (Receipt)
  movements            Historial con pestañas, filtros y paginación
  link                 Pre-confirmación, generación y "Mis links"
  paymentselector      Entry point, selector de métodos y selector de países
```

**Reglas** (se respetan hoy, conviene mantenerlas):

- `feature → core, designsystem`. **Nunca `feature → feature`.**
- `core` es Kotlin puro: no conoce Android ni Compose (por eso los tests de red corren en la JVM).
- `designsystem` no conoce features; recibe parámetros simples, no modelos del BFF (salvo `core`).
- `cia-kit` es el único que conoce a todos los features y los une.
- Los modelos que viajan entre features (opciones de la Home, Movimientos, Congrats, Recibo…) viven en `core/model`; los que son internos de una feature, en su `data/`.
- Estructura de cada feature: `data/` (modelos del BFF + repositorio), `domain/` (interfaz de caso de uso) y `ui/` (`ViewModel`, estado, `Screen`, `Route`).

---

## 5. Cómo correrlo

### Requisitos

- **JDK 17 a 24** para ejecutar Gradle (Gradle 8.14 **no** soporta Java 25). Recomendado **JDK 21**.
- **Android SDK** con `platform-tools`, `emulator`, una `platforms;android-36` y una imagen de sistema con *Google APIs* (API 35 o 36).
- Un emulador o dispositivo conectado.

### Android Studio

1. *Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK*** = 21 (o 17). **No** el 25 que puede traer por defecto el IDE.
2. Abrí la carpeta `cia_android` y hacé **Gradle sync**.
3. Elegí la configuración **app** y un dispositivo, y dale ▶.

Más detalle y atajos en [docs/ANDROID_STUDIO.md](docs/ANDROID_STUDIO.md).

### Terminal

```bash
export JAVA_HOME=<ruta a un JDK 17-24>
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

./gradlew :app:installDebug                       # compilar e instalar el sandbox
./gradlew test                                    # todos los tests unitarios
./gradlew :feature:movements:testDebugUnitTest    # los de un módulo
./gradlew :app:assembleDebug                      # solo el APK (app/build/outputs/apk/debug/)
```

Crear un emulador por línea de comandos:

```bash
sdkmanager "platforms;android-36" "system-images;android-36;google_apis;arm64-v8a"
avdmanager create avd -n cia_pixel -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_8
emulator -avd cia_pixel &
```

> Si `./gradlew` falla con un error de Java, casi siempre es el `JAVA_HOME`. Con JDK 21 funciona; con el 25 no.

---

## 6. El sandbox: Testing App y mocks

El sandbox replica la app `CIA` de iOS y la "Testing App" de Flutter.

- **Pantalla inicial ("COBROS IN APP - TESTING APP"):** chips de tenant (Individuos / Empresas), lista de flujos con radio e **"Iniciar flujo"**. Hoy están habilitados **Home Cobros** y **Selector de cobros**; los demás muestran "Próximamente".
- **Configurar mocks:** engranaje arriba a la derecha, o **agitando** el dispositivo (en el emulador: *Extended controls → Virtual sensors → Move*). Es una lista agrupada por endpoint, con la respuesta elegida marcada, **Restablecer** y **Listo**. La respuesta elegida se persiste y se usa en el **próximo pedido**; no reinicia CIA.
- **Latencia simulada:** 1200 ms por pedido (igual que iOS), para ver los skeletons.

### Extras de intent (para automatizar)

| Extra | Efecto |
|-------|--------|
| `--ez autostart true` | Abre CIA al iniciar |
| `--es flow home\|payment_selector` | Flujo a abrir |
| `--es tenant EMPRESAS` | Tenant inicial |
| `--ez reset_mocks true` | Descarta las respuestas elegidas a mano |

```bash
adb shell am start -n com.ueno.cia.sandbox/.MainActivity --ez reset_mocks true --ez autostart true --es flow payment_selector
```

### Qué mock elegir para ver cada pantalla

| Quiero ver | Dónde (engranaje) |
|-----------|-------------------|
| Resultado de cobro exitoso y comprobante | **QR → Confirmar pago del QR** → "Pago confirmado" |
| Resultado con error | **QR → Confirmar pago del QR** → "Pago con error" |
| QR vencido, sin esperar 5 minutos | **QR → Generar QR** → "QR que vence en 12 s (solo Android)" |
| Error semántico al generar el QR | **QR → Generar QR** → "QR generado + error semántico" |
| Pantalla de CPF | **Validar límite** → "QR con pantalla de CPF" |
| Tope transaccional | **Validar límite** → "Tope transaccional alcanzado" |
| Monto del link con resumen de comisión | **Validar límite (x-type LINK)** → "Link de pago con resumen de comisión" |
| Historial con filtros activos y formulario | **Movimientos** → "Cobros con formulario de filtros y filtros activos" |
| Lista de links vacía | **Link de pago → Mis links** → "Lista vacía" |
| Un error 500 de cualquier sección | La opción "Error 500" de ese endpoint |

Catálogo actual: Home (layout, header, opciones, banners, recientes, alertas, total de ventas), Validar límite, QR, Link de pago, Selector de cobros, Movimientos y Detalle. Los **contratos JSON** viven en `app/src/main/assets/contratos/<flujo>/` y son **los mismos que usan Flutter e iOS**; los que son solo de Android llevan el prefijo `android_`.

> Con los mocks activos no se ve tráfico en el Network Inspector de Android Studio, porque no pasa por OkHttp.

---

## 7. Tests

**305 tests unitarios** que corren en la JVM (sin emulador), más **12 instrumentados** que corren en un emulador o dispositivo:

| Qué | Dónde |
|-----|-------|
| Decodificación de **todos los contratos** del BFF contra los modelos | Tests de cada feature |
| **ViewModels**: estados, reglas, navegación y analytics | `feature/*/src/test` |
| Polling del QR con tiempo virtual (confirmación, timeout, sin red, errores, cambio de proveedor) | `feature/qr` |
| Reglas del monto (mínimo, máximo, comisión, descripción) | `feature/inputamount` |
| Formato de moneda, plantillas y serialización de montos | `core` |
| Repositorio de PaymentSelector (headers y query reales) | `feature/paymentselector` |
| Cliente HTTP del host (headers por tenant, errores, decodificación) con **MockK** | `core` |
| Feature flags, reparto de experimentos A/B y analítica con varios destinos | `core` |
| Cifrado AES-GCM, almacén seguro, caché con vencimiento y flags en DataStore | `storage` |
| **Instrumentados**: Android Keystore real y Room (8) | `storage/src/androidTest` |
| **Instrumentados**: UI del sandbox y etiquetas de accesibilidad (4) | `app/src/androidTest` |

Convenciones:

- Los tests leen los contratos reales de `app/src/main/assets/contratos` (cada módulo tiene su `Contratos.kt` de test), así un cambio en el contrato del BFF rompe el test.
- Se usan `StandardTestDispatcher` y `Dispatchers.setMain`; para tiempo virtual, `advanceTimeBy`.
- Un `SharedFlow` sin suscriptores **descarta** las emisiones: en los tests hay que suscribirse antes de emitir.

Los instrumentados se corren con `./gradlew :storage:connectedDebugAndroidTest :app:connectedDebugAndroidTest` (necesitan un emulador). Pendiente: correrlos en el CI y tests de snapshot del design system. Detalle en [docs/12_CALIDAD_Y_CI.md](docs/12_CALIDAD_Y_CI.md).

---

## 8. Equivalencias Flutter ↔ iOS ↔ Android

| Concepto | Flutter | iOS (CIA) | Android |
|----------|---------|-----------|---------|
| Lenguaje | Dart | Swift | Kotlin |
| UI | Widgets `Gu*` | UIKit programático | Compose |
| Patrón | Screen → Notifier → UseCase → Repository | VC → ViewModel → UseCase → Repository | Screen → ViewModel → UseCase → Repository |
| Estado | Riverpod `AsyncValue` | `BehaviorRelay` → `Driver` | `StateFlow<UiState>` |
| Eventos de navegación | `BaseCoordinator` | `Signal<Destination>` | `Channel<Destination>` |
| Navegación | `go_router` | `CIARouter` + `CIARoute` | Navigation Compose + `CIARoute` |
| Red | `IMultitenantHttpClient` | `HTTPClient` → `Single` | `HttpClient` → `suspend` |
| Modelos | `json_serializable` | `Codable` | `@Serializable` |
| Asincronía | `Future` / `Timer` | RxSwift | Coroutines / Flow |
| Estado compartido de un flujo | Providers `keepAlive` | Objetos `Session` | `ViewModel` en el scope de una entrada del grafo (ej. `MovementsSession`) |
| Mocks | `MockConfig` | `MockHTTPClient` | `MockHttpClient` (mismos JSON) |
| Tests | `flutter_test` + `mocktail` | XCTest + RxTest | JUnit + coroutines-test |
| Íconos / ilustraciones | Assets del DS Flutter | SVG en `xcassets` | Los mismos SVG en `assets/ds/` (Coil) |
| Compartir imagen | `RepaintBoundary` | `drawHierarchy` | `GraphicsLayer` → PNG → `FileProvider` |

---

## 9. Cómo migrar una pantalla nueva

La receta que se siguió en las fases 1 y 2:

1. **Leer** el código Flutter (`lib/src/features/<feature>`) y la versión iOS (`cia_ios/CIA/CIAKit/Sources/CIAKit/Features/<Feature>`), más la referencia visual de `cia_ios/CIA/docs/13_REFERENCIA_VISUAL.md`.
2. **Modelos:** si viajan en una ruta o los usan varias features, a `core/model`; si no, a `feature/<x>/data`. Mismos nombres de campo y nulabilidad que Flutter.
3. **Ruta:** agregarla a `CIARoute` (`core/navigation`) y, si lleva modelos, registrar su `typeMap` con `jsonNavType` en `cia-kit`.
4. **Datos y dominio:** interfaz de caso de uso + repositorio (mismos paths y tipos de evento que Flutter).
5. **ViewModel** con su estado y `Destination`; tests del ViewModel con los contratos.
6. **UI:** `Screen` sin estado propio + `Route` que conecta el ViewModel. Usar `CIAScaffold` y los componentes del design system.
7. **Cablear** en `CIANavHost` (fábrica del ViewModel y manejo de destinos) y quitar el placeholder.
8. **Mocks:** agregar el flujo a `MockCatalog` con las mismas respuestas que iOS.
9. **Verificar** en el emulador y actualizar [docs/10_PLAN_DE_MIGRACION.md](docs/10_PLAN_DE_MIGRACION.md).

Los `testTag` replican los `accessibilityIdentifier` de iOS (`cia_…`) y se exponen como `resource-id` (`testTagsAsResourceId`), lo que permite automatizar con `uiautomator` o Espresso.

---

## 10. Design system

- **Tokens** (`designsystem/tokens`): espaciados, radios y colores del tema ueno, idénticos a Flutter e iOS. **Tipografía:** Inter y Montserrat con el alto de línea de Flutter (`size × height`).
- **Componentes**: botones, `CIAScaffold` (barra con atrás + título), footer con sombra y resumen, input de monto (separador de miles en vivo, sacudida al máximo de dígitos), campos de texto y de selección, pestañas, acordeón, stepper, tarjetas, banners y carruseles, skeletons con shimmer, bottom sheet, mensajes y estados de error.
- **Íconos e ilustraciones:** los **mismos SVG** que iOS, en `designsystem/src/main/assets/ds/`, renderizados con Coil y teñibles. Un ícono desconocido del BFF se reemplaza por `plus`, igual que iOS.
- **Bottom sheet:** réplica del de iOS (grabber, título, scrim al 40 %). El contenido recibe un `dismiss { … }` que **cierra primero y navega después**.
- **Captura a imagen:** `capturable()` / `toPng()` (QR y comprobante) y compartir con `FileProvider`.

---

## 11. Decisiones y desvíos respecto de la guía original

La propuesta inicial está en [docs/ANDROID_MIGRACION.md](docs/ANDROID_MIGRACION.md). Lo que cambió al implementarla:

| Tema | Propuesto | Hecho | Por qué |
|------|-----------|-------|---------|
| DI | Hilt | Contenedor manual | `cia-kit` es embebible; no se le impone Hilt a la app host. |
| Red | Retrofit | `HttpClient` propio sobre OkHttp | Igual que iOS: el host provee el cliente. |
| Convention plugins | `build-logic` | Config común en el `build.gradle.kts` raíz | Menos piezas en la fase 0. |
| Versiones | Las últimas | Compose BOM 2026.04.01, Navigation 2.9.6, etc. | Las últimas exigen AGP 9.1+ y `compileSdk 37`. |
| Rutas | `@Serializable` simples | Ídem + `jsonNavType` para modelos del BFF | Navigation solo pasa primitivos y enums. |
| Tap to Phone | Solo Android | La Home lo muestra y navega a un placeholder | Se migra en la fase 5. |
| Sombras | Las de iOS | Elevación de Compose | Aproximación visual. |

---

## 12. Lecciones aprendidas y gotchas

- **Gradle y Java:** Gradle 8.14 soporta hasta Java 24. El JBR 25 de Android Studio da error; usar 17–24.
- **Versiones de librerías:** subir Compose, Navigation, etc. al último release puede exigir AGP 9.1+. Subir ambos a la vez es una migración aparte.
- **Comentarios KDoc:** en Kotlin, `/*` y `/**` dentro de un comentario abren uno **anidado**. Una ruta como `contratos/**` o `mock_settings/*` en un KDoc rompe la compilación.
- **`jsonNavType`:** `parseValue` **no** debe volver a decodificar el valor. Navigation ya lo hace y una segunda decodificación corrompe textos con `%` (por ejemplo "Comisión 2.9% + IVA").
- **Bottom sheets y tags:** el sheet es una ventana aparte; para que sus `testTag` se vean hay que volver a habilitar `testTagsAsResourceId` dentro del sheet (ya hecho en `DSBottomSheet`).
- **Teclado y barras del sistema:** la app usa edge-to-edge; el footer sube con el teclado (`imePadding` en `CIAScaffold`).
- **Emulador:** no soporta NFC (Tap to Phone se prueba en un dispositivo físico). Su tutorial de lápiz táctil puede aparecer al enfocar un campo de texto; y la ventana del teclado puede hacer fallar el volcado de `uiautomator`.
- **`SharedFlow` en tests:** sin suscriptores descarta las emisiones; suscribirse antes de emitir.
- **Cache de Gradle:** si falla al guardar una entrada de la caché (disco externo), reintentar con `--no-build-cache`.

---

## 13. Pendientes conocidos

- **Fases 3, 4 y 5** (ver [estado](#1-estado-de-la-migración)): Onboarding, Perfil, UpdateField, ValidateRUC, productos y add-ons, uPOS, Benefits, **Tap to Phone** e integración con el host real.
- **Eventos de error al BFF** (`/events`): el sandbox usa un reportador vacío; falta el `EventsRepository` real.
- **Marca de agua en mosaico** del comprobante (iOS la tiene).
- **Conectividad real** para el polling del QR: hoy el valor por defecto es "siempre conectado"; el host debe proveer la implementación.
- **CI:** el workflow de GitHub Actions está escrito pero **nunca corrió en GitHub**; no corre los tests instrumentados.
- **Adaptadores de Firebase y Amplitude** y crash reporting: necesitan credenciales. Ver [docs/15_FEATURE_FLAGS_Y_ANALITICA.md](docs/15_FEATURE_FLAGS_Y_ANALITICA.md).
- **Publicación en Google Play**: el release firmado con R8 y el AAB funcionan, pero falta la app en Play Console y el pipeline de subida. Ver [docs/16_PUBLICACION_PLAY.md](docs/16_PUBLICACION_PLAY.md).
- **Profiling** (Macrobenchmark, Baseline Profile), **Hilt** en la app host, **SonarQube** y cobertura. Ver [docs/17_ROADMAP_PRODUCCION.md](docs/17_ROADMAP_PRODUCCION.md).
- **Accesibilidad** completa (TalkBack, escala de fuente) y **tests de snapshot**: hoy solo se verifica que los controles tengan etiqueta.
- **`applicationId`** definitivo y configuraciones Dev/Staging.
- **`gradle-daemon-jvm.properties`** y convention plugins.
- **Decisiones abiertas** con el equipo: `minSdk` definitivo, cómo provee el host el cliente de red, y si se migra a AGP 9.

---

## 14. Documentación

| Documento | Para qué sirve |
|-----------|----------------|
| [docs/10_PLAN_DE_MIGRACION.md](docs/10_PLAN_DE_MIGRACION.md) | Estado por fase y bloque, desvíos y cómo probar cada flujo en el sandbox |
| [docs/ANDROID_MIGRACION.md](docs/ANDROID_MIGRACION.md) | Propuesta inicial: tecnología elegida, buenas prácticas y cómo compilar en un emulador |
| [docs/ANDROID_STUDIO.md](docs/ANDROID_STUDIO.md) | Guía de Android Studio con equivalencias de Xcode y Flutter |
| [docs/12_CALIDAD_Y_CI.md](docs/12_CALIDAD_Y_CI.md) | ktlint, Detekt, MockK y el pipeline de GitHub Actions |
| [docs/13_TRUNK_BASED.md](docs/13_TRUNK_BASED.md) | Estrategia de ramas |
| [docs/14_DATOS_Y_SEGURIDAD.md](docs/14_DATOS_Y_SEGURIDAD.md) | DataStore, Room y cifrado con Android Keystore |
| [docs/15_FEATURE_FLAGS_Y_ANALITICA.md](docs/15_FEATURE_FLAGS_Y_ANALITICA.md) | Feature flags, A/B testing y analítica con varios destinos |
| [docs/16_PUBLICACION_PLAY.md](docs/16_PUBLICACION_PLAY.md) | Release firmado, R8, AAB y ciclo de publicación en Google Play |
| [docs/17_ROADMAP_PRODUCCION.md](docs/17_ROADMAP_PRODUCCION.md) | Qué está verificado, qué falta y cómo comprobarlo |
| [docs/PROPUESTA_MOCK_KIT.md](docs/PROPUESTA_MOCK_KIT.md) | Propuesta para ofrecer la estrategia de mocks como librería a otros equipos |
| `cia_ios/CIA/docs/` | Guías de la migración a iOS (arquitectura, reglas, navegación, networking, modelos, UI, testing, inventario de features…) |
| `smart_charge_microapp/` | Código Flutter de origen (fuente de verdad del comportamiento) |
