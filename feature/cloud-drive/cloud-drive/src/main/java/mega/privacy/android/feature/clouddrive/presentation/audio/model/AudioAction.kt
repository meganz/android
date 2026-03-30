package mega.privacy.android.feature.clouddrive.presentation.audio.model

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Audio action
 * This interface defines the UI actions that can be performed in the Audio screen.
 */
sealed interface AudioAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val node: TypedNode) : AudioAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : AudioAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : AudioAction
}
