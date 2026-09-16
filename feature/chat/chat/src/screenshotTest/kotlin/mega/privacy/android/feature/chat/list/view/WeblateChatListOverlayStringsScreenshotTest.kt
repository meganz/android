package mega.privacy.android.feature.chat.list.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateContentDescriptionScreenshot
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate reference render for the do-not-disturb label, which only ever appears inside the top app
 * bar's overflow dropdown, something the screenshot engine cannot capture.
 *
 * [WeblateContentDescriptionScreenshot] assembles a MEGA tooltip bubble directly in the hierarchy
 * and points it at the control the string belongs to, so translators see the text together with
 * where it comes from.
 */
class WeblateChatListOverlayStringsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DoNotDisturbOverflowAction() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(sharedR.string.title_dialog_mute_chat_notifications),
                tooltipAlignment = Alignment.TopEnd,
                tooltipOffset = DpOffset(x = (-8).dp, y = 64.dp),
            ) {
                Column {
                    ChatListTopAppBar(
                        chatStatus = ChatStatus.Online,
                        isSearchAvailable = true,
                        hasArchivedChats = true,
                        navigationType = AppBarNavigationType.None,
                        onPresenceClick = {},
                        onSearchClick = {},
                        onArchivedClick = {},
                        onOpenLinkClick = {},
                        onDoNotDisturbClick = {},
                    )
                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }
    }
}
