package mega.privacy.android.app.main.model

import mega.privacy.android.domain.entity.Feature

/**
 * State for invite Contact
 * @property enabledFeatureFlags Set of enabled feature flags
 */
data class InviteContactState(
    val enabledFeatureFlags: Set<Feature> = emptySet(),
)