package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFeatureFlagRepositoryTest {
    private lateinit var underTest: FeatureFlagRepository

    private val defaultProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }
    private val secondaryDefaultProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }
    private val configurationFileProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }
    private val buildTimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }
    private val remoteToggledProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }
    private val runtimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> { onBlocking { isEnabled(any()) }.thenReturn(null) }

    private val providerMocks = mapOf(
        FeatureFlagValuePriority.Default to defaultProviderMock,
        FeatureFlagValuePriority.ConfigurationFile to configurationFileProviderMock,
        FeatureFlagValuePriority.BuildTimeOverride to buildTimeOverrideProviderMock,
        FeatureFlagValuePriority.RemoteToggled to remoteToggledProviderMock,
        FeatureFlagValuePriority.RuntimeOverride to runtimeOverrideProviderMock,
    )

    private val fake1 = mock<FeatureFlagValueProvider>()
    private val fake2 = mock<FeatureFlagValueProvider>()

    private val featureFlagValueProviders = providerMocks.mapKeys {
        FeatureFlagPriorityKey(
            implementingClass = FeatureFlagValueProvider::class,
            priority = it.key
        )
    }.plus(
        FeatureFlagPriorityKey(
            implementingClass = FakeFeatureFlagValueProvider::class,
            priority = FeatureFlagValuePriority.Default
        ) to secondaryDefaultProviderMock
    ).plus(
        FeatureFlagPriorityKey(
            implementingClass = Fake1.FakeProvider::class,
            priority = FeatureFlagValuePriority.RemoteToggled
        ) to Fake1(fake1).fakeProvider
    ).plus(
        FeatureFlagPriorityKey(
            implementingClass = Fake2.FakeProvider::class,
            priority = FeatureFlagValuePriority.RemoteToggled
        ) to Fake2(fake2).fakeProvider
    )

    @BeforeEach
    fun setUp() {
        underTest = DefaultFeatureFlagRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            featureFlagValueProvider = featureFlagValueProviders,
        )
    }

    @Test
    fun `test that null is returned if a value is not found`() = runTest {
        val feature = mock<Feature>()
        assertThat(underTest.getFeatureValue(feature)).isNull()
    }

    @Test
    fun `test that a value is returned if found`() = runTest {
        val feature = mock<Feature>()
        whenever(defaultProviderMock.isEnabled(feature)).thenReturn(true)
        assertThat(underTest.getFeatureValue(feature)).isTrue()
    }

    @Test
    fun `test that values for the higher priority overrides the lower`() = runTest {
        val feature = mock<Feature>()
        whenever(defaultProviderMock.isEnabled(feature)).thenReturn(true)
        whenever(configurationFileProviderMock.isEnabled(feature)).thenReturn(false)
        assertThat(underTest.getFeatureValue(feature)).isFalse()
    }

    @Test
    internal fun `test that all providers for a given priority are used`() = runTest {
        val primaryFeature = mock<Feature>()
        val secondaryFeature = mock<Feature>()
        whenever(defaultProviderMock.isEnabled(primaryFeature)).thenReturn(true)
        whenever(defaultProviderMock.isEnabled(secondaryFeature)).thenReturn(null)
        whenever(secondaryDefaultProviderMock.isEnabled(secondaryFeature)).thenReturn(true)
        whenever(secondaryDefaultProviderMock.isEnabled(primaryFeature)).thenReturn(null)

        assertThat(underTest.getFeatureValue(primaryFeature)).isTrue()
        assertThat(underTest.getFeatureValue(secondaryFeature)).isTrue()
    }

    @Test
    internal fun `test that two providers with the same simple name are both used`() = runTest {
        val primaryFeature = mock<Feature>()
        val secondaryFeature = mock<Feature>()
        whenever(fake1.isEnabled(primaryFeature)).thenReturn(true)
        whenever(fake1.isEnabled(secondaryFeature)).thenReturn(null)
        whenever(fake2.isEnabled(secondaryFeature)).thenReturn(true)
        whenever(fake2.isEnabled(primaryFeature)).thenReturn(null)

        assertThat(underTest.getFeatureValue(primaryFeature)).isTrue()
        assertThat(underTest.getFeatureValue(secondaryFeature)).isTrue()
    }
}

private interface FakeFeatureFlagValueProvider : FeatureFlagValueProvider

class Fake1(featureFlagValueProvider: FeatureFlagValueProvider) {
    val fakeProvider = FakeProvider(featureFlagValueProvider)

    class FakeProvider(private val featureFlagValueProvider: FeatureFlagValueProvider) :
        FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            featureFlagValueProvider.isEnabled(feature)
    }
}

class Fake2(featureFlagValueProvider: FeatureFlagValueProvider) {
    val fakeProvider = FakeProvider(featureFlagValueProvider)

    class FakeProvider(private val featureFlagValueProvider: FeatureFlagValueProvider) :
        FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            featureFlagValueProvider.isEnabled(feature)
    }
}