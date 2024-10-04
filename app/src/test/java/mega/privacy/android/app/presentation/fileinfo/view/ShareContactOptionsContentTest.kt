package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ShareContactOptionsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that a label with contact alias is shown`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactPermissionForPreview, true, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithText(
            contactPermissionForPreview.contactItem.contactData.alias ?: "foo"
        ).assertExists()
    }

    @Test
    fun `test that a label with correct access permission is shown`() {
        var contactPermission by mutableStateOf(contactPermissionForPreview)
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactPermission, true, {}, {}, {})
            }
        }
        AccessPermission.entries.filter { it != AccessPermission.UNKNOWN }.forEach {
            println("checking $it")
            contactPermission = contactPermissionForPreview.copy(accessPermission = it)
            composeTestRule.onNodeWithText(
                contactPermission.accessPermission.description() ?: -1
            ).assertExists("permission text not found for $it")
        }
    }

    @Test
    fun `test that change access permission item is shown when allowChangePermission is true`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactPermissionForPreview, true, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION)
            .assertExists()
    }

    @Test
    fun `test that change access permission item is not shown when allowChangePermission is false`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactPermissionForPreview, false, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that callback is invoked when info item is clicked`() {
        val onInfoClicked = mock<() -> Unit>()
        val onChangePermissionClicked = mock<() -> Unit>()
        val onRemoveClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(
                    contactPermission = contactPermissionForPreview,
                    allowChangePermission = true,
                    onInfoClicked = onInfoClicked,
                    onChangePermissionClicked = onChangePermissionClicked,
                    onRemoveClicked = onRemoveClicked,
                )
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_INFO).performClick()
        verify(onInfoClicked).invoke()
        verifyNoInteractions(onChangePermissionClicked, onRemoveClicked)
    }

    @Test
    fun `test that callback is invoked when change permission item is clicked`() {
        val onInfoClicked = mock<() -> Unit>()
        val onChangePermissionClicked = mock<() -> Unit>()
        val onRemoveClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(
                    contactPermission = contactPermissionForPreview,
                    allowChangePermission = true,
                    onInfoClicked = onInfoClicked,
                    onChangePermissionClicked = onChangePermissionClicked,
                    onRemoveClicked = onRemoveClicked,
                )
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION).performClick()
        verify(onChangePermissionClicked).invoke()
        verifyNoInteractions(onInfoClicked, onRemoveClicked)
    }

    @Test
    fun `test that callback is invoked when remove item is clicked`() {
        val onInfoClicked = mock<() -> Unit>()
        val onChangePermissionClicked = mock<() -> Unit>()
        val onRemoveClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(
                    contactPermission = contactPermissionForPreview,
                    allowChangePermission = true,
                    onInfoClicked = onInfoClicked,
                    onChangePermissionClicked = onChangePermissionClicked,
                    onRemoveClicked = onRemoveClicked,
                )
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_REMOVE).performClick()
        verify(onRemoveClicked).invoke()
        verifyNoInteractions(onInfoClicked, onChangePermissionClicked)
    }
}