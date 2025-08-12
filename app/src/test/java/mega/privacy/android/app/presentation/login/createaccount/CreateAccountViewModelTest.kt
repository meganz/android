package mega.privacy.android.app.presentation.login.createaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.android.authentication.domain.usecase.regex.DoesTextContainMixedCaseUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainNumericUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainSpecialCharacterUseCase
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_CONFIRM_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_EMAIL
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_LAST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_TERMS_OF_SERVICE
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateAccountViewModelTest {

    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase = mock()
    private val isEmailValidUseCase: IsEmailValidUseCase = mock()
    private val createAccountUseCase: CreateAccountUseCase = mock()
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase = mock()
    private val doesTextContainSpecialCharacterUseCase: DoesTextContainSpecialCharacterUseCase =
        mock()
    private val doesTextContainNumericUseCase: DoesTextContainNumericUseCase = mock()
    private val doesTextContainMixedCaseUseCase: DoesTextContainMixedCaseUseCase = mock()
    private val savedStateHandle = SavedStateHandle()
    private val connectivityFlow = MutableStateFlow(true)

    private lateinit var underTest: CreateAccountViewModel
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val applicationScope: CoroutineScope = CoroutineScope(testDispatcher)
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock {
        on { invoke() }.thenReturn(flowOf(ThemeMode.System))
    }

    @BeforeAll
    fun initViewModel() {
        Dispatchers.setMain(testDispatcher)
        whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
        underTest = CreateAccountViewModel(
            savedStateHandle = savedStateHandle,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getPasswordStrengthUseCase = getPasswordStrengthUseCase,
            isEmailValidUseCase = isEmailValidUseCase,
            createAccountUseCase = createAccountUseCase,
            saveEphemeralCredentialsUseCase = saveEphemeralCredentialsUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase,
            saveLastRegisteredEmailUseCase = saveLastRegisteredEmailUseCase,
            applicationScope = applicationScope,
            doesTextContainSpecialCharacterUseCase = doesTextContainSpecialCharacterUseCase,
            doesTextContainNumericUseCase = doesTextContainNumericUseCase,
            doesTextContainMixedCaseUseCase = doesTextContainMixedCaseUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase
        )
    }

    @BeforeEach
    fun init() {
        reset(
            monitorConnectivityUseCase,
            getPasswordStrengthUseCase,
            isEmailValidUseCase,
            createAccountUseCase,
            saveEphemeralCredentialsUseCase,
            clearEphemeralCredentialsUseCase,
            saveLastRegisteredEmailUseCase,
            doesTextContainSpecialCharacterUseCase,
            doesTextContainNumericUseCase,
            doesTextContainMixedCaseUseCase,
        )
    }


    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that first name is saved to savedStateHandle and validation is triggered when onFirstNameInputChanged is called`() =
        runTest {
            underTest.onFirstNameInputChanged("first Name")

            assertThat(savedStateHandle.get<String>(KEY_FIRST_NAME)).isEqualTo("first Name")

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFirstNameValid).isTrue()
                assertThat(state.isFirstNameLengthExceeded).isFalse()
            }
        }

    @Test
    fun `test that last name is saved to savedStateHandle and validation is triggered when onLastNameInputChanged is called`() =
        runTest {
            underTest.onLastNameInputChanged("last Name")

            assertThat(savedStateHandle.get<String>(KEY_LAST_NAME)).isEqualTo("last Name")

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLastNameValid).isTrue()
                assertThat(state.isLastNameLengthExceeded).isFalse()
            }
        }

    @Test
    fun `test that email is saved to savedStateHandle when onEmailInputChanged is called`() =
        runTest {
            val email = "abc@mega.co.nz"
            whenever(isEmailValidUseCase(email)).thenReturn(true)
            underTest.onEmailInputChanged(email)

            assertThat(savedStateHandle.get<String>(KEY_EMAIL)).isEqualTo(email)

            underTest.uiState.test {
                assertThat(awaitItem().isEmailValid).isTrue()
            }
        }


    @Test
    fun `test that isEmailValid is set to false when onEmailInputChanged is called with invalid email`() =
        runTest {
            val email = "abc@mega@.co.nz"
            whenever(isEmailValidUseCase(email)).thenReturn(false)
            underTest.onEmailInputChanged(email)

            assertThat(savedStateHandle.get<String>(KEY_EMAIL)).isEqualTo(email)

            underTest.uiState.test {
                assertThat(awaitItem().isEmailValid).isFalse()
            }
        }

    @Test
    fun `test that confirm password is saved to savedStateHandle and ui state is reset when onConfirmPasswordInputChanged is called`() =
        runTest {
            underTest.onConfirmPasswordInputChanged("password")

            assertThat(savedStateHandle.get<String>(KEY_CONFIRM_PASSWORD)).isEqualTo("password")

            underTest.uiState.test {
                assertThat(awaitItem().isConfirmPasswordMatched).isNull()
            }
        }

    @Test
    fun `test that terms of service is saved to savedStateHandle and ui state is set when termsOfServiceAgreed is checked`() =
        runTest {
            underTest.termsOfServiceAgreedChanged(true)

            assertThat(savedStateHandle.get<Boolean>(KEY_TERMS_OF_SERVICE)).isTrue()

            underTest.uiState.test {
                assertThat(awaitItem().isTermsOfServiceAgreed).isTrue()
            }
        }

    @Test
    fun `test that password is saved to savedStateHandle when onPasswordInputChanged is called`() =
        runTest {
            val password = "password"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            assertThat(savedStateHandle.get<String>(KEY_PASSWORD)).isEqualTo(password)
        }

    @Test
    fun `test that isPasswordValid is reset when onPasswordInputChanged is called`() =
        runTest {
            val password = "password"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)

            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordValid).isEqualTo(null)
            }
        }

    @Test
    fun `test that password length sufficiency is checked in new design when password is short`() =
        runTest {
            val password = "passwo"
            val passwordStrength = PasswordStrength.WEAK
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordLengthSufficient).isEqualTo(false)
            }
        }


    @Test
    fun `test that password length sufficiency is checked in new design when password is long`() =
        runTest {
            val password = "password1234"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordLengthSufficient).isEqualTo(true)
            }
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that password contains numeric use case is invoked in new design`(value: Boolean) =
        runTest {
            val password = "password1234"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(doesTextContainNumericUseCase(password)).thenReturn(value)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().doesPasswordContainNumeric).isEqualTo(value)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that password contains mixed cases use case is invoked in new design`(value: Boolean) =
        runTest {
            val password = "password1234"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(doesTextContainMixedCaseUseCase(password)).thenReturn(value)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().doesPasswordContainMixedCase).isEqualTo(value)
            }
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that password contains special character use case is invoked in new design`(value: Boolean) =
        runTest {
            val password = "password1234"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(doesTextContainSpecialCharacterUseCase(password)).thenReturn(value)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().doesPasswordContainSpecialCharacter).isEqualTo(value)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that confirm password matching is done when user changes password and confirm password is already present`(
        value: Boolean,
    ) =
        runTest {
            val password = "password"
            val confirmPassword = "password123"
            val passwordStrength = PasswordStrength.GOOD
            underTest.onConfirmPasswordInputChanged(confirmPassword)
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().isConfirmPasswordMatched).isEqualTo(false)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the password strength is updated when onPasswordInputChanged is called`(value: Boolean) =
        runTest {
            val password = "password"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().passwordStrength).isEqualTo(passwordStrength)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the password strength is updated as Invalid when password is empty`(value: Boolean) =
        runTest {
            val password = ""
            val passwordStrength = PasswordStrength.INVALID
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().passwordStrength).isEqualTo(passwordStrength)
            }
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that when confirm password matches password should set state to true`(value: Boolean) =
        runTest {
            val password = "password"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            initViewModel()
            underTest.onConfirmPasswordInputChanged(password)
            underTest.onPasswordInputChanged(password)

            underTest.uiState.test {
                assertThat(awaitItem().isConfirmPasswordMatched).isEqualTo(true)
            }
        }


    @Test
    fun `test that create account is not invoked when email is invalid`() =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(false)


            initInputFields()

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isEmailValid).isFalse()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account is not invoked when password is invalid`(value: Boolean) =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.INVALID)
            whenever(isEmailValidUseCase(any())).thenReturn(true)

            initViewModel()
            initInputFields()

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isPasswordValid).isFalse()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account is not invoked when password is very weak`(value: Boolean) =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.VERY_WEAK)
            whenever(isEmailValidUseCase(any())).thenReturn(true)

            initViewModel()
            initInputFields()

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isPasswordValid).isFalse()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account is not invoked when confirm password does not match password`(
        value: Boolean,
    ) =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(true)

            initViewModel()
            initInputFields()

            underTest.onConfirmPasswordInputChanged("password1")

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isConfirmPasswordMatched).isFalse()
            }
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account is not invoked when terms of service is not agreed`(value: Boolean) =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(true)

            initViewModel()
            initInputFields()

            underTest.termsOfServiceAgreedChanged(false)

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isTermsOfServiceAgreed).isFalse()
            }
        }

    @Test
    fun `test that create account is not invoked when there is no internet`() =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(true)

            initViewModel()
            initInputFields()

            connectivityFlow.emit(false)

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.showNoNetworkWarning).isTrue()
            }
        }


    @Test
    fun `test that create account success event is triggered when account creation is successful`() =
        runTest {
            val credentials = mock<EphemeralCredentials>()
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(createAccountUseCase(any(), any(), any(), any())).thenReturn(credentials)
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            connectivityFlow.emit(true)

            initViewModel()
            initInputFields()

            underTest.createAccount()

            verify(createAccountUseCase).invoke(any(), any(), any(), any())
            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.createAccountStatusEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                if (item.createAccountStatusEvent is StateEventWithContentTriggered) {
                    assertThat((item.createAccountStatusEvent as StateEventWithContentTriggered<CreateAccountStatus>).content)
                        .isInstanceOf(CreateAccountStatus.Success::class.java)
                }
                assertThat(item.isLoading).isFalse()
            }
        }

    @Test
    fun `test that create account error event is triggered when account creation throws Unknown error`() =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(
                createAccountUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { throw CreateAccountException.Unknown(mock()) }
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            connectivityFlow.emit(true)

            initViewModel()
            initInputFields()

            underTest.createAccount()

            verify(createAccountUseCase).invoke(any(), any(), any(), any())
            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.createAccountStatusEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                if (item.createAccountStatusEvent is StateEventWithContentTriggered) {
                    assertThat((item.createAccountStatusEvent as StateEventWithContentTriggered<CreateAccountStatus>).content)
                        .isInstanceOf(CreateAccountStatus.UnknownError::class.java)
                }
                assertThat(item.isLoading).isFalse()
            }
        }

    @Test
    fun `test that create account error event is triggered when account creation throws AccountAlreadyExists error`() =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(
                createAccountUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenAnswer { throw CreateAccountException.AccountAlreadyExists }
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            connectivityFlow.emit(true)

            initInputFields()

            underTest.createAccount()

            verify(createAccountUseCase).invoke(any(), any(), any(), any())
            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.createAccountStatusEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                if (item.createAccountStatusEvent is StateEventWithContentTriggered) {
                    assertThat((item.createAccountStatusEvent as StateEventWithContentTriggered<CreateAccountStatus>).content)
                        .isInstanceOf(CreateAccountStatus.AccountAlreadyExists::class.java)
                }
                assertThat(item.isLoading).isFalse()
            }
        }


    @Test
    fun `test that credentials are saved when onCreateAccountSuccess is called`() =
        runTest {
            val credentials = mock<EphemeralCredentials>()

            underTest.onCreateAccountSuccess(credentials)

            verify(clearEphemeralCredentialsUseCase).invoke()
            verify(saveEphemeralCredentialsUseCase).invoke(credentials)
        }

    @Test
    fun `test that last registered email is set when onCreateAccountSuccess is called`() =
        runTest {
            val myEmail = "abc@mega.co.nz"
            val credentials = mock<EphemeralCredentials> {
                on { email }.thenReturn(myEmail)
            }

            underTest.onCreateAccountSuccess(credentials)

            verify(saveLastRegisteredEmailUseCase).invoke(myEmail)
        }

    @Test
    fun `test that first name with exactly 40 characters is valid`() = runTest {
        val firstName40Chars = "a".repeat(40)
        underTest.onFirstNameInputChanged(firstName40Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isTrue()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that first name with more than 40 characters is invalid and sets length exceeded flag`() =
        runTest {
            val firstName41Chars = "a".repeat(41)
            underTest.onFirstNameInputChanged(firstName41Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFirstNameValid).isFalse()
                assertThat(state.isFirstNameLengthExceeded).isTrue()
            }
        }

    @Test
    fun `test that last name with exactly 40 characters is valid`() = runTest {
        val lastName40Chars = "a".repeat(40)
        underTest.onLastNameInputChanged(lastName40Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameValid).isTrue()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that last name with more than 40 characters is invalid and sets length exceeded flag`() =
        runTest {
            val lastName41Chars = "a".repeat(41)
            underTest.onLastNameInputChanged(lastName41Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLastNameValid).isFalse()
                assertThat(state.isLastNameLengthExceeded).isTrue()
            }
        }

    @Test
    fun `test that first name length exceeded flag is reset when input becomes valid`() = runTest {
        // First set an invalid name
        val firstName41Chars = "a".repeat(41)
        underTest.onFirstNameInputChanged(firstName41Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameLengthExceeded).isTrue()
        }

        // Then set a valid name
        val firstName40Chars = "a".repeat(40)
        underTest.onFirstNameInputChanged(firstName40Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isTrue()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that last name length exceeded flag is reset when input becomes valid`() = runTest {
        // First set an invalid name
        val lastName41Chars = "a".repeat(41)
        underTest.onLastNameInputChanged(lastName41Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameLengthExceeded).isTrue()
        }

        // Then set a valid name
        val lastName40Chars = "a".repeat(40)
        underTest.onLastNameInputChanged(lastName40Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameValid).isTrue()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that empty first name is invalid but does not set length exceeded flag`() = runTest {
        underTest.onFirstNameInputChanged("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isFalse()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that empty last name is invalid but does not set length exceeded flag`() = runTest {
        underTest.onLastNameInputChanged("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameValid).isFalse()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that NAME_CHAR_LIMIT constant is 40`() {
        assertThat(CreateAccountViewModel.NAME_CHAR_LIMIT).isEqualTo(40)
    }

    // Tests for new pure validation functions
    @Test
    fun `test that checkFirstNameValidity returns correct validation result for valid name`() = runTest {
        val validName = "John"
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(validName)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkFirstNameValidity returns correct validation result for empty name`() = runTest {
        val emptyName = ""
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(emptyName)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkFirstNameValidity returns correct validation result for name with exactly 40 characters`() = runTest {
        val name40Chars = "a".repeat(40)
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(name40Chars)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkFirstNameValidity returns correct validation result for name with more than 40 characters`() = runTest {
        val name41Chars = "a".repeat(41)
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(name41Chars)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isTrue()
    }

    @Test
    fun `test that checkLastNameValidity returns correct validation result for valid name`() = runTest {
        val validName = "Doe"
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(validName)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkLastNameValidity returns correct validation result for empty name`() = runTest {
        val emptyName = ""
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(emptyName)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkLastNameValidity returns correct validation result for name with exactly 40 characters`() = runTest {
        val name40Chars = "b".repeat(40)
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(name40Chars)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkLastNameValidity returns correct validation result for name with more than 40 characters`() = runTest {
        val name41Chars = "b".repeat(41)
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(name41Chars)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isTrue()
    }

    @Test
    fun `test that checkFirstNameValidity handles whitespace correctly`() = runTest {
        val whitespaceName = "   "
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(whitespaceName)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkLastNameValidity handles whitespace correctly`() = runTest {
        val whitespaceName = "   "
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(whitespaceName)
        
        assertThat(isValid).isFalse()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkFirstNameValidity handles unicode characters correctly`() = runTest {
        val unicodeName = "José-María"
        val (isValid, isLengthExceeded) = underTest.checkFirstNameValidity(unicodeName)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    @Test
    fun `test that checkLastNameValidity handles unicode characters correctly`() = runTest {
        val unicodeName = "Müller-Žáček"
        val (isValid, isLengthExceeded) = underTest.checkLastNameValidity(unicodeName)
        
        assertThat(isValid).isTrue()
        assertThat(isLengthExceeded).isFalse()
    }

    // Edge Cases and Boundary Testing
    @Test
    fun `test that first name with 39 characters is valid`() = runTest {
        val firstName39Chars = "a".repeat(39)
        underTest.onFirstNameInputChanged(firstName39Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isTrue()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that last name with 39 characters is valid`() = runTest {
        val lastName39Chars = "b".repeat(39)
        underTest.onLastNameInputChanged(lastName39Chars)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameValid).isTrue()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that first name with 41 characters is invalid and sets length exceeded flag`() =
        runTest {
            val firstName41Chars = "a".repeat(41)
            underTest.onFirstNameInputChanged(firstName41Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFirstNameValid).isFalse()
                assertThat(state.isFirstNameLengthExceeded).isTrue()
            }
        }

    @Test
    fun `test that last name with 41 characters is invalid and sets length exceeded flag`() =
        runTest {
            val lastName41Chars = "b".repeat(41)
            underTest.onLastNameInputChanged(lastName41Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLastNameValid).isFalse()
                assertThat(state.isLastNameLengthExceeded).isTrue()
            }
        }

    @Test
    fun `test that first name with 100 characters is invalid and sets length exceeded flag`() =
        runTest {
            val firstName100Chars = "a".repeat(100)
            underTest.onFirstNameInputChanged(firstName100Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFirstNameValid).isFalse()
                assertThat(state.isFirstNameLengthExceeded).isTrue()
            }
        }

    @Test
    fun `test that last name with 100 characters is invalid and sets length exceeded flag`() =
        runTest {
            val lastName100Chars = "b".repeat(100)
            underTest.onLastNameInputChanged(lastName100Chars)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLastNameValid).isFalse()
                assertThat(state.isLastNameLengthExceeded).isTrue()
            }
        }

    // Whitespace and Special Character Testing
    @Test
    fun `test that first name with only spaces is invalid but does not set length exceeded flag`() =
        runTest {
            val firstNameSpaces = "   "
            underTest.onFirstNameInputChanged(firstNameSpaces)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isFirstNameValid).isFalse()
                assertThat(state.isFirstNameLengthExceeded).isFalse()
            }
        }

    @Test
    fun `test that last name with only spaces is invalid but does not set length exceeded flag`() =
        runTest {
            val lastNameSpaces = "   "
            underTest.onLastNameInputChanged(lastNameSpaces)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLastNameValid).isFalse()
                assertThat(state.isLastNameLengthExceeded).isFalse()
            }
        }

    @Test
    fun `test that first name with 41 spaces is invalid and sets length exceeded flag`() = runTest {
        val firstName41Spaces = " ".repeat(41)
        underTest.onFirstNameInputChanged(firstName41Spaces)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isFalse()
            assertThat(state.isFirstNameLengthExceeded).isTrue()
        }
    }

    @Test
    fun `test that first name with special characters under 40 chars is valid`() = runTest {
        val firstNameSpecial = "João-María.O'Connor"
        underTest.onFirstNameInputChanged(firstNameSpecial)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isTrue()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
        }
    }

    @Test
    fun `test that last name with unicode characters under 40 chars is valid`() = runTest {
        val lastNameUnicode = "Müller-Žáček"
        underTest.onLastNameInputChanged(lastNameUnicode)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLastNameValid).isTrue()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    // State Transition Testing
    @Test
    fun `test that changing from valid to invalid first name updates state correctly`() = runTest {
        // Start with valid name
        underTest.onFirstNameInputChanged("John")

        underTest.uiState.test {
            val validState = awaitItem()
            assertThat(validState.isFirstNameValid).isTrue()
            assertThat(validState.isFirstNameLengthExceeded).isFalse()
        }

        // Change to invalid name
        underTest.onFirstNameInputChanged("a".repeat(50))

        underTest.uiState.test {
            val invalidState = awaitItem()
            assertThat(invalidState.isFirstNameValid).isFalse()
            assertThat(invalidState.isFirstNameLengthExceeded).isTrue()
        }
    }

    @Test
    fun `test that changing from invalid to valid last name updates state correctly`() = runTest {
        // Start with invalid name
        underTest.onLastNameInputChanged("b".repeat(50))

        underTest.uiState.test {
            val invalidState = awaitItem()
            assertThat(invalidState.isLastNameValid).isFalse()
            assertThat(invalidState.isLastNameLengthExceeded).isTrue()
        }

        // Change to valid name
        underTest.onLastNameInputChanged("Doe")

        underTest.uiState.test {
            val validState = awaitItem()
            assertThat(validState.isLastNameValid).isTrue()
            assertThat(validState.isLastNameLengthExceeded).isFalse()
        }
    }

    // Integration Testing
    @Test
    fun `test that both invalid first and last names prevent account creation`() = runTest {
        whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
        whenever(isEmailValidUseCase(any())).thenReturn(true)
        connectivityFlow.emit(true)

        // Set invalid names (too long)
        underTest.onFirstNameInputChanged("a".repeat(50))
        underTest.onLastNameInputChanged("b".repeat(50))
        underTest.onEmailInputChanged("test@example.com")
        underTest.onPasswordInputChanged("ValidPassword123!")
        underTest.onConfirmPasswordInputChanged("ValidPassword123!")
        underTest.termsOfServiceAgreedChanged(true)

        underTest.createAccount()

        // Verify createAccountUseCase is not called due to validation failure
        verify(createAccountUseCase, never()).invoke(any(), any(), any(), any())
    }

    @Test
    fun `test that valid names with other valid inputs allow account creation attempt`() = runTest {
        whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
        whenever(isEmailValidUseCase(any())).thenReturn(true)
        whenever(createAccountUseCase(any(), any(), any(), any())).thenReturn(mock())
        connectivityFlow.emit(true)

        // Set valid names (exactly 40 chars)
        underTest.onFirstNameInputChanged("a".repeat(40))
        underTest.onLastNameInputChanged("b".repeat(40))
        underTest.onEmailInputChanged("test@example.com")
        underTest.onPasswordInputChanged("ValidPassword123!")
        underTest.onConfirmPasswordInputChanged("ValidPassword123!")
        underTest.termsOfServiceAgreedChanged(true)

        underTest.createAccount()

        // Verify createAccountUseCase is called with valid inputs
        verify(createAccountUseCase).invoke(
            "test@example.com",
            "ValidPassword123!",
            "a".repeat(40),
            "b".repeat(40)
        )
    }

    @Test
    fun `test that mixed valid and invalid name states work correctly`() = runTest {
        // Valid first name, invalid last name
        underTest.onFirstNameInputChanged("John")
        underTest.onLastNameInputChanged("a".repeat(50))

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isTrue()
            assertThat(state.isFirstNameLengthExceeded).isFalse()
            assertThat(state.isLastNameValid).isFalse()
            assertThat(state.isLastNameLengthExceeded).isTrue()
        }

        // Invalid first name, valid last name
        underTest.onFirstNameInputChanged("b".repeat(50))
        underTest.onLastNameInputChanged("Doe")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirstNameValid).isFalse()
            assertThat(state.isFirstNameLengthExceeded).isTrue()
            assertThat(state.isLastNameValid).isTrue()
            assertThat(state.isLastNameLengthExceeded).isFalse()
        }
    }

    private suspend fun initInputFields() {
        underTest.onFirstNameInputChanged("first Name")
        underTest.onLastNameInputChanged("last Name")
        underTest.onEmailInputChanged("abc@mega.co.nz")
        underTest.onPasswordInputChanged("password")
        underTest.onConfirmPasswordInputChanged("password")
        underTest.termsOfServiceAgreedChanged(true)
    }
}