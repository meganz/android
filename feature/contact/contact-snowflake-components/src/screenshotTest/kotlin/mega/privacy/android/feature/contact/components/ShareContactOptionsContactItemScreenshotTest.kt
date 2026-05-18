package mega.privacy.android.feature.contact.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the migrated `ShareContactOptionsContent` row, which
 * renders a [ContactItemView] with [ContactItemStatus.Unknown] (no inline
 * status dot) and a permission label as the subtitle.
 */
class ShareContactOptionsContactItemScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareContactOptionsContactItemReadAndWrite() {
        AndroidThemeForPreviews {
            ContactItemView(
                state = ContactItemUiState(
                    displayName = "Alice Anderson",
                    statusText = "Read & write",
                    status = ContactItemStatus.Unknown,
                    avatar = AvatarData.Initials(
                        initials = "A",
                        avatarColor = Color(0xFF2E7D32),
                    ),
                    isVerified = false,
                ),
                onClick = null,
                showDivider = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareContactOptionsContactItemReadOnlyVerified() {
        AndroidThemeForPreviews {
            ContactItemView(
                state = ContactItemUiState(
                    displayName = "Bob Brown",
                    statusText = "Read only",
                    status = ContactItemStatus.Unknown,
                    avatar = AvatarData.Initials(
                        initials = "B",
                        avatarColor = Color(0xFF1565C0),
                    ),
                    isVerified = true,
                ),
                onClick = null,
                showDivider = true,
            )
        }
    }
}
