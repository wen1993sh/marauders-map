package com.marauders.map.data

import com.marauders.map.model.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarMathTest {

    @Test
    fun distance_increases_as_rssi_weakens() {
        assertTrue(rssiToDistance(-50) < rssiToDistance(-80))
    }

    @Test
    fun distance_close_to_one_meter_at_reference_power() {
        val d = rssiToDistance(MEASURED_POWER)
        assertTrue("distance at reference should be ~1m, was $d", d in 0.8..1.3)
    }

    @Test
    fun angle_is_stable_and_in_range() {
        val a1 = angleFromAddress("AA:BB:CC:DD:EE:FF")
        val a2 = angleFromAddress("AA:BB:CC:DD:EE:FF")
        assertEquals(a1, a2)
        assertTrue(a1 in 0f..360f)
    }

    private fun dev(mac: String, distance: Double) =
        UiDevice(mac, mac, true, -50, distance, 0f, null, 1, 0L)

    @Test
    fun focus_picks_nearest_n() {
        val devs = (0..10).map { dev("mac$it", it.toDouble()) }
        assertEquals(setOf("mac0", "mac1", "mac2"), focusMacs(devs, 3))
    }

    @Test
    fun focus_returns_all_when_fewer_than_n() {
        val devs = (0..2).map { dev("m$it", it.toDouble()) }
        assertEquals(3, focusMacs(devs, 8).size)
    }
}
