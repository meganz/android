package mega.privacy.android.feature.sync.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetMyBackupsFolderUseCaseTest {
    private lateinit var underTest: SetMyBackupsFolderUseCase
    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetMyBackupsFolderUseCase(backupRepository = backupRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(backupRepository)
    }

    @Test
    fun `test that the node id is returned`() = runTest {
        val nodeId = mock<NodeId>()
        whenever(backupRepository.setMyBackupsFolder(localizedName = "Backups")).thenReturn(nodeId)
        assertThat(underTest(localizedName = "Backups")).isEqualTo(nodeId)
    }
}