package mega.privacy.android.feature.sync.domain

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.IsSyncPausedByTheUserUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsSyncPausedByTheUserUseCaseTest {

    private lateinit var underTest: IsSyncPausedByTheUserUseCase
    private val syncPreferencesRepository = mock<SyncPreferencesRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsSyncPausedByTheUserUseCase(syncPreferencesRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesRepository)
    }

    @Test
    fun `test that getUserPausedSync returns true when sync is paused`() = runTest {
        val syncId = 123L
        whenever(syncPreferencesRepository.isSyncPausedByTheUser(syncId)).thenReturn(true)

        val result = underTest(syncId)

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that getUserPausedSync returns false when sync is not paused`() = runTest {
        val syncId = 123L
        whenever(syncPreferencesRepository.isSyncPausedByTheUser(syncId)).thenReturn(false)

        val result = underTest(syncId)

        Truth.assertThat(result).isFalse()
    }
}
