package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class UnlockPasscodeUseCaseTest {
    private lateinit var underTest: UnlockPasscodeUseCase

    private val passcodeRepository = mock<PasscodeRepository>()
    private val logoutUseCase = mock<LogoutUseCase>()
    private val checkPasscodeUseCase = mock<CheckPasscodeUseCase>()
    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = UnlockPasscodeUseCase(
            passcodeRepository = passcodeRepository,
            logoutUseCase = logoutUseCase,
            checkPasscodeUseCase = checkPasscodeUseCase,
            accountRepository = accountRepository,
        )
    }

    @Test
    internal fun `test that passcode requests call the passcode check`() = runTest {
        checkPasscodeUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(true)
        }
        val expected = "passcode"
        underTest(UnlockPasscodeRequest.PasscodeRequest(expected))
        verify(checkPasscodeUseCase).invoke(expected)
    }

    @Test
    internal fun `test that password requests call the password check`() = runTest {
        accountRepository.stub {
            onBlocking { isCurrentPassword(any()) }.thenReturn(true)
        }
        val expected = "password"
        underTest(UnlockPasscodeRequest.PasswordRequest(expected))
        verify(accountRepository).isCurrentPassword(expected)
    }

    @ParameterizedTest
    @MethodSource("getArgs")
    internal fun `test that a failed check increments the failed attempts`(request: UnlockPasscodeRequest) =
        runTest {
            Mockito.clearInvocations(passcodeRepository)
            val initialCount = 1
            passcodeRepository.stub {
                on { monitorFailedAttempts() }.thenReturn(flow {
                    emit(initialCount)
                    awaitCancellation()
                })
            }
            accountRepository.stub {
                onBlocking { isCurrentPassword(any()) }.thenReturn(false)
            }
            checkPasscodeUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(false)
            }

            underTest.invoke(request)

            verifyBlocking(passcodeRepository) {
                passcodeRepository.setFailedAttempts(
                    initialCount + 1
                )
            }
        }

    @ParameterizedTest
    @MethodSource("getArgs")
    internal fun `test that a successful check sets failed attempts to 0`(request: UnlockPasscodeRequest) =
        runTest {
            Mockito.clearInvocations(passcodeRepository)
            val initialCount = 1
            passcodeRepository.stub {
                on { monitorFailedAttempts() }.thenReturn(flow {
                    emit(initialCount)
                    awaitCancellation()
                })
            }
            accountRepository.stub {
                onBlocking { isCurrentPassword(any()) }.thenReturn(true)
            }
            checkPasscodeUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(true)
            }

            underTest.invoke(request)

            verifyBlocking(passcodeRepository) {
                passcodeRepository.setFailedAttempts(0)
            }
        }

    @ParameterizedTest
    @MethodSource("getArgs")
    internal fun `test that a successful check sets the passcode state to unlocked`(request: UnlockPasscodeRequest) =
        runTest {
            Mockito.clearInvocations(passcodeRepository)
            val initialCount = 1
            passcodeRepository.stub {
                on { monitorFailedAttempts() }.thenReturn(flow {
                    emit(initialCount)
                    awaitCancellation()
                })
            }
            accountRepository.stub {
                onBlocking { isCurrentPassword(any()) }.thenReturn(true)
            }

            checkPasscodeUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(true)
            }

            underTest.invoke(request)

            verifyBlocking(passcodeRepository) {
                passcodeRepository.setLocked(false)
            }
        }

    @ParameterizedTest
    @MethodSource("getArgs")
    internal fun `test that a tenth failed check calls the logout use case`(request: UnlockPasscodeRequest) =
        runTest {
            Mockito.clearInvocations(passcodeRepository, logoutUseCase)
            val initialCount = 9
            passcodeRepository.stub {
                on { monitorFailedAttempts() }.thenReturn(flow {
                    emit(initialCount)
                    awaitCancellation()
                })
            }
            accountRepository.stub {
                onBlocking { isCurrentPassword(any()) }.thenReturn(false)
            }

            checkPasscodeUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(false)
            }

            underTest.invoke(request)

            verifyBlocking(logoutUseCase) { invoke() }
        }

    private fun getArgs(): Stream<UnlockPasscodeRequest> = Stream.of(
        UnlockPasscodeRequest.PasscodeRequest("A passcode"),
        UnlockPasscodeRequest.PasswordRequest("A password"),
    )


}