# Android Studio para CIA Android

Guía práctica para desarrollar `cia_android` en Android Studio, con equivalencias de Xcode y Flutter.

## 1. Configuración inicial

| Paso | Dónde | Qué poner |
|------|-------|-----------|
| Gradle JDK | *Settings (⌘,) → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* | JDK 17 o 21. **No el 25**: Gradle 8.14.5 soporta hasta Java 24. |
| Imports automáticos | *Settings → Editor → General → Auto Import → Kotlin* | Activar *Add unambiguous imports on the fly* y *Optimize imports on the fly*. |
| Formato | *Settings → Editor → Code Style → Kotlin* | *Set from… → Kotlin style guide*. El proyecto usa `kotlin.code.style=official`. |
| Dispositivo | *Device Manager* | `cia_pixel8` (API 35) o uno con API 36 y Google APIs. |

Kotlin y el soporte de Compose ya vienen con Android Studio: no hay que instalar ningún plugin para empezar.

## 2. Equivalencias

| Android Studio | Equivalente |
|---|---|
| Gradle sync | Resolver paquetes SPM / `flutter pub get` |
| Device Manager | Simuladores de Xcode |
| Run configuration (`app`) | Scheme |
| Build Variants (`debug` / `release`) | Configurations |
| Logcat | Consola de Xcode / `flutter logs` |
| Layout Inspector | View Hierarchy debugger |
| `libs.versions.toml` | `pubspec.yaml` |
| *File → Invalidate Caches* | Limpiar DerivedData |

## 3. Trabajar en el proyecto

- **Qué correr:** la configuración **app** (el sandbox). Los demás módulos (`core`, `designsystem`, `feature:home`, `cia-kit`) son librerías y no se ejecutan solas.
- **Gradle sync:** hacerlo cada vez que cambia un `build.gradle.kts`, `settings.gradle.kts` o `gradle/libs.versions.toml`. Si algo "no existe" y el código está bien, casi siempre falta un sync.
- **Vista del proyecto:** cambiar el panel de *Android* a *Project* para ver las carpetas reales.
- **Capas:** `feature` depende de `core` y `designsystem`, nunca de otra `feature`. `core` es Kotlin puro.
- **Versiones de librerías:** se cambian en `gradle/libs.versions.toml`. Las últimas exigen AGP 9.1+ y `compileSdk` 37 (ver `10_PLAN_DE_MIGRACION.md`).
- **Terminal:** el `./gradlew` del repo usa el JDK de la terminal, no el de Studio. Si falla, exportar `JAVA_HOME` a un JDK 17 o 21.

```bash
./gradlew :app:installDebug      # compilar e instalar el sandbox
./gradlew test                   # tests unitarios
./gradlew :feature:home:testDebugUnitTest
```

## 4. Compose

- **`@Preview`:** muestra un composable sin correr la app (modo *Split* o *Design*).
- **Live Edit:** actualiza la UI en el dispositivo sin reinstalar. No es tan completo como el hot reload de Flutter: a veces hay que volver a correr.
- **Layout Inspector:** muestra el árbol de composables y cuántas veces se recompuso cada uno.
- **Recomposición:** Compose redibuja solo lo que cambió de estado. Conviene que los modelos sean inmutables y que los parámetros sean estables.
- **Estado y ciclo de vida:** el sistema puede destruir y recrear la `Activity` (rotación, memoria). Por eso el estado vive en el `ViewModel`.

## 5. Depurar

- **Logcat:** filtrar por `package:mine` o por tag. Los eventos de analytics del sandbox salen con el tag `CIA-Analytics`.
- **Breakpoints:** funcionan en coroutines. El debugger muestra la pila lógica.
- **App Inspection:** inspecciona base de datos y red reales. Con los mocks del sandbox no se ve tráfico, porque no pasa por OkHttp.
- **Emulador:** *Extended controls (⋯)* permite simular rotación, ubicación y batería. Para probar el shake de los mocks, *Virtual sensors → Move*.

## 6. Atajos (macOS)

| Acción | Atajo |
|---|---|
| Buscar en todo | doble ⇧ |
| Ir a clase / archivo | ⌘O / ⇧⌘O |
| Acciones y ajustes | ⇧⌘A |
| Ir a la definición / usos | ⌘B / ⌥F7 |
| Renombrar | ⇧F6 |
| Reformatear código | ⌥⌘L |
| Correr / depurar | ⌃R / ⌃D |
| Estructura del archivo | ⌘F12 |

## 7. Cosas que suelen confundir

- **`res/` vs `assets/`:** `res/` son recursos que Android compila (strings, fuentes, íconos) con nombres en minúscula y guión bajo, por eso las fuentes son `inter_bold.ttf`. `assets/` son archivos crudos: acá viven los SVG del design system y los fixtures JSON.
- **`AndroidManifest.xml`:** declara permisos y actividades. Se va a usar para NFC con Tap to Phone.
- **R8 en `release`:** achica y ofusca el código. Hoy está apagado. Al activarlo hay que agregar reglas para `kotlinx.serialization`.
- **Emulador y NFC:** el emulador no soporta NFC; Tap to Phone se prueba en un dispositivo físico.

## 8. Pendiente de configurar en el proyecto

Todavía no están: **Detekt** y **ktlint** (o Spotless), y un `gradle-daemon-jvm.properties` que fije la versión de Java para quien abra el proyecto.
