package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.app.di.featuretoggle.FeatureFlagPriorityKey
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFeatureFlagRepositoryTest {
    private lateinit var underTest: FeatureFlagRepository

    private val defaultProviderMock =
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

    private val featureFlagValueProviders = providerMocks.mapKeys {
        FeatureFlagPriorityKey(FeatureFlagValueProvider::class,
            it.key)
    }

    @Before
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
}