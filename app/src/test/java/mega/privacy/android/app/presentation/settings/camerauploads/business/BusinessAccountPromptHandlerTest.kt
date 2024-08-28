package mega.privacy.android.app.presentation.settings.camerauploads.business

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.account.business.BUSINESS_ACCOUNT_SUSPENDED_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.business.BusinessAccountPromptHandler
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [BusinessAccountPromptHandler]
 */
@RunWith(AndroidJUnit4::class)
internal class BusinessAccountPromptHandlerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that no prompt is shown when the user is on a non business account`() {
        initializeComposeContent(null)

        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertDoesNotExist()
    }

    @Test
    fun `test that no prompt is shown when the user can normally enable camera uploads`() {
        initializeComposeContent(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)

        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertDoesNotExist()
    }

    @Test
    fun `test that the regular business account prompt is shown when the business account sub user is active`() {
        initializeComposeContent(EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)

        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertDoesNotExist()
    }

    @Test
    fun `test that the suspended business account prompt is shown when the business account sub user is suspended`() {
        initializeComposeContent(EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT)

        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the suspended business account prompt is shown when the business account administrator is suspended`() {
        initializeComposeContent(EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT)

        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
    }

    private fun initializeComposeContent(businessAccountPromptType: EnableCameraUploadsStatus?) {
        composeTestRule.setContent {
            BusinessAccountPromptHandler(
                businessAccountPromptType = businessAccountPromptType,
                onBusinessAccountPromptDismissed = {},
                onRegularBusinessAccountSubUserPromptAcknowledged = {},
            )
        }
    }
}