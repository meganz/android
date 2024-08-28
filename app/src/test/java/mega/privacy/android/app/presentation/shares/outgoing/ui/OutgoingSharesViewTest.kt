package mega.privacy.android.app.presentation.shares.outgoing.ui

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import mega.privacy.android.app.onNodeWithText

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class OutgoingSharesViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    @Test
    fun outgoingSharesViewRendersEmptyView() {
        val uiState = OutgoingSharesState(
            nodesList = listOf(),
            isLoading = false,
            currentViewType = ViewType.LIST
        )

        composeTestRule.setContent {
            OutgoingSharesView(
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
                onVerifyContactDialogDismissed = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }

        composeTestRule
            .onNodeWithTag(NODES_EMPTY_VIEW_VISIBLE)
            .assertIsDisplayed()
    }

    @Test
    fun outgoingSharesViewRendersAlertDialog() {
        val uiState = OutgoingSharesState(
            verifyContactDialog = "user@mail.com",
            isLoading = false,
            currentViewType = ViewType.LIST
        )

        composeTestRule.setContent {
            OutgoingSharesView(
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
                onVerifyContactDialogDismissed = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composeTestRule.onNodeWithText(R.string.shared_items_contact_not_in_contact_list_dialog_title)
            .assertExists()
    }
}
