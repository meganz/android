package mega.privacy.android.navigation.contract.navkey

/**
 * Marker interface for NavKeys whose navigation events can be suppressed
 * (deferred) when the current screen declares overlay suppression via
 * [mega.privacy.android.navigation.contract.suppression.OverlaySuppressionMetadata].
 *
 * NavKeys that do NOT implement this interface will always be delivered
 * immediately, regardless of the current screen's suppression state.
 *
 * @see mega.privacy.android.navigation.contract.suppression.OverlaySuppressionMetadata
 */
interface Suppressable
