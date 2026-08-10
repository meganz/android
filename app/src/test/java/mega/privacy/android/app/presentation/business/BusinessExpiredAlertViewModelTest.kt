package mega.privacy.android.app.presentation.business

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.business.model.BusinessExpiredAlertUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
class BusinessExpiredAlertViewModelTest {

    private lateinit var underTest: BusinessExpiredAlertViewModel

    private val getAccountTypeUseCase = mock<GetAccountTypeUseCase>()
    private val isMasterBusinessAccountUseCase = mock<IsMasterBusinessAccountUseCase>()

    @BeforeEach
    fun setUp() {
        getAccountTypeUseCase.stub {
            on { invoke() } doReturn AccountType.UNKNOWN
        }
        isMasterBusinessAccountUseCase.stub {
            on { invoke() } doReturn false
        }
        underTest = BusinessExpiredAlertViewModel(
            getAccountTypeUseCase = getAccountTypeUseCase,
            isMasterBusinessAccountUseCase = isMasterBusinessAccountUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getAccountTypeUseCase,
            isMasterBusinessAccountUseCase,
        )
    }

    @Test
    fun `test that initial state has default values`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(BusinessExpiredAlertUiState())
    }

    @ParameterizedTest(name = "test that isProFlexiAccount is {1} when account type is {0}")
    @MethodSource("provideAccountTypes")
    fun `test that isProFlexiAccount maps correctly from account type`(
        accountType: AccountType,
        expectedIsProFlexi: Boolean,
    ) = runTest {
        getAccountTypeUseCase.stub {
            on { invoke() } doReturn accountType
        }

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isProFlexiAccount).isEqualTo(expectedIsProFlexi)
        }
    }

    @ParameterizedTest(name = "test that isMasterBusinessAccount is {0} when use case returns {0}")
    @MethodSource("provideMasterBusinessValues")
    fun `test that isMasterBusinessAccount maps correctly from use case`(
        isMaster: Boolean,
    ) = runTest {
        isMasterBusinessAccountUseCase.stub {
            on { invoke() } doReturn isMaster
        }

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isMasterBusinessAccount).isEqualTo(isMaster)
        }
    }

    @Test
    fun `test that isProFlexiAccount is false when getAccountTypeUseCase throws`() = runTest {
        getAccountTypeUseCase.stub {
            on { invoke() }.thenThrow(RuntimeException())
        }

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isProFlexiAccount).isFalse()
        }
    }

    @Test
    fun `test that isMasterBusinessAccount is false when isMasterBusinessAccountUseCase throws`() =
        runTest {
            isMasterBusinessAccountUseCase.stub {
                on { invoke() }.thenThrow(RuntimeException())
            }

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMasterBusinessAccount).isFalse()
            }
        }

    companion object {
        @JvmStatic
        private fun provideAccountTypes(): Stream<Arguments> = Stream.of(
            Arguments.of(AccountType.PRO_FLEXI, true),
            Arguments.of(AccountType.FREE, false),
            Arguments.of(AccountType.PRO_I, false),
            Arguments.of(AccountType.PRO_II, false),
            Arguments.of(AccountType.PRO_III, false),
            Arguments.of(AccountType.PRO_LITE, false),
            Arguments.of(AccountType.BUSINESS, false),
            Arguments.of(AccountType.STARTER, false),
            Arguments.of(AccountType.BASIC, false),
            Arguments.of(AccountType.ESSENTIAL, false),
            Arguments.of(AccountType.UNKNOWN, false),
        )

        @JvmStatic
        private fun provideMasterBusinessValues(): Stream<Arguments> = Stream.of(
            Arguments.of(true),
            Arguments.of(false),
        )
    }
}
