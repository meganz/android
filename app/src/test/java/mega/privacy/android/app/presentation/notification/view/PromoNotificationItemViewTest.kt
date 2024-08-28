package mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_PREVIEW_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DIVIDER
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_ICON_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PromoNotificationItemView
import mega.privacy.android.domain.entity.notifications.PromoNotification
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
        notification: PromoNotification,
    ) {
        composeRule.setContent {
            PromoNotificationItemView(
                modifier = Modifier,
                notification = notification,
                onClick = {})
        }
    }

    @Test
    fun `test that promo notification has all the required views`() {
        val notification = PromoNotification(
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

        setupRule(notification)

        composeRule.onNodeWithTag(PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_TITLE_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DIVIDER, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo preview is displayed when imageURL is not empty`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            iconURL = "",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)

        composeRule.onNodeWithTag(PROMO_PREVIEW_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()

    }

    @Test
    fun `test that promo preview is NOT displayed when imageURL is empty`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            iconURL = "https://www.mega.co.nz",
            imageURL = "",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)

        composeRule.onNodeWithTag(PROMO_PREVIEW_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that promo notification icon is NOT displayed when imageURL is not empty`() {
        val notification = PromoNotification(
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

        setupRule(notification)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_PREVIEW_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification icon is displayed when imageURL is empty`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            iconURL = "https://www.mega.co.nz",
            imageURL = "",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification icon is not displayed when iconURL and imageURL are empty`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            iconURL = "",
            imageURL = "",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_PREVIEW_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that promo notification description is displayed when description is not empty`() {
        val notification = PromoNotification(
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

        setupRule(notification)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification description is NOT displayed when description is empty`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "",
            iconURL = "https://www.mega.co.nz",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG).assertDoesNotExist()
    }
}