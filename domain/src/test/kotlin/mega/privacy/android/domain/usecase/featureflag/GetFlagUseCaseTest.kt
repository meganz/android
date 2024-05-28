package mega.privacy.android.domain.usecase.featureflag

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetFlagUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: GetFlagUseCase

    @BeforeEach
    fun setup() {
        underTest = GetFlagUseCase(callRepository)
    }

    @Test
    fun `test that get flag is called with correct parameters`() = runTest {
        val name = "chmon"

        underTest.invoke(nameFlag = name)

        verify(callRepository).getFlag(name)
    }
}