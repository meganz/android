package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeLocationByIdUseCaseTest {
    private lateinit var underTest: GetNodeLocationByIdUseCase

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getNodeLocationUseCase = mock<GetNodeLocationUseCase>()

    private val nodeId = NodeId(100L)

    @BeforeAll
    fun setUp() {
        underTest = GetNodeLocationByIdUseCase(
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodeLocationUseCase = getNodeLocationUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            getNodeLocationUseCase,
        )
    }

    @Test
    fun `test that null is returned if node is null`() = runTest {
        whenever(getNodeByIdUseCase(nodeId)) doReturn null

        assertThat(underTest(nodeId)).isNull()
    }

    @Test
    fun `test that node location is returned if node is not null`() = runTest {
        val node = mock<TypedFileNode>()
        val nodeLocation = mock<NodeLocation>()

        whenever(getNodeByIdUseCase(nodeId)) doReturn node
        whenever(getNodeLocationUseCase(node)) doReturn nodeLocation

        assertThat(underTest(nodeId)).isEqualTo(nodeLocation)
    }
}