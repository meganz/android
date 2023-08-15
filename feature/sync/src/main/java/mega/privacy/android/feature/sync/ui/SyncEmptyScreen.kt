package mega.privacy.android.feature.sync.ui

import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.appbar.TopAppBar
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R

@Composable
internal fun SyncEmptyScreen(getStartedClicked: () -> Unit) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(topBar = {
        TopAppBar(modifier = Modifier.testTag(TAG_SYNC_EMPTY_SCREEN_TOOLBAR),
            title = stringResource(R.string.sync_toolbar_title),
            subtitle = null,
            elevation = false,
            onBackPressed = {
                onBackPressedDispatcher?.onBackPressed()
            })
    }, content = { paddingValues ->
        SyncEmptyScreenContent(
            Modifier.padding(paddingValues), getStartedClicked
        )
    })
}

@Composable
private fun SyncEmptyScreenContent(
    modifier: Modifier,
    getStartedClicked: () -> Unit,
) {
    Column(modifier) {
        Image(
            /**
             * Temporary image placeholder, the design for production image is not ready yet
             */
            painterResource(R.drawable.sync_image_placeholder),
            contentDescription = null,
            modifier = Modifier
                .padding(
                    top = 48.dp, bottom = 32.dp
                )
                .testTag(TAG_SYNC_EMPTY_SCREEN_ILLUSTRATION)
                .fillMaxWidth()
        )

        Text(
            text = "Sync",
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .testTag(TAG_SYNC_EMPTY_ONBOARDING_TITLE),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.textColorPrimary)
        )
        Text(
            text = "You will need to set up a local folder on your \n" + "device that would pair with a chosen folder on \n" + "your Cloud Drive.",
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary)
        )
        Box(
            Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            RaisedDefaultMegaButton(
                /**
                 *  The string for this screen is not available on transifex yet,
                 *  So I'm reusing a similar string from :app module
                 *
                 */
                textId = R.string.start_screen_setting,
                onClick = getStartedClicked,
            )
        }
    }
}

internal const val TAG_SYNC_EMPTY_SCREEN_ILLUSTRATION = "sync_empty_screen_illustration"
internal const val TAG_SYNC_EMPTY_SCREEN_TOOLBAR = "sync_empty_screen_toolbar_test_tag"
internal const val TAG_SYNC_EMPTY_ONBOARDING_TITLE = "sync_empty_screen_onboarding_title_test_tag"

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncEmptyScreenPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncEmptyScreen({})
    }
}
