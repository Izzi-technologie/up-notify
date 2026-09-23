package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.alarm.AlarmAudio
import com.wayscompany.webhookalarm.alarm.AlertEngine
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.alarm.PlaybackRequest
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.AlertPolicies
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertEngineTest {
    @Test
    fun infoPlaysOnceThenClears() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onAlert(event("info-1", Severity.INFO))
        val play = audio.plays.single()
        assertEquals("info", play.soundName)
        assertEquals(40, play.volumePercent)
        assertEquals(1, play.repeatCount)
        assertFalse(play.loop)
        assertTrue(engine.state.value is AlertState.Active)
        audio.complete()
        runCurrent()
        assertTrue(engine.state.value is AlertState.Idle)
    }

    @Test
    fun warningRepeatsThreeTimes() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onAlert(event("warn-1", Severity.WARNING))
        val play = audio.plays.single()
        assertEquals("warning", play.soundName)
        assertEquals(75, play.volumePercent)
        assertEquals(3, play.repeatCount)
        assertFalse(play.loop)
    }

    @Test
    fun criticalLoopsUntilAcknowledge() = runTest {
        val audio = FakeAudio()
        val acks = mutableListOf<String>()
        val engine = engine(audio, this, acks)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        val play = audio.plays.single()
        assertEquals("critical", play.soundName)
        assertEquals(100, play.volumePercent)
        assertTrue(play.loop)
        assertTrue(engine.state.value is AlertState.Active)
        assertEquals("crit-1", engine.acknowledge())
        assertEquals(listOf("crit-1"), acks)
        assertTrue(engine.state.value is AlertState.Acknowledged)
        assertTrue(audio.stopCount > 0)
    }

    @Test
    fun criticalReplacesWarningAndInfoDoesNot() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onAlert(event("warn-1", Severity.WARNING))
        engine.onAlert(event("info-1", Severity.INFO))
        assertEquals("warn-1", (engine.state.value as AlertState.Active).event.id)
        assertEquals(1, audio.plays.size)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        assertEquals("crit-1", (engine.state.value as AlertState.Active).event.id)
        assertEquals(2, audio.plays.size)
        assertTrue(audio.plays.last().loop)
    }

    @Test
    fun newerCriticalReplacesCurrentCritical() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        engine.onAlert(event("crit-2", Severity.CRITICAL))
        assertEquals("crit-2", (engine.state.value as AlertState.Active).event.id)
        assertEquals(2, audio.plays.size)
    }

    @Test
    fun resolvedClearsMatchingAlertOnly() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onResolved("missing")
        assertTrue(engine.state.value is AlertState.Idle)

        engine.onAlert(event("crit-1", Severity.CRITICAL))
        engine.onResolved("other")
        assertTrue(engine.state.value is AlertState.Active)

        engine.onResolved("crit-1")
        assertTrue(engine.state.value is AlertState.Resolved)
        advanceTimeBy(2_999)
        assertTrue(engine.state.value is AlertState.Resolved)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(engine.state.value is AlertState.Idle)
    }

    @Test
    fun acknowledgeIsNotResolve() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        engine.acknowledge()
        assertTrue(engine.state.value is AlertState.Acknowledged)
        engine.onAlert(event("info-1", Severity.INFO))
        assertTrue(engine.state.value is AlertState.Acknowledged)
        engine.onResolved("crit-1")
        assertEquals("crit-1", (engine.state.value as AlertState.Resolved).event.id)
    }

    @Test
    fun dismissCriticalAcknowledgesAndClears() = runTest {
        val audio = FakeAudio()
        val acks = mutableListOf<String>()
        val engine = engine(audio, this, acks)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        engine.dismiss()
        assertEquals(listOf("crit-1"), acks)
        assertTrue(engine.state.value is AlertState.Idle)
        assertTrue(audio.stopCount > 0)
    }

    @Test
    fun dismissAcknowledgedClearsWithoutAnotherAck() = runTest {
        val audio = FakeAudio()
        val acks = mutableListOf<String>()
        val engine = engine(audio, this, acks)
        engine.onAlert(event("crit-1", Severity.CRITICAL))
        engine.acknowledge()
        engine.dismiss()
        assertEquals(listOf("crit-1"), acks)
        assertTrue(engine.state.value is AlertState.Idle)
    }

    @Test
    fun dismissInfoClearsWithoutAck() = runTest {
        val audio = FakeAudio()
        val acks = mutableListOf<String>()
        val engine = engine(audio, this, acks)
        engine.onAlert(event("info-1", Severity.INFO))
        engine.dismiss()
        assertTrue(acks.isEmpty())
        assertTrue(engine.state.value is AlertState.Idle)
        assertTrue(audio.stopCount > 0)
    }

    @Test
    fun acknowledgeWithoutActiveAlertDoesNothing() = runTest {
        val audio = FakeAudio()
        val engine = engine(audio, this)
        assertNull(engine.acknowledge())
        engine.onAlert(event("info-1", Severity.INFO))
        assertNull(engine.acknowledge())
        assertTrue(engine.state.value is AlertState.Active)
    }

    private fun engine(
        audio: FakeAudio,
        scope: kotlinx.coroutines.test.TestScope,
        acks: MutableList<String> = mutableListOf(),
    ) = AlertEngine(
        audio = audio,
        scope = scope,
        policies = { AlertPolicies.Default },
        onAcknowledge = { acks += it },
        resolvedHoldMs = 3_000L,
    )

    private fun event(id: String, severity: Severity) = AlertEvent(
        id = id,
        type = "alert",
        severity = severity,
        title = "Production API",
        message = "Production API is down",
        timestamp = "2026-09-23T10:42:00Z",
    )
}

private class FakeAudio : AlarmAudio {
    val plays = mutableListOf<PlaybackRequest>()
    var stopCount = 0
    private var finished: (() -> Unit)? = null

    override fun play(request: PlaybackRequest, onFinished: () -> Unit) {
        plays += request
        finished = if (request.loop) null else onFinished
    }

    override fun stop() {
        stopCount += 1
        finished = null
    }

    fun complete() {
        finished?.invoke()
    }
}
