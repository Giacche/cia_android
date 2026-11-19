# 10 · Plan de migración a Android

Mismo orden de fases que iOS (`cia_ios/CIA/docs/10_PLAN_DE_MIGRACION.md`). Guía general: [ANDROID_MIGRACION.md](ANDROID_MIGRACION.md).

## Estado (19/11/2026)

| Bloque | Estado |
|--------|--------|
| Fase 0 · proyecto | Hecho: Gradle 8.14.5, AGP 8.13.1, Kotlin 2.3.10, version catalog, módulos `app`, `cia-kit`, `core`, `designsystem`, `feature:home`. Pendiente: applicationId definitivo, Detekt/ktlint, CI, configuraciones Dev/Staging, convention plugins. |
| Fase 0 · Core | Hecho: `HttpClient` + `CIAHttpClient` (headers multitenant, envelope, errores), `OkHttpHttpClient`, `TenantContext`, `EventsReporting`, `Analytics`, `CIARoute`, modelos compartidos. Pendiente: `DeeplinkResolver`, `CurrencyFormatter`, conectividad. |
| Fase 0 · Design system | Hecho (lo que usan Home y Monto; + `CIAScaffold`, `LoaderScreen`, `AmountInput`, `DSTextField`, `ScreenFooter`): tokens, tipografía Inter/Montserrat, botones, icon button, link, skeleton, quick action + grilla, sección, inline message, banner, carruseles, movement row, empty/retry/feedback, data card, bottom sheet. Íconos e ilustraciones SVG (mismos de iOS) vía Coil. |
| Fase 0 · Sandbox | Hecho, igual que iOS: pantalla "Testing App" (tenant + flujo + "Iniciar flujo"), mocks desde el engranaje o agitando (lista agrupada por endpoint, checkmark, Restablecer / Listo, latencia 1200 ms), `MockHttpClient` con los contratos de la Home y Movimientos. Extras: `--ez autostart true`, `--es tenant EMPRESAS`, `--ez reset_mocks true`. |
| **Home** | **Migrada** como `feature:home`: layout dinámico, header (comercio / sucursales), alertas, opciones (+ modal), banners, total de ventas y últimos cobros; skeleton, reintento por sección, pull to refresh, selección de sucursal. 29 tests unitarios en verde. Las pantallas destino son placeholders "Próximamente". |
| **Fase 1 · QR** | **Migrada** (módulos `feature:validatelimit`, `inputamount`, `cpf`, `qr`, `congrats`, `detail`): ValidateLimit, InputAmount, CPF, QRGenerate (polling cada `interval`, timeout, selector de proveedor, compartir la tarjeta como imagen), QRTimeout, Congrats (semántico / neutro / error, con copiar, compartir, llamar y abrir link) y Receipt (compartir y guardar como PNG). Con mocks y tests. Pendiente de la fase: marca de agua del comprobante y `EventsRepository` real (hoy `NoOpEvents`). |
| **Fase 2 · Home, historial y link** | **Migrada** (módulos `feature:movements`, `detail`, `link`, `paymentselector`): Movimientos con pestañas, filtros activos, paginación y pull to refresh; formulario de filtros (canal, método dependiente del canal, estado, período con máscara dd/mm/aa y monto con miles) con la sesión compartida entre historial y filtros; Detalle con stepper, medio de pago, detalle y descuentos colapsable; Link (pre-confirmación con T&C, generación, "Mis links" con pestañas, mensaje cerrable y paginación de a 10); PaymentSelector (entry point, selector de métodos, selector de países con banderas, alertas de RUC). Home ya estaba completa. Con mocks y tests. |
| Fase 3 en adelante | Pendiente (ver iOS). **Tap to Phone** (solo Android) en la fase 5. |

## Desvíos respecto de ANDROID_MIGRACION.md

| Tema | Propuesto | Hecho | Por qué |
|------|-----------|-------|---------|
| DI | Hilt | Contenedor manual (`CIAContainer`) | `cia-kit` es un módulo embebible: no debe imponerle Hilt a la app host. Decisión A2 sigue abierta. |
| Red | Retrofit | `HttpClient` propio sobre OkHttp | Igual que iOS: el host provee el cliente y CIA solo agrega headers. |
| Convention plugins | `build-logic` | Config común en el `build.gradle.kts` raíz | Menos piezas en la fase 0. |
| Versiones | Últimas | Compose BOM 2026.04.01, Navigation 2.9.6, etc. | Las últimas exigen AGP 9.1+ y `compileSdk` 37. Subir cuando se migre a AGP 9. |
| Navegación | Rutas `@Serializable` | Igual + `jsonNavType` para modelos del BFF | Navigation Compose solo pasa primitivos y enums. |
| TTP | Solo Android | La Home lo muestra y navega a un placeholder | Se migra en la fase 5. |

## Cómo correrlo

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

emulator -avd cia_pixel8 &            # AVD Pixel 8, API 35 (google_apis, arm64)
./gradlew :app:installDebug
adb shell am start -n com.ueno.cia.sandbox/.MainActivity --ez reset_mocks true
./gradlew test                         # tests unitarios
```

## Cómo probar el flujo de QR en el sandbox

Los mocks se eligen con el engranaje (o agitando). Por defecto el confirm responde "todavía sin pago", así que el QR vence a los 5 minutos.

| Quiero ver | Mock a elegir |
|------------|---------------|
| Resultado exitoso y comprobante | QR → *Confirmar pago del QR* → "Pago confirmado" |
| Resultado con error | *Confirmar pago del QR* → "Pago con error" |
| Pantalla de timeout sin esperar | QR → *Generar QR* → "QR que vence en 12 s (solo Android)" |
| Pantalla de CPF | *Validar límite* → "QR con pantalla de CPF" |
| Error semántico al generar | *Generar QR* → "QR generado + error semántico" |
| Tope transaccional | *Validar límite* → "Tope transaccional alcanzado" |
