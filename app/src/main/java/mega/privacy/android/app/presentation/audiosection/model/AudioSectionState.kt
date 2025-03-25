package mega.privacy.android.app.presentation.audiosection.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * The state for the audio section
 *
 * @property allAudios the all audio items
 * @property currentViewType the current view type
 * @property sortOrder the sort order of audio items
 * @property isPendingRefresh
 * @property progressBarShowing the progress bar showing state
 * @property scrollToTop the scroll to top state
 * @property selectedAudioHandles the selected audio handles
 * @property isInSelection if list is in selection mode or not
 * @property accountType the account type
 * @property isHiddenNodesOnboarded if is hidden nodes onboarded
 * @property clickedItem the clicked item
 * @property isBusinessAccountExpired if the business account is expired
 * @property hiddenNodeEnabled if hidden node is enabled
 */
data class AudioSectionState(
    val allAudios: List<AudioUiEntity> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false,
    val progressBarShowing: Boolean = true,
    val scrollToTop: Boolean = false,
    val selectedAudioHandles: List<Long> = emptyList(),
    val isInSelection: Boolean = false,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val clickedItem: TypedAudioNode? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
)
