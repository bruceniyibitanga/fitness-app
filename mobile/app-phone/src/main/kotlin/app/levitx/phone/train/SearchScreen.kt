package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.intervals.intervalPhoto
import app.levitx.phone.intervals.intervalRowMeta
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.DataUnavailable
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/**
 * Search's destination. Reuses [TrainViewModel], so results show the same
 * programs and workouts as the Train tab, with the person's progress.
 */
@Composable
fun SearchRoute(navigation: TrainNavigation, onBack: () -> Unit, viewModel: TrainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(uiState, navigation, onBack)
}

private val FieldHeight = 40.dp

/**
 * Search, laid out as the Search artboard: back, the field and clear across
 * the top, then programs, workouts and exercises that match. The keyboard
 * opens straight away. The query stays on the phone and isn't stored.
 */
@Composable
fun SearchScreen(
    uiState: TrainUiState,
    navigation: TrainNavigation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SearchBar(query, onQuery = { query = it }, onBack = onBack)
        Rule()
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            val ready = uiState as? TrainUiState.Ready
            when {
                query.isBlank() -> Lede(stringResource(R.string.train_search_start), Modifier.padding(ScreenPadding))
                ready != null -> Results(query, ready, navigation)
                uiState == TrainUiState.Unavailable -> DataUnavailable()
            }
        }
    }
}

/** Back, the field with the keyboard up, and clear once there's something to clear. */
@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val hint = stringResource(R.string.train_search_hint)
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        BackButton(onBack, modifier = Modifier.padding(start = 4.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(color = LevitxColors.Text, fontSize = 16.sp),
            cursorBrush = SolidColor(LevitxColors.Accent),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .weight(1f)
                .height(FieldHeight)
                .background(LevitxColors.Surface)
                .border(1.dp, LevitxColors.Accent)
                .focusRequester(focus)
                .semantics { contentDescription = hint },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (query.isEmpty()) Text(hint, fontSize = 16.sp, color = LevitxColors.TextFaint)
                    field()
                }
            },
        )
        ClearButton(visible = query.isNotEmpty(), onClear = { onQuery("") })
    }
}

/** The cross that empties the field. Keeps its space when hidden so the field doesn't jump. */
@Composable
private fun ClearButton(visible: Boolean, onClear: () -> Unit) {
    val label = stringResource(R.string.train_search_clear)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(48.dp)
            .then(
                if (visible) {
                    Modifier.clickable(role = Role.Button, onClick = onClear).semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
    ) {
        if (visible) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = LevitxColors.TextMuted,
            )
        }
    }
}

/** Each kind of match under its own heading with a count, or a line saying there's nothing. */
@Composable
private fun Results(query: String, ready: TrainUiState.Ready, navigation: TrainNavigation) {
    val names = WorkoutFocus.entries.associateWith { focusName(it) }
    val results = remember(query, ready, names) { searchTrain(query, ready, names::getValue) }
    if (results.isEmpty) {
        Column(modifier = Modifier.padding(ScreenPadding).semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(
                stringResource(R.string.train_search_none_title, query.trim()),
                style = LevitxType.Figure,
                color = LevitxColors.Text,
            )
            Lede(stringResource(R.string.train_search_none_body), Modifier.padding(top = 12.dp))
        }
        return
    }
    if (results.programs.isNotEmpty()) {
        Heading(R.string.train_search_programs, results.programs.size)
        results.programs.forEach { program ->
            ProgramRow(program, onClick = { navigation.onOpenProgram(program.path.id) })
            Rule()
        }
    }
    val workouts = results.workouts.size + results.intervals.size
    if (workouts > 0) {
        Heading(R.string.train_search_workouts, workouts)
        results.workouts.forEach { workout ->
            WorkoutRow(workout, onClick = { navigation.onOpenWorkout(workout.focus.name) })
            Rule()
        }
        results.intervals.forEach { workout ->
            PhotoListRow(intervalPhoto(workout.kind), workout.name, intervalRowMeta(workout), onClick = {
                navigation.onOpenInterval(workout.id)
            })
            Rule()
        }
    }
    if (results.exercises.isNotEmpty()) {
        Heading(R.string.train_search_exercises, results.exercises.size)
        results.exercises.forEach { exercise ->
            ExerciseRow(exercise, onClick = { navigation.onOpenExercise(exercise.id) })
            Rule()
        }
    }
}

/** "PROGRAMS · 3". */
@Composable
private fun Heading(title: Int, count: Int) {
    Caption(
        stringResource(R.string.train_search_section, stringResource(title), count),
        modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 4.dp),
    )
}

@Preview
@Composable
private fun SearchScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        SearchScreen(
            uiState = TrainUiState.Ready(programsFor(model), workoutsFor(model, Coach(), nowEpochMillis = 0L)),
            navigation = previewTrainNavigation,
            onBack = {},
            initialQuery = "pull",
        )
    }
}
