package mega.privacy.android.feature.contact.group.create.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class GroupImagePickerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GroupImagePickerButtonWithoutImage() {
        AndroidThemeForPreviews {
            GroupImagePickerButton(
                imageUri = null,
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GroupImageSourceOptions() {
        AndroidThemeForPreviews {
            GroupImageSourceSheetContent(
                onTakePhoto = {},
                onChooseFromGallery = {},
            )
        }
    }
}
