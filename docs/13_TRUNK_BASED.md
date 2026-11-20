# Estrategia de ramas: trunk-based development

## Idea

Una sola rama principal (`main`, el *trunk*) que siempre se puede compilar y probar. El trabajo se hace en ramas **cortas** que se integran en horas o pocos días, no en ramas de larga vida por versión o por feature.

## Reglas

1. **`main` siempre verde.** Ningún merge si el CI falla (`ktlintCheck`, `detekt`, tests, build).
2. **Ramas cortas:** `feature/<tema>`, `fix/<tema>`. Si una rama vive más de un par de días, se parte el trabajo.
3. **Pull requests chicos:** un cambio que se pueda revisar en minutos. Mejor tres PR pequeños que uno grande.
4. **Cambios incompletos detrás de un feature flag** (ver `15_FEATURE_FLAGS_Y_ANALITICA.md`): el código llega a `main` apagado y se prende cuando está listo. Así no hace falta una rama larga para esconder trabajo a medias.
5. **Squash merge** con un mensaje que explique el *por qué*; el historial de `main` queda legible.
6. **Sin commits directos a `main`:** todo pasa por PR y por el CI.

## Releases

- Los releases salen **desde `main`** con una etiqueta (`v1.4.0`).
- Un arreglo urgente sobre una versión ya publicada: rama `release/1.4` creada **desde la etiqueta**, el arreglo se aplica en `main` primero y se lleva con *cherry-pick*. Es la excepción, no la norma.

## Nombres de commits

Prefijo de tipo, en imperativo y corto: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `ci:`.

## Por qué así

- Menos conflictos de integración: se integra seguido.
- Se detectan los problemas cuando el cambio es chico y está fresco.
- Los feature flags separan **desplegar** de **publicar**: el código puede estar en producción sin estar visible.

## Qué exige a cambio

- Un CI rápido y confiable (si tarda 40 minutos, las ramas se alargan).
- Disciplina para partir el trabajo y para usar flags en lo incompleto.
- Tests que den confianza para integrar a diario.
