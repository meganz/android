package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ShareNonContactOptionsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that non contact email is shown`() {
        val email = "xyz@mega.co.nz"
        initComposeRuleContent(nonContactEmail = email)
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()
    }


    @Test
    fun `test that correct access permission is shown`() {
        val (accessPermission, accessPermissionDescription) = Pair(
            AccessPermission.FULL, AccessPermission.FULL.description() ?: 0
        )
        initComposeRuleContent(accessPermission = accessPermission)
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(id = accessPermissionDescription)).assertIsDisplayed()
    }

    @Test
    fun `test that change access permission item is shown when allowChangePermission is true`() {
        initComposeRuleContent(allowChangePermission = true)
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_CHANGE_PERMISSION)
            .assertExists()
    }

    @Test
    fun `test that change access permission item is not shown when allowChangePermission is false`() {
        initComposeRuleContent(allowChangePermission = false)
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_CHANGE_PERMISSION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that callback is invoked when change permission item is clicked`() {
        val onChangePermissionClicked = mock<() -> Unit>()
        val onRemoveClicked = mock<() -> Unit>()

        initComposeRuleContent(
            allowChangePermission = true,
            onChangePermissionClicked = onChangePermissionClicked,
            onRemoveClicked = onRemoveClicked
        )
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_CHANGE_PERMISSION).performClick()
        verify(onChangePermissionClicked).invoke()
        verifyNoInteractions(onRemoveClicked)
    }

    @Test
    fun `test that callback is invoked when remove item is clicked`() {
        val onChangePermissionClicked = mock<() -> Unit>()
        val onRemoveClicked = mock<() -> Unit>()

        initComposeRuleContent(
            allowChangePermission = true,
            onChangePermissionClicked = onChangePermissionClicked,
            onRemoveClicked = onRemoveClicked
        )
        composeTestRule.onNodeWithTag(SHARE_NON_CONTACT_OPTIONS_REMOVE).performClick()
        verify(onRemoveClicked).invoke()
        verifyNoInteractions(onChangePermissionClicked)
    }

    private fun initComposeRuleContent(
        nonContactEmail: String = "test@example.com",
        accessPermission: AccessPermission = AccessPermission.READ,
        avatarColor: Int = 2,
        allowChangePermission: Boolean = true,
        onChangePermissionClicked: () -> Unit = {},
        onRemoveClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            Column {
                ShareNonContactOptionsContent(
                    nonContactEmail = nonContactEmail,
                    accessPermission = accessPermission,
                    avatarColor = avatarColor,
                    allowChangePermission = allowChangePermission,
                    onChangePermissionClicked = onChangePermissionClicked,
                    onRemoveClicked = onRemoveClicked
                )
            }
        }
    }
}