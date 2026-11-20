# Feature flags, A/B testing y analítica

## Feature flags

`FeatureFlags` (en `core`) es un **puerto**: la microapp pregunta `isEnabled(flag)` y el host decide de dónde sale el valor.

```kotlin
val QrNuevo = FeatureFlag(key = "cia_qr_nuevo", default = false)

if (featureFlags.isEnabled(QrNuevo)) { /* flujo nuevo */ } else { /* flujo actual */ }
// o, desde una pantalla:
val enabled by featureFlags.observe(QrNuevo).collectAsState(initial = QrNuevo.default)
```

| Implementación | Dónde | Uso |
|---|---|---|
| `InMemoryFeatureFlags` | `core` | Valor por defecto del host y fake en tests |
| `DataStoreFeatureFlags` | `:storage` | Override local persistido (pantalla de debug, QA) o caché del último valor remoto |
| Proveedor remoto (Firebase Remote Config u otro) | **No incluido** | Se implementa `FeatureFlags` en la app host |

El `default` de cada bandera rige si ninguna fuente la define. Así una bandera nueva nunca rompe una versión que todavía no la recibe.

### Banderas y trunk-based

Un feature incompleto llega a `main` **apagado** (`default = false`) y se prende cuando está listo. Es lo que permite ramas cortas (ver `13_TRUNK_BASED.md`). Cuando la bandera ya está al 100 % y estable, se **borra** junto con el código de la rama vieja: una bandera olvidada es deuda.

## A/B testing

```kotlin
val experiment = Experiment("home_cta", listOf(Variant("control"), Variant("b")))
val variant = ExperimentAssigner.assign(userId, experiment)
```

- **Determinístico:** el mismo usuario cae siempre en la misma variante, sin guardar nada.
- **Independiente por experimento:** la clave del experimento entra en el hash, así dos experimentos no reparten igual a los mismos usuarios.
- **Pesos:** `Variant("b", weight = 1)` frente a `Variant("control", weight = 9)` da un reparto 90/10.
- **Hash FNV-1a de 32 bits:** estable entre plataformas y versiones, por lo que iOS y Android pueden asignar igual.
- **Apagado:** `enabled = false` devuelve siempre el control.

Los tests (`ExperimentAssignerTest`) verifican el reparto sobre 10.000 usuarios y la independencia entre experimentos.

**Para medir un experimento** hay que enviar un evento de exposición con la variante por el mismo canal de analítica; el análisis estadístico queda fuera de la app.

## Analítica

`Analytics` (en `core`) arma los eventos y los entrega a un puerto `AnalyticsTracking` que implementa el host.

### Varios destinos: `CompositeAnalyticsTracking`

```kotlin
val tracking = CompositeAnalyticsTracking(
    listOf(amplitudeTracking, firebaseTracking, logTracking),
    onError = { tracker, error -> Log.w("Analytics", "Falló ${tracker::class.simpleName}", error) },
)
```

Cada evento va a todos los destinos. **La falla de uno no rompe a los demás ni a la pantalla**: se captura y se informa a `onError`. La analítica nunca debe ser la causa de un crash.

### Adaptadores de Firebase y Amplitude

**No están incluidos:** necesitan credenciales (`google-services.json`, clave de API de Amplitude) y decisiones del equipo (qué eventos y propiedades se envían). Se implementan en la app host como un `AnalyticsTracking`:

```kotlin
class FirebaseTracking(private val firebase: FirebaseAnalytics) : AnalyticsTracking {
    override fun track(event: AnalyticsEvent) {
        firebase.logEvent(event.kind.name.lowercase(), bundleOf(*event.parameters.toList().toTypedArray()))
    }
}
```

(Ejemplo ilustrativo: no se compiló en este repo.)

### Observabilidad

Además de la analítica hay dos canales, hoy sin destino real:

- **`EventsReporting`:** errores de repositorio (endpoint, tipo de error). El host decide a dónde van (Crashlytics, un backend propio).
- **Logs:** el sandbox imprime los eventos con la etiqueta `CIA-Analytics`.

Pendiente: un destino de crash reporting y métricas de rendimiento reales.
