package com.ueno.cia.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** DataStore real sobre un archivo temporal: los tests no necesitan Android para ejercitarlo. */
internal fun testDataStore(scope: CoroutineScope, name: String): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
    File.createTempFile("cia-$name", ".preferences_pb").also {
        it.delete()
        it.deleteOnExit()
    }
}
