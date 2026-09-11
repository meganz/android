package mega.privacy.android.feature.contact.info.view

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class ContactInfoOptionsSectionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val invocations = mutableListOf<String>()

    @Test
    fun `test that tapping the nickname row invokes onNicknameClick`() {
        setSection()

        composeTestRule.onNodeWithTag(CONTACT_INFO_NICKNAME_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onNicknameClick")
    }

    @Test
    fun `test that the nickname row offers to set a nickname when there is none`() {
        setSection(nickname = null)

        onNodeWithText(sharedR.string.contact_info_set_nickname).assertExists()
        onNodeWithText(sharedR.string.contact_info_edit_nickname).assertDoesNotExist()
    }

    @Test
    fun `test that the nickname row shows the nickname as subtitle when one is set`() {
        setSection(nickname = "Ally")

        onNodeWithText(sharedR.string.contact_info_edit_nickname).assertExists()
        composeTestRule.onNodeWithText("Ally").assertExists()
    }

    @Test
    fun `test that tapping verify credentials invokes onVerifyCredentialsClick`() {
        setSection(showVerifyCredentials = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_VERIFY_CREDENTIALS_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onVerifyCredentialsClick")
    }

    @Test
    fun `test that the verify credentials row is not shown when showVerifyCredentials is false`() {
        setSection(showVerifyCredentials = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_VERIFY_CREDENTIALS_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the verified icon and subtitle are shown when credentials are verified`() {
        setSection(showVerifyCredentials = true, areCredentialsVerified = true)

        verifiedIcon().assertExists()
        onNodeWithText(sharedR.string.contact_info_credentials_verified).assertExists()
    }

    @Test
    fun `test that the verified icon is not shown when credentials are not verified`() {
        setSection(showVerifyCredentials = true, areCredentialsVerified = false)

        verifiedIcon().assertDoesNotExist()
        onNodeWithText(sharedR.string.contact_info_credentials_not_verified).assertExists()
    }

    @Test
    fun `test that tapping share contact invokes onShareContactClick`() {
        setSection(showShareContact = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARE_CONTACT_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onShareContactClick")
    }

    @Test
    fun `test that the share contact row is not shown when showShareContact is false`() {
        setSection(showShareContact = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARE_CONTACT_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tapping shared folders invokes onSharedFoldersClick when there are folders`() {
        setSection(showSharedFolders = true, sharedFoldersCount = 3)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARED_FOLDERS_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onSharedFoldersClick")
    }

    @Test
    fun `test that tapping shared folders invokes no callback when there are no folders`() {
        setSection(showSharedFolders = true, sharedFoldersCount = 0)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARED_FOLDERS_ROW_TAG)
            .performTouchInput { click(percentOffset(ROW_BODY_HORIZONTAL_PERCENT, 0.5f)) }

        assertThat(invocations).isEmpty()
    }

    @Test
    fun `test that the shared folders row shows the folder count`() {
        setSection(showSharedFolders = true, sharedFoldersCount = 3)

        val expected = context.resources.getQuantityString(
            sharedR.plurals.num_of_folders_with_parameter,
            3,
            3,
        )
        composeTestRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun `test that the shared folders row is not shown when showSharedFolders is false`() {
        setSection(showSharedFolders = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARED_FOLDERS_ROW_TAG).assertDoesNotExist()
    }

    private fun setSection(
        nickname: String? = null,
        areCredentialsVerified: Boolean = false,
        showVerifyCredentials: Boolean = true,
        showShareContact: Boolean = true,
        showSharedFolders: Boolean = true,
        sharedFoldersCount: Int = 3,
    ) {
        composeTestRule.setContent {
            ContactInfoOptionsSection(
                nickname = nickname,
                areCredentialsVerified = areCredentialsVerified,
                showVerifyCredentials = showVerifyCredentials,
                showShareContact = showShareContact,
                showSharedFolders = showSharedFolders,
                sharedFoldersCount = sharedFoldersCount,
                onNicknameClick = { invocations.add("onNicknameClick") },
                onVerifyCredentialsClick = { invocations.add("onVerifyCredentialsClick") },
                onShareContactClick = { invocations.add("onShareContactClick") },
                onSharedFoldersClick = { invocations.add("onSharedFoldersClick") },
            )
        }
    }

    private fun verifiedIcon() = composeTestRule.onNodeWithTag(
        CONTACT_INFO_CREDENTIALS_VERIFIED_ICON_TAG,
        useUnmergedTree = true,
    )

    private fun onNodeWithText(@StringRes id: Int) =
        composeTestRule.onNodeWithText(context.getString(id))

    private companion object {
        const val ROW_BODY_HORIZONTAL_PERCENT = 0.1f
    }
}
