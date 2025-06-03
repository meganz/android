package mega.privacy.android.feature.chat.settings.calls.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.settings.calls.model.CallSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Call settings screen
 */
@Composable
internal fun CallSettingsScreen(
    uiState: CallSettingsUiState,
    onBackPressed: () -> Unit,
    onSoundNavigationChanged: (Boolean) -> Unit,
) {
    CallSettingsView(
        onBackPressed = onBackPressed,
        isSoundNotificationActive = uiState.isSoundNotificationActive,
        onSoundNotificationChanged = onSoundNavigationChanged,
    )
}

@Composable
internal fun CallSettingsView(
    onBackPressed: () -> Unit = {},
    isSoundNotificationActive: Boolean?,
    onSoundNotificationChanged: (Boolean) -> Unit,
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val soundNotificationEnabled = isSoundNotificationActive != null
    val soundNotificationActive = isSoundNotificationActive == true
    MegaScaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        snackbarHost = {
            MegaSnackbar(snackbarHostState)
        },
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.settings_calls_title),
                navigationType = AppBarNavigationType.Back(onBackPressed),
            )
        },
        bottomBar = { },
        content = {
            Column(
                Modifier
                    .padding(it)
            ) {
                FlexibleLineListItem(
                    modifier = Modifier.testTag(SOUND_NOTIFICATION_TEST_TAG),
                    enableClick = soundNotificationEnabled,
                    onClickListener = {
                        onSoundNotificationChanged(!soundNotificationActive)
                    },
                    title = stringResource(sharedR.string.settings_calls_sound_notifications_title),
                    subtitle = stringResource(sharedR.string.settings_calls_sound_notifications_body),
                    trailingElement = {
                        Toggle(
                            modifier = Modifier.testTag(SOUND_NOTIFICATION_TOGGLE_TEST_TAG),
                            isChecked = soundNotificationActive,
                            onCheckedChange = onSoundNotificationChanged,
                            isEnabled = soundNotificationEnabled,
                        )
                    }
                )
            }
        }
    )
}

@Composable
@Preview
private fun MainComposeViewPreview() {
    var soundNotification by remember { mutableStateOf(false) }
    AndroidThemeForPreviews {
        CallSettingsView(isSoundNotificationActive = soundNotification) {
            soundNotification = it
        }
    }
}

internal const val SOUND_NOTIFICATION_TEST_TAG = "calls_settings_screen:list_item"
internal const val SOUND_NOTIFICATION_TOGGLE_TEST_TAG = "calls_settings_screen:toggle"
