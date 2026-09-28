package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleToBase64UseCaseTest {
    private lateinit var underTest: HandleToBase64UseCase
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HandleToBase64UseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that invoke returns the base64 from the repository`() = runTest {
        val handle = 1234567890L
        val base64 = "AbCdEf123"
        whenever(nodeRepository.convertHandleToBase64(handle)).thenReturn(base64)

        assertThat(underTest(handle)).isEqualTo(base64)
    }
}
