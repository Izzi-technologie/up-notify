package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.settings.DeviceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class DeviceKeyTest {
    @Test
    fun acceptsFiveCharactersFromTheReadableAlphabet() {
        assertTrue(DeviceKey.isValid("K7M2P"))
        assertFalse(DeviceKey.isValid("device-001"))
        assertFalse(DeviceKey.isValid("k7m2p"))
        assertFalse(DeviceKey.isValid("K7M2"))
        assertFalse(DeviceKey.isValid("0OOOO"))
        assertFalse(DeviceKey.isValid("I1LLL"))
    }

    @Test
    fun generateIsStableForASeedAndResolveKeepsAValidKey() {
        val first = DeviceKey.generate(Random(1))
        val second = DeviceKey.generate(Random(1))
        assertEquals(first, second)
        assertTrue(DeviceKey.isValid(first))
        assertEquals(DeviceKey.LENGTH, first.length)
        assertTrue(first.all { it in DeviceKey.ALPHABET })
        assertEquals("K7M2P", DeviceKey.resolve("K7M2P"))
        assertTrue(DeviceKey.isValid(DeviceKey.resolve("device-001", Random(2))))
    }
}