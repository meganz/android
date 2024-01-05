package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactVerificationWarningUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private lateinit var underTest: GetContactVerificationWarningUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetContactVerificationWarningUseCase(nodeRepository = nodeRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `test that the use case returns the correct warning state`(warningEnabled: Boolean) =
        runTest {
            whenever(nodeRepository.getContactVerificationEnabledWarning()).thenReturn(
                warningEnabled
            )
            val value = underTest()
            assertThat(value).isEqualTo(warningEnabled)
        }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

}