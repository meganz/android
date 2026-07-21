package mega.privacy.android.feature.contact.info.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState

class ContactInfoScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoScreenLoading() {
        AndroidThemeForPreviews {
            ContactInfoScreen(
                state = ContactInfoUiState.Loading(closeEvent = consumed),
                onNavigateBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoScreenLoaded() {
        AndroidThemeForPreviews {
            ContactInfoScreen(
                state = ContactInfoUiState.Data(
                    displayName = "Alice Anderson",
                    email = "alice@example.com",
                    userHandle = 1L,
                    chatRoomId = 123L,
                    isFromContacts = true,
                    closeEvent = consumed,
                ),
                onNavigateBack = {},
            )
        }
    }
}
