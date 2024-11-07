package mega.privacy.android.shared.original.core.ui.controls.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DATE_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DESCRIPTION_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_DIVIDER
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_GREEN_ICON_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_SECTION_TITLE_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_TITLE_ROW_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemDefinition
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemType
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
        notification: NotificationItemDefinition,
    ) {
        composeRule.setContent {
            NotificationItemView(
                notification.type,
                notification.typeTitle,
                notification.title,
                notification.description,
                notification.subText,
                notification.date,
                notification.isNew,
            ){}
        }
    }

    @Test
    fun `test that notification has all the required views`() {
        val notification = NotificationItemDefinition(
            type = NotificationItemType.Others,
            typeTitle = "CONTACTS",
            title = "New Contact",
            description = "xyz@gmail.com is now a contact",
            subText = null,
            date = "11 October 2022 6:46 pm",
            isNew = true,
        )

        setupRule(notification)

        composeRule.onNodeWithTag(NOTIFICATION_SECTION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_GREEN_ICON_TEST_TAG, useUnmergedTree = true)
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
        val notification = NotificationItemDefinition(
            type = NotificationItemType.Others,
            typeTitle = "CONTACTS",
            title = "New Contact",
            description = "xyz@gmail.com is now a contact",
            subText = null,
            date = "11 October 2022 6:46 pm",
            isNew = true,
        )

        setupRule(notification)
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that notification description is NOT displayed when description is empty`() {
        val notification = NotificationItemDefinition(
            type = NotificationItemType.Others,
            typeTitle = "CONTACTS",
            title = "New Contact",
            description = "",
            subText = null,
            date = "11 October 2022 6:46 pm",
            isNew = true,
        )

        setupRule(notification)
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG).assertDoesNotExist()
    }
}