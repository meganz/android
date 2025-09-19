package mega.privacy.android.app.presentation.notification.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DESCRIPTION_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_DIVIDER_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_GREEN_ICON_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_ICON_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_SECTION_TITLE_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PROMO_NOTIFICATION_TITLE_M3_TEST_TAG
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PromoNotificationItemViewM3
import mega.privacy.android.domain.entity.notifications.PromoNotification
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PromoNotificationItemViewM3Test {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        notification: PromoNotification,
    ) {
        composeRule.setContent {
            PromoNotificationItemViewM3(
                modifier = Modifier,
                notification = notification,
                onClick = {}
            )
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

        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_GREEN_ICON_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_TITLE_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DIVIDER_M3_TEST_TAG, useUnmergedTree = true)
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

        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_M3_TEST_TAG).assertDoesNotExist()
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

        composeRule.onNodeWithTag(PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG).assertDoesNotExist()
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
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_M3_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG,
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
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG,
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
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_ICON_M3_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG).assertDoesNotExist()
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
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
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
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DESCRIPTION_M3_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that promo notification uses M3 styling and components`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "M3 Promo Title",
            description = "M3 Promo Description",
            iconURL = "https://www.mega.co.nz",
            imageURL = "",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)

        // Test that M3-specific components are present
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_SECTION_TITLE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_TITLE_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DESCRIPTION_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_GREEN_ICON_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DIVIDER_M3_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification with preview shows correct date placement`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "Preview Promo",
            description = "Description with preview",
            iconURL = "",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)

        // With preview, date should be at the bottom
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that promo notification without preview shows correct date placement`() {
        val notification = PromoNotification(
            promoID = 1,
            title = "No Preview Promo",
            description = "Description without preview",
            iconURL = "https://www.mega.co.nz",
            imageURL = "",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        setupRule(notification)

        // Without preview, date should be in the main content area
        composeRule.onNodeWithTag(
            PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_M3_TEST_TAG,
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_DATE_WITH_PREVIEW_M3_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(PROMO_NOTIFICATION_PREVIEW_IMAGE_M3_TEST_TAG)
            .assertDoesNotExist()
    }
}
