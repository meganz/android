package test.mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.view.NotificationView
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.NOTIFICATION_GREEN_ICON_TEST_TAG
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that no notifications displays empty view`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = emptyList()),
                onClick = {},
                onNotificationsLoaded = {})
        }
        composeRule.onNodeWithTag("NotificationEmptyView").assertIsDisplayed()
    }

    @Test
    fun `test that one or more notifications displays notification list view`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag("NotificationListView").assertIsDisplayed()
    }

    @Test
    fun `test that there is one row created for each notification`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(
                Notification(
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
                ) {},
                Notification(
                    sectionTitle = { "INCOMING SHARES" },
                    sectionColour = R.color.orange_400_orange_300,
                    sectionIcon = null,
                    title = { "New Incoming Share" },
                    titleTextSize = 16.sp,
                    description = { "Access to the folders shared by xyz@gmail.com were removed" },
                    schedMeetingNotification = null,
                    dateText = { "13 May 2022 5:46 am" },
                    isNew = true,
                    backgroundColor = { "#D3D3D3" },
                    separatorMargin = { 0 },
                ) {}
            )))
        }
        composeRule.onAllNodesWithTag("NotificationItemView").assertCountEquals(2)
    }

    @Test
    fun `test that section title is displayed`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag("SectionTitle", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that notification title is displayed`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag("Title", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that notification description is displayed if present`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag("Description", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that notification description is not displayed when null`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16.sp,
                description = { null },
                schedMeetingNotification = null,
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                backgroundColor = { "#D3D3D3" },
                separatorMargin = { 0 },
            ) {}
            )))
        }
        composeRule.onNodeWithTag("Description").assertDoesNotExist()
    }

    @Test
    fun `test that notification date is displayed`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag("DateText", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that New is displayed when isNew is true`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
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
            )))
        }
        composeRule.onNodeWithTag(NOTIFICATION_GREEN_ICON_TEST_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that New is not displayed when isNew is false`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16.sp,
                description = { "xyz@gmail.com is now a contact" },
                schedMeetingNotification = null,
                dateText = { "11 October 2022 6:46 pm" },
                isNew = false,
                backgroundColor = { "#D3D3D3" },
                separatorMargin = { 0 },
            ) {}
            )))
        }
        composeRule.onNodeWithTag(NOTIFICATION_GREEN_ICON_TEST_TAG).assertDoesNotExist()
    }
}