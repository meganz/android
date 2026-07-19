package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.featuretoggle.PersistedFeatureFlagSnapshotGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFeatureFlagRepositoryTest {
    private lateinit var underTest: FeatureFlagRepository

    private val defaultProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.Default)
        }
    private val secondaryDefaultProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.Default)
        }
    private val configurationFileProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.ConfigurationFile)
        }
    private val buildTimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.BuildTimeOverride)
        }
    private val remoteToggledProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
            on { priority }.thenReturn(FeatureFlagValuePriority.RemoteToggled)
        }
    private val runtimeOverrideProviderMock =
        mock<FeatureFlagValueProvider> {
            on { isEnabled(any()) }.thenReturn(null)
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

    private var persistedSnapshot: Map<Feature, Boolean> = emptyMap()

    private val persistedFeatureFlagSnapshotGateway: PersistedFeatureFlagSnapshotGateway = mock {
        on { currentSnapshot() } doAnswer { persistedSnapshot }
    }

    private val managedFeatures: Set<Feature> = setOf(TestFeature.A, TestFeature.B)

    @BeforeEach
    fun setUp() {
        underTest = DefaultFeatureFlagRepository(
            ioDispatcher = UnconfinedTestDispatcher(),
            featureFlagValueProviderSet = featureFlagValueProviders,
            persistedFeatureFlagSnapshotGateway = persistedFeatureFlagSnapshotGateway,
            managedFeatures = managedFeatures,
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
    fun `test that getFeatureValue with priorities filters providers to that set`() = runTest {
        val feature = mock<Feature>()
        whenever(remoteToggledProviderMock.isEnabled(feature)).thenReturn(true)
        whenever(defaultProviderMock.isEnabled(feature)).thenReturn(false)

        val onlyDefault = underTest.getFeatureValue(
            feature,
            setOf(FeatureFlagValuePriority.Default),
        )
        val onlyRemote = underTest.getFeatureValue(
            feature,
            setOf(FeatureFlagValuePriority.RemoteToggled),
        )

        assertThat(onlyDefault).isFalse()
        assertThat(onlyRemote).isTrue()
    }

    @Test
    fun `test that getCurrentPersistedSnapshot returns the gateway snapshot`() = runTest {
        persistedSnapshot = mapOf(TestFeature.A to true, TestFeature.B to false)

        assertThat(underTest.getCurrentPersistedSnapshot()).containsExactly(
            TestFeature.A, true,
            TestFeature.B, false,
        )
    }

    @Test
    fun `test that applySnapshot delegates the snapshot to the gateway`() = runTest {
        val snapshot = mapOf<Feature, Boolean>(TestFeature.A to true, TestFeature.B to false)

        underTest.applySnapshot(snapshot)

        val captor = argumentCaptor<Map<Feature, Boolean>>()
        verify(persistedFeatureFlagSnapshotGateway).applySnapshot(captor.capture())
        assertThat(captor.firstValue).isEqualTo(snapshot)
    }

    @Test
    fun `test that clearPersistedSnapshot delegates to the gateway`() = runTest {
        underTest.clearPersistedSnapshot()

        verify(persistedFeatureFlagSnapshotGateway).clear()
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

private enum class TestFeature(override val description: String) : Feature {
    A("A"),
    B("B"),
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
