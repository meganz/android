package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.contact.model.ContactPermissionUiState

/**
 * Screen-level screenshot tests for [ShareContactOptionsContent].
 *
 * Renders the full bottom-sheet content (migrated [ContactItemView] header
 * row plus the permission/info/remove menu actions) inside a [Column] so the
 * `ColumnScope` extension can be composed.
 *
 * Goldens recorded against the post-migration rendering. They are used to
 * detect any unintended regressions in the surrounding screen as a result
 * of the AND-23427 migration.
 */
class ShareContactOptionsContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareContactOptionsContentReadAndWriteUnverified() {
        AndroidThemeForPreviews {
            Column {
                ShareContactOptionsContent(
                    contactPermission = previewContactPermission(
                        displayName = "Alice Anderson",
                        email = "alice@mega.io",
                        isVerified = false,
                        permission = AccessPermission.READWRITE,
                    ),
                    allowChangePermission = true,
                    onInfoClicked = {},
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareContactOptionsContentReadOnlyVerified() {
        AndroidThemeForPreviews {
            Column {
                ShareContactOptionsContent(
                    contactPermission = previewContactPermission(
                        displayName = "Bob Brown",
                        email = "bob@mega.io",
                        isVerified = true,
                        permission = AccessPermission.READ,
                    ),
                    allowChangePermission = true,
                    onInfoClicked = {},
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                )
            }
        }
    }

    private fun previewContactPermission(
        displayName: String,
        email: String,
        isVerified: Boolean,
        permission: AccessPermission,
    ): ContactPermissionUiState = ContactPermissionUiState(
        contactItemUiState = ContactItemUiState(
            handle = -1L,
            displayName = displayName,
            status = ContactItemStatus.Online,
            lastSeen = null,
            avatar = AvatarData.Initials(
                initials = displayName.take(1),
                avatarColor = Color(0xFF2E7D32),
            ),
            isVerified = isVerified,
        ),
        email = email,
        permission = permission,
    )
}
