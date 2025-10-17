package mega.privacy.android.app.menu.presentation

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.BADGE
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.LOGOUT_BUTTON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.MY_ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_BADGE
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_ICON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_HEADER
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.TOOLBAR
import mega.privacy.android.navigation.contract.DefaultNumberBadge
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.NavDrawerItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MenuHomeScreeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Parcelize
    object TestDestination : Parcelable, NavKey

    val badgeFlow = MutableStateFlow<MainNavItemBadge?>(null)

    val myAccountItems = mapOf(
        1 to NavDrawerItem.Account(
            destination = TestDestination,
            icon = Icons.Default.Home,
            title = android.R.string.ok,
            subTitle = MutableStateFlow("Storage"),
            actionLabel = android.R.string.copy
        ),
        2 to NavDrawerItem.Account(
            destination = TestDestination,
            icon = Icons.Default.Settings,
            title = android.R.string.cancel,
            subTitle = MutableStateFlow("Free"),
            actionLabel = null,
            badge = badgeFlow
        )
    )

    val privacySuiteItems = mapOf(
        3 to NavDrawerItem.PrivacySuite(
            destination = TestDestination,
            icon = Icons.Default.Settings,
            title = android.R.string.ok,
            subTitle = android.R.string.cancel,
            link = "https://mega.app",
            appPackage = null
        )
    )

    private fun setupRule(
        uiState: MenuUiState = createDefaultMenuUiState(),
        navigateToFeature: (NavKey) -> Unit = {},
        onLogoutClicked: () -> Unit = {},
        onResetTestPasswordScreenEvent: () -> Unit = {},
        onResetLogoutConfirmationEvent: () -> Unit = {},
    ) {
        composeRule.setContent {
            MenuHomeScreenUi(
                uiState = uiState,
                navigateToFeature = navigateToFeature,
                onLogoutClicked = onLogoutClicked,
                onResetTestPasswordScreenEvent = onResetTestPasswordScreenEvent,
                onResetLogoutConfirmationEvent = onResetLogoutConfirmationEvent,
            )
        }
    }

    private fun createDefaultMenuUiState(
        notificationCount: Int = 0,
    ) = MenuUiState(
        myAccountItems = myAccountItems,
        privacySuiteItems = privacySuiteItems,
        email = "test@example.com",
        name = "Test User",
        avatar = null,
        avatarColor = Color.Blue,
        isConnectedToNetwork = true,
        showTestPasswordScreenEvent = consumed,
        showLogoutConfirmationEvent = consumed,
        unreadNotificationsCount = notificationCount,
    )

    @Test
    fun `test that toolbar is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun `test that notification icon is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(NOTIFICATION_ICON).assertIsDisplayed()
    }

    @Test
    fun `test that my account items are displayed`() {
        setupRule()
        composeRule.onNodeWithTag(MY_ACCOUNT_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that user name is displayed in profile section`() {
        setupRule()
        composeRule.onNodeWithText("Test User").assertIsDisplayed()
    }

    @Test
    fun `test that user email is displayed in profile section`() {
        setupRule()
        composeRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }

    @Test
    fun `test that account items are displayed`() {
        setupRule()
        composeRule.onAllNodesWithTag(ACCOUNT_ITEM).assertCountEquals(myAccountItems.size)
    }

    @Test
    fun `test that privacy suite header is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(PRIVACY_SUITE_HEADER).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `test that privacy suite items are displayed when expanded`() {
        setupRule()
        composeRule.onNodeWithTag(PRIVACY_SUITE_HEADER).performScrollTo()
        composeRule.onAllNodesWithTag(PRIVACY_SUITE_ITEM).assertCountEquals(privacySuiteItems.size)
    }

    @Test
    fun `test that UI displays correctly with empty account items`() {
        setupRule(
            uiState = createDefaultMenuUiState().copy(
                myAccountItems = emptyMap()
            )
        )
        composeRule.onNodeWithTag(TOOLBAR).assertIsDisplayed()
        composeRule.onNodeWithTag(MY_ACCOUNT_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(LOGOUT_BUTTON).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `test that UI displays correctly with empty privacy suite items`() {
        setupRule(
            uiState = createDefaultMenuUiState().copy(
                privacySuiteItems = emptyMap()
            )
        )
        composeRule.onNodeWithTag(TOOLBAR).assertIsDisplayed()
        composeRule.onNodeWithTag(MY_ACCOUNT_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(PRIVACY_SUITE_HEADER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(LOGOUT_BUTTON).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `test that logout button click triggers logout callback`() {
        val onLogoutClicked = mock<() -> Unit>()
        setupRule(
            uiState = createDefaultMenuUiState().copy(
                privacySuiteItems = emptyMap()
            ), onLogoutClicked = onLogoutClicked

        )
        composeRule.onNodeWithTag(LOGOUT_BUTTON).performScrollTo().performClick()
        verify(onLogoutClicked).invoke()
    }

    @Test
    fun `test that test password screen event triggers navigation and reset callback`() {
        val navigateToFeature = mock<(NavKey) -> Unit>()
        val onResetTestPasswordScreenEvent = mock<() -> Unit>()

        setupRule(
            uiState = createDefaultMenuUiState().copy(
                showTestPasswordScreenEvent = triggered
            ),
            navigateToFeature = navigateToFeature,
            onResetTestPasswordScreenEvent = onResetTestPasswordScreenEvent
        )

        // Wait for the event to be processed
        composeRule.waitForIdle()

        verify(navigateToFeature).invoke(any())
        verify(onResetTestPasswordScreenEvent).invoke()
    }

    @Test
    fun `test that consumed test password screen event does not trigger navigation or reset callback`() {
        val navigateToFeature = mock<(NavKey) -> Unit>()
        val onResetTestPasswordScreenEvent = mock<() -> Unit>()

        setupRule(
            uiState = createDefaultMenuUiState().copy(
                showTestPasswordScreenEvent = consumed
            ),
            navigateToFeature = navigateToFeature,
            onResetTestPasswordScreenEvent = onResetTestPasswordScreenEvent
        )

        composeRule.waitForIdle()

        verify(navigateToFeature, never()).invoke(any())
        verify(onResetTestPasswordScreenEvent, never()).invoke()
    }

    @Test
    fun `test that logout confirmation event triggers navigation and reset callback`() {
        val navigateToFeature = mock<(NavKey) -> Unit>()
        val onResetLogoutConfirmationEvent = mock<() -> Unit>()

        setupRule(
            uiState = createDefaultMenuUiState().copy(
                showLogoutConfirmationEvent = triggered
            ),
            navigateToFeature = navigateToFeature,
            onResetLogoutConfirmationEvent = onResetLogoutConfirmationEvent
        )

        // Wait for the event to be processed
        composeRule.waitForIdle()

        verify(navigateToFeature).invoke(any())
        verify(onResetLogoutConfirmationEvent).invoke()
    }

    @Test
    fun `test that consumed logout confirmation event does not trigger navigation or reset callback`() {
        val navigateToFeature = mock<(NavKey) -> Unit>()
        val onResetLogoutConfirmationEvent = mock<() -> Unit>()

        setupRule(
            uiState = createDefaultMenuUiState().copy(
                showLogoutConfirmationEvent = consumed
            ),
            navigateToFeature = navigateToFeature,
            onResetLogoutConfirmationEvent = onResetLogoutConfirmationEvent
        )

        composeRule.waitForIdle()

        verify(navigateToFeature, never()).invoke(any())
        verify(onResetLogoutConfirmationEvent, never()).invoke()
    }

    @Test
    fun `test that item badge is updated when flow is updated`() {
        setupRule()
        composeRule.onNodeWithTag(BADGE, useUnmergedTree = true).assertDoesNotExist()
        badgeFlow.value = DefaultNumberBadge(1)
        composeRule.onNodeWithTag(BADGE, useUnmergedTree = true).assertTextEquals("1")
    }

    @Test
    fun `test that notification badge is shown when notification count is greater than 0`() {
        setupRule(createDefaultMenuUiState(1))
        composeRule.onNodeWithTag(NOTIFICATION_BADGE, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NOTIFICATION_BADGE, useUnmergedTree = true).assertTextEquals("1")
    }

    @Test
    fun `test that notification badge is not shown when notification count is 0`() {
        setupRule(createDefaultMenuUiState(0))
        composeRule.onNodeWithTag(NOTIFICATION_BADGE, useUnmergedTree = true).assertIsNotDisplayed()
    }
}