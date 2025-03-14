package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeAccessUseCaseTest {
    private lateinit var underTest: GetNodeAccessUseCase
    private val nodeRepository = mock<NodeRepository>()
    private val testNodeId = NodeId(1L)

    @BeforeAll
    fun setUp() {
        underTest = GetNodeAccessUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(nodeRepository)
    }

    @ParameterizedTest(name = "when node access permission is {0}")
    @MethodSource("provideParameters")
    fun `test that the result is returned as expected`(
        accessPermission: AccessPermission?
    ) =
        runTest {
            whenever(nodeRepository.getNodeAccessPermission(testNodeId)).thenReturn(accessPermission)
            val actual = underTest(testNodeId)
            assertThat(actual).isEqualTo(accessPermission)
        }

    private fun provideParameters() = listOf(
        null,
        AccessPermission.UNKNOWN,
        AccessPermission.READ,
        AccessPermission.READWRITE,
        AccessPermission.FULL,
        AccessPermission.OWNER
    )
}