package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.mobile.analytics.event.ChatConversationAddToCloudDriveActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationAddToCloudDriveActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.core.test.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class ImportMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: ImportMessageAction


    private val folderPicker = mock<(Context, ActivityResultLauncher<Intent>) -> Unit>()
    private val collisionsActivity =
        mock<(Context, List<NameCollisionUiEntity>, ActivityResultLauncher<Intent>) -> Unit>()

    @Before
    fun setUp() {
        underTest = ImportMessageAction(
            launchFolderPicker = folderPicker,
            launchNameCollisionActivity = collisionsActivity,
        )
    }

    @Test
    fun `test that action applies to attachment messages`() {
        assertThat(
            underTest.appliesTo(
                setOf(mock<NodeAttachmentMessage> {
                    on { exists } doReturn true
                })
            )
        ).isTrue()
    }

    @Test
    fun `test that action does not apply to non existent node attachment messages`() {
        assertThat(
            underTest.appliesTo(
                setOf(mock<NodeAttachmentMessage> {
                    on { exists } doReturn false
                })
            )
        ).isFalse()
    }

    @Test
    fun `test that action does not apply other type of messages`() {
        assertThat(underTest.appliesTo(setOf(mock<LocationMessage>()))).isFalse()
    }

    @Test
    fun `test that composable contains edit bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<NodeAttachmentMessage>()),
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that on trigger launches the folder picker`() {
        composeTestRule.setContent {
            underTest.OnTrigger(
                messages = setOf(mock<NodeAttachmentMessage>()),
                onHandled = mock()
            )
        }
        verify(folderPicker).invoke(any(), any())
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a bottom sheet`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.BottomSheet)

        assertThat(analyticsRule.events).contains(ChatConversationAddToCloudDriveActionMenuItemEvent)
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a toolbar`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.Toolbar)

        assertThat(analyticsRule.events).contains(ChatConversationAddToCloudDriveActionMenuEvent)
    }
}