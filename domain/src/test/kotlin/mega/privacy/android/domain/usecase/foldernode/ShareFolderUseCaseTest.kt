package mega.privacy.android.domain.usecase.foldernode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderUseCaseTest {
    private val nodeRepository: NodeRepository = mock()

    private val underTest = ShareFolderUseCase(nodeRepository = nodeRepository)

    @BeforeAll
    fun setDispatchers() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @ParameterizedTest(name = "Share folders {0} with users {1}")
    @MethodSource("provideParams")
    fun `test that the expected result is provided when folders are shared to multiple users`(
        nodeIds: List<NodeId>,
        contactData: List<String>,
        accessPermission: AccessPermission,
        expected: MoveRequestResult.ShareMovement,
    ) = runTest {
        whenever(
            nodeRepository.shareFolder(SUCCESS_NODE_ID, "sample@mega.co.nz", accessPermission)
        ).thenReturn(Unit)
        whenever(
            nodeRepository.shareFolder(FAILED_NODE_ID, "sample@mega.co.nz", accessPermission)
        ).thenThrow(RuntimeException::class.java)
        val actual = underTest(
            nodeIds = nodeIds,
            contactData = contactData,
            accessPermission = accessPermission
        )

        assertThat(actual.count).isEqualTo(expected.count)
        assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(
            listOf(SUCCESS_NODE_ID),
            listOf(SAMPLE_EMAIL),
            AccessPermission.FULL,
            MoveRequestResult.ShareMovement(
                count = 1,
                errorCount = 0,
                nodes = listOf()
            )
        ),
        Arguments.of(
            listOf(FAILED_NODE_ID),
            listOf(SAMPLE_EMAIL),
            AccessPermission.READ,
            MoveRequestResult.ShareMovement(
                count = 1,
                errorCount = 1,
                nodes = listOf()
            )
        ),
        Arguments.of(
            listOf(FAILED_NODE_ID, SUCCESS_NODE_ID),
            listOf(SAMPLE_EMAIL),
            AccessPermission.READWRITE,
            MoveRequestResult.ShareMovement(
                count = 2,
                errorCount = 1,
                nodes = listOf()
            )
        )
    )

    @AfterEach
    fun resetMocks() {
        reset(
            nodeRepository
        )
    }

    companion object {
        private val SUCCESS_NODE_ID = NodeId(1234L)
        private val FAILED_NODE_ID = NodeId(2345L)
        private const val SAMPLE_EMAIL = "sample@mega.co.nz"
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}