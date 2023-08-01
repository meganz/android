package mega.privacy.android.app.presentation.openlink

import mega.privacy.android.domain.entity.Feature

/**
 * Open link state
 *
 * @property isLoggedOut checks if app is logged in or not
 * @property enabledFeatureFlags set of enabled feature flags
 */
data class OpenLinkState(
    val isLoggedOut: Boolean = false,
    val enabledFeatureFlags: Set<Feature>? = null
)