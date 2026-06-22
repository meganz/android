package mega.privacy.android.data.featuretoggle.persisted

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentFeatureFlagMemoryCacheTest {

    private val managedFeatures: Set<Feature> = setOf(TestFeature.A, TestFeature.B)

    private var persistedSnapshot: Map<String, Boolean> = emptyMap()

    private val persistedFeatureFlagCache: PersistedFeatureFlagCache = mock {
        on { read() } doAnswer { persistedSnapshot }
    }

    private val defaultProvider = mock<FeatureFlagValueProvider> {
        on { priority } doReturn FeatureFlagValuePriority.Default
    }

    private val underTest by lazy {
        PersistentFeatureFlagMemoryCache(
            defaultProviders = setOf(defaultProvider),
            managedFeatures = managedFeatures,
            persistedFeatureFlagCache = persistedFeatureFlagCache,
        )
    }

    @Test
    fun `test that enabled returns null for a feature outside the managed set`() = runTest {
        assertThat(underTest.enabled(TestFeature.UNMANAGED)).isNull()
    }

    @Test
    fun `test that enabled returns the default when the persisted file is empty`() = runTest {
        whenever(defaultProvider.isEnabled(TestFeature.A)).thenReturn(true)
        whenever(defaultProvider.isEnabled(TestFeature.B)).thenReturn(false)

        assertThat(underTest.enabled(TestFeature.A)).isTrue()
        assertThat(underTest.enabled(TestFeature.B)).isFalse()
    }

    @Test
    fun `test that enabled returns the persisted value when the store has it`() = runTest {
        persistedSnapshot = mapOf(
            keyOf(TestFeature.A) to true,
            keyOf(TestFeature.B) to false,
        )
        whenever(defaultProvider.isEnabled(TestFeature.A)).thenReturn(false)
        whenever(defaultProvider.isEnabled(TestFeature.B)).thenReturn(true)

        assertThat(underTest.enabled(TestFeature.A)).isTrue()
        assertThat(underTest.enabled(TestFeature.B)).isFalse()
    }

    @Test
    fun `test that currentSnapshot returns a sparse map of only persisted entries`() = runTest {
        persistedSnapshot = mapOf(keyOf(TestFeature.A) to true)

        val snapshot = underTest.currentSnapshot()

        assertThat(snapshot).containsExactly(TestFeature.A, true)
    }

    @Test
    fun `test that currentSnapshot returns empty when nothing is persisted`() = runTest {
        assertThat(underTest.currentSnapshot()).isEmpty()
    }

    @Test
    fun `test that applySnapshot writes the snapshot to the on-disk cache`() = runTest {
        val snapshot = mapOf<Feature, Boolean>(TestFeature.A to true, TestFeature.B to false)

        underTest.applySnapshot(snapshot)

        val captor = argumentCaptor<Map<String, Boolean>>()
        verify(persistedFeatureFlagCache).write(captor.capture())
        assertThat(captor.firstValue).containsExactly(
            keyOf(TestFeature.A), true,
            keyOf(TestFeature.B), false,
        )
    }

    @Test
    fun `test that applySnapshot updates in-memory value for a not-yet-read feature`() = runTest {
        whenever(defaultProvider.isEnabled(any())).thenReturn(false)

        underTest.applySnapshot(mapOf(TestFeature.A to true, TestFeature.B to true))

        assertThat(underTest.enabled(TestFeature.A)).isTrue()
        assertThat(underTest.enabled(TestFeature.B)).isTrue()
    }

    @Test
    fun `test that applySnapshot does not change in-memory value for an already-read feature`() =
        runTest {
            whenever(defaultProvider.isEnabled(any())).thenReturn(false)
            assertThat(underTest.enabled(TestFeature.A)).isFalse()

            underTest.applySnapshot(mapOf(TestFeature.A to true, TestFeature.B to true))

            assertThat(underTest.enabled(TestFeature.A)).isFalse()
            assertThat(underTest.enabled(TestFeature.B)).isTrue()
        }

    @Test
    fun `test that applySnapshot marks the cache as loaded so subsequent reads skip the file`() =
        runTest {
            underTest.applySnapshot(mapOf(TestFeature.A to true, TestFeature.B to false))
            underTest.enabled(TestFeature.A)

            verify(persistedFeatureFlagCache, never()).read()
        }

    @Test
    fun `test that clear deletes the persisted on-disk cache`() = runTest {
        underTest.clear()

        verify(persistedFeatureFlagCache).clear()
    }

    @Test
    fun `test that clear resets in-memory state so subsequent reads re-seed from the file`() =
        runTest {
            whenever(defaultProvider.isEnabled(any())).thenReturn(false)
            underTest.applySnapshot(mapOf(TestFeature.A to true))
            assertThat(underTest.enabled(TestFeature.A)).isTrue()

            underTest.clear()
            persistedSnapshot = mapOf(keyOf(TestFeature.A) to false)

            assertThat(underTest.enabled(TestFeature.A)).isFalse()
            verify(persistedFeatureFlagCache).read()
        }

    private fun keyOf(feature: Feature): String =
        "${feature::class.java.name}#${feature.name}"

    private enum class TestFeature(override val description: String) : Feature {
        A("A"),
        B("B"),
        UNMANAGED("U"),
    }
}
