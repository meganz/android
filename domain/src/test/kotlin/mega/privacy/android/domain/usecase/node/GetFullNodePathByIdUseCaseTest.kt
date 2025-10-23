package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFullNodePathByIdUseCaseTest {

    lateinit var underTest: GetFullNodePathByIdUseCase
    private val nodeRepository = mock<NodeRepository>()

    private val id = NodeId(1234L)

    @BeforeAll
    fun setUp() {
        underTest = GetFullNodePathByIdUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Cloud Drive/path"])
    fun `test that use case invokes and returns correctly`(path: String?) =
        runTest {
            whenever(nodeRepository.getFullNodePathById(id)) doReturn path

            assertThat(underTest(id)).isEqualTo(path)
        }
}