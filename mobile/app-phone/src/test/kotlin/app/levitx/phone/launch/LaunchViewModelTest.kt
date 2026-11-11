package app.levitx.phone.launch

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.CoachState
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.coachState
import app.levitx.phone.navigation.Destination
import app.levitx.testing.FakeCoachRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LaunchViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun someoneNewStartsWithSetup() {
        assertEquals(Destination.Onboarding, LaunchViewModel(FakeCoachRepository()).startDestination.value)
    }

    @Test
    fun someoneSetUpGoesStraightToTheTabs() {
        assertEquals(Destination.Main, LaunchViewModel(FakeCoachRepository(coachState())).startDestination.value)
    }

    @Test
    fun lockedDataGoesToTheTabsSoSetupCantOverwriteIt() {
        val locked = object : CoachRepository by FakeCoachRepository() {
            override fun observeState(): Flow<CoachState?> = flow { throw DataUnavailableException("locked") }
        }
        assertEquals(Destination.Main, LaunchViewModel(locked).startDestination.value)
    }
}
