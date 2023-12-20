package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.RefreshSyncUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class RefreshSyncUseCaseTest {
    private val syncRepository: SyncRepository = mock()
    private val underTest = RefreshSyncUseCase(syncRepository)

    @Test
    fun `test that when invoke called it executes refresh on repository`() = runTest {
        underTest()
        verify(syncRepository).refreshSync()
    }
}