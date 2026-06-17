package mega.privacy.android.shared.nodes.extension

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetRootNodeIdExtTest {

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase)
    }

    @Test
    fun `test that orInvalid returns the root node id when the use case succeeds`() = runTest {
        val expected = NodeId(42L)
        getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn expected }

        assertThat(getRootNodeIdUseCase.orInvalid()).isEqualTo(expected)
    }

    @Test
    fun `test that orInvalid falls back to invalid id when the use case returns null`() = runTest {
        getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn null }

        assertThat(getRootNodeIdUseCase.orInvalid()).isEqualTo(NodeId(-1))
    }

    @Test
    fun `test that orInvalid falls back to invalid id when the use case throws`() = runTest {
        getRootNodeIdUseCase.stub { onBlocking { invoke() } doAnswer { throw RuntimeException("boom") } }

        assertThat(getRootNodeIdUseCase.orInvalid()).isEqualTo(NodeId(-1))
    }
}
