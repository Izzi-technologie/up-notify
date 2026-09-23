package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.data.AlertParser
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.model.WsIncoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertParserTest {
    @Test
    fun parsesCanonicalAlert() {
        val parsed = AlertParser.parse(
            """
            {
              "type": "alert",
              "id": "evt_123456",
              "severity": "critical",
              "title": "Production API",
              "message": "Production API is down",
              "timestamp": "2026-09-23T10:42:00Z"
            }
            """.trimIndent(),
        )
        val alert = (parsed as WsIncoming.Alert).event
        assertEquals("evt_123456", alert.id)
        assertEquals(Severity.CRITICAL, alert.severity)
        assertEquals("Production API", alert.title)
        assertEquals("Production API is down", alert.message)
        assertEquals("2026-09-23T10:42:00Z", alert.timestamp)
    }

    @Test
    fun parsesCheckmateShape() {
        val parsed = AlertParser.parse(
            """
            {
              "type": "alert",
              "severity": "critical",
              "text": "Production API is down",
              "monitor": { "name": "Production API" }
            }
            """.trimIndent(),
        ) { "evt_generated" }
        val alert = (parsed as WsIncoming.Alert).event
        assertEquals("evt_generated", alert.id)
        assertEquals("Production API", alert.title)
        assertEquals("Production API is down", alert.message)
        assertEquals(Severity.CRITICAL, alert.severity)
    }

    @Test
    fun missingSeverityDefaultsToInfo() {
        val parsed = AlertParser.parse("""{"type":"alert","text":"hello"}""") { "evt_1" }
        val alert = (parsed as WsIncoming.Alert).event
        assertEquals(Severity.INFO, alert.severity)
        assertEquals("hello", alert.title)
        assertEquals("hello", alert.message)
    }

    @Test
    fun parsesResolvedAndConnected() {
        val resolved = AlertParser.parse("""{"type":"alert_resolved","id":"evt_123"}""")
        assertEquals(WsIncoming.Resolved("evt_123"), resolved)
        val connected = AlertParser.parse("""{"type":"connected","deviceId":"tv-001"}""")
        assertEquals(WsIncoming.Connected("tv-001"), connected)
    }

    @Test
    fun invalidPayloadDoesNotThrow() {
        assertNull(AlertParser.parse("not-json"))
        assertNull(AlertParser.parse("""{"type":"unknown"}"""))
        assertNull(AlertParser.parse("""{"type":"alert_resolved"}"""))
        assertNull(AlertParser.parse("[]"))
        assertTrue(AlertParser.parse("""{"type":"alert"}""") { "evt_x" } is WsIncoming.Alert)
    }
}
