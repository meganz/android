package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED_DOTS
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.contact.model.ContactPermissionUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class SharedInfoContactItemViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val clickEvent = mock<() -> Unit>()
    private val longClickEvent = mock<() -> Unit>()
    private val moreOptionsClickEvent = mock<() -> Unit>()

    val contact = ContactItemUiState(
        handle = 2L,
        displayName = "Bob Brown",
        status = ContactItemStatus.Away,
        lastSeen = 65535,
        avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
        isVerified = false,
    )
    private val contactItem = mock<ContactPermissionUiState> {
        on { permission }.thenReturn(AccessPermission.READWRITE)
        on { email }.thenReturn("contactItemForPreviews@mega.co.nz")
        on { contactItemUiState }.thenReturn(contact)
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            SharedInfoContactItemView(
                contactItem = contactItem,
                onClick = clickEvent,
                onLongClick = longClickEvent,
                onMoreOptionsClick = moreOptionsClickEvent,
                selected = false,
            )
        }
    }

    @Test
    fun `test that the on click event is fired when view is clicked`() {
        composeTestRule.onNodeWithTag(TEST_TAG_CONTACT_ITEM_SHARED).performClick()
        verify(clickEvent).invoke()
        verifyNoInteractions(longClickEvent)
        verifyNoInteractions(moreOptionsClickEvent)
    }

    @Test
    fun `test that the on long click event is fired when view is long clicked`() {
        composeTestRule.onNodeWithTag(TEST_TAG_CONTACT_ITEM_SHARED)
            .performTouchInput { longClick() }
        verify(longClickEvent).invoke()
        verifyNoInteractions(clickEvent)
        verifyNoInteractions(moreOptionsClickEvent)
    }

    @Test
    fun `test that the on more options click event is fired when view is long clicked`() {
        composeTestRule.onNodeWithTag(TEST_TAG_CONTACT_ITEM_SHARED_DOTS).performClick()
        verify(moreOptionsClickEvent).invoke()
        verifyNoInteractions(clickEvent)
        verifyNoInteractions(longClickEvent)
    }
}