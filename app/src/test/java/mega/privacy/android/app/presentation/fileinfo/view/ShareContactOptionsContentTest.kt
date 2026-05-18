package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.contact.model.ContactPermissionUiState
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

    val contact = ContactItemUiState(
        handle = 2L,
        displayName = "Bob Brown",
        status = ContactItemStatus.Away,
        lastSeen = 65535,
        avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
        isVerified = false,
    )
    private val contactItem = ContactPermissionUiState(
        permission = AccessPermission.READWRITE,
        email = "contactItemForPreviews@mega.co.nz",
        contactItemUiState = contact,
    )

    @Test
    fun `test that a label with contact alias is shown`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactItem, true, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithText(
            contactItem.contactItemUiState.displayName
        ).assertExists()
    }

    @Test
    fun `test that a label with correct access permission is shown`() {
        var contactPermission by mutableStateOf(contactItem)
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactPermission, true, {}, {}, {})
            }
        }
        AccessPermission.entries.filter { it != AccessPermission.UNKNOWN }.forEach {
            println("checking $it")
            contactPermission = contactItem.copy(permission = it)
            composeTestRule.onNodeWithText(
                contactPermission.permission.description() ?: -1
            ).assertExists("permission text not found for $it")
        }
    }

    @Test
    fun `test that change access permission item is shown when allowChangePermission is true`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactItem, true, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION)
            .assertExists()
    }

    @Test
    fun `test that change access permission item is not shown when allowChangePermission is false`() {
        composeTestRule.setContent {
            Column {
                ShareContactOptionsContent(contactItem, false, {}, {}, {})
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
                    contactPermission = contactItem,
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
                    contactPermission = contactItem,
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
                    contactPermission = contactItem,
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