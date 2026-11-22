package app.levitx.phone.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.MIN_PAIN_RATING
import app.levitx.core.pain.PainCheckIn
import app.levitx.core.pain.assessPain
import app.levitx.data.coach.CoachRepository
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stores the answer to "how was your elbow?" on the workout summary. Kept
 * apart from [WorkoutViewModel], whose job ends when the workout is saved.
 *
 * Only "fine" is stored: it puts the area back to normal training. "Still
 * sore" leaves the advice as it is, so there is nothing to write.
 */
@HiltViewModel
class PainAnswerViewModel @Inject constructor(private val coachRepository: CoachRepository) : ViewModel() {
    /**
     * Records that [area] felt fine in the session just done. The write runs
     * to the end even if the person taps Done straight after. If the data
     * can't be opened the answer is dropped: the area stays on light work,
     * which is the careful side to fail on.
     */
    fun settled(area: BodyArea) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                handlingDataFailures(onFailure = {}) {
                    coachRepository.recordPain(area, assessPain(PainCheckIn(duringExercise = MIN_PAIN_RATING)))
                }
            }
        }
    }
}
