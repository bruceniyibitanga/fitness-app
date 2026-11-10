package app.levitx.phone.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.goal.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lists the goals and saves the one the person picks.
 *
 * Saving finishes in the ViewModel, so rotating the phone mid-save can't lose
 * it. The screen moves on when [goalSaved] turns true, then calls
 * [onNavigated] so coming back to this screen doesn't move on again.
 */
@HiltViewModel
class GoalPickerViewModel @Inject constructor(private val goalRepository: GoalRepository) : ViewModel() {
    /** Every goal on offer, easiest first. Fixed, so no flow is needed. */
    val goals: List<SkillPath> = SkillCatalogue.all

    private val saved = MutableStateFlow(false)

    /** True once the chosen goal is saved and the screen should move on. */
    val goalSaved: StateFlow<Boolean> = saved.asStateFlow()

    /** Saves [path] as the active goal. */
    fun choose(path: SkillPath) {
        viewModelScope.launch {
            goalRepository.setActiveGoal(path)
            saved.value = true
        }
    }

    /** The screen has moved on. */
    fun onNavigated() {
        saved.value = false
    }
}
