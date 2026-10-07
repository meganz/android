package mega.privacy.android.navigation.contract.featureflag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

    // The last value is saved so a recreated Activity composes the resolved branch in its first
    // frame: content that only appears a frame later has lost its retained state, because the
    // retain store discards exited values once the first frame after restoration completes.
    // The flag is still resolved on every entry, as before, so a changed value is picked up.
    var flagValue by rememberSaveable(feature) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(feature) {
        val resolver = localResolver ?: FeatureFlagResolver { f ->
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                GetFeatureFlagValueEntryPoint::class.java
            ).getFeatureFlagValueUseCase(f)
        }
        flagValue = runCatching { resolver.invoke(feature) }.getOrDefault(false)
    }

    when (flagValue) {
        null -> loading()
        true -> enabled()
        false -> disabled()
    }
}
