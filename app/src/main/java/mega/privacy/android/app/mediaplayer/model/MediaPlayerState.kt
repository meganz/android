package mega.privacy.android.app.mediaplayer.model

import mega.privacy.android.domain.entity.AccountType

/**
 * The state for general media player
 *
 * @property accountType the account type
 * @property isBusinessAccountExpired if the business account is expired
 * @property isHiddenNodesOnboarded if the user has been onboarded with hidden nodes
 * @property hiddenNodeEnabled whether the hidden node is enabled
 * @property showHiddenItems whether to show hidden items
 */
data class MediaPlayerState(
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val showHiddenItems: Boolean? = null,
)