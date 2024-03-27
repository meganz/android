package mega.privacy.android.app.main.model

import mega.privacy.android.domain.entity.Feature

/**
 * State for invite Contact
 *
 * @property enabledFeatureFlags Set of enabled feature flags
 * @property onContactsInitialized True if successfully initialized contacts, false otherwise
 */
data class InviteContactUiState(
    val enabledFeatureFlags: Set<Feature> = emptySet(),
    val onContactsInitialized: Boolean = false,
)
