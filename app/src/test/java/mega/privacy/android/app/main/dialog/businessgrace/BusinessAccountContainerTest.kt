package mega.privacy.android.app.main.dialog.businessgrace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BusinessAccountContainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val businessAccountViewModel = mock<BusinessAccountViewModel>()

    private val viewModelStore = ViewModelStore()

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore =
            this@BusinessAccountContainerTest.viewModelStore
    }

    @Before
    fun setup() {
        whenever(businessAccountViewModel.unverifiedBusinessAccountState) doReturn flowOf(true)
    }

    @Test
    fun `test that the unverified business dialog is displayed`() {
        composeRuleScope {
            setContainer()

            onNodeWithTag(BUSINESS_ACCOUNT_CONTAINER_UNVERIFIED_BUSINESS_ACCOUNT_DIALOG_TAG).assertIsDisplayed()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setContainer(
        content: @Composable () -> Unit = {},
    ) {
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                BusinessAccountContainer(
                    viewModel = businessAccountViewModel,
                    content = content
                )
            }
        }
    }
}
