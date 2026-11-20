# Mock Kit: una librería de mocks reutilizable por otros equipos

Propuesta y guía paso a paso para convertir la estrategia de mocks de CIA en una librería que otros equipos puedan adoptar. Empieza por Android y después se lleva a iOS.

## 1. Idea en una frase

Una librería **sin conocimiento del negocio** que simula las respuestas de un backend a partir de archivos que cada equipo escribe: sus **contratos** (JSON), sus **endpoints** y su **configuración**. La librería pone el motor y la pantalla de ajustes; cada equipo pone el contenido.

La prueba de que quedó bien: otro equipo la usa **sin escribir una línea de código dentro de la librería**.

## 2. Problema que resuelve

- Se puede construir y probar una feature aunque el servicio todavía no exista o no esté disponible.
- El mismo mock sirve para tests, demos y revisiones de stores.
- Hoy la estrategia vive copiada en cada app. Cada copia se desvía, los arreglos no se propagan y nadie es dueño.

## 3. Qué es "la estrategia de mocks"

Son tres cosas distintas, y conviene tratarlas por separado:

| Pieza | Depende de la plataforma | Quién la pone |
|---|---|---|
| **Contratos**: los JSON de respuesta | No | Cada equipo |
| **Manifiesto**: flujo → endpoint → respuestas posibles | No (es un formato) | Cada equipo, con el esquema de la librería |
| **Motor**: cliente HTTP simulado y pantalla de ajustes | Sí | La librería |

La convención más importante: **se mockea en la frontera HTTP**, no más adentro. Así el código real de todas las capas corre sin cambios.

## 4. Opciones evaluadas

| Opción | Pros | Contras |
|---|---|---|
| **A. Copiar el código** | Cero fricción al empezar | Cada copia se desvía, sin dueño |
| **B. Librería interna por plataforma** | Arreglos centralizados, API estable, se adopta con una dependencia | Hay que mantener una implementación por plataforma |
| **C. Contratos + manifiesto como fuente de verdad** | Lo único compartido es lo que no cambia; validable en CI | Hay que definir y gobernar el esquema |
| **D. Servidor de mocks externo** (WireMock, Mockoon, Prism) | Sirve a web, QA y backend | Más red, difícil en dispositivo y CI offline, sin selector dentro de la app, no sirve para un modo demo |
| **E. Grabar y reproducir tráfico** | Genera mocks rápido | Puede arrastrar datos reales; los contratos envejecen |

Un "plugin" en el sentido de Flutter (canal de plataforma) no hace falta. Una CLI o un plugin de IDE sí puede ser un complemento útil más adelante.

### Recomendación

**C + B**, y D solo si lo piden:

1. Un repo de contratos y esquema como fuente de verdad.
2. Una librería delgada por plataforma, empezando por Android.
3. Un servidor externo opcional para equipos web o de QA.

## 5. Principios de diseño

1. **Motor genérico, contenido ajeno.** La librería no sabe de tenants, cabeceras ni nombres de flujos de nadie. Lo específico entra por configuración o por un hook que implementa el equipo.
2. **Convención sobre configuración.** Una carpeta y un nombre de manifiesto por defecto. Solo se configura lo que se aparta de eso.
3. **Solo en debug.** El motor no entra en el build de release.
4. **Fallar claro.** Si el manifiesto apunta a un archivo inexistente o es inválido, se falla al arrancar con un mensaje que diga qué y dónde.
5. **Versionar el manifiesto desde el día uno.** Un cambio de formato rompe a todos los equipos a la vez.
6. **Una v1 chica.** Un caso de uso completo y simple; las opciones se suman cuando un equipo real las pide.

## 6. Qué pone cada parte

| La librería (genérica) | Cada equipo (su contenido) |
|---|---|
| Interceptor / cliente HTTP simulado | Carpeta de contratos |
| Lectura y validación del manifiesto | El manifiesto: flujos, endpoints, respuestas |
| Pantalla de ajustes y selector de flujos | Nombres y textos de sus flujos |
| Latencia, errores y fallback configurables | Sus valores: latencia, cabeceras, tenant |
| Convención de carpetas | Excepciones a la convención |

## 7. El manifiesto

Es la pieza más portable: iOS lo va a reutilizar tal cual.

### Ejemplo (genérico, sin nada de un dominio real)

```json
{
  "version": 1,
  "flows": [
    {
      "name": "Pedidos",
      "endpoints": [
        {
          "method": "GET",
          "path": "/api/orders",
          "latencyMs": 800,
          "responses": [
            { "label": "Lista con datos", "file": "orders/list_ok.json" },
            { "label": "Lista vacía",     "file": "orders/list_empty.json" },
            { "label": "Error 500",       "error": { "status": 500 } }
          ]
        },
        {
          "method": "POST",
          "path": "/api/orders",
          "responses": [
            { "label": "Creado", "file": "orders/create_ok.json", "status": 201 }
          ]
        }
      ]
    }
  ]
}
```

### Convención de carpetas

```
assets/mocks/
├── manifest.json
└── orders/
    ├── list_ok.json
    ├── list_empty.json
    └── create_ok.json
```

### Reglas del esquema (v1)

- `version`: entero, obligatorio.
- `flows[].name`: texto que se muestra en el selector.
- `endpoints[]`: `method` + `path` identifican el endpoint. No debe haber dos iguales.
- `responses[]`: la primera es la que se usa por defecto. Cada una tiene `label` y **o bien** `file` **o bien** `error`.
- `latencyMs`: opcional, por endpoint, con un valor global por defecto.
- Los archivos se resuelven relativos a la carpeta del manifiesto.

El esquema se publica como **JSON Schema** en `schema/`, para validarlo en CI y en el editor.

### Declararlo: manifiesto o DSL

- **Manifiesto JSON (fuente de verdad):** independiente de la plataforma, validable, reutilizable en iOS.
- **DSL en Kotlin (opcional, después):** más cómodo para quien vive en Android, pero genera el mismo modelo. No va en la v1.

## 8. Estructura del repo

Un repo nuevo e independiente, por ejemplo `mock-kit`, no dentro de ninguna app. Un monorepo con una carpeta por plataforma y lo compartido aparte:

```
mock-kit/
├── schema/              # JSON Schema del manifiesto + versión
├── android/
│   ├── mocks-core/      # motor, Kotlin puro
│   ├── mocks-ui/        # pantalla de ajustes, Compose
│   └── mocks-no-op/     # misma API, sin nada adentro
├── ios/                 # llega en la fase 4
├── examples/
│   └── android-sample/  # app mínima con un manifiesto de ejemplo
└── docs/                # guía de adopción, convenciones, versionado
```

Por qué un repo aparte:

- Tiene su propio versionado, su CI y sus dueños.
- Obliga a que la app de CIA sea **un cliente más**, que es la mejor prueba de que la librería es genérica.
- Cambiar el esquema y las dos plataformas puede ir en un mismo cambio.

## 9. Diseño de los módulos de Android

### mocks-core (Kotlin puro, sin Android ni Compose)

- Modelo del manifiesto (`@Serializable`) y su lector con validación.
- Un **`MockEngine`** que, dado un método y un path, devuelve la respuesta elegida (o "sin mock").
- Un **`MockSelection`** que guarda qué respuesta está elegida por endpoint. Se persiste detrás de una interfaz, para no atarlo a una tecnología.
- Un **`MockSource`** que abstrae de dónde se leen los archivos: `assets/` en la app, o un directorio en los tests.

### Punto de integración: interceptor de OkHttp

La mayoría de los equipos usa OkHttp o Retrofit. El motor se expone como un **`Interceptor`**:

```kotlin
class MockInterceptor(
    private val engine: MockEngine,
    private val fallback: Fallback = Fallback.PassThrough,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mock = engine.resolve(request.method, request.url.encodedPath)
            ?: return when (fallback) {
                Fallback.PassThrough -> chain.proceed(request)
                Fallback.Fail -> error("Sin mock para ${request.method} ${request.url.encodedPath}")
            }
        Thread.sleep(mock.latencyMs)
        return mock.toResponse(request)
    }
}
```

Para quien tenga su propia interfaz de cliente HTTP (como CIA), se ofrece un adaptador opcional encima del mismo motor.

### mocks-ui (Compose)

- Pantalla de ajustes: lista de flujos, endpoints y respuestas, con selección.
- Selector de flujos y un gesto para abrirla (por ejemplo, agitar el dispositivo).
- Recibe el `MockEngine` y el `MockSelection` por parámetro. No conoce el negocio.

### mocks-no-op

La misma API pública, vacía. Es lo que usa el build de release.

### Debug-only

```kotlin
// build.gradle.kts de la app o feature
dependencies {
    debugImplementation("com.tuorg.mockkit:mocks-core:1.0.0")
    debugImplementation("com.tuorg.mockkit:mocks-ui:1.0.0")
    releaseImplementation("com.tuorg.mockkit:mocks-no-op:1.0.0")
}
```

Es el mismo patrón que usan herramientas como LeakCanary o Chucker: el código de release compila igual y no lleva ningún mock.

## 10. Paso a paso

Cada fase termina en algo verificable. No pases a la siguiente sin cumplir el criterio.

### Fase 0. Decisiones previas

1. Decidí dónde se aloja el repo (en la organización o en uno personal mientras se valida) y si hace falta avisar o pedir aprobación antes de crear algo "oficial".
2. Elegí un nombre que no sea el del banco ni el de tu microapp.
3. Elegí el Maven donde se va a publicar.

**Listo cuando:** hay un nombre, un lugar para el repo y un registro de artefactos definidos.

### Fase 1. El manifiesto y el esquema

1. Revisá los contratos y el catálogo actuales de la app (hoy en `app/src/main/assets/contratos/` y `MockCatalog.kt`).
2. Escribí el JSON Schema de la sección 7.
3. Convertí **dos o tres flujos reales** de CIA al manifiesto nuevo, para ver si el formato alcanza.
4. Contrastá el formato con los mocks de Flutter e iOS: si alguno no se puede expresar, ajustá el esquema **antes de congelarlo**.
5. Congelá `version: 1`.

**Listo cuando:** dos flujos reales de CIA se describen completos en el manifiesto y validan contra el esquema.

### Fase 2. Crear el repo y extraer el motor

1. Creá el repo con `schema/`, los tres módulos de Android y `examples/android-sample/`.
2. Mové a `mocks-core` lo genérico de `MockHttpClient` y de `MockCatalog`. Dejá en la app todo lo que mencione flujos, tenants o endpoints propios.
3. Implementá el lector del manifiesto con validación y mensajes de error claros.
4. Implementá `MockInterceptor` y el adaptador para la interfaz propia de CIA.
5. Escribí tests del motor sin Android: selección de respuesta, latencia, error, endpoint inexistente y manifiesto inválido.
6. Movéle a `mocks-ui` la pantalla de ajustes y el selector, parametrizados.
7. Creá `mocks-no-op` con la misma API.

**Listo cuando:** `examples/android-sample` corre con su propio manifiesto y la pantalla de ajustes funciona, sin una sola referencia al dominio de CIA dentro de la librería.

### Fase 3. Que CIA sea el primer cliente

1. En `cia_android`, reemplazá el código local por la dependencia (debug) y el no-op (release).
2. Dejá en la app solo el manifiesto, los contratos y la configuración propia.
3. Corré la suite de tests y los flujos del sandbox. Todo debe comportarse igual.
4. Si algo sigue necesitando código de la app dentro del motor, la librería todavía no es genérica: corregilo.
5. Verificá que el build de release no contiene `mocks-core` ni `mocks-ui`.

**Listo cuando:** CIA corre solo con la librería, sin copias del motor, y el release está limpio.

### Fase 4. Publicar y pilotear

1. Configurá `maven-publish`, versionado semántico y la publicación en el Maven interno.
2. Agregá un CI que corra los tests, valide los manifiestos de `examples/` contra el esquema y publique al crear un tag.
3. Escribí la guía de adopción corta (sección 11).
4. Elegí **un equipo piloto**, integrá con ellos y medí el esfuerzo real.
5. Anotá todo lo que no entendieron o les costó; eso es la lista de mejoras de la v1.1.

**Listo cuando:** un equipo ajeno lo integró en una feature suya, sin tu ayuda en el código.

### Fase 5. iOS

1. Reutilizá `schema/` y el mismo formato de manifiesto.
2. El equivalente del interceptor es un **`URLProtocol`**; el equivalente de los módulos, un paquete SPM con target "real" y target "no-op".
3. Extraé el motor desde la implementación de CIA iOS, quitando lo propio de la app.
4. Verificá con los mismos manifiestos de `examples/` que **iOS y Android se comportan igual**.
5. Repetí el piloto con un equipo de iOS.

**Listo cuando:** el mismo manifiesto y los mismos contratos producen el mismo comportamiento en las dos plataformas.

## 11. Guía de adopción para otros equipos

Debe caber en una página:

1. Agregá la dependencia (debug) y el no-op (release).
2. Creá `assets/mocks/manifest.json` y tus contratos.
3. Registrá el interceptor en tu cliente HTTP, solo en debug.
4. Abrí la pantalla de ajustes y elegí el flujo.
5. Si un endpoint no tiene mock, definí qué querés que pase (pasar al servicio real, fallar o registrar).

Sumá un comando o plantilla que cree el esqueleto de mocks de un feature nuevo.

## 12. Decisiones que conviene cerrar antes de publicar

- **Dónde viven los contratos:** en cada app, o en un repo aparte validado en CI contra el esquema o el OpenAPI del backend. Con varios equipos, aparte.
- **Varios módulos aportan mocks:** definí si se registran de forma explícita o se descubren, y qué pasa con endpoints duplicados.
- **Fallback sin mock:** configurable: pasar, fallar o registrar.
- **Hooks para lo específico:** cabeceras, tenant o validación de request entran como un hook que implementa el equipo, no como lógica de la librería.
- **Dueño y proceso de cambios:** quién aprueba cambios al esquema y a la API pública, y cómo se propone uno (inner source).

## 13. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Los contratos se desfasan del backend | Validación en CI y pruebas de contrato contra el esquema u OpenAPI |
| Mocks en producción | Artefactos separados (`no-op`) y una verificación en el pipeline de release |
| El manifiesto queda sesgado a Android | Contrastarlo con Flutter e iOS antes de congelarlo |
| Librería acoplada a Compose o a la interfaz de CIA | `mocks-core` sin dependencias de UI ni del cliente propio |
| La API se infla con pedidos del piloto | Lista corta de lo que entra en la v1 |
| Datos reales dentro de los contratos | Regla: solo datos ficticios; revisión al agregar contratos |
| Quedar como único mantenedor | Acordar dueños antes de publicar |

## 14. Cómo plantearlo en la organización

- Empezá con **un equipo piloto**, no con un anuncio general.
- Mostrá el beneficio con datos: cuánto se tarda en probar un flujo con y sin mocks, y el caso en que el backend dejó de estar disponible y la app siguió corriendo.
- Definí desde el inicio dueño, proceso de cambios y versionado.
- Tratalo como un producto interno de plataforma: tiene usuarios, documentación y un responsable.

## 15. Resumen de decisiones

| Tema | Decisión |
|---|---|
| Forma | Librería por plataforma + manifiesto compartido |
| Repo | Nuevo, independiente, monorepo por plataforma |
| Fuente de verdad | Manifiesto JSON con JSON Schema versionado |
| Integración en Android | `Interceptor` de OkHttp, con adaptador para clientes propios |
| Release | Artefacto `no-op`, nada de mocks en producción |
| Primer cliente | La app de CIA, para probar que es genérica |
| Orden | Esquema → Android → piloto → iOS |
