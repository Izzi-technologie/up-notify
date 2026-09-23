package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.utils.PhoneAlertReadiness
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAlertReadinessTest {
    @Test
    fun allGrantedRequiresEveryPermission() {
        val ready = PhoneAlertReadiness(
            notificationsGranted = true,
            overlayGranted = true,
            fullScreenIntentGranted = true,
            batteryExempt = true,
        )

        assertTrue(ready.allGranted)
        assertFalse(ready.copy(notificationsGranted = false).allGranted)
        assertFalse(ready.copy(overlayGranted = false).allGranted)
        assertFalse(ready.copy(fullScreenIntentGranted = false).allGranted)
        assertFalse(ready.copy(batteryExempt = false).allGranted)
    }
}
