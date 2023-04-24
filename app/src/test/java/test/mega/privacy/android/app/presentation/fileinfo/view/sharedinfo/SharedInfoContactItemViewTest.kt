package test.mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED_DOTS
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.SharedInfoContactItemView
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
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
    private val contactItem = mock<ContactPermission> {
        on { accessPermission }.thenReturn(AccessPermission.READWRITE)
        on { contactItem }.thenReturn(contactItemForPreviews)
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            SharedInfoContactItemView(
                contactItem = contactItem,
                onClick = clickEvent,
                onLongClick = longClickEvent,
                onMoreOptionsClick = moreOptionsClickEvent
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