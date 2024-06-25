package mega.privacy.android.feature.sync.domain.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearSelectedMegaFolderUseCaseTest {

    private val repository: SyncNewFolderParamsRepository = mock()
    private val underTest = ClearSelectedMegaFolderUseCase(repository)

    @Test
    fun `test that clearSelectedMegaFolder calls repository`() {
        underTest()

        verify(repository).clearSelectedMegaFolder()
    }
}
