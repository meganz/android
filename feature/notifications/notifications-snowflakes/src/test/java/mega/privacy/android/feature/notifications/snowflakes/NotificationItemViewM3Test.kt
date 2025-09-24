package mega.privacy.android.feature.notifications.snowflakes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.values.TextColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NotificationItemViewM3Test {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        titleColor: TextColor = TextColor.Primary,
        typeTitle: String = "CONTACTS",
        title: String = "Test Notification",
        description: String? = "Test description",
        subText: AnnotatedString? = null,
        date: String = "11 October 2022 6:46 pm",
        isNew: Boolean = false,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            NotificationItemViewM3(
                titleColor = titleColor,
                typeTitle = typeTitle,
                title = title,
                description = description,
                subText = subText,
                date = date,
                isNew = isNew,
                onClick = onClick
            )
        }
    }

    @Test
    fun `test that notification item has all required views`() {
        setupRule()

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DATE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_STRONG_DIVIDER_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that NEW chip is displayed when isNew is true`() {
        setupRule(isNew = true)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that NEW chip is not displayed when isNew is false`() {
        setupRule(isNew = false)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that description is displayed when description is not null`() {
        setupRule(description = "Test description")

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that description is not displayed when description is null`() {
        setupRule(description = null)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that description is not displayed when description is empty`() {
        setupRule(description = "")

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that subText is displayed when subText is not null`() {
        val subText = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append("Meeting scheduled for ")
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.Companion.SemiBold)) {
                append("tomorrow at 2:00 PM")
            }
        }
        setupRule(subText = subText)

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that subText is not displayed when subText is null`() {
        setupRule(subText = null)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that different title colors are applied correctly`() {
        setupRule(titleColor = TextColor.Error)

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that different notification types display correctly`() {
        setupRule(
            typeTitle = "INCOMING SHARES",
            title = "File Shared with You",
            titleColor = TextColor.Warning
        )

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that title row shows reduced lines when description is present`() {
        setupRule(description = "Has description")

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that title row shows full lines when description is not present`() {
        setupRule(description = null)

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that span styling works correctly in title`() {
        setupRule(title = "Title with [A]styled text[/A] and [B]more styling[/B]")

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that span styling works correctly in description`() {
        setupRule(description = "Description with [A]styled text[/A] and [B]more styling[/B]")

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that background color changes based on isNew state`() {
        setupRule(isNew = true)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that subtle divider is displayed when notification is new`() {
        setupRule(isNew = true)

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SUBTLE_DIVIDER_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_STRONG_DIVIDER_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun `test that strong divider is displayed when notification is not new`() {
        setupRule(isNew = false)

        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_STRONG_DIVIDER_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SUBTLE_DIVIDER_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun `test that notification with all elements displays correctly`() {
        val subText = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append("Scheduled meeting for ")
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.Companion.SemiBold)) {
                append("tomorrow at 3:00 PM")
            }
        }

        setupRule(
            titleColor = TextColor.Accent,
            typeTitle = "SCHEDULED MEETINGS",
            title = "Meeting Reminder",
            description = "Your meeting is scheduled for tomorrow",
            subText = subText,
            date = "12 October 2022 2:30 pm",
            isNew = true
        )

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DATE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that minimal notification displays correctly`() {
        setupRule(
            titleColor = TextColor.Secondary,
            typeTitle = "OTHERS",
            title = "Simple Notification",
            description = null,
            subText = null,
            date = "10 October 2022 1:00 pm",
            isNew = false
        )

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            NOTIFICATION_ITEM_VIEW_DATE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that clicking on notification item calls onClick lambda`() {
        val onClick: () -> Unit = mock()

        setupRule(onClick = onClick)

        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG, useUnmergedTree = true)
            .performClick()

        verify(onClick).invoke()
    }
}