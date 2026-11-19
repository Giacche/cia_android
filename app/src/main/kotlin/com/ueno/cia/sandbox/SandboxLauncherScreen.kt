package com.ueno.cia.sandbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import androidx.compose.material3.Text

/** Flujo que se puede abrir desde el sandbox. `entry == null` = todavía no migrado a Android. */
data class SandboxFlow(val id: String, val title: String, val entry: CIARoute?)

val SandboxFlows = listOf(
    SandboxFlow("onboarding", "Onboarding", entry = null),
    SandboxFlow("onboarding_legacy", "Onboarding legacy (retrocompatibilidad)", entry = null),
    SandboxFlow("payment_selector", "Selector de cobros", entry = CIARoute.PaymentSelectorEntryPoint()),
    SandboxFlow("home", "Home Cobros", entry = CIARoute.Home),
    SandboxFlow("upos", "Solicitud de UPos", entry = null),
)

// El sandbox de iOS usa la fuente del sistema (no Inter/Montserrat): acá Roboto, la del sistema de Android.
private fun system(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = DSColor.Black) =
    TextStyle(fontFamily = FontFamily.Default, fontSize = size.sp, fontWeight = weight, color = color)

/**
 * Pantalla inicial del sandbox (la "Testing App" de Flutter / `SandboxRootViewController` de iOS): elige tenant y
 * flujo y abre CIA como lo haría la app host. Los mocks se configuran desde el engranaje (o agitando el dispositivo).
 */
@Composable
fun SandboxLauncherScreen(
    tenant: Tenant,
    selectedFlow: String,
    onTenantChange: (Tenant) -> Unit,
    onFlowChange: (String) -> Unit,
    onStart: () -> Unit,
    onOpenMocks: () -> Unit,
    onBack: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    Box(Modifier.fillMaxSize().background(DSColor.Neutral50).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l)
                    .padding(top = Spacing.m),
            ) {
                TopBar(onBack = onBack, onOpenMocks = onOpenMocks)
                Spacer(Modifier.height(Spacing.m))
                TenantChips(tenant, onTenantChange)
                Spacer(Modifier.height(Spacing.m))
                Text("Selecciona el flujo a probar", style = system(20, FontWeight.Bold))
                Spacer(Modifier.height(Spacing.xs))
                Box(Modifier.fillMaxWidth().height(1.5.dp).background(DSColor.Black))
                Spacer(Modifier.height(Spacing.m))
                Text(
                    "Esta aplicación permite iniciar los flujos configurados. " +
                        "Puede configurar los mocks en la sección de ajustes en la parte superior.",
                    style = system(14, color = DSColor.Neutral600),
                )
                Spacer(Modifier.height(Spacing.xl))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    SandboxFlows.forEach { flow ->
                        FlowCard(flow, selected = flow.id == selectedFlow, onClick = { onFlowChange(flow.id) })
                    }
                }
                Spacer(Modifier.height(Spacing.m))
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.l)
                    .padding(top = Spacing.m, bottom = Spacing.xs)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DSColor.Black)
                    .clickable(role = Role.Button, onClick = onStart)
                    .testTag("sandbox_open_cia"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Iniciar flujo", style = system(16, FontWeight.SemiBold, DSColor.White))
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onOpenMocks: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        CircleButton(label = "Volver", onClick = onBack, modifier = Modifier.testTag("sandbox_back")) {
            DSIcon("arrowLeft", tint = DSColor.Black, size = 16.dp)
        }
        Text(
            "COBROS IN APP - TESTING APP",
            style = system(13, FontWeight.ExtraBold, DSColor.Neutral600),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        CircleButton(label = "Configurar mocks", onClick = onOpenMocks, modifier = Modifier.testTag("sandbox_mocks")) {
            Icon(Icons.Outlined.Settings, contentDescription = null, tint = DSColor.Black, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CircleButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, DSColor.Neutral300, CircleShape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TenantChips(tenant: Tenant, onChange: (Tenant) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Tenant", style = system(14, FontWeight.Medium, DSColor.Neutral600))
        listOf(Tenant.INDIVIDUOS to "Individuos", Tenant.EMPRESAS to "Empresas").forEach { (value, name) ->
            val selected = value == tenant
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) DSColor.Blue100 else Color.Transparent)
                    .border(BorderStroke(1.5.dp, if (selected) Color.Transparent else DSColor.Neutral400), RoundedCornerShape(16.dp))
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Button) { onChange(value) }
                    .padding(horizontal = Spacing.m)
                    .testTag("sandbox_tenant_${name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, style = system(13, color = if (selected) DSColor.Blue800 else DSColor.Black))
            }
        }
    }
}

@Composable
private fun FlowCard(flow: SandboxFlow, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.s)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DSColor.White, shape)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) DSColor.Black else DSColor.Neutral200), shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = Spacing.xl, end = Spacing.m, top = Spacing.l, bottom = Spacing.l)
            .testTag("sandbox_flow_${flow.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(flow.title, style = system(16, color = DSColor.Neutral700), modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (selected) DSColor.Black else Color.Transparent, CircleShape)
                .then(if (selected) Modifier else Modifier.border(1.dp, DSColor.Neutral200, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).background(DSColor.White, CircleShape))
        }
    }
}
