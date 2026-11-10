package app.levitx.phone.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.phone.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

/**
 * Decides the first screen: choosing a goal for someone new, home for
 * everyone else. Decided once per launch; navigation takes over after that.
 */
@HiltViewModel
class LaunchViewModel @Inject constructor(goalRepository: GoalRepository) : ViewModel() {
    /** The first screen, or null while it is being worked out (a few milliseconds). */
    val startDestination: StateFlow<Destination?> = goalRepository.observeActiveGoal()
        .take(1)
        .map<SkillPath?, Destination?> { goal -> if (goal == null) Destination.GoalPicker else Destination.Home }
        // Locked data: home shows the explanation, so go there.
        .catch { error -> if (error is DataUnavailableException) emit(Destination.Home) else throw error }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
