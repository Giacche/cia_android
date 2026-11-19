package com.ueno.cia.sandbox

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.network.HttpMethod

/** Respuesta que puede devolver un endpoint mockeado. */
sealed interface MockResponse {
    val description: String

    /** Fixture JSON de `assets/fixtures/<flujo>/` (ya trae el envelope `{ "data": ... }`). */
    data class Success(override val description: String, val fixture: String) : MockResponse
    data class Json(override val description: String, val body: String) : MockResponse
    data class ApiError(override val description: String, val statusCode: Int) : MockResponse
}

data class MockEndpoint(
    val name: String,
    val method: HttpMethod,
    val path: String,
    val responses: List<MockResponse>,
) {
    val key: String get() = "$method $path"

    /** El path puede llevar query (`/x?xType=QR`): el request debe traer esos pares. */
    fun matches(requestMethod: HttpMethod, requestPath: String, requestQuery: Map<String, String>): Boolean {
        if (requestMethod != method) return false
        val parts = path.split("?", limit = 2)
        if (parts.size == 2) {
            val required = parts[1].split("&").mapNotNull { pair ->
                pair.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }
            if (!required.all { (key, value) -> requestQuery[key] == value }) return false
        }
        val pure = parts[0]
        if (!pure.contains("{")) return pure == requestPath
        // El path puede llevar parámetros (`/charges/{ticketNumber}/detail`): cada `{…}` calza con un segmento
        val pattern = pure.split("/")
        val candidate = requestPath.split("/")
        return pattern.size == candidate.size && pattern.zip(candidate).all { (a, b) -> a.startsWith("{") || a == b }
    }
}

data class MockFlow(val name: String, val endpoints: List<MockEndpoint>)

/**
 * Equivale a `mock_settings` de Flutter y a `MockCatalog` de iOS. La primera respuesta de cada
 * endpoint es el default. Los fixtures son los mismos JSON del BFF que usan las otras dos plataformas.
 */
object MockCatalog {
    private fun success(description: String, fixture: String) = MockResponse.Success(description, fixture)
    private fun error500() = MockResponse.ApiError("Error 500", 500)

