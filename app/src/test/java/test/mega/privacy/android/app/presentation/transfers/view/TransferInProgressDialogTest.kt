package test.mega.privacy.android.app.presentation.transfers.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.view.dialog.PROGRESS_TAG
import mega.privacy.android.app.presentation.transfers.view.dialog.TransferInProgressDialog
import mega.privacy.android.domain.entity.transfer.TransferStage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.fromPluralId
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class TransferInProgressDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that progress dialog is showed when first composed`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_NONE
                )
            ) {}
        }
        composeTestRule.onNodeWithTag(PROGRESS_TAG).assertExists()
    }

    @Test
    fun `test that on confirm canceled is triggered when cancel is clicked`() {
        val lambdaMock = mock<() -> Unit>()
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_NONE),
                onCancelConfirmed = lambdaMock
            )
        }
        composeTestRule.onNodeWithText(R.string.cancel_transfers).performClick()
        verify(lambdaMock).invoke()
    }

    @Test
    fun `test that the correct title is shown when stage is none`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_NONE)
            ) {}
        }
        composeTestRule.onNodeWithText(R.string.scanning_transfers).assertExists()
    }

    @Test
    fun `test that the correct title is shown when stage is scanning`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_SCANNING)
            ) {}
        }
        composeTestRule.onNodeWithText(R.string.scanning_transfers).assertExists()
    }

    @Test
    fun `test that the correct title is shown when stage is creating tree`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_CREATING_TREE)
            ) {}
        }
        composeTestRule.onNodeWithText(sharedR.string.transfers_scanning_folders_dialog_creating_folders_title)
            .assertExists()
    }

    @Test
    fun `test that the correct title is shown when stage is creating tree and all folders are created`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_CREATING_TREE,
                    folderCount = 10,
                    createdFolderCount = 10
                )
            ) {}
        }
        composeTestRule.onNodeWithText(sharedR.string.transfers_scanning_folders_dialog_starting_transfers_title)
            .assertExists()
    }

    @Test
    fun `test that the correct title is shown when stage is transferring files`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_TRANSFERRING_FILES)
            ) {}
        }
        composeTestRule.onNodeWithText(sharedR.string.transfers_scanning_folders_dialog_starting_transfers_title)
            .assertExists()
    }

    @Test
    fun `test that the correct subtitle is shown when stage is none`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_NONE,
                    folderCount = 1,
                    fileCount = 1
                )
            ) {}
        }
        composeTestRule.onNodeWithText(sharedR.string.transfers_scanning_folders_dialog_cancel_info_subtitle)
            .assertExists()
    }

    @Test
    fun `test that the correct subtitle is shown when stage is scanning`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_SCANNING,
                    folderCount = 1,
                    fileCount = 1,
                )
            ) {}
        }
        composeTestRule.onNodeWithText(
            substring = true,
            text = fromPluralId(
                sharedR.plurals.transfers_scanning_folders_dialog_scanning_files_and_folders_subtitle,
                quantity = 1,
                fromPluralId(
                    sharedR.plurals.transfers_scanning_folders_dialog_scanning_folders_subtitle,
                    1
                ),
                1
            ),
        ).assertExists()
    }

    @Test
    fun `test that the correct pluralization is applied to subtitle when stage is scanning`() {
        var filesState by mutableStateOf(1)
        var foldersState by mutableStateOf(1)
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_SCANNING,
                    folderCount = foldersState,
                    fileCount = filesState,
                )
            ) {}
        }
        (1..2).forEach { files ->
            (1..2).forEach { folders ->
                filesState = files
                foldersState = folders
                composeTestRule.onNodeWithText(
                    substring = true,
                    text = fromPluralId(
                        sharedR.plurals.transfers_scanning_folders_dialog_scanning_files_and_folders_subtitle,
                        quantity = files,
                        fromPluralId(
                            sharedR.plurals.transfers_scanning_folders_dialog_scanning_folders_subtitle,
                            folders
                        ),
                        files,
                    ),
                ).assertExists()
            }
        }
    }

    @Test
    fun `test that the correct subtitle is shown when stage is creating tree`() {
        composeTestRule.setContent {
            TransferInProgressDialog(
                StartTransferJobInProgress.ScanningTransfers(
                    TransferStage.STAGE_CREATING_TREE,
                    folderCount = 5,
                    createdFolderCount = 2,
                )
            ) {}
        }
        composeTestRule.onNodeWithText(
            substring = true,
            text = fromPluralId(
                sharedR.plurals.transfers_scanning_folders_dialog_creating_folders_subtitle,
                1,
                2,
                5,
            ),
        ).assertExists()
    }
}