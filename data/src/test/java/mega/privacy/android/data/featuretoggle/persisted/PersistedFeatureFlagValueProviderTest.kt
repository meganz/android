package mega.privacy.android.data.featuretoggle.persisted

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PersistedFeatureFlagValueProviderTest {

    private val memoryCache = mock<PersistentFeatureFlagMemoryCache>()

    private val underTest = PersistedFeatureFlagValueProvider(
        persistentFeatureFlagMemoryCache = memoryCache,
    )

    @Test
    fun `test that priority is Cached`() {
        assertThat(underTest.priority).isEqualTo(FeatureFlagValuePriority.Cached)
    }

    @Test
    fun `test that isEnabled delegates to the memory cache`() = runTest {
        val feature = mock<Feature>()
        whenever(memoryCache.enabled(feature)).thenReturn(true)

        assertThat(underTest.isEnabled(feature)).isTrue()
    }

    @Test
    fun `test that isEnabled returns null when the memory cache returns null`() = runTest {
        val feature = mock<Feature>()
        whenever(memoryCache.enabled(feature)).thenReturn(null)

        assertThat(underTest.isEnabled(feature)).isNull()
    }
}
