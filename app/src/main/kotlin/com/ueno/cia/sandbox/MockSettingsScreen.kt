package com.ueno.cia.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.images.DSIcon

private val GroupedBackground = Color(0xFFF2F2F7)
private val SystemBlue = Color(0xFF007AFF)
private val Separator = Color(0xFFC6C6C8)
private val SecondaryLabel = Color(0x993C3C43)

private fun system(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Color.Black) =
    TextStyle(fontFamily = FontFamily.Default, fontSize = size.sp, fontWeight = weight, color = color)

/**
 * `MockSettingsViewController` de iOS: lista de endpoints mockeados agrupada ("insetGrouped"), un grupo por endpoint
 * con el encabezado `nombre · MÉTODO path`; tocar una respuesta la deja seleccionada (checkmark). "Restablecer"
 * vuelve a las respuestas por defecto y "Listo" cierra. No recarga CIA: la respuesta elegida se usa en el próximo pedido.
 */
@Composable
fun MockSettingsScreen(
    flows: List<MockFlow>,
    selection: MockSelectionStore,
    onClose: () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    BackHandler(onBack = onClose)
    val endpoints = remember(flows) { flows.flatMap { it.endpoints } }
    val selected = remember {
        mutableStateMapOf<String, Int>().apply { endpoints.forEach { put(it.key, selection.selectedIndex(it)) } }
    }

    Column(Modifier.fillMaxSize().background(GroupedBackground).statusBarsPadding().testTag("cia_mocks_screen")) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp)) {
            Text(
                "Restablecer",
                style = system(17, color = SystemBlue),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(role = Role.Button) {
                        selection.reset()
                        endpoints.forEach { selected[it.key] = 0 }
                    }
                    .testTag("cia_mocks_reset"),
            )
            Text("Mocks", style = system(17, FontWeight.SemiBold), modifier = Modifier.align(Alignment.Center))
            Text(
                "Listo",
                style = system(17, FontWeight.SemiBold, SystemBlue),
                modifier = Modifier.align(Alignment.CenterEnd).clickable(role = Role.Button, onClick = onClose).testTag("cia_mocks_done"),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).navigationBarsPadding(),
        ) {
            items(endpoints, key = { it.key }) { endpoint ->
                Text(
                    "${endpoint.name} · ${endpoint.method} ${endpoint.path}",
                    style = system(13, color = SecondaryLabel),
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
                )
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                ) {
                    endpoint.responses.forEachIndexed { index, response ->
                        if (index > 0) Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(Separator))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .clickable(role = Role.RadioButton) {
                                    selected[endpoint.key] = index
                                    selection.select(index, endpoint)
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                                .testTag("cia_mock_${endpoint.name}_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(response.description, style = system(17), modifier = Modifier.weight(1f))
                            if (selected[endpoint.key] == index) {
                                Spacer(Modifier.width(12.dp))
                                DSIcon("check", tint = SystemBlue, size = 18.dp)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
