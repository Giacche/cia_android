package com.ueno.cia.sandbox

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/** Equivale a `motionEnded(.motionShake)` de iOS: agitar el dispositivo abre la configuración de mocks. */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastShake = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val force = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (force < THRESHOLD_G) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShake < COOLDOWN_MS) return
        lastShake = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val THRESHOLD_G = 2.7f
        const val COOLDOWN_MS = 1000L
    }
}
