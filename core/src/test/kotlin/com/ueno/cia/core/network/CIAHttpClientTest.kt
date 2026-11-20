package com.ueno.cia.core.network

import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Prueba [CIAHttpClient] con un [HttpClient] simulado con MockK. Se usa MockK cuando importa **verificar la
 * interacción** (qué request recibió el host); para respuestas con contratos reales se usan fakes escritos a mano.
 */
class CIAHttpClientTest {
    @Serializable
    private data class Ping(val value: String)

    private val host = mockk<HttpClient>()
    private val captured = slot<HttpRequest>()

    private fun client(tenant: TenantContext) = CIAHttpClient(host, tenant)

    private fun respond(status: Int = 200, body: String = """{"data":{"value":"ok"}}""") {
        coEvery { host.send(capture(captured)) } returns HttpResponse(status, body)
    }

    @Test
    fun `decodifica el envelope del BFF`() = runTest {
        respond()

        val result = client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")

        assertEquals(Ping("ok"), result)
    }

    @Test
    fun `empresas agrega el origin solo en GET`() = runTest {
        respond()
        val tenant = TenantContext(Tenant.EMPRESAS)

        client(tenant).get(Ping.serializer(), "/ping")

        assertEquals(CIAHttpClient.ORIGIN_HOME_BUSINESS, captured.captured.headers[CIAHttpClient.ORIGIN_HEADER])
    }

    @Test
    fun `individuos no agrega el origin`() = runTest {
        respond()

        client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")

        assertNull(captured.captured.headers[CIAHttpClient.ORIGIN_HEADER])
    }

    @Test
    fun `el origin explicito del request gana sobre el del contexto`() = runTest {
        respond()

        client(TenantContext(Tenant.EMPRESAS))
            .get(Ping.serializer(), "/ping", headers = mapOf(CIAHttpClient.ORIGIN_HEADER to "CUSTOM"))

        assertEquals("CUSTOM", captured.captured.headers[CIAHttpClient.ORIGIN_HEADER])
    }

    @Test
    fun `la sucursal del contexto siempre se envia y pisa la del request`() = runTest {
        respond()
        val tenant = TenantContext(Tenant.EMPRESAS).apply { setActiveBranchId("suc-1") }

        client(tenant).get(Ping.serializer(), "/ping", headers = mapOf(CIAHttpClient.BRANCH_ID_HEADER to "otra"))

        assertEquals("suc-1", captured.captured.headers[CIAHttpClient.BRANCH_ID_HEADER])
    }

    @Test
    fun `los headers se calculan con el tenant vigente al enviar`() = runTest {
        respond()
        val tenant = TenantContext(Tenant.INDIVIDUOS)
        val client = client(tenant)

        tenant.switchTenant(Tenant.EMPRESAS)
        client.get(Ping.serializer(), "/ping")

        assertEquals(CIAHttpClient.ORIGIN_HOME_BUSINESS, captured.captured.headers[CIAHttpClient.ORIGIN_HEADER])
    }

    @Test
    fun `un status no 2xx se traduce a APIError Server con codigo y mensaje`() = runTest {
        respond(status = 422, body = """{"error":{"code":"LIMIT","message":"Supera el limite"}}""")

        val error =
            assertFailsWith<APIError.Server> {
                client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")
            }

        assertEquals(422, error.statusCode)
        assertEquals("LIMIT", error.code)
        assertEquals("Supera el limite", error.serverMessage)
    }

    @Test
    fun `un error con cuerpo ilegible conserva el status`() = runTest {
        respond(status = 500, body = "<html>boom</html>")

        val error =
            assertFailsWith<APIError.Server> {
                client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")
            }

        assertEquals(500, error.statusCode)
        assertNull(error.code)
    }

    @Test
    fun `un cuerpo que no respeta el envelope es un error de decodificacion`() = runTest {
        respond(body = """{"otra":"cosa"}""")

        assertFailsWith<APIError.Decoding> {
            client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")
        }
    }

    @Test
    fun `una falla de red del host se propaga sin transformarse`() = runTest {
        coEvery { host.send(any()) } throws APIError.Connectivity()

        assertFailsWith<APIError.Connectivity> {
            client(TenantContext(Tenant.INDIVIDUOS)).get(Ping.serializer(), "/ping")
        }
        coVerify(exactly = 1) { host.send(any()) }
    }
}
