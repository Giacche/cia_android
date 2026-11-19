package com.ueno.cia.core

/** Paths del BFF (`/bff/smartcharge/...`). Los mismos que los repositorios Flutter. */
object CIAEndpoints {
    object Home {
        const val LAYOUT = "/bff/smartcharge/v4/home/layout"
        const val HEADER = "/bff/smartcharge/v4/home/header"
        const val OPTIONS = "/bff/smartcharge/v4/home/options"
        const val BANNERS = "/bff/smartcharge/v4/home/banners"
        const val RECENTS = "/bff/smartcharge/v4/home/recents"
        const val ALERTS = "/bff/smartcharge/v3/home/alerts"
        const val PENDING_SETTLEMENT = "/bff/smartcharge/v4/home/total_sales"
        const val CHARGES = "/bff/smartcharge/v3/charges"
    }

    object ValidateLimit {
        const val PATH = "/bff/smartcharge/v2/validate"
    }

    object QR {
        const val GENERATE = "/bff/smartcharge/qr/generate"
        const val CONFIRM = "/bff/smartcharge/qr/confirm"
    }

    object Events {
        const val PATH = "/bff/smartcharge/v1/events"
    }

    object Detail {
        /** Para los mocks del sandbox. */
        const val PATH_TEMPLATE = "/bff/smartcharge/v3/charges/{ticketNumber}/detail"
        fun path(ticketNumber: String) = "/bff/smartcharge/v3/charges/$ticketNumber/detail"
    }

    object Link {
        const val GENERATE = "/bff/smartcharge/link/generate"
        const val LIST = "/bff/smartcharge/link/v2/myLinks"
    }

    object PaymentSelector {
        const val PATH = "/bff/smartcharge/paymentEntryPoint"
    }
}
