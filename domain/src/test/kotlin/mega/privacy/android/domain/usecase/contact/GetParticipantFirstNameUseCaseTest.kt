package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetParticipantFirstNameUseCaseTest {
    private lateinit var underTest: GetParticipantFirstNameUseCase
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetParticipantFirstNameUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that use case invokes correctly repository with contemplate email as default`() =
        runTest {
            val handle = 123L

            underTest(handle)
            verify(chatRepository).getParticipantFirstName(handle, true)
        }

    @ParameterizedTest(name = " as {0}")
    @ValueSource(booleans = [true, false])
    fun `test that use case invokes correctly repository with contemplate email`(
        contemplateEmail: Boolean,
    ) = runTest {
        val handle = 123L

        underTest(handle, contemplateEmail)
        verify(chatRepository).getParticipantFirstName(handle, contemplateEmail)
    }
}