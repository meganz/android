package mega.privacy.android.shared.original.core.ui.controls.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DATE_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DESCRIPTION_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DIVIDER
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_UNREAD_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_SECTION_TITLE_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_TITLE_ROW_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class NotificationItemViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        titleColor: TextColor = TextColor.Primary,
        typeTitle: String = "CONTACTS",
        title: String = "Test Notification",
        description: String? = "Test description",
        subText: AnnotatedString? = null,
        date: String = "11 October 2022 6:46 pm",
        isNew: Boolean = true,
    ) {
        composeRule.setContent {
            NotificationItemView(
                titleColor = titleColor,
                typeTitle = typeTitle,
                title = title,
                description = description,
                subText = subText,
                date = date,
                isUnread = isNew,
                onClick = {}
            )
        }
    }

    @Test
    fun `test that notification has all the required views`() {
        setupRule()

        composeRule.onNodeWithTag(NOTIFICATION_SECTION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_UNREAD_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_TITLE_ROW_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_DATE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_DIVIDER, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that notification description is displayed when description is not empty`() {
        setupRule(description = "xyz@gmail.com is now a contact")
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that notification description is NOT displayed when description is empty`() {
        setupRule(description = "")
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG).assertDoesNotExist()
    }
}