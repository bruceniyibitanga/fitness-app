package app.levitx.data.backup

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
import app.levitx.data.coach.CoachCodec
import app.levitx.data.coach.coachDocumentOf
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupDocumentTest {
    private val now = 1_800_000_000_000L
    private val coachJson = CoachCodec.encode(
        coachDocumentOf(
            AthleteModel.starting(CoachProfile("first_pull_up"), PullUpAnswer.NOT_YET),
            setOf(DayOfWeek.MONDAY),
        ),
    )
    private val session = BackedUpSession(
        id = "s1",
        gateId = "first_pull_up.dead_hang",
        startedAt = now - 3_600_000,
        finishedAt = now - 1_800_000,
        bodyweightKg = 74.2,
        workoutId = "w1",
        sets = listOf(BackedUpSet(0, 30, 2, false), BackedUpSet(1, 28, null, true, addedKg = 5.0)),
    )
    private val good = BackupDocument(
        v = BACKUP_DOCUMENT_VERSION,
        madeAt = now,
        coach = coachJson,
        goal = BackedUpGoal("first_pull_up", now - 86_400_000),
        weighIns = listOf(BackedUpWeighIn(74.2, now - 86_400_000)),
        bands = listOf(BackedUpBand("b1", "Red", 22.5, now - 86_400_000)),
        gateBests = listOf(BackedUpGateBest("first_pull_up.dead_hang", 30, now - 1_800_000)),
        sessions = listOf(session),
    )

    private fun problemOf(document: BackupDocument): BackupProblem = try {
        document.checked(now)
        fail("the document passed")
        error("unreachable")
    } catch (error: BackupException) {
        error.problem
    }

    @Test
    fun aRealDocumentPassesAndSurvivesTheCodec() {
        assertEquals(good, BackupCodec.decode(BackupCodec.encode(good)).checked(now))
    }

    @Test
    fun anEmptyPhoneBacksUpToo() {
        val empty = BackupDocument(BACKUP_DOCUMENT_VERSION, now, coach = null)
        assertEquals(empty, empty.checked(now))
    }

    @Test
    fun aNewerFormatAsksForAnUpdate() {
        assertEquals(BackupProblem.NEWER_VERSION, problemOf(good.copy(v = BACKUP_DOCUMENT_VERSION + 1)))
    }

    @Test
    fun valuesOutsideTheLimitsAreRefused() {
        val broken = listOf(
            good.copy(v = 0),
            good.copy(madeAt = now + 3 * 86_400_000L),
            good.copy(coach = "{not json"),
            good.copy(coach = coachJson.replace("first_pull_up", "a_goal_that_does_not_exist")),
            good.copy(weighIns = listOf(BackedUpWeighIn(Double.NaN, now))),
            good.copy(weighIns = listOf(BackedUpWeighIn(9.0, now))),
            good.copy(bands = listOf(BackedUpBand("b1", "x".repeat(31), 10.0, now))),
            good.copy(bands = List(2) { BackedUpBand("same", "Red", 10.0, now) }),
            good.copy(bands = listOf(BackedUpBand("b1", "Red", -1.0, now))),
            good.copy(gateBests = listOf(BackedUpGateBest("gate\u0000", 3, now))),
            good.copy(sessions = listOf(session, session)),
            good.copy(sessions = listOf(session.copy(finishedAt = session.startedAt - 1))),
            good.copy(sessions = listOf(session.copy(id = "x".repeat(MAX_ID_LENGTH + 1)))),
            good.copy(sessions = listOf(session.copy(sets = listOf(BackedUpSet(0, 5), BackedUpSet(0, 6))))),
            good.copy(sessions = listOf(session.copy(sets = listOf(BackedUpSet(0, 5, repsInReserve = 11))))),
            good.copy(sessions = listOf(session.copy(sets = listOf(BackedUpSet(0, MAX_SET_VALUE + 1))))),
            good.copy(sessions = listOf(session.copy(sets = List(MAX_SETS_PER_SESSION + 1) { BackedUpSet(it, 5) }))),
            good.copy(
                sessions = listOf(session.copy(sets = listOf(BackedUpSet(0, 5, addedKg = Double.POSITIVE_INFINITY)))),
            ),
        )
        broken.forEachIndexed { index, document ->
            assertEquals("case $index", BackupProblem.UNREADABLE_CONTENTS, problemOf(document))
        }
    }

    @Test
    fun contentsThatArentADocumentAreUnreadable() {
        listOf(ByteArray(10) { 7 }, gzip("{\"v\": \"one\"}"), gzip("[]")).forEach { bytes ->
            try {
                BackupCodec.decode(bytes)
                fail("decoded")
            } catch (error: BackupException) {
                assertEquals(BackupProblem.UNREADABLE_CONTENTS, error.problem)
            }
        }
    }

    @Test
    fun contentsThatUnpackPastTheCapAreRefused() {
        val bomb = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { zip ->
                repeat(MAX_CONTENTS_BYTES / 1_048_576 + 1) { zip.write(ByteArray(1_048_576)) }
            }
        }.toByteArray()
        try {
            BackupCodec.decode(bomb)
            fail("decoded")
        } catch (error: BackupException) {
            assertEquals(BackupProblem.UNREADABLE_CONTENTS, error.problem)
        }
    }

    private fun gzip(text: String): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }.toByteArray()
}
