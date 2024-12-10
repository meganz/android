package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.Default)
        }
    private val secondaryDefaultProviderMock =
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.Default)
        }
    private val configurationFileProviderMock =
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.ConfigurationFile)
        }
    private val buildTimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.BuildTimeOverride)
        }
    private val remoteToggledProviderMock =
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.RemoteToggled)
        }
    private val runtimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> {
            onBlocking { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.RuntimeOverride)
        }

    private val providerMocks = setOf(
        defaultProviderMock,
        configurationFileProviderMock,
        buildTimeOverrideProviderMock,
        remoteToggledProviderMock,
        runtimeOverrideProviderMock,
    )

    private val fake1 = mock<FeatureFlagValueProvider>()
    private val fake2 = mock<FeatureFlagValueProvider>()


    private val featureFlagValueProviders = providerMocks.plus(
        secondaryDefaultProviderMock
    ).plus(
        Fake1(fake1, FeatureFlagValuePriority.RemoteToggled).fakeProvider
    ).plus(
        Fake2(fake2, FeatureFlagValuePriority.RemoteToggled).fakeProvider
    )

    @BeforeEach
    fun setUp() {
        underTest = DefaultFeatureFlagRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            featureFlagValueProviderSet = featureFlagValueProviders,
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

class Fake1(
    featureFlagValueProvider: FeatureFlagValueProvider,
    priority: FeatureFlagValuePriority,
) {
    val fakeProvider = FakeProvider(featureFlagValueProvider, priority)

    class FakeProvider(
        private val featureFlagValueProvider: FeatureFlagValueProvider,
        override val priority: FeatureFlagValuePriority,
    ) : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            featureFlagValueProvider.isEnabled(feature)
    }
}

class Fake2(
    featureFlagValueProvider: FeatureFlagValueProvider,
    priority: FeatureFlagValuePriority,
) {
    val fakeProvider = FakeProvider(featureFlagValueProvider, priority)

    class FakeProvider(
        private val featureFlagValueProvider: FeatureFlagValueProvider,
        override val priority: FeatureFlagValuePriority,
    ) :
        FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            featureFlagValueProvider.isEnabled(feature)
    }
}
