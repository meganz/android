package test.mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.view.NotificationView
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
            NotificationView(state = NotificationState(emptyList()))
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
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
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
                    titleTextSize = 16f,
                    description = { "xyz@gmail.com is now a contact" },
                    dateText = { "11 October 2022 6:46 pm" },
                    isNew = true,
                    onClick = {}),
                Notification(
                    sectionTitle = { "INCOMING SHARES" },
                    sectionColour = R.color.orange_400_orange_300,
                    sectionIcon = null,
                    title = { "New Incoming Share" },
                    titleTextSize = 16f,
                    description = { "Access to the folders shared by xyz@gmail.com were removed" },
                    dateText = { "13 May 2022 5:46 am" },
                    isNew = true,
                    onClick = {}))))
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
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("SectionTitle").assertIsDisplayed()
    }

    @Test
    fun `test that notification title is displayed`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("Title").assertIsDisplayed()
    }

    @Test
    fun `test that notification description is displayed if present`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("Description").assertIsDisplayed()
    }

    @Test
    fun `test that notification description is not displayed when null`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16f,
                description = { null },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
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
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("DateText").assertIsDisplayed()
    }

    @Test
    fun `test that New is displayed when isNew is true`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = true,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("IsNew").assertIsDisplayed()
    }

    @Test
    fun `test that New is not displayed when isNew is false`() {
        composeRule.setContent {
            NotificationView(state = NotificationState(notifications = listOf(Notification(
                sectionTitle = { "CONTACTS" },
                sectionColour = R.color.orange_400_orange_300,
                sectionIcon = null,
                title = { "New Contact" },
                titleTextSize = 16f,
                description = { "xyz@gmail.com is now a contact" },
                dateText = { "11 October 2022 6:46 pm" },
                isNew = false,
                onClick = {}))))
        }
        composeRule.onNodeWithTag("IsNew").assertDoesNotExist()
    }
}