package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.ChangeSyncLocalRootUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeSyncLocalRootUseCaseTest {

    private lateinit var underTest: ChangeSyncLocalRootUseCase
    private val syncRepository = mock<SyncRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ChangeSyncLocalRootUseCase(syncRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository)
    }

    @Test
    fun `test that when invoked sync root is changed`() = runTest {
        val syncId = 123L
        val newLocalPath = "content://com.example.app/syncs/123"
        whenever(syncRepository.changeSyncLocalRoot(syncId, newLocalPath)).thenReturn(syncId)

        val result = underTest(syncId, newLocalPath)

        Truth.assertThat(result).isEqualTo(syncId)
    }
}
