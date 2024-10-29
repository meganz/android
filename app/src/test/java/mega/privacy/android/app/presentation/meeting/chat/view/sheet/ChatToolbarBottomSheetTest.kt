package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.mobile.analytics.event.ChatConversationContactMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationFileMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationGIFMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationGalleryMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationLocationMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationScanMenuItemEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [ChatToolbarBottomSheetTest]
 */
@RunWith(AndroidJUnit4::class)
internal class ChatToolbarBottomSheetTest {
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onAttachFileClicked: (List<Uri>) -> Unit = mock()
    private val onPickLocation: () -> Unit = mock()
    private val onAttachScan: () -> Unit = mock()

    private val chatGalleryViewModel = mock<ChatGalleryViewModel> {
        on { state } doReturn MutableStateFlow(ChatGalleryState())
    }

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ChatGalleryViewModel::class.java.canonicalName.orEmpty()) }) } doReturn chatGalleryViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that gallery list exists`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_GALLERY_LIST).assertExists()
    }

    @Test
    fun `test that gallery list is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GALLERY).assertIsDisplayed()
    }

    @Test
    fun `test that file button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_FILE).assertIsDisplayed()
    }

    @Test
    fun `test that gif button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GIF).assertIsDisplayed()
    }

    @Test
    fun `test that scan button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_SCAN).assertIsDisplayed()
    }

    @Test
    fun `test that location button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_LOCATION).assertIsDisplayed()
    }

    @Test
    fun `test that contact button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_CONTACT).assertIsDisplayed()
    }

    @Test
    fun `test that gallery list click records analytics event`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GALLERY).performClick()
        assertThat(analyticsRule.events).contains(ChatConversationGalleryMenuItemEvent)
    }

    @Test
    fun `test that location button click records analytics event and is passed to upper caller`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_LOCATION).performClick()
        verify(onPickLocation).invoke()
        assertThat(analyticsRule.events).contains(ChatConversationLocationMenuItemEvent)
    }

    @Test
    fun `test that file button click records analytics event`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_FILE).performClick()
        assertThat(analyticsRule.events).contains(ChatConversationFileMenuItemEvent)
    }

    @Test
    fun `test that gif button click records analytics event`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GIF).performClick()
        assertThat(analyticsRule.events).contains(ChatConversationGIFMenuItemEvent)
    }

    @Test
    fun `test that scan button click records analytics event and is passed to upper caller`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_SCAN).performClick()
        verify(onAttachScan).invoke()
        assertThat(analyticsRule.events).contains(ChatConversationScanMenuItemEvent)
    }

    @Test
    fun `test that contact button click records analytics event`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_CONTACT).performClick()
        assertThat(analyticsRule.events).contains(ChatConversationContactMenuItemEvent)
    }

    private fun initComposeRuleContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ChatToolbarBottomSheet(
                    onPickLocation = onPickLocation,
                    onSendGiphyMessage = { },
                    hideSheet = {},
                    isVisible = true,
                    closeModal = {},
                    uiState = ChatUiState(),
                    scaffoldState = rememberScaffoldState(),
                    navigateToFileModal = {},
                    onAttachContacts = {},
                    onAttachFiles = onAttachFileClicked,
                    onAttachScan = onAttachScan,
                )
            }
        }
    }
}