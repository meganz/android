package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

class GetAllowedSharingPermissionsUseCaseTest {
    lateinit var underTest: GetAllowedSharingPermissionsUseCase

    private val checkBackupNodeTypeUseCase = mock<CheckBackupNodeTypeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetAllowedSharingPermissionsUseCase(
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            checkBackupNodeTypeUseCase,
            getNodeByIdUseCase,
        )
    }

    @Test
    fun `test that NodeNotFoundException is thrown when node is not found`() = runTest {
        getNodeByIdUseCase.stub { onBlocking { invoke(any()) }.thenReturn(null) }
        assertThrows<NodeDoesNotExistsException> {
            underTest(nodeId = NodeId(1L))
        }
    }

    @Test
    fun `test that all permissions are returned for non backup nodes`() = runTest {
        getNodeByIdUseCase.stub { onBlocking { invoke(any()) }.thenReturn(mock()) }
        checkBackupNodeTypeUseCase.stub { onBlocking { invoke(any()) }.thenReturn(BackupNodeType.NonBackupNode) }
        val result = underTest(nodeId = NodeId(1L))
        assertThat(result).containsExactlyElementsIn(AccessPermission.entries)
    }

    @ParameterizedTest(name = "type: {0}")
    @EnumSource(
        value = BackupNodeType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["NonBackupNode"]
    )
    fun `test that only read permission is returned for backup node`(backupType: BackupNodeType) =
        runTest {
            getNodeByIdUseCase.stub { onBlocking { invoke(any()) }.thenReturn(mock()) }
            checkBackupNodeTypeUseCase.stub { onBlocking { invoke(any()) }.thenReturn(backupType) }
            val result = underTest(nodeId = NodeId(1L))
            assertThat(result).containsExactly(AccessPermission.READ)
        }
}