package mega.privacy.mobile.home.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.prompt.ErrorPrompt
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Composable for home screen when network is offline
 *
 * @param hasOfflineFiles Whether the user has offline files available
 * @param onViewOfflineFilesClick Callback when user clicks to view offline files
 * @param modifier Modifier for the composable
 */
@Composable
internal fun HomeOfflineScreen(
    hasOfflineFiles: Boolean,
    onViewOfflineFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ErrorPrompt(
            stringResource(sharedR.string.sync_no_network_state),
            forceRiceTopAppBar = true
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(iconPackR.drawable.ic_no_cloud),
                    modifier = Modifier
                        .width(120.dp)
                        .testTag(HOME_OFFLINE_ICON_TEST_TAG),
                    contentDescription = "No Network Icon"
                )
                if (hasOfflineFiles) {
                    MegaText(
                        stringResource(sharedR.string.home_screen_no_network_desc_with_offline_files),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    MegaText(
                        stringResource(sharedR.string.home_screen_no_network_desc),
                        textAlign = TextAlign.Center
                    )
                }

                if (hasOfflineFiles) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryFilledButton(
                        onClick = onViewOfflineFilesClick,
                        modifier = Modifier.testTag(HOME_OFFLINE_VIEW_FILES_BUTTON_TEST_TAG),
                        text = stringResource(sharedR.string.home_screen_no_network_view_offline_files_button),
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun HomeOfflineScreenWithFilesPreview() {
    AndroidThemeForPreviews {
        HomeOfflineScreen(
            hasOfflineFiles = true,
            onViewOfflineFilesClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun HomeOfflineScreenWithoutFilesPreview() {
    AndroidThemeForPreviews {
        HomeOfflineScreen(
            hasOfflineFiles = false,
            onViewOfflineFilesClick = {},
        )
    }
}

internal const val HOME_OFFLINE_ICON_TEST_TAG = "home_offline_screen:icon"
internal const val HOME_OFFLINE_VIEW_FILES_BUTTON_TEST_TAG = "home_offline_screen:view_files_button"
