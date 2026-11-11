package app.levitx.phone.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.equipment.EquipmentRoute
import app.levitx.phone.path.PathRoute
import app.levitx.phone.skills.SkillsRoute
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.today.TodayRoute
import app.levitx.phone.train.TrainScreen

/** Material's minimum touch target, so every tab is easy to hit. */
private val TabHeight = 56.dp
private val IndicatorWidth = 24.dp
private val IndicatorHeight = 2.dp

/**
 * The five tabs, in the order they appear.
 *
 * @property label The tab's name.
 */
enum class MainTab(@param:StringRes val label: Int) {
    /** The next session the coach planned. */
    TODAY(R.string.tab_today),

    /** Workouts the person picks themselves. */
    TRAIN(R.string.tab_train),

    /** The skill path toward the goal. */
    PLAN(R.string.tab_plan),

    /** Every movement's ladder and where the person is on it. */
    SKILLS(R.string.tab_skills),

    /** Bodyweight and bands. */
    BODY(R.string.tab_body),
}

/**
 * The app once setup is done: a tab bar and the tab's content.
 *
 * Tabs swap content in place rather than pushing screens, so back from any
 * tab leaves the app, as people expect from a bottom bar. The chosen tab
 * survives rotation and process death.
 *
 * @param onOpenWorkout Opens the workout in progress, over the tabs.
 */
@Composable
fun MainRoute(onOpenWorkout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
    MainScreen(selected = tab, onSelect = { tab = it }) {
        when (tab) {
            MainTab.TODAY -> TodayRoute(onOpenWorkout = onOpenWorkout)
            MainTab.TRAIN -> TrainScreen()
            MainTab.PLAN -> PathRoute()
            MainTab.SKILLS -> SkillsRoute()
            MainTab.BODY -> EquipmentRoute()
        }
    }
}

/** The tab bar around [content]. Stateless, so it previews and tests without the tabs' data. */
@Composable
fun MainScreen(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { TabBar(selected, onSelect) },
    ) { innerPadding ->
        // Consuming the insets here stops a tab's own Scaffold padding for the system bars a second time.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) { content() }
    }
}

/**
 * Text tabs in the app's label style, with a short accent bar under the
 * chosen one. Each is a selectable item in one group, so screen readers
 * announce "tab, selected, 1 of 5".
 */
@Composable
private fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Column(
        modifier = Modifier
            .background(LevitxColors.Background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(color = LevitxColors.Line)
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TabHeight)
                        .selectable(selected = isSelected, onClick = { onSelect(tab) }, role = Role.Tab)
                        .padding(top = LevitxSpacing.Medium),
                ) {
                    Text(
                        text = stringResource(tab.label),
                        style = LevitxType.Label,
                        color = if (isSelected) LevitxColors.Text else LevitxColors.TextFaint,
                    )
                    Spacer(Modifier.height(LevitxSpacing.Small))
                    Box(
                        Modifier
                            .width(IndicatorWidth)
                            .height(IndicatorHeight)
                            .background(if (isSelected) LevitxColors.Accent else LevitxColors.Background),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    LevitxTheme {
        MainScreen(selected = MainTab.TODAY, onSelect = {}) { TrainScreen() }
    }
}
