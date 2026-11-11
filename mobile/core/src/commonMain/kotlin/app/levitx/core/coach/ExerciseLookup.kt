package app.levitx.core.coach

import app.levitx.core.skills.SkillCatalogue

/**
 * Finds any exercise the coach can plan by its id: a ladder exercise from
 * [ExerciseCatalogue], or a skill gate's training from [SkillTraining].
 *
 * Stored plans and drafts keep only exercise ids and look them up here, so the
 * catalogue stays the one place an exercise is described.
 */
public object ExerciseLookup {
    /** The exercise with [id], or null when no catalogue knows it. */
    public fun byId(id: String): Exercise? =
        ExerciseCatalogue.byId(id) ?: SkillCatalogue.gateById(id)?.let(SkillTraining::exerciseFor)
}
