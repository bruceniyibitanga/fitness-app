package app.levitx.phone.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.CoachState
import app.levitx.phone.isExpectedDataFailure
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
 * Decides the first screen: setup for someone the coach doesn't know yet, the
 * tabs for everyone else. Decided once per launch; navigation takes over after that.
 */
@HiltViewModel
class LaunchViewModel @Inject constructor(coachRepository: CoachRepository) : ViewModel() {
    /** The first screen, or null while it is being worked out (a few milliseconds). */
    val startDestination: StateFlow<Destination?> = coachRepository.observeState()
        .take(1)
        .map<CoachState?, Destination?> { state -> if (state == null) Destination.Onboarding else Destination.Main }
        // Data that can't be read: the tabs explain it, and nothing is overwritten by setup.
        .catch { error -> if (error.isExpectedDataFailure()) emit(Destination.Main) else throw error }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
