package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChildrenCountUseCaseTest {

    private lateinit var underTest: GetChildrenCountUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetChildrenCountUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that invoke returns the count from the repository`() = runTest {
        val nodeId = NodeId(1L)
        whenever(nodeRepository.getChildrenCount(nodeId)).thenReturn(213_456)

        val actual = underTest(nodeId)

        assertThat(actual).isEqualTo(213_456)
    }
}
