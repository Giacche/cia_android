package com.ueno.cia.core.tenant

import java.util.concurrent.atomic.AtomicReference

enum class Tenant { INDIVIDUOS, EMPRESAS }

/**
 * Tenant activo y sucursal seleccionada. Se escribe desde la UI y se lee desde el cliente HTTP (otro hilo),
 * por eso el estado vive en un [AtomicReference] inmutable.
 */
class TenantContext(tenant: Tenant) {
    data class Snapshot(
        val tenant: Tenant,
        val individuosBranchId: String? = null,
        val empresasBranchId: String? = null,
    ) {
        val activeBranchId: String?
            get() = if (tenant == Tenant.EMPRESAS) empresasBranchId else individuosBranchId
    }

    private val state = AtomicReference(Snapshot(tenant))

    val snapshot: Snapshot get() = state.get()
    val tenant: Tenant get() = snapshot.tenant

    fun switchTenant(tenant: Tenant) {
        state.updateAndGet { it.copy(tenant = tenant) }
    }

    /** Lo llama la Home al seleccionar o deslizar sucursal; `null` la limpia. */
    fun setActiveBranchId(branchId: String?) {
        state.updateAndGet {
            when (it.tenant) {
                Tenant.EMPRESAS -> it.copy(empresasBranchId = branchId)
                Tenant.INDIVIDUOS -> it.copy(individuosBranchId = branchId)
            }
        }
    }
}
