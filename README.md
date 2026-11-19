# CIA Android

Migración nativa (Kotlin + Jetpack Compose) de **Smart Charge Microapp** ("Cobros in App"), hoy en Flutter (`smart_charge_microapp`) y en iOS (`cia_ios/CIA`).

- Guía de migración: [docs/ANDROID_MIGRACION.md](docs/ANDROID_MIGRACION.md)
- Plan y estado: [docs/10_PLAN_DE_MIGRACION.md](docs/10_PLAN_DE_MIGRACION.md)

## Módulos

| Módulo | Contenido |
|--------|-----------|
| `app` | Sandbox: `MainActivity`, `MockHttpClient`, catálogo de mocks, pantalla "Mocks" |
| `cia-kit` | `CIAModule` (punto de entrada para la app host), `CIANavHost`, placeholders |
| `core` | Red, tenant, analytics, eventos, modelos compartidos, `CIARoute` (Kotlin puro) |
| `designsystem` | Tokens, tipografía, componentes Compose, íconos e ilustraciones |
| `feature:home` | Home modular: data / domain / ui (`HomeViewModel`, `HomeScreen`, una sección por archivo) |

## Correr

```bash
./gradlew :app:installDebug   # con un emulador o dispositivo conectado
./gradlew test
```

Los fixtures JSON de `app/src/main/assets/fixtures` son los mismos que usan Flutter e iOS.
