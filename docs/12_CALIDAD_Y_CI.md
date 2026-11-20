# Calidad de código y CI

Qué se verifica, cómo correrlo en local y qué hace el pipeline.

## Herramientas

| Herramienta | Para qué | Config |
|---|---|---|
| **ktlint** (plugin `org.jlleitschuh.gradle.ktlint`) | Formato y estilo | [.editorconfig](../.editorconfig) con el estilo `intellij_idea` |
| **Detekt** | Complejidad, malas prácticas, nombres | [config/detekt/detekt.yml](../config/detekt/detekt.yml) sobre la configuración por defecto |
| **JUnit + kotlin.test + coroutines-test + Turbine** | Tests unitarios de dominio, ViewModels y flujos | cada módulo |
| **MockK** | Tests donde importa **verificar la interacción** con una dependencia | `core` (ver `CIAHttpClientTest`) |

Ambos plugins se aplican a **todos los módulos** desde el `build.gradle.kts` raíz.

## Correrlo en local

```bash
./gradlew ktlintCheck detekt        # verifica (lo mismo que hace el CI)
./gradlew ktlintFormat              # corrige el formato automáticamente
./gradlew testDebugUnitTest :core:test
```

`ktlintFormat` arregla casi todo. Lo que no pueda corregir solo (una línea de más de 140 caracteres, por ejemplo) se parte a mano.

## Decisiones de configuración

- **Estilo `intellij_idea` y no `ktlint_official`:** el segundo exige un formato muy estricto (envolver cada expresión multilínea) que genera diffs enormes sin mejorar la legibilidad. El primero coincide con lo que ya produce Android Studio.
- **Composables en PascalCase:** `ktlint_function_naming_ignore_when_annotated_with = Composable` y `FunctionNaming.ignoreAnnotated`.
- **Nombre de archivo libre:** se desactivan `filename` (ktlint) y `MatchingDeclarationName` (Detekt). Un archivo puede agrupar modelos relacionados (`QRModels.kt`).
- **Los ViewModels capturan `Throwable`:** `TooGenericExceptionCaught` y `SwallowedException` están desactivadas **solo en `*ViewModel.kt`**. El ViewModel es la frontera de errores: convierte cualquier falla del caso de uso en estado de UI. Siempre se relanza `CancellationException` antes. Fuera de los ViewModels las reglas siguen activas.
- **Sin baseline:** el código existente se formateó una vez, así que no hay deuda escondida en un archivo de baseline.

## Pipeline (GitHub Actions)

Definido en [.github/workflows/ci.yml](../.github/workflows/ci.yml). Corre en cada push a `main` y en cada pull request.

| Job | Qué hace | Depende de |
|---|---|---|
| `quality` | `detekt` + `ktlintCheck` | — |
| `test` | `testDebugUnitTest` + `:core:test` | — |
| `build` | `:app:assembleDebug` y sube el APK como artefacto | `quality`, `test` |

- **`quality` y `test` corren en paralelo** y el build solo arranca si ambos pasan.
- **`concurrency`:** al subir otro commit a la misma rama se cancela la corrida anterior.
- **Permisos mínimos:** `contents: read`.
- **Reportes:** si falla el lint o los tests, se suben como artefactos para verlos sin reproducir en local.

### Qué no cubre todavía

- **Tests instrumentados** (`androidTest`): necesitan un emulador en CI. Existen en `:storage` (Keystore y Room) y en `:app` (UI y accesibilidad) y pasan en local, pero el pipeline aún no los corre (ver `17_ROADMAP_PRODUCCION.md`).

### Dos detalles de los tests de dispositivo

- **Android 16 y Espresso:** el Espresso que trae Compose falla en API 36 (`InputManager.getInstance` fue eliminado). Por eso `app` fija `espresso-core` 3.7.0.
- **Instalación:** si en el dispositivo queda instalado un build con otra firma o con un `versionCode` mayor (por ejemplo un release de prueba), la instalación del build debug falla con `INSTALL_FAILED_VERSION_DOWNGRADE`. Desinstalar la app antes de correr `connectedDebugAndroidTest`.
- **Cobertura y SonarQube:** no hay medición de cobertura ni análisis estático externo.
- **Publicación:** el pipeline no firma ni sube a Google Play.
