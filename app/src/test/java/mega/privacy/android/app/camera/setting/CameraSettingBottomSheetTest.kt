package mega.privacy.android.app.camera.setting

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CameraSettingBottomSheetTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that save location switch is shown`() {
        initComposeRule()
        composeRule.onNodeWithTag(SAVE_LOCATION_SWITCH_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that texts are displayed`() {
        initComposeRule()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.camera_settings_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.camera_settings_save_location_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.camera_settings_save_location_description))
            .assertIsDisplayed()
    }

    @Test
    fun `test that onEnableGeoTagging invoke false in case switch is off`() {
        val onEnableGeoTagging = mock<(Boolean) -> Unit>()
        initComposeRule(onEnableGeoTagging = onEnableGeoTagging)
        composeRule.onNodeWithTag(SAVE_LOCATION_SWITCH_TEST_TAG).performClick()
        verify(onEnableGeoTagging).invoke(false)
    }

    private fun initComposeRule(
        onEnableGeoTagging: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            CameraSettingBottomSheet(
                state = CameraSettingUiState(isGeoTaggingEnabled = true),
                onEnableGeoTagging = onEnableGeoTagging,
                showPermissionDeniedSnackbar = {}
            )
        }
    }
}