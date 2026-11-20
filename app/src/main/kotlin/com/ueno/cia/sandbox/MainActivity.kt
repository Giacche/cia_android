package com.ueno.cia.sandbox

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import com.ueno.cia.kit.CIADependencies
import com.ueno.cia.kit.CIAHost
import com.ueno.cia.kit.CIAModule
import com.ueno.cia.storage.CIAStorage
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Sandbox de CIA (equivale a la app `CIA` de iOS). Arranca en la "Testing App": se elige tenant y flujo y recién con
 * "Iniciar flujo" se abre [CIAModule]. Los mocks se configuran con el engranaje o agitando el dispositivo.
 *
 * Extras de intent para automatizar (equivalen a `-cia-autostart`, `-cia-tenant` y `-cia-reset-mocks`):
 * `--ez autostart true`, `--es flow payment_selector`, `--es tenant EMPRESAS`, `--ez reset_mocks true`.
 */
class MainActivity : ComponentActivity() {
    private val shakes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val shakeDetector = ShakeDetector { shakes.tryEmit(Unit) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialTenant = intent.getStringExtra("tenant")
            ?.let { name -> Tenant.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            ?: Tenant.INDIVIDUOS
        val selection = MockSelectionStore(this)
        if (intent.getBooleanExtra("reset_mocks", false)) selection.reset()
        val autostart = intent.getBooleanExtra("autostart", false)
        val flow = intent.getStringExtra("flow")
        setContent { SandboxRoot(initialTenant, selection, autostart, flow, shakes) }
    }

    override fun onResume() {
        super.onResume()
        val sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(shakeDetector)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SandboxRoot(
    initialTenant: Tenant,
    selection: MockSelectionStore,
    autostart: Boolean,
    initialFlow: String?,
    shakes: MutableSharedFlow<Unit>,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var tenant by remember { mutableStateOf(initialTenant) }
    var selectedFlow by remember { mutableStateOf(initialFlow ?: "home") }
    var module by remember { mutableStateOf<CIAModule?>(null) }
    var startRoute by remember { mutableStateOf<CIARoute>(CIARoute.Home) }
    var showMocks by remember { mutableStateOf(false) }
    var showPending by remember { mutableStateOf(false) }

    // El MockHttpClient lee la respuesta elegida en cada pedido: cambiarla no reinicia CIA (igual que en iOS)
    val httpClient = remember { MockHttpClient(context, MockCatalog.all, selection) }
    val scope = rememberCoroutineScope()
    val featureFlags = remember { CIAStorage.featureFlags(context, scope) }

    fun openCia(route: CIARoute = CIARoute.Home) {
        startRoute = route
        module = CIAModule(
            CIADependencies(
                httpClient = httpClient,
                tenantContext = TenantContext(tenant),
                featureFlags = featureFlags,
                analyticsTracking = AnalyticsTracking { event: AnalyticsEvent ->
                    Log.d("CIA-Analytics", "${event.kind} ${event.subFlow} ${event.parameters} ${event.extraParams}")
                },
            ),
        )
    }

    fun startSelectedFlow() {
        val entry = SandboxFlows.first { it.id == selectedFlow }.entry
        if (entry != null) openCia(entry) else showPending = true
    }

    LaunchedEffect(Unit) { if (autostart) openCia(SandboxFlows.first { it.id == selectedFlow }.entry ?: CIARoute.Home) }
    LaunchedEffect(Unit) { shakes.collect { showMocks = true } }

    val host = remember {
        object : CIAHost {
            override fun onExit() {
                module = null
            }

            override fun onOpenHelpCenter(deeplink: String) {
                Toast.makeText(context, "Centro de ayuda: $deeplink", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        val running = module
        if (running == null) {
            SandboxLauncherScreen(
                tenant = tenant,
                selectedFlow = selectedFlow,
                onTenantChange = { tenant = it },
                onFlowChange = { selectedFlow = it },
                onStart = ::startSelectedFlow,
                onOpenMocks = { showMocks = true },
                onBack = { activity.finish() },
            )
        } else {
            // Atrás desde la raíz de CIA vuelve al launcher (el NavHost interno atiende el resto)
            BackHandler { module = null }
            running.Content(host = host, startRoute = startRoute)
        }
        if (showMocks) {
            MockSettingsScreen(flows = MockCatalog.all, selection = selection, onClose = { showMocks = false })
        }
    }

    if (showPending) {
        AlertDialog(
            onDismissRequest = { showPending = false },
            title = { Text("Próximamente") },
            text = { Text("Este flujo todavía no fue migrado a Android.") },
            confirmButton = { TextButton(onClick = { showPending = false }) { Text("Entendido") } },
        )
    }
}
