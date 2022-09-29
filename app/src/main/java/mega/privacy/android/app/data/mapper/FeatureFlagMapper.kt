package mega.privacy.android.app.data.mapper

import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.domain.entity.Feature

/**
 * Type alias to make map output readable
 */
typealias FeatureFlagMapper = (
    @JvmSuppressWildcards Preferences.Key<*>,
    @JvmSuppressWildcards Boolean,
) ->
@JvmSuppressWildcards Pair<@JvmSuppressWildcards Feature, @JvmSuppressWildcards Boolean>

/**
 * Maps preferences data to @FeatureFlag
 */
internal fun toFeatureFlag(
    key: Preferences.Key<*>,
    value: Boolean,
): Pair<Feature, Boolean> = object : Feature {
    override val name: String = key.toString()
    override val description: String = ""
} to value
