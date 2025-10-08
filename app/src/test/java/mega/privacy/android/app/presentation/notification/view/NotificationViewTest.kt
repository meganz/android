package mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationItemType
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_TEST_TAG
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.notifications.NOTIFICATION_UNREAD_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val normalNotification = Notification(
        sectionTitle = { "CONTACTS" },
        sectionType = NotificationItemType.Others,
        title = { "New Contact" },
        titleTextSize = 16.sp,
        description = { "xyz@gmail.com is now a contact" },
        schedMeetingNotification = null,
        dateText = { "11 October 2022 6:46 pm" },
        isUnread = true,
        onClick = {},
        destination = null
    )

    private val promoNotification = PromoNotification(
        promoID = 1,
        title = "Title",
        description = "Description",
        iconURL = "https://www.mega.co.nz",
        imageURL = "https://www.mega.co.nz",
        startTimeStamp = 1,
        endTimeStamp = 1,
        actionName = "Action name",
        actionURL = "https://www.mega.co.nz"
    )

    private fun setupRule(
        uiState: NotificationState,
    ) {
        composeRule.setContent {
            NotificationView(
                state = uiState,
                onNotificationClick = {},
                onPromoNotificationClick = {},
                onNotificationsLoaded = {})
        }
    }

    @Test
    fun `test that no notifications displays empty view`() {
        setupRule(NotificationState(notifications = emptyList(), promoNotifications = emptyList()))
        composeRule.onNodeWithTag(NOTIFICATION_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that one or more notifications displays notification list view`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_LIST_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that there is one row created for each notification`() {
        setupRule(
            NotificationState(
                notifications = listOf(
                    normalNotification,
                ),
                promoNotifications = listOf(
                    promoNotification,
                )
            )
        )
        composeRule.onAllNodesWithTag(NOTIFICATION_TEST_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(PROMO_NOTIFICATION_TEST_TAG).assertCountEquals(1)
    }

    @Test
    fun `test that Unread tag is displayed when isUnread is true`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_UNREAD_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that Unread tag is not displayed when isUnread is false`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification.copy(isUnread = false)),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_UNREAD_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that loading view is displayed when isLoading is true`() {
        setupRule(
            NotificationState(
                notifications = emptyList(),
                promoNotifications = emptyList(),
                isLoading = true
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_LOADING_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that loading view is not displayed when isLoading is false`() {
        setupRule(
            NotificationState(
                notifications = emptyList(),
                promoNotifications = emptyList(),
                isLoading = false
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_LOADING_VIEW_TEST_TAG).assertDoesNotExist()
    }
}