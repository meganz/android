package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import mega.privacy.android.core.test.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class AvailableOfflineMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val viewModel = mock<ChatViewModel>()
    private val underTest: AvailableOfflineMessageAction = AvailableOfflineMessageAction(viewModel)

    private val managementMessageViewModel = mock<NodeAttachmentMessageViewModel>()

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(NodeAttachmentMessageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn managementMessageViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    private val contactMessage = mock<ContactAttachmentMessage> {
        on { isContact } doReturn true
    }

    @Test
    fun `test that action applies to node attachment messages`() {
        Truth.assertThat(
            underTest.appliesTo(
                setOf(mock<NodeAttachmentMessage> {
                    on { exists } doReturn true
                })
            )
        ).isTrue()
    }

    @Test
    fun `test that action does not apply to non existent node attachment messages`() {
        Truth.assertThat(
            underTest.appliesTo(
                setOf(mock<NodeAttachmentMessage> {
                    on { exists } doReturn false
                })
            )
        ).isFalse()
    }

    @Test
    fun `test that action does not apply to non node attachment messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<TextMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to invalid messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<InvalidMessage>()))).isFalse()
    }


    @Test
    fun `test that open with option shows correctly`() = runTest {
        val hideBottomSheet = mock<() -> Unit>()
        whenever(managementMessageViewModel.isAvailableOffline(anyOrNull())).thenReturn(true)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                underTest.bottomSheetMenuItem(
                    setOf(mock<NodeAttachmentMessage>()),
                    hideBottomSheet,
                    {}
                ).invoke()
            }
        }
        with(composeTestRule) {
            onNodeWithTag(underTest.bottomSheetItemTestTag).assertIsDisplayed()
            onNodeWithText(composeTestRule.activity.getString(R.string.file_properties_available_offline)).assertIsDisplayed()
            onNodeWithTag(OFFLINE_SWITCH_TEST_TAG).assertIsDisplayed()
            onNodeWithTag(underTest.bottomSheetItemTestTag).performClick()
            verify(hideBottomSheet).invoke()
        }
    }
}