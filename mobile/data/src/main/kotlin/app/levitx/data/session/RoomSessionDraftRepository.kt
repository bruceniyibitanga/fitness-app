package app.levitx.data.session

import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SessionPlan
import app.levitx.core.session.SessionRun
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.SessionDraftEntity
import app.levitx.data.database.SessionDraftRows
import app.levitx.data.database.SessionDraftSetEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Stores the session in progress in the encrypted database. */
@Singleton
internal class RoomSessionDraftRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    SessionDraftRepository {
    override suspend fun load(): SessionRun? {
        val dao = databaseProvider.database().sessionDraftDao()
        val rows = dao.load() ?: return null
        // A draft that no longer makes sense (its gate left the catalogue in an
        // update, say) is dropped, so the app starts fresh instead of crashing
        // every time it opens.
        val run = rows.toSessionRun()
        if (run == null) dao.clear()
        return run
    }

    override suspend fun save(run: SessionRun) {
        databaseProvider.database().sessionDraftDao().replace(run.toRows())
    }

    override suspend fun clear() {
        databaseProvider.database().sessionDraftDao().clear()
    }
}

private fun SessionRun.toRows() = SessionDraftRows(
    draft = SessionDraftEntity(
        sessionId = id,
        gateId = plan.gate.id,
        previousBest = previousBest,
        startedAtEpochMillis = startedAtEpochMillis,
        restSeconds = plan.restSeconds,
        repsInReserve = plan.repsInReserve,
        restEndsAtElapsedMillis = restEndsAtElapsedMillis,
        addedKg = load.addedKg,
        assistanceKg = load.assistanceKg,
    ),
    sets = plan.sets.mapIndexed { index, planned ->
        val done = logged.getOrNull(index)
        SessionDraftSetEntity(
            position = index,
            target = planned.target,
            isBestEffort = planned.isBestEffort,
            value = done?.value,
            loggedRepsInReserve = done?.repsInReserve,
            addedKg = done?.addedKg ?: 0.0,
            assistanceKg = done?.assistanceKg ?: 0.0,
        )
    },
)

/** Rebuilds the run, or returns null when the rows don't describe a valid one. */
private fun SessionDraftRows.toSessionRun(): SessionRun? {
    val gate = SkillCatalogue.gateById(draft.gateId) ?: return null
    // Logged sets are always the first ones, so stop at the first set not done yet.
    val logged = sets.takeWhile { it.value != null }.mapNotNull { set ->
        set.value?.let { value ->
            runCatching {
                LoggedSet(value, set.loggedRepsInReserve, set.isBestEffort, set.addedKg, set.assistanceKg)
            }.getOrNull()
        }
    }
    return runCatching {
        SessionRun(
            id = draft.sessionId,
            plan = SessionPlan(
                gate = gate,
                sets = sets.map { PlannedSet(it.target, it.isBestEffort) },
                restSeconds = draft.restSeconds,
                repsInReserve = draft.repsInReserve,
            ),
            previousBest = draft.previousBest,
            startedAtEpochMillis = draft.startedAtEpochMillis,
            logged = logged,
            restEndsAtElapsedMillis = draft.restEndsAtElapsedMillis,
            load = SetLoad(draft.addedKg, draft.assistanceKg),
        )
    }.getOrNull()?.takeIf { it.plan.sets.isNotEmpty() }
}
