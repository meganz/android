package mega.privacy.android.app.menu.presentation

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.LOGOUT_BUTTON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.MY_ACCOUNT_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.NOTIFICATION_ICON
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_HEADER
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.PRIVACY_SUITE_ITEM
import mega.privacy.android.app.menu.presentation.MenuHomeScreenUiTestTags.TOOLBAR
import mega.privacy.android.navigation.contract.NavDrawerItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuHomeScreeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Parcelize
    object TestDestination : Parcelable

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
            actionLabel = null
        )
    )

    val privacySuiteItems = mapOf(
        3 to NavDrawerItem.PrivacySuite(
            destination = TestDestination,
            icon = Icons.Default.Settings,
            title = android.R.string.ok,
            subTitle = android.R.string.cancel,
            link = "https://mega.nz",
            appPackage = null
        )
    )

    private fun setupRule(
        uiState: MenuUiState = createDefaultMenuUiState(),
        navigateToFeature: (Any) -> Unit = {},
    ) {
        composeRule.setContent {
            MenuHomeScreenUi(
                uiState = uiState,
                navigateToFeature = navigateToFeature
            )
        }
    }

    private fun createDefaultMenuUiState() = MenuUiState(
        myAccountItems = myAccountItems,
        privacySuiteItems = privacySuiteItems,
        email = "test@example.com",
        name = "Test User",
        avatar = null,
        avatarColor = Color.Blue,
        isConnectedToNetwork = true
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
} 