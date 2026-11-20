package com.ueno.cia.core.flags

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExperimentAssignerTest {
    private val experiment = Experiment("nuevo_home", listOf(Variant("control"), Variant("b")))

    @Test
    fun `el mismo usuario siempre recibe la misma variante`() {
        val first = ExperimentAssigner.assign("user-42", experiment)

        repeat(50) { assertEquals(first, ExperimentAssigner.assign("user-42", experiment)) }
    }

    @Test
    fun `el reparto 50 50 queda cerca de la mitad con una muestra grande`() {
        val counts =
            (1..10_000)
                .map { ExperimentAssigner.assign("user-$it", experiment).name }
                .groupingBy { it }
                .eachCount()

        val b = counts.getValue("b")
        assertTrue(b in 4_700..5_300, "b recibió $b de 10000, se esperaba cerca de 5000")
    }

    @Test
    fun `los pesos se respetan`() {
        val weighted = Experiment("x", listOf(Variant("control", 9), Variant("b", 1)))

        val b = (1..10_000).count { ExperimentAssigner.assign("u$it", weighted).name == "b" }

        assertTrue(b in 800..1_200, "b recibió $b de 10000, se esperaba cerca de 1000")
    }

    @Test
    fun `un experimento apagado devuelve el control`() {
        val off = experiment.copy(enabled = false)

        assertEquals("control", ExperimentAssigner.assign("cualquiera", off).name)
    }

    @Test
    fun `dos experimentos no reparten igual a los mismos usuarios`() {
        val other = Experiment("otro", listOf(Variant("control"), Variant("b")))

        val different =
            (1..1_000).count {
                ExperimentAssigner.assign("u$it", experiment) != ExperimentAssigner.assign("u$it", other)
            }

        assertTrue(different > 300, "los experimentos están correlacionados: solo $different diferencias")
    }

    @Test
    fun `un experimento necesita variantes validas`() {
        assertFailsWith<IllegalArgumentException> { Experiment("x", listOf(Variant("solo"))) }
        assertFailsWith<IllegalArgumentException> { Experiment("x", listOf(Variant("a"), Variant("a"))) }
        assertFailsWith<IllegalArgumentException> { Variant("a", 0) }
    }
}
