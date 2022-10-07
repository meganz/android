package mega.privacy.android.app.di.featuretoggle

import dagger.MapKey
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import kotlin.reflect.KClass


/**
 * Feature flag priority key
 *
 * @property implementingClass
 * @property priority
 */
@MapKey(unwrapValue = false)
annotation class FeatureFlagPriorityKey(
    val implementingClass: @JvmSuppressWildcards KClass<out FeatureFlagValueProvider>,
    val priority: FeatureFlagValuePriority
)
