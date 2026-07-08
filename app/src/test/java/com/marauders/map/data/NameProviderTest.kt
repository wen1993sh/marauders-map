package com.marauders.map.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameProviderTest {

    @Test
    fun same_mac_always_same_nickname() {
        val mac = "11:22:33:44:55:66"
        assertEquals(NameProvider.nameFor(mac), NameProvider.nameFor(mac))
    }

    @Test
    fun nickname_is_from_list() {
        val name = NameProvider.nameFor("AA:BB:CC:DD:EE:FF")
        assertTrue("nickname $name should be from the list", NameProvider.names.contains(name))
    }
}
