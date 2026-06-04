package mega.privacy.android.feature.contact.group.model

import de.palm.composestateevents.StateEventWithContent

/**
 * Contact group ui state
 */
sealed interface ContactGroupUiState {

    /**
     * Loading
     */
    data object Loading : ContactGroupUiState

    /**
     * Data
     *
     * @property groups
     * @property groupChatCreated
     */
    data class Data(
        val groups: List<ContactGroupItem>,
        val groupChatCreated: StateEventWithContent<Long>,
    ) : ContactGroupUiState

    companion object {
        /**
         * Invalid Group Chat ID
         */
        const val INVALID_GROUP_CHAT_ID = -1L
    }
}
