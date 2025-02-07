package mega.privacy.android.app.textEditor

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.AccountType

/**
 * Text editor view state
 * @param transferEvent event to trigger transfer actions
 * @property accountType the account type
 * @property isHiddenNodesOnboarded if the user has been onboarded with hidden nodes
 * @property isNodeInBackups if the node is in backups
 * @property isBusinessAccountExpired if the business account is expired
 * @property isMarkDownFile if the file is a markdown file
 * @property convertedHtmlContent the converted HTML content
 */
data class TextEditorViewState(
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val isNodeInBackups: Boolean = false,
    val isBusinessAccountExpired: Boolean = false,
    val isMarkDownFile: Boolean = false,
    val convertedHtmlContent: String? = null,
)