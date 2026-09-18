package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Screenshot baselines for the chat management message announcing a new history clearing time,
 * covering the single-year and multi-year forms of
 * [subtitle_properties_manage_chat_label_years].
 */
class RetentionTimeUpdatedMessageViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RetentionTimeUpdatedOneYear() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            RetentionTimeUpdatedMessageView(
                ownerActionFullName = "Alice Anderson",
                newRetentionTime = SECONDS_IN_YEAR,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RetentionTimeUpdatedSeveralYears() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            RetentionTimeUpdatedMessageView(
                ownerActionFullName = "Alice Anderson",
                newRetentionTime = 2 * SECONDS_IN_YEAR,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
