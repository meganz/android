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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.android.authentication.domain.usecase.regex.DoesTextContainMixedCaseUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainNumericUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainSpecialCharacterUseCase
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_CONFIRM_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_E2EE
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_EMAIL
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_LAST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_TERMS_OF_SERVICE
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val doesTextContainSpecialCharacterUseCase: DoesTextContainSpecialCharacterUseCase =
        mock()
    private val doesTextContainNumericUseCase: DoesTextContainNumericUseCase = mock()
    private val doesTextContainMixedCaseUseCase: DoesTextContainMixedCaseUseCase = mock()
    private val savedStateHandle = SavedStateHandle()
    private val connectivityFlow = MutableStateFlow(true)

    private lateinit var underTest: CreateAccountViewModel
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val applicationScope: CoroutineScope = CoroutineScope(testDispatcher)

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
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            applicationScope = applicationScope,
            doesTextContainSpecialCharacterUseCase = doesTextContainSpecialCharacterUseCase,
            doesTextContainNumericUseCase = doesTextContainNumericUseCase,
            doesTextContainMixedCaseUseCase = doesTextContainMixedCaseUseCase,
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
            getFeatureFlagValueUseCase,
            doesTextContainSpecialCharacterUseCase,
            doesTextContainNumericUseCase,
            doesTextContainMixedCaseUseCase,
        )
    }


    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that ui state is updated based on value from RegistrationRevamp feature flag on init`(
        value: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
        initViewModel()
        underTest.uiState.test {
            assertThat(awaitItem().isNewRegistrationUiEnabled).isEqualTo(value)
        }
    }

    @Test
    fun `test that first name is saved to savedStateHandle and ui state is reset when onFirstNameInputChanged is called`() =
        runTest {
            underTest.onFirstNameInputChanged("first Name")

            assertThat(savedStateHandle.get<String>(KEY_FIRST_NAME)).isEqualTo("first Name")

            underTest.uiState.test {
                assertThat(awaitItem().isFirstNameValid).isNull()
            }
        }

    @Test
    fun `test that last name is saved to savedStateHandle and ui state is reset when onLastNameInputChanged is called`() =
        runTest {
            underTest.onLastNameInputChanged("last Name")

            assertThat(savedStateHandle.get<String>(KEY_LAST_NAME)).isEqualTo("last Name")

            underTest.uiState.test {
                assertThat(awaitItem().isLastNameValid).isNull()
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
    fun `test that end to end encryption is saved to savedStateHandle and ui state is set when e2eeAgreed is checked`() =
        runTest {
            underTest.e2eeAgreedChanged(true)

            assertThat(savedStateHandle.get<Boolean>(KEY_E2EE)).isTrue()

            underTest.uiState.test {
                assertThat(awaitItem().isE2EEAgreed).isTrue()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that password is saved to savedStateHandle when onPasswordInputChanged is called`(
        value: Boolean,
    ) =
        runTest {
            val password = "password"
            val passwordStrength = PasswordStrength.GOOD
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(getPasswordStrengthUseCase(password)).thenReturn(passwordStrength)
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(true)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(true)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(true)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(true)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(true)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)

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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)

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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)

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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)

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
    fun `test that create account is not invoked when end to end encryption is not agreed for legacy design`() =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(false)

            initViewModel()
            initInputFields()

            underTest.e2eeAgreedChanged(false)

            underTest.createAccount()

            verifyNoInteractions(createAccountUseCase)

            underTest.uiState.test {
                val item = awaitItem()
                assertThat(item.isE2EEAgreed).isFalse()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account is not invoked when there is no internet`(value: Boolean) =
        runTest {
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)

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


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account success event is triggered when account creation is successful`(
        value: Boolean,
    ) =
        runTest {
            val credentials = mock<EphemeralCredentials>()
            whenever(getPasswordStrengthUseCase(any())).thenReturn(PasswordStrength.GOOD)
            whenever(createAccountUseCase(any(), any(), any(), any())).thenReturn(credentials)
            whenever(isEmailValidUseCase(any())).thenReturn(true)
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account error event is triggered when account creation throws Unknown error`(
        value: Boolean,
    ) =
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that create account error event is triggered when account creation throws AccountAlreadyExists error`(
        value: Boolean,
    ) =
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)).thenReturn(value)
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

    private suspend fun initInputFields() {
        underTest.onFirstNameInputChanged("first Name")
        underTest.onLastNameInputChanged("last Name")
        underTest.onEmailInputChanged("abc@mega.co.nz")
        underTest.onPasswordInputChanged("password")
        underTest.onConfirmPasswordInputChanged("password")
        underTest.termsOfServiceAgreedChanged(true)
        underTest.e2eeAgreedChanged(true)
    }
}