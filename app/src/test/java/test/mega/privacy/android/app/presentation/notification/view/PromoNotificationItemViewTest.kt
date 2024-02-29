package test.mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_BANNER_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_EXPIRY_DATE_WITH_BANNER_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_EXPIRY_DATE_WITH_NO_BANNER_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_ICON_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PromoNotificationItemView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class PromoNotificationItemViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        notification: Notification,
        hasBanner: Boolean,
    ) {
        composeRule.setContent {
            PromoNotificationItemView(
                notification = notification,
                position = 0,
                hasBanner = hasBanner,
                notifications = listOf(notification),
                onClick = {})
        }
    }

    @Test
    fun `test that promo notification has all the required views`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}

        setupRule(notification, false)

        composeRule.onNodeWithTag(PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo banner is displayed when hasBanner is true`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}

        setupRule(notification, true)

        composeRule.onNodeWithTag(PROMO_BANNER_TEST_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()

    }

    @Test
    fun `test that promo banner is NOT displayed when hasBanner is false`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}

        setupRule(notification, false)

        composeRule.onNodeWithTag(PROMO_BANNER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that promo notification icon is not displayed when hasBanner is true`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}
        setupRule(notification, true)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_EXPIRY_DATE_WITH_NO_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_EXPIRY_DATE_WITH_BANNER_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification icon is displayed when hasBanner is false`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}
        setupRule(notification, false)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_EXPIRY_DATE_WITH_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_EXPIRY_DATE_WITH_NO_BANNER_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification description is displayed when description is not null nor empty`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}
        setupRule(notification, false)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification description is NOT displayed when description is null nor empty`() {
        val notification = Notification(
            sectionTitle = { "Limited time offer" },
            sectionColour = R.color.jade_600_jade_300,
            sectionIcon = null,
            title = { "Get Pro 1 for 50% off" },
            titleTextSize = 16.sp,
            description = { "Tap here to get 1 year of Pro 1 for 50% off" },
            schedMeetingNotification = null,
            dateText = { "11 October 2022 6:46 pm" },
            isNew = false,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 }
        ) {}
        setupRule(notification, false)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG).assertDoesNotExist()
    }
}