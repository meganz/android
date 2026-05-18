package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.shares.AccessPermission

class ShareNonContactOptionsContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareNonContactOptionsContentReadAllowChange() {
        AndroidThemeForPreviews {
            Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
                ShareNonContactOptionsContent(
                    nonContactEmail = "alice@mega.co.nz",
                    accessPermission = AccessPermission.READ,
                    avatarColor = Color.Red.toArgb(),
                    allowChangePermission = true,
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                    emojify = { it },
                    extractEmojiList = { null },
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareNonContactOptionsContentReadWriteAllowChange() {
        AndroidThemeForPreviews {
            Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
                ShareNonContactOptionsContent(
                    nonContactEmail = "bob@mega.co.nz",
                    accessPermission = AccessPermission.READWRITE,
                    avatarColor = Color.Blue.toArgb(),
                    allowChangePermission = true,
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                    emojify = { it },
                    extractEmojiList = { null },
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareNonContactOptionsContentFullDisallowChange() {
        AndroidThemeForPreviews {
            Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
                ShareNonContactOptionsContent(
                    nonContactEmail = "charlie@mega.co.nz",
                    accessPermission = AccessPermission.FULL,
                    avatarColor = Color.Magenta.toArgb(),
                    allowChangePermission = false,
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                    emojify = { it },
                    extractEmojiList = { null },
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareNonContactOptionsContentUnknownAccess() {
        AndroidThemeForPreviews {
            Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
                ShareNonContactOptionsContent(
                    nonContactEmail = "diana@mega.co.nz",
                    accessPermission = AccessPermission.UNKNOWN,
                    avatarColor = Color.Yellow.toArgb(),
                    allowChangePermission = true,
                    onChangePermissionClicked = {},
                    onRemoveClicked = {},
                    emojify = { it },
                    extractEmojiList = { null },
                )
            }
        }
    }
}