    val home = MockFlow(
        name = "Home",
        endpoints = listOf(
            MockEndpoint(
                "Layout", HttpMethod.GET, CIAEndpoints.Home.LAYOUT,
                listOf(
                    success("Default (header, alertas, opciones, banners, recientes)", "home_get_home_layout.json"),
                    success("Variante 2", "home_get_home_layout_02.json"),
                    success("Variante 3", "home_get_home_layout_03.json"),
                    success("Variante 4", "home_get_home_layout_04.json"),
                    success("Empresas", "home_get_home_layout_empresas.json"),
                    success("Individuos con sucursales", "home_get_home_layout_individuos_carousel.json"),
                    success("Error de layout Empresas (errorScreen)", "home_get_home_layout_error_empresas.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Header", HttpMethod.GET, CIAEndpoints.Home.HEADER,
                listOf(
                    success("Individuos", "home_get_home_header.json"),
                    success("Empresas (sucursales)", "home_get_home_header_empresas.json"),
                    success("Individuos con sucursales", "home_get_home_header_individuos_carousel.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Opciones", HttpMethod.GET, CIAEndpoints.Home.OPTIONS,
                listOf(
                    success("QR internacional (video)", "home_get_home_options_qrs_inter.json"),
                    success("Default", "home_get_home_options.json"),
                    success("Variante 2", "home_get_home_options_02.json"),
                    success("Empresas", "home_get_home_options_empresas.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Banners", HttpMethod.GET, CIAEndpoints.Home.BANNERS,
                listOf(
                    success("Varios banners", "home_get_home_banners.json"),
                    success("Add-ons Esc-01 (solo uPOS)", "home_get_home_banners_add_ons_esc01.json"),
                    success("Add-ons Esc-02", "home_get_home_banners_add_ons_esc02.json"),
                    success("Add-ons Esc-03 (solo facturación)", "home_get_home_banners_add_ons_esc03.json"),
                    success("Empresas", "home_get_home_banners_empresas.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Recientes", HttpMethod.GET, CIAEndpoints.Home.RECENTS,
                listOf(success("Default", "home_get_home_recents.json"), error500()),
            ),
            MockEndpoint(
                "Alertas", HttpMethod.GET, CIAEndpoints.Home.ALERTS,
                listOf(
                    MockResponse.Json("Sin alertas (como en el video)", """{"data":{"type":"HOME_ALERTS","informationAlert":[]}}"""),
                    success("Una alerta", "home_get_home_alerts.json"),
                    success("Varias alertas", "home_get_home_multiple_alerts.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Total de ventas", HttpMethod.GET, CIAEndpoints.Home.PENDING_SETTLEMENT,
                listOf(success("Default", "home_get_home_pending_settlement.json"), error500()),
            ),
        ),
    )

    /** La Home pide la primera página de cobros (`/v3/charges`). */
    val movements = MockFlow(
        name = "Movimientos",
        endpoints = listOf(
            MockEndpoint(
                "Cobros (history-tab / page / size + filtros)", HttpMethod.GET, CIAEndpoints.Home.CHARGES,
                listOf(
                    success("Cobros", "bff_charges_tabs_cobros_get_response.json"),
                    error500(),
                    success("Servicios", "bff_charges_tabs_servicios_get_response.json"),
                    success("Servicios con filtros aplicados", "bff_charges_tabs_servicios_filters_get_response.json"),
                    success("Cobros con formulario de filtros y filtros activos", "bff_charges_filters_get_response.json"),
                    success("Servicios con formulario de filtros", "bff_charges_filters_services_get_response.json"),
                ),
            ),
        ),
    )

    val validate = MockFlow(
        name = "Validar límite",
        endpoints = listOf(
            MockEndpoint(
                "Validar límite - Link de pago (x-type LINK)", HttpMethod.GET, CIAEndpoints.ValidateLimit.PATH + "?x-type=LINK",
                listOf(
                    // Igual que el QR: sin resumen de comisión en el footer del monto (el BFF no manda los textos)
                    success("Link de pago sin resumen de comisión (como el QR)", "android_get_validate_link_sin_resumen_response.json"),
                    success("Link de pago con resumen de comisión", "bff_get_validate_link_response.json"),
                    success("Tope transaccional alcanzado", "bff_get_validate_transaction_limit_response.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Validar límite (headers x-type / x-provider)", HttpMethod.GET, CIAEndpoints.ValidateLimit.PATH,
                listOf(
                    success("QR sin CPF, sin resumen (como en el video)", "ios_get_validate_qr_sin_cpf_response.json"),
                    success("QR con pantalla de CPF", "bff_get_validate_qr_response.json"),
                    success("Link de pago (con pre-confirmación)", "bff_get_validate_link_response.json"),
                    success("Tope transaccional alcanzado", "bff_get_validate_transaction_limit_response.json"),
                    error500(),
                ),
            ),
        ),
    )

    val qr = MockFlow(
        name = "QR",
        endpoints = listOf(
            MockEndpoint(
                "Generar QR", HttpMethod.POST, CIAEndpoints.QR.GENERATE,
                listOf(
                    success("QR generado", "bff_qr_post_generate_response.json"),
                    success("QR generado + error semántico (CPF no coincide)", "bff_qr_post_generate_response_semantic.json"),
                    success("QR que vence en 12 s (solo Android, para ver el timeout)", "android_qr_post_generate_response_short_timeout.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Confirmar pago del QR (polling)", HttpMethod.POST, CIAEndpoints.QR.CONFIRM,
                listOf(
                    MockResponse.Json("Todavía sin pago (el QR vence)", """{"data":{"responseScreen":{},"receiptScreen":{}}}"""),
                    success("Pago confirmado", "bff_qr_post_confirm_response_success.json"),
                    success("Pago con error", "bff_qr_post_confirm_response_error.json"),
                    MockResponse.ApiError("Error 500 (reintenta en el próximo ciclo)", 500),
                ),
            ),
        ),
    )

    val link = MockFlow(
        name = "Link de pago",
        endpoints = listOf(
            MockEndpoint(
                "Generar link", HttpMethod.POST, CIAEndpoints.Link.GENERATE,
                listOf(success("Link generado", "bff_link_post_generate_response.json"), error500()),
            ),
            MockEndpoint(
                "Mis links (status / page / size)", HttpMethod.GET, CIAEndpoints.Link.LIST,
                listOf(
                    success("Lista con links", "bff_link_get_list_response.json"),
                    success("Lista vacía", "bff_link_get_list_empty_state_response.json"),
                    error500(),
                ),
            ),
        ),
    )

    val paymentSelector = MockFlow(
        name = "Selector de cobros",
        endpoints = listOf(
            // Más específico primero: gana el primero que calza (en Flutter se distingue por `?xType=`)
            MockEndpoint(
                "Entry point · QR internacional (x-type)", HttpMethod.GET, CIAEndpoints.PaymentSelector.PATH + "?xType=QR_INTERNATIONAL",
                listOf(
                    success("QR internacional · países (Argentina, Pix, Bolivia, Perú, Colombia)", "bff_get_payment_selector_qr_inter_response.json"),
                    success("QR internacional con alerta de RUC", "ios_get_payment_selector_qr_inter_alert_response.json"),
                    error500(),
                ),
            ),
            MockEndpoint(
                "Entry point · selector de cobros", HttpMethod.GET, CIAEndpoints.PaymentSelector.PATH,
                listOf(
                    success("Todos los medios habilitados + QR internacional", "ios_get_payment_selector_todos_con_qr_internacional_response.json"),
                    success("Todos los medios habilitados", "bff_get_payment_selector_all_enabled_response.json"),
                    success("Con QR internacional", "bff_get_payment_selector_con_qr_internacional_response.json"),
                    success("Con alerta de RUC y medios deshabilitados", "bff_get_payment_selector_response.json"),
                    error500(),
                ),
            ),
        ),
    )

    val detail = MockFlow(
        name = "Detalle de cobro",
        endpoints = listOf(
            MockEndpoint(
                "Detalle (por Nro. de ticket)", HttpMethod.GET, CIAEndpoints.Detail.PATH_TEMPLATE,
                listOf(success("Detalle con descuentos", "bff_get_detail_response_v3.json"), error500()),
            ),
        ),
    )

    val all: List<MockFlow> = listOf(home, validate, qr, link, paymentSelector, movements, detail)
}
