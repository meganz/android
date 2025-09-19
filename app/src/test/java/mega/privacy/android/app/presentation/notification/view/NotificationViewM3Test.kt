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
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_M3_TEST_TAG
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.feature.notifications.snowflakes.NOTIFICATION_ITEM_VIEW_M3_TEST_TAG
import mega.privacy.android.feature.notifications.snowflakes.NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationViewM3Test {

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
        isNew = true,
    ) {}

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
            NotificationViewM3(
                state = uiState,
                onNotificationClick = {},
                onPromoNotificationClick = {},
                onNotificationsLoaded = {}
            )
        }
    }

    @Test
    fun `test that no notifications displays empty view`() {
        setupRule(NotificationState(notifications = emptyList(), promoNotifications = emptyList()))
        composeRule.onNodeWithTag(NOTIFICATION_EMPTY_VIEW_M3_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that one or more notifications displays notification list view`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_LIST_VIEW_M3_TEST_TAG).assertIsDisplayed()
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
        composeRule.onAllNodesWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(PROMO_NOTIFICATION_M3_TEST_TAG).assertCountEquals(1)
    }

    @Test
    fun `test that New is displayed when isNew is true`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that New is not displayed when isNew is false`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification.copy(isNew = false)),
                promoNotifications = listOf(promoNotification)
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG).assertDoesNotExist()
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
        composeRule.onNodeWithTag(NOTIFICATION_LOADING_VIEW_M3_TEST_TAG).assertIsDisplayed()
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
        composeRule.onNodeWithTag(NOTIFICATION_LOADING_VIEW_M3_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that top app bar is displayed`() {
        setupRule(
            NotificationState(
                notifications = listOf(normalNotification),
                promoNotifications = emptyList()
            )
        )
        composeRule.onNodeWithTag(NOTIFICATION_TOP_BAR_M3_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that multiple notifications are displayed correctly`() {
        val secondNotification = normalNotification.copy(
            sectionTitle = { "INCOMING SHARES" },
            sectionType = NotificationItemType.IncomingShares,
            title = { "File Shared" },
            description = { "A file has been shared with you" },
            isNew = false
        )

        setupRule(
            NotificationState(
                notifications = listOf(normalNotification, secondNotification),
                promoNotifications = emptyList()
            )
        )

        composeRule.onAllNodesWithTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG).assertCountEquals(2)
        composeRule.onNodeWithTag(NOTIFICATION_LIST_VIEW_M3_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view shows correct content`() {
        setupRule(NotificationState(notifications = emptyList(), promoNotifications = emptyList()))

        composeRule.onNodeWithTag(NOTIFICATION_EMPTY_VIEW_M3_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_EMPTY_VIEW_IMAGE_M3_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_EMPTY_VIEW_TEXT_M3_TEST_TAG).assertIsDisplayed()
    }
}
