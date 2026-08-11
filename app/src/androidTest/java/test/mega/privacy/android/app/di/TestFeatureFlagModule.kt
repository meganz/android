package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

/**
 * A highest-priority ([FeatureFlagValuePriority.RuntimeOverride]) feature-flag provider that
 * instrumented tests seed to force a flag on or off. Only explicitly overridden flags are affected;
 * anything left unset returns null so resolution falls through to the real providers.
 *
 * Injected directly by tests to seed values, and contributed additively via `@IntoSet` so it
 * augments the production providers rather than replacing them.
 */
class FakeFeatureFlagValueProvider : FeatureFlagValueProvider {

    private val overrides = ConcurrentHashMap<String, Boolean>()

    fun set(feature: Feature, enabled: Boolean) {
        overrides[feature.name] = enabled
    }

    fun clear() {
        overrides.clear()
    }

    override suspend fun isEnabled(feature: Feature): Boolean? = overrides[feature.name]

    override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.RuntimeOverride
}

@Module
@InstallIn(SingletonComponent::class)
object TestFeatureFlagModule {

    @Provides
    @Singleton
    fun provideFakeFeatureFlagValueProvider(): FakeFeatureFlagValueProvider =
        FakeFeatureFlagValueProvider()

    @Provides
    @IntoSet
    fun bindFakeFeatureFlagValueProvider(
        provider: FakeFeatureFlagValueProvider,
    ): @JvmSuppressWildcards FeatureFlagValueProvider = provider
}
