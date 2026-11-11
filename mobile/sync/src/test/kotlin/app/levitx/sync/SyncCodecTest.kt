package app.levitx.sync

import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.coach.QueuedWorkout
import app.levitx.data.equipment.Band
import app.levitx.data.session.StoredSession
import app.levitx.testing.startedWorkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCodecTest {
    private val path = SkillCatalogue.muscleUp
    private val dips = path.gates[1]
    private val sessionId = "3f2b8c1e-7a4d-4e6f-9b0a-1c2d3e4f5a6b"
    private val stored = StoredSession(
        id = sessionId,
        session = CompletedSession(
            gateId = dips.id,
            startedAtEpochMillis = 1_000,
            finishedAtEpochMillis = 2_000,
            sets = listOf(LoggedSet(4, 2, false, addedKg = 5.0), LoggedSet(7, null, true, assistanceKg = 12.5)),
        ),
    )

    private val workoutId = "00000000-0000-4000-8000-00000000000a"
    private val run = startedWorkout(nowEpochMillis = 1_700_000_000_000, id = workoutId).let { started ->
        started.logSet(checkNotNull(started.nextSet).target, 2, nowElapsedMillis = 10_000)
    }

    private fun session(json: String) = SyncCodec.decodeSession(json.encodeToByteArray())

    private fun state(json: String) = SyncCodec.decodeState(json.encodeToByteArray())

    private fun sessionJson(
        id: String = sessionId,
        gateId: String = dips.id,
        startedAt: Long = 1_000,
        finishedAt: Long = 2_000,
        sets: String = """[{"value":4}]""",
    ) = """{"v":1,"id":"$id","gateId":"$gateId","startedAt":$startedAt,"finishedAt":$finishedAt,"sets":$sets}"""

    @Test
    fun aSessionSurvivesTheRoundTrip() {
        assertEquals(Decoded.Ok(stored), SyncCodec.decodeSession(SyncCodec.encodeSession(stored)))
    }

    @Test
    fun bodyweightNeverGoesOnTheWire() {
        val withBodyweight = stored.copy(session = stored.session.copy(bodyweightKg = 74.0))
        val wire = SyncCodec.encodeSession(withBodyweight).decodeToString()
        assertTrue("74.0" !in wire && "bodyweight" !in wire.lowercase())
    }

    @Test
    fun stateSurvivesTheRoundTrip() {
        val state = PhoneState(path, mapOf(dips.id to 6), listOf(Band("b1", "Red", 20.0)))
        assertEquals(Decoded.Ok(state), SyncCodec.decodeState(SyncCodec.encodeState(state)))
    }

    @Test
    fun stateWithTheNextSessionSurvivesTheRoundTrip() {
        val state = PhoneState(
            path = null,
            bests = emptyMap(),
            bands = emptyList(),
            next = run.session,
            recentWorkoutIds = listOf(workoutId),
        )
        assertEquals(Decoded.Ok(state), SyncCodec.decodeState(SyncCodec.encodeState(state)))
    }

    @Test
    fun aWorkoutSurvivesTheRoundTrip() {
        val workout = QueuedWorkout(run, finishedAtEpochMillis = 2_000_000)
        assertEquals(Decoded.Ok(workout), SyncCodec.decodeWorkout(SyncCodec.encodeWorkout(workout)))
    }

    @Test
    fun impossibleWorkoutsAreRejected() {
        val impossible = listOf(
            // Nothing done.
            QueuedWorkout(run.copy(logged = run.session.plans.map { emptyList() }), 2_000_000),
            QueuedWorkout(run, finishedAtEpochMillis = 0),
            QueuedWorkout(run.copy(id = "../../etc/passwd"), 2_000_000),
            QueuedWorkout(run.copy(startedAtEpochMillis = 0), 2_000_000),
            QueuedWorkout(run.copy(logged = run.logged.withFirst(LoggedSet(MAX_RESULT + 1, null, false))), 2_000_000),
        )
        impossible.forEach { workout ->
            assertEquals(workout.toString(), Decoded.Invalid, SyncCodec.decodeWorkout(SyncCodec.encodeWorkout(workout)))
        }
        // A workout whose id doesn't match the id it travels with.
        val encoded = SyncCodec.encodeWorkout(QueuedWorkout(run, 2_000_000)).decodeToString()
        val mismatched = encoded.replaceFirst(workoutId, "00000000-0000-4000-8000-00000000000b")
        assertEquals(Decoded.Invalid, SyncCodec.decodeWorkout(mismatched.encodeToByteArray()))
    }

    @Test
    fun recentWorkoutIdsMustBeIds() {
        assertEquals(Decoded.Invalid, state("""{"v":2,"recent":["not-an-id"]}"""))
        assertEquals(Decoded.Invalid, state("""{"v":2,"next":"not a plan"}"""))
        assertTrue(state("""{"v":2,"recent":["$workoutId"]}""") is Decoded.Ok)
    }

    @Test
    fun aValidSessionIsAccepted() {
        assertTrue(session(sessionJson()) is Decoded.Ok)
    }

    @Test
    fun newerVersionsAreKeptForLater() {
        assertEquals(Decoded.NewerVersion, session(sessionJson().replace("\"v\":1", "\"v\":3")))
        assertEquals(Decoded.NewerVersion, state("""{"v":99,"somethingNew":true}"""))
    }

    @Test
    fun unknownExtraFieldsAreIgnored() {
        assertTrue(state("""{"v":1,"pathId":"${path.id}","addedLater":[1,2,3]}""") is Decoded.Ok)
    }

    @Test
    fun rubbishIsRejectedWithoutThrowing() {
        val rubbish = listOf("", "not json", "[]", "{}", """{"v":"one"}""", """{"v":0}""", """{"v":1}""")
        rubbish.forEach { payload ->
            assertEquals(payload, Decoded.Invalid, session(payload))
            assertEquals(payload, Decoded.Invalid, state(payload))
        }
        assertEquals(Decoded.Invalid, SyncCodec.decodeSession(ByteArray(MAX_PAYLOAD_BYTES + 1) { ' '.code.toByte() }))
    }

    @Test
    fun impossibleSessionsAreRejected() {
        val impossible = listOf(
            sessionJson(id = "not-a-uuid"),
            sessionJson(id = "../../etc/passwd"),
            sessionJson(gateId = "no.such.gate"),
            sessionJson(startedAt = 0),
            sessionJson(startedAt = 2_000, finishedAt = 1_000),
            sessionJson(sets = "[]"),
            sessionJson(sets = """[{"value":-1}]"""),
            sessionJson(sets = """[{"value":10001}]"""),
            sessionJson(sets = """[{"value":4,"rir":-2}]"""),
            sessionJson(sets = """[{"value":4,"addedKg":-5.0}]"""),
            sessionJson(sets = """[{"value":4,"assistKg":100000.0}]"""),
            sessionJson(sets = List(MAX_SETS + 1) { """{"value":1}""" }.joinToString(",", "[", "]")),
        )
        impossible.forEach { payload -> assertEquals(payload, Decoded.Invalid, session(payload)) }
    }

    @Test
    fun impossibleStateIsRejected() {
        val impossible = listOf(
            """{"v":1,"pathId":"no_such_path"}""",
            """{"v":1,"pathId":"${path.id}","bests":{"${dips.id}":-1}}""",
            """{"v":1,"pathId":"${path.id}","bests":{"${dips.id}":10001}}""",
            """{"v":1,"pathId":"${path.id}","bands":[{"id":"b","name":" ","kg":20.0}]}""",
            """{"v":1,"pathId":"${path.id}","bands":[{"id":"b","name":"Red","kg":0.0}]}""",
            """{"v":1,"pathId":"${path.id}","bands":[{"id":"","name":"Red","kg":20.0}]}""",
            """{"v":1,"pathId":"${path.id}","bands":[{"id":"b","name":"${"x".repeat(31)}","kg":20.0}]}""",
        )
        impossible.forEach { payload -> assertEquals(payload, Decoded.Invalid, state(payload)) }
    }

    @Test
    fun bestsForUnknownGatesAreLeftOut() {
        val decoded = state("""{"v":1,"pathId":"${path.id}","bests":{"${dips.id}":6,"future.gate":3}}""")
        assertEquals(mapOf(dips.id to 6), (decoded as Decoded.Ok).value.bests)
    }
}

private fun List<List<LoggedSet>>.withFirst(set: LoggedSet) = mapIndexed { i, sets ->
    if (i ==
        0
    ) {
        listOf(set)
    } else {
        sets
    }
}
