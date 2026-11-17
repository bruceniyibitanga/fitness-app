package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.intervals.intervalPhoto
import app.levitx.phone.intervals.intervalRowMeta
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Chip
import app.levitx.phone.ui.ChipRow
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.TabHeader

/** Train tab: connects [TrainViewModel] to [TrainScreen]. */
@Composable
fun TrainRoute(navigation: TrainNavigation, viewModel: TrainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrainScreen(uiState, navigation)
}

/**
 * Where the Train tab, and search, can lead.
 *
 * @property onOpenWorkout Opens the workout for a focus, by its name.
 * @property onOpenProgram Opens a skill program, by its id.
 * @property onOpenInterval Opens a cardio or mobility workout, by its id.
 * @property onOpenExercise Opens an exercise's how-to, by its id.
 * @property onOpenSearch Opens search.
 * @property onOpenAllPrograms Opens the list of every program.
 */
data class TrainNavigation(
    val onOpenWorkout: (String) -> Unit,
    val onOpenProgram: (String) -> Unit,
    val onOpenInterval: (String) -> Unit,
    val onOpenExercise: (String) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenAllPrograms: () -> Unit,
)

/** Goes nowhere, for previews. */
internal val previewTrainNavigation = TrainNavigation({}, {}, {}, {}, {}, {})

/**
 * The Train tab, laid out as the Train artboard: chips for everything, each
 * part of the body, cardio and mobility; filters under them; then skill
 * programs as photo cards, single workouts and cardio and mobility workouts
 * as photo rows. Sections the chip and filters leave empty aren't shown.
 */
@Composable
fun TrainScreen(uiState: TrainUiState, navigation: TrainNavigation, modifier: Modifier = Modifier) {
    // Both survive rotation and coming back from a workout.
    var chip by rememberSaveable { mutableStateOf(TrainChip.ALL) }
    var filters by rememberSaveable(stateSaver = FilterSaver) { mutableStateOf(emptySet<TrainFilter>()) }
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background)) {
        TabHeader(stringResource(R.string.train_label)) { SearchButton(navigation.onOpenSearch) }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            when (uiState) {
                TrainUiState.Loading -> Unit

                TrainUiState.Unavailable -> Lede(
                    text = stringResource(R.string.data_unavailable),
                    modifier = Modifier.padding(ScreenPadding),
                )

                is TrainUiState.Ready -> {
                    Chips(selected = chip, onSelect = { chip = it })
                    FilterRow(on = filters, onToggle = { filters = filters.toggled(it) })
                    val lists = trainLists(uiState, chip, filters)
                    if (lists.isEmpty) NoMatches(onClear = { filters = emptySet() })
                    if (lists.programs.isNotEmpty()) Programs(lists.programs, uiState.programs.size, navigation)
                    if (lists.workouts.isNotEmpty()) Workouts(lists.workouts, navigation.onOpenWorkout)
                    if (lists.intervals.isNotEmpty()) Intervals(lists.intervals, navigation.onOpenInterval)
                }
            }
        }
    }
}

/** Saves the filters as their names, which a saved-state bundle can hold. */
private val FilterSaver = listSaver<Set<TrainFilter>, String>(
    save = { filters -> filters.map { it.name } },
    restore = { names -> names.mapNotNull { name -> TrainFilter.entries.firstOrNull { it.name == name } }.toSet() },
)

/** "BODY FOCUS" and a chip for each [TrainChip], scrolling sideways. */
@Composable
private fun Chips(selected: TrainChip, onSelect: (TrainChip) -> Unit) {
    Caption(
        stringResource(R.string.train_body_focus),
        modifier = Modifier.padding(start = ScreenPadding, top = 16.dp, bottom = 10.dp),
    )
    ChipRow {
        TrainChip.entries.forEach { chip ->
            Chip(chipName(chip), selected = selected == chip, onClick = { onSelect(chip) })
        }
    }
}

/** Shown when the filters leave nothing: what happened and the way out. */
@Composable
private fun NoMatches(onClear: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.train_no_matches_title),
            style = LevitxType.Figure,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Lede(stringResource(R.string.train_no_matches_body))
        OutlineBar(text = stringResource(R.string.train_clear_filters), onClick = onClear)
    }
}

/** The programs heading with ALL and the count on the right, then the cards, scrolling sideways. */
@Composable
private fun Programs(programs: List<ProgramSummary>, total: Int, navigation: TrainNavigation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = ScreenPadding, top = 8.dp),
    ) {
        Caption(stringResource(R.string.train_programs), modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = navigation.onOpenAllPrograms)
                .padding(horizontal = ScreenPadding),
        ) { Caption(stringResource(R.string.train_all_programs_link, total), color = LevitxColors.Accent) }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        programs.forEach { program -> ProgramCard(program, onClick = { navigation.onOpenProgram(program.path.id) }) }
    }
}

/** The magnifying glass in the header. */
@Composable
private fun SearchButton(onOpenSearch: () -> Unit) {
    val label = stringResource(R.string.train_search)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onOpenSearch)
            .semantics { contentDescription = label },
    ) { Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = LevitxColors.Text) }
}

/** The single workouts heading and a row for each. */
@Composable
private fun Workouts(workouts: List<WorkoutSummary>, onOpen: (String) -> Unit) {
    Caption(
        stringResource(R.string.train_workouts),
        modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 4.dp),
    )
    workouts.forEach { workout ->
        WorkoutRow(workout, onClick = { onOpen(workout.focus.name) })
        Rule()
    }
}

/** The cardio and mobility heading and a row for each. They come from the catalogue: nothing personal. */
@Composable
private fun Intervals(workouts: List<IntervalWorkout>, onOpen: (String) -> Unit) {
    Caption(
        stringResource(R.string.train_intervals),
        modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 4.dp),
    )
    workouts.forEach { workout ->
        PhotoListRow(intervalPhoto(workout.kind), workout.name, intervalRowMeta(workout), onClick = {
            onOpen(workout.id)
        })
        Rule()
    }
}

@Preview
@Composable
private fun TrainScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR, Equipment.LOW_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        TrainScreen(
            uiState = TrainUiState.Ready(programsFor(model), workoutsFor(model, Coach(), nowEpochMillis = 0L)),
            navigation = previewTrainNavigation,
        )
    }
}
