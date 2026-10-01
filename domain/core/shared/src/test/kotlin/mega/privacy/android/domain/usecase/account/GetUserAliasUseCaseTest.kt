package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
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
class GetUserAliasUseCaseTest {

    private lateinit var underTest: GetUserAliasUseCase
    private lateinit var accountRepository: AccountRepository

    private val alias = "alias"
    private val userHandle = 1L

    @BeforeAll
    fun setup() {
        accountRepository = mock()
        underTest = GetUserAliasUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @ParameterizedTest(name = "{2} when getting it from cache is {0} and getting it from request is {1}")
    @MethodSource("provideParameters")
    fun `test that Alias is`(
        aliasFromCache: String?,
        aliasFromRequest: String?,
        expectedAlias: String?,
    ) = runTest {
        whenever(accountRepository.getUserAliasFromCache(userHandle)).thenReturn(aliasFromCache)
        whenever(accountRepository.getUserAlias(userHandle)).thenReturn(aliasFromRequest)

        Truth.assertThat(underTest.invoke(userHandle)).isEqualTo(expectedAlias)
    }

    private fun provideParameters(): Stream<Arguments?>? =
        Stream.of(
            Arguments.of(null, null, null),
            Arguments.of(alias, null, alias),
            Arguments.of(null, alias, alias),
            Arguments.of(alias, alias, alias),
        )
}