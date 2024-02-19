package mega.privacy.android.app.presentation.shares.incoming.ui

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.domain.entity.preference.ViewType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class IncomingSharesViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that empty state is shown when there are no incoming shares`() {
        val uiState = IncomingSharesState(
            nodesList = listOf(),
            isLoading = false,
            currentViewType = ViewType.LIST
        )

        composeTestRule.setContent {
            IncomingSharesView(
                uiState = uiState,
                emptyState = Pair(
                    R.drawable.outgoing_shares_empty,
                    R.string.context_empty_outgoing
                ),
                onToggleAppBarElevation = {},
                onItemClick = {},
                onLongClick = {},
                onMenuClick = {},
                sortOrder = "",
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onLinkClicked = {},
            )
        }

        composeTestRule
            .onNodeWithTag(NODES_EMPTY_VIEW_VISIBLE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that contact credential verification banner is shown`() {
        val uiState = IncomingSharesState(
            nodesList = listOf(),
            isLoading = false,
            currentViewType = ViewType.LIST,
            showContactNotVerifiedBanner = true,
            isContactVerificationOn = true,
            currentNodeName = "name"
        )

        composeTestRule.setContent {
            IncomingSharesView(
                uiState = uiState,
                emptyState = Pair(
                    R.drawable.outgoing_shares_empty,
                    R.string.context_empty_outgoing
                ),
                onToggleAppBarElevation = {},
                onItemClick = {},
                onLongClick = {},
                onMenuClick = {},
                sortOrder = "",
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                onLinkClicked = {},
            )
        }

        composeTestRule
            .onNodeWithTag(VERIFICATION_BANNER_TAG)
            .assertIsDisplayed()
    }
}
