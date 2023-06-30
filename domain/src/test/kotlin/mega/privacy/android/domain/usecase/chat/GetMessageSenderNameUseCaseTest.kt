package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.account.GetUserAliasUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMessageSenderNameUseCaseTest {

    private lateinit var underTest: GetMessageSenderNameUseCase
    private lateinit var getUserAliasUseCase: GetUserAliasUseCase
    private lateinit var participantsRepository: ChatParticipantsRepository

    private val alias = "alias"
    private val fullName = "fullName"
    private val email = "email@test.com"
    private val userHandle = 1L
    private val chatId = 2L

    @BeforeAll
    fun setup() {
        getUserAliasUseCase = mock()
        participantsRepository = mock()
        underTest = GetMessageSenderNameUseCase(getUserAliasUseCase, participantsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getUserAliasUseCase, participantsRepository)
    }

    @ParameterizedTest(name = "{3} when alias is {0}, full name is {1} and email is {2}")
    @MethodSource("provideParameters")
    fun `test that sender name is`(
        alias: String?,
        fullName: String?,
        email: String?,
        expectedName: String?,
    ) = runTest {
        whenever(getUserAliasUseCase(userHandle)).thenReturn(alias)
        whenever(participantsRepository.loadUserAttributes(chatId, listOf(userHandle)))
            .thenReturn(Unit)
        whenever(participantsRepository.getUserFullNameFromCache(userHandle)).thenReturn(fullName)
        whenever(participantsRepository.getUserEmailFromCache(userHandle)).thenReturn(email)

        Truth.assertThat(underTest.invoke(userHandle, chatId)).isEqualTo(expectedName)
    }

    private fun provideParameters(): Stream<Arguments?>? =
        Stream.of(
            Arguments.of(null, null, null, null),
            Arguments.of(alias, null, null, alias),
            Arguments.of(alias, fullName, null, alias),
            Arguments.of(alias, fullName, email, alias),
            Arguments.of(null, fullName, null, fullName),
            Arguments.of(null, fullName, email, fullName),
            Arguments.of(null, null, email, email),
        )
}