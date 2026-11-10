package app.levitx.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

/**
 * Root composable of the watch app.
 *
 * AppScaffold keeps the time text in place across screens, and ScreenScaffold
 * ties the scroll indicator to the list, as the Wear OS Material 3 guidance
 * recommends. TransformingLazyColumn scales items at the curved screen edges.
 */
@Composable
fun LevitxWearApp() {
    MaterialTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(scrollState = listState) { contentPadding ->
                TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                    item { ListHeader { Text(stringResource(R.string.app_name)) } }
                    item { Text(stringResource(R.string.home_placeholder)) }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun LevitxWearAppPreview() {
    LevitxWearApp()
}
