package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.SetUserPausedSyncUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetUserPausedSyncUseCaseTest {

    private lateinit var underTest: SetUserPausedSyncUseCase
    private val syncPreferencesRepository = mock<SyncPreferencesRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetUserPausedSyncUseCase(syncPreferencesRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesRepository)
    }

    @Test
    fun `test that setUserPausedSync is called when paused is true`() = runTest {
        val syncId = 123L
        underTest(syncId, paused = true)

        verify(syncPreferencesRepository).setUserPausedSync(syncId)
    }

    @Test
    fun `test that deleteUserPausedSync is called when paused is false`() = runTest {
        val syncId = 123L
        underTest(syncId, paused = false)

        verify(syncPreferencesRepository).deleteUserPausedSync(syncId)
    }
}
