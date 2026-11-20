package com.ueno.cia.designsystem.tokens

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.ueno.cia.designsystem.R

private val InterRegular = FontFamily(Font(R.font.inter_regular, FontWeight.Normal))
private val InterMedium = FontFamily(Font(R.font.inter_medium, FontWeight.Medium))
private val InterSemiBold = FontFamily(Font(R.font.inter_semibold, FontWeight.SemiBold))
private val InterExtraBold = FontFamily(Font(R.font.inter_extrabold, FontWeight.ExtraBold))
private val MontserratBold = FontFamily(Font(R.font.montserrat_bold, FontWeight.Bold))
private val MontserratExtraBold = FontFamily(Font(R.font.montserrat_extrabold, FontWeight.ExtraBold))

/**
 * `EGuTextStyle` del design system Flutter (foundation/typography).
 * Tamaños: FontSizesToken. Alto de línea: LineHeightFoundation (multiplica el tamaño). Kern: LetterSpacingFoundation.
 */
enum class DSTextStyle(
    private val family: FontFamily,
    private val weight: FontWeight,
    private val size: Float,
    private val lineHeight: Float,
    private val kern: Float,
    private val underline: Boolean = false,
) {
    BodyS(InterRegular, FontWeight.Normal, 10f, 1.6f, 0.3f),
    BodySStrong(InterSemiBold, FontWeight.SemiBold, 10f, 1.6f, 0.3f),
    BodyM(InterRegular, FontWeight.Normal, 12f, 1.333f, 0.24f),
    BodyMStrong(InterSemiBold, FontWeight.SemiBold, 12f, 1.333f, 0.24f),
    BodyMLink(InterRegular, FontWeight.Normal, 12f, 1.333f, 0.24f, underline = true),
    BodyL(InterRegular, FontWeight.Normal, 14f, 1.4286f, 0.14f),
    BodyLStrong(InterSemiBold, FontWeight.SemiBold, 14f, 1.4286f, 0.14f),
    Caption(InterMedium, FontWeight.Medium, 10f, 1.6f, 0.3f),
    OverlineS(InterExtraBold, FontWeight.ExtraBold, 10f, 1.6f, 0.3f),
    OverlineM(InterExtraBold, FontWeight.ExtraBold, 12f, 1.333f, 0.24f),
    DisplayX2S(MontserratBold, FontWeight.Bold, 12f, 1.333f, 0.24f),
    DisplayXS(MontserratBold, FontWeight.Bold, 14f, 1.4286f, 0.24f),
    DisplayS(MontserratExtraBold, FontWeight.ExtraBold, 16f, 1.5f, 0.24f),
    DisplayM(MontserratExtraBold, FontWeight.ExtraBold, 20f, 1.4f, 0.24f),
    DisplayL(MontserratExtraBold, FontWeight.ExtraBold, 24f, 1.333f, 0.24f),
    DisplayXL(MontserratExtraBold, FontWeight.ExtraBold, 32f, 1.25f, 0f),
    DisplayX2L(MontserratExtraBold, FontWeight.ExtraBold, 40f, 1.2f, 0.24f),
    ;

    val textStyle: TextStyle
        get() = TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            // Flutter `height` multiplica el tamaño de fuente: alto de línea fijo = tamaño × height
            lineHeight = Math.round(size * lineHeight).sp,
            letterSpacing = kern.sp,
            textDecoration = if (underline) TextDecoration.Underline else null,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            // Centra el texto dentro del alto de línea fijo
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
}
