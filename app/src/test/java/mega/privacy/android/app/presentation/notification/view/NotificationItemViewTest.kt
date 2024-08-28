package mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_DATE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_DESCRIPTION_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_DIVIDER
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_GREEN_ICON_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_SECTION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_TITLE_ROW_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NotificationItemView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class NotificationItemViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        notification: Notification,
    ) {
        composeRule.setContent {
            NotificationItemView(
                modifier = Modifier,
                notification = notification,
                onClick = {})
        }
    }

    @Test
    fun `test that notification has all the required views`() {
        val notification = Notification(
            sectionTitle = { "CONTACTS" },
            sectionColour = R.color.orange_400_orange_300,
            sectionIcon = null,
            title = { "New Contact" },
            titleTextSize = 16.sp,
            description = { "xyz@gmail.com is now a contact" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

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
        val notification = Notification(
            sectionTitle = { "CONTACTS" },
            sectionColour = R.color.orange_400_orange_300,
            sectionIcon = null,
            title = { "New Contact" },
            titleTextSize = 16.sp,
            description = { "xyz@gmail.com is now a contact" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

        setupRule(notification)
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that notification description is NOT displayed when description is empty`() {
        val notification = Notification(
            sectionTitle = { "CONTACTS" },
            sectionColour = R.color.orange_400_orange_300,
            sectionIcon = null,
            title = { "New Contact" },
            titleTextSize = 16.sp,
            description = { "" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

        setupRule(notification)
        composeRule.onNodeWithTag(NOTIFICATION_DESCRIPTION_TEST_TAG).assertDoesNotExist()
    }
}