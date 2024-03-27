package mega.privacy.android.app.main.model

import mega.privacy.android.domain.entity.Feature

/**
 * State for Add Contact
 * @property isContactVerificationWarningEnabled contact verification flag is enabled or not
 * @property enabledFeatureFlags Set of enabled feature flags
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property shouldShowParticipantsLimitWarning True, if should show participants limit warning. False, otherwise.
 */
data class AddContactState(
    val isContactVerificationWarningEnabled: Boolean = false,
    val enabledFeatureFlags: Set<Feature> = emptySet(),
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val shouldShowParticipantsLimitWarning: Boolean = false,
)
