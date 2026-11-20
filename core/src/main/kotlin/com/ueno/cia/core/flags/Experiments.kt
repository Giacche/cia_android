package com.ueno.cia.core.flags

/** Una variante de un experimento y su peso relativo (no hace falta que sumen 100). */
data class Variant(val name: String, val weight: Int = 1) {
    init {
        require(weight > 0) { "El peso de la variante '$name' debe ser positivo" }
    }
}

/**
 * Experimento A/B. La primera variante es el control y rige cuando el experimento está apagado o el usuario no
 * entra en la muestra.
 */
data class Experiment(val key: String, val variants: List<Variant>, val enabled: Boolean = true) {
    init {
        require(variants.size >= 2) { "Un experimento necesita al menos dos variantes" }
        require(variants.map { it.name }.toSet().size == variants.size) { "Las variantes deben tener nombres únicos" }
    }

    val control: Variant get() = variants.first()
}

/**
 * Asigna una variante de forma **determinística**: el mismo usuario siempre cae en la misma variante de un
 * experimento, sin guardar nada, y distintos experimentos reparten distinto (la clave entra en el hash).
 */
object ExperimentAssigner {
    fun assign(userId: String, experiment: Experiment): Variant {
        if (!experiment.enabled) return experiment.control
        val total = experiment.variants.sumOf { it.weight }
        val bucket = bucket("${experiment.key}:$userId", total)
        var acc = 0
        for (variant in experiment.variants) {
            acc += variant.weight
            if (bucket < acc) return variant
        }
        return experiment.control
    }

    /** Hash FNV-1a de 32 bits: estable entre plataformas y versiones, a diferencia de `String.hashCode` en otros lenguajes. */
    private fun bucket(input: String, buckets: Int): Int {
        var hash = FNV_OFFSET
        for (byte in input.encodeToByteArray()) {
            hash = (hash xor (byte.toInt() and 0xFF)) * FNV_PRIME
        }
        return ((hash.toLong() and 0xFFFFFFFFL) % buckets).toInt()
    }

    private const val FNV_OFFSET = -0x7ee3623b // 0x811C9DC5
    private const val FNV_PRIME = 0x01000193
}
