package mega.privacy.android.feature.sync.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.data.gateway.SyncPromotionDataStore
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncPromotionPreferencesRepositoryImplTest {

    private lateinit var underTest: SyncPromotionPreferencesRepositoryImpl
    private val syncPromotionDataStore = mock<SyncPromotionDataStore>()

    @BeforeAll
    fun setUp() {
        underTest = SyncPromotionPreferencesRepositoryImpl(
            syncPromotionDataStore = syncPromotionDataStore
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPromotionDataStore)
    }

    @Test
    fun `test that getLastShownTimestamp returns value from datastore`() = runTest {
        whenever(syncPromotionDataStore.getLastShownTimestamp()).thenReturn(1234L)
        val result = underTest.getLastShownTimestamp()
        assertThat(result).isEqualTo(1234L)
    }

    @Test
    fun `test that setLastShownTimestamp calls setLastShownTimestamp on datastore`() = runTest {
        underTest.setLastShownTimestamp(1234L)
        verify(syncPromotionDataStore).setLastShownTimestamp(1234L)
    }

    @Test
    fun `test that getNumberOfTimesShown returns value from datastore`() = runTest {
        whenever(syncPromotionDataStore.getNumberOfTimesShown()).thenReturn(1)
        val result = underTest.getNumberOfTimesShown()
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `test that setNumberOfTimesShown calls setNumberOfTimesShown on datastore`() = runTest {
        underTest.setNumberOfTimesShown(1)
        verify(syncPromotionDataStore).setNumberOfTimesShown(1)
    }

    @Test
    fun `test that increaseNumberOfTimesShown calls increaseNumberOfTimesShown on datastore`() =
        runTest {
            underTest.increaseNumberOfTimesShown(1234L)
            verify(syncPromotionDataStore).increaseNumberOfTimesShown(1234L)
        }
}