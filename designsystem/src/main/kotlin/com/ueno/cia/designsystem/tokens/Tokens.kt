package com.ueno.cia.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.tenant.Tenant

// Tokens del design system Flutter (design_system/lib/src/foundation y tokens/impl/ueno_color_token.dart).

/** `SpacingFoundation`. */
object Spacing {
    val x4s = 0.dp
    val x3s = 2.dp
    val x2s = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val x2l = 32.dp
    val x3l = 48.dp
    val x4l = 64.dp
    val x5l = 72.dp
}

/** `BorderRadiusFoundation`. */
object Radius {
    val x3s = 4.dp
    val x2s = 8.dp
    val xs = 12.dp
    val s = 16.dp
    val m = 20.dp
    val l = 24.dp
    val xl = 28.dp
    val x2l = 32.dp
}

/** `SizeFoundation` (solo los usados). */
object DSSize {
    val xl = 36.dp
}

/** `ColorFoundation` con los valores del tema ueno. */
object DSColor {
    val White = Color.White
    val Black = Color(0xFF252529)
    val Black400 = Color(0xFF52525B)

    val Neutral50 = Color(0xFFF7F7F7)
    val Neutral100 = Color(0xFFEAEAEB)
    val Neutral200 = Color(0xFFD8D8DE)
    val Neutral300 = Color(0xFFC6C6CC)
    val Neutral400 = Color(0xFFB4B4BF)
    val Neutral500 = Color(0xFF6C6C78)
    val Neutral600 = Color(0xFF505059)
    val Neutral700 = Color(0xFF404047)

    val Blue50 = Color(0xFFF2F8FF)
    val Blue100 = Color(0xFFD9EAFF)
    val Blue200 = Color(0xFFCCE4FF)
    val Blue300 = Color(0xFF91C4FF)
    val Blue500 = Color(0xFF268BFF)
    val Blue800 = Color(0xFF004799)

    val Green50 = Color(0xFFF2FFF9)
    val Green200 = Color(0xFFAFFAD9)
    val Green300 = Color(0xFF7AF5C0)
    val Green500 = Color(0xFF1DBF79)
    val Green600 = Color(0xFF16A366)
    val Green700 = Color(0xFF158052)
    val Green800 = Color(0xFF146542)

    val Violet50 = Color(0xFFF5F5FF)
    val Violet200 = Color(0xFFDBDBFF)
    val Violet500 = Color(0xFF7373E5)
    val Violet800 = Color(0xFF333399)

    val Yellow50 = Color(0xFFFFFAF2)
    val Yellow300 = Color(0xFFFFD98C)
    val Yellow500 = Color(0xFFEB9C00)
    val Yellow800 = Color(0xFF664400)

    val Pink50 = Color(0xFFFFF1F4)
    val Pink300 = Color(0xFFFFA6BF)
    val Pink500 = Color(0xFFFA3E74)
    val Pink600 = Color(0xFFE5225A)
    val Pink800 = Color(0xFF99173B)
}

/** Colores que cambian por tenant (Individuos usa negro, Empresas violeta). */
class Palette(val tenant: Tenant) {
    val homeHeaderBackground: Color
        get() = if (tenant == Tenant.EMPRESAS) DSColor.Violet800 else DSColor.Black
}
