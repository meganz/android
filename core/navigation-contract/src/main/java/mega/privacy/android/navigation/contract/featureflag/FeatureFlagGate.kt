package mega.privacy.android.navigation.contract.featureflag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.domain.entity.Feature

/**
 * Functional interface for resolving feature flag values.
 * Exposed for testing and preview support via [LocalFeatureFlagResolver].
 */
fun interface FeatureFlagResolver {
    suspend fun invoke(feature: Feature): Boolean
}

/**
 * CompositionLocal for overriding feature flag resolution in tests and previews.
 */
internal val LocalFeatureFlagResolver = compositionLocalOf<FeatureFlagResolver?> { null }

/**
 * A composable gate that shows [enabled] or [disabled] content based on a feature flag value.
 *
 * @param feature The feature flag to check.
 * @param loading Content to show while the flag value is loading. Defaults to nothing.
 * @param disabled Content to show when the flag is disabled. Defaults to nothing.
 * @param enabled Content to show when the flag is enabled (trailing lambda).
 */
@Composable
fun FeatureFlagGate(
    feature: Feature,
    loading: @Composable () -> Unit = {},
    disabled: @Composable () -> Unit = {},
    enabled: @Composable () -> Unit,
) {
    val localResolver = LocalFeatureFlagResolver.current
    val context = LocalContext.current

    val flagValue = produceState<Boolean?>(initialValue = null, key1 = feature) {
        val resolver = localResolver ?: FeatureFlagResolver { f ->
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                GetFeatureFlagValueEntryPoint::class.java
            ).getFeatureFlagValueUseCase(f)
        }
        value = runCatching { resolver.invoke(feature) }.getOrDefault(false)
    }

    when (flagValue.value) {
        null -> loading()
        true -> enabled()
        false -> disabled()
    }
}
