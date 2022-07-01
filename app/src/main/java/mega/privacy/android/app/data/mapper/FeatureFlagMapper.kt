package mega.privacy.android.app.data.mapper

import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Type alias to make map output readable
 */
typealias FeatureFlagMapper = (
    @JvmSuppressWildcards Preferences.Key<*>,
    @JvmSuppressWildcards Boolean,
) ->
@JvmSuppressWildcards FeatureFlag

/**
 * Maps preferences data to @FeatureFlag
 */
internal fun toFeatureFlag(
    key: Preferences.Key<*>,
    value: Boolean,
) = FeatureFlag(key.toString(), value)