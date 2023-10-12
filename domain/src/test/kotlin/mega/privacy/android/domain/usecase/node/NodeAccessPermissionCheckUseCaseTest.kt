package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAccessPermissionCheckUseCaseTest {

    private lateinit var underTest: NodeAccessPermissionCheckUseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = NodeAccessPermissionCheckUseCase(
            nodeRepository = nodeRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @ParameterizedTest(name = "test that checkAccessPermission return {0} CheckNodeHasRequiredAccessPermissionUseCase returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when checkAccessPermission return boolean then NodeAccessPermissionCheckUseCase returns the same value`(
        expected: Boolean,
    ) = runTest {
        val nodeId = NodeId(123L)
        val level = AccessPermission.FULL
        whenever(
            nodeRepository.checkIfNodeHasTheRequiredAccessLevelPermission(
                nodeId,
                level
            )
        ).thenReturn(expected)
        val result = underTest(nodeId, level)
        Truth.assertThat(result).isEqualTo(expected)
    }
}