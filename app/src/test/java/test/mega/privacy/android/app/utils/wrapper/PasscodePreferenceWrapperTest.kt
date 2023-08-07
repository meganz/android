package test.mega.privacy.android.app.utils.wrapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PIN_4
import mega.privacy.android.app.utils.Constants.PIN_6
import mega.privacy.android.app.utils.Constants.PIN_ALPHANUMERIC
import mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.PasscodeUtil.Companion.REQUIRE_PASSCODE_IMMEDIATE
import mega.privacy.android.app.utils.wrapper.PasscodePreferenceWrapper
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.extensions.asHotFlow

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasscodePreferenceWrapperTest {
    private lateinit var underTest: PasscodePreferenceWrapper

    private val databaseHandler = mock<DatabaseHandler?>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorPasscodeLockPreferenceUseCase = mock<MonitorPasscodeLockPreferenceUseCase?>()
    private val passcodeRepository = mock<PasscodeRepository?>()
    private val accountRepository = mock<AccountRepository?>()


    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodePreferenceWrapper(
            databaseHandler = databaseHandler,
            ioDispatcher = StandardTestDispatcher(),
            megaApi = megaApiGateway,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorPasscodeLockPreferenceUseCase = monitorPasscodeLockPreferenceUseCase,
            passcodeRepository = passcodeRepository,
            accountRepository = accountRepository,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Legacy {
        @BeforeAll
        fun initialise() {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(AppFeatures.PasscodeBackend) }.thenReturn(
                    false
                )
            }
        }

        @AfterAll
        fun cleanUp() {
            Mockito.reset(getFeatureFlagValueUseCase)
        }

        @AfterEach
        fun reset() {
            resetMocks()
        }

        @Test
        internal fun `test that lock enabled returns preference value`() = runTest {
            val expected = true
            val megaPreferences = mock<MegaPreferences> {
                on { passcodeLockEnabled }.thenReturn(expected.toString())
            }
            databaseHandler.stub { on { preferences }.thenReturn(megaPreferences) }

            assertThat(underTest.isPasscodeEnabled()).isEqualTo(expected)
        }

        @Test
        internal fun `test that get passcode returns preference value`() = runTest {
            val expected = "passcode"
            val megaPreferences = mock<MegaPreferences> {
                on { passcodeLockCode }.thenReturn(expected)
            }
            databaseHandler.stub { on { preferences }.thenReturn(megaPreferences) }

            assertThat(underTest.getPasscode()).isEqualTo(expected)
        }

        @Test
        internal fun `test that passcode timeout returns database handler value`() = runTest {
            val expected = 12
            databaseHandler.stub {
                on { passcodeRequiredTime }.thenReturn(expected)
            }

            assertThat(underTest.getPasscodeTimeOut()).isEqualTo(expected)
        }

        @Test
        internal fun `test that passcode timeout is set on database handler`() = runTest {
            val expected = 44
            underTest.setPasscodeTimeOut(expected)
            verify(databaseHandler).passcodeRequiredTime = expected
        }

        @Test
        internal fun `test that enable biometrics value is set on the database handler`() =
            runTest {
                val expected = true
                underTest.setFingerprintLockEnabled(expected)
                verify(databaseHandler).isFingerprintLockEnabled = expected
            }

        @Test
        internal fun `test that database handler value is returned for is fingerprint enabled`() =
            runTest {
                val expected = true
                databaseHandler.stub {
                    on { isFingerprintLockEnabled }.thenReturn(expected)
                }

                assertThat(underTest.isFingerPrintLockEnabled()).isEqualTo(expected)
            }

        @Test
        internal fun `test that database handler value is updated when setting passcode enabled`() =
            runTest {
                val expected = true
                underTest.setPasscodeEnabled(expected)

                verify(databaseHandler).isPasscodeLockEnabled = expected
            }

        @Test
        internal fun `test that database handler value is updated when setting passcode type`() =
            runTest {
                val expected = Constants.PIN_6
                underTest.setPasscodeLockType(expected)

                verify(databaseHandler).passcodeLockType = expected
            }

        @Test
        internal fun `test that database handler value is updated when passcode is set`() =
            runTest {
                val expected = "1234"
                underTest.setPasscode(expected)

                verify(databaseHandler).passcodeLockCode = expected
            }

        @Test
        internal fun `test that database attributes attempts are returned for failed attempts`() =
            runTest {
                val expected = 144
                val megaAttributes = mock<MegaAttributes> {
                    on { attempts }.thenReturn(expected)
                }
                databaseHandler.stub {
                    on { attributes }.thenReturn(megaAttributes)
                }

                assertThat(underTest.getFailedAttemptsCount()).isEqualTo(expected)
            }

        @Test
        internal fun `test that attributes value is set when setting failed attempts`() = runTest {
            val expected = 411

            underTest.setFailedAttemptsCount(expected)

            verify(databaseHandler).setAttrAttempts(expected)
        }

        @Test
        internal fun `test that preference value is returned when fetching passcode type`() =
            runTest {
                val expected = PIN_6
                val megaPreferences = mock<MegaPreferences> {
                    on { passcodeLockType }.thenReturn(expected)
                }

                databaseHandler.stub {
                    on { preferences }.thenReturn(megaPreferences)
                }

                assertThat(underTest.getPasscodeType()).isEqualTo(expected)
            }

        @Test
        internal fun `test that default of pin 4 is returned if preferences type is null`() =
            runTest {
                val megaPreferences = mock<MegaPreferences> {
                    on { passcodeLockType }.thenReturn(null)
                }

                databaseHandler.stub {
                    on { preferences }.thenReturn(megaPreferences)
                }

                assertThat(underTest.getPasscodeType()).isEqualTo(PIN_4)
            }

        @Test
        internal fun `test that password is checked against the mega api gateway`() = runTest {
            val expected = "A password, huzzah!"

            underTest.checkPassword(expected)

            verify(megaApiGateway).isCurrentPassword(expected)
        }
    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NewImplementation {
        @BeforeAll
        fun initialise() {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(AppFeatures.PasscodeBackend) }.thenReturn(
                    true
                )
            }
        }

        @AfterAll
        fun cleanUp() {
            Mockito.reset(getFeatureFlagValueUseCase)
        }

        @AfterEach
        fun reset() {
            resetMocks()
        }

        @Test
        internal fun `test that lock enabled returns monitor use case value`() = runTest {
            val expected = true
            monitorPasscodeLockPreferenceUseCase.stub {
                on { invoke() }.thenReturn(expected.asHotFlow())
            }

            assertThat(underTest.isPasscodeEnabled()).isEqualTo(expected)
        }

        @Test
        internal fun `test that get passcode returns passcode repository value`() = runTest {
            val expected = "PasscodeRepoPasscode"
            passcodeRepository.stub {
                onBlocking { getPasscode() }.thenReturn(expected)
            }

            assertThat(underTest.getPasscode()).isEqualTo(expected)
        }

        @TestFactory
        fun `test that passcode timeout is returned from repository and mapped correctly`() =
            mapOf(
                PasscodeTimeout.Immediate to REQUIRE_PASSCODE_IMMEDIATE,
                PasscodeTimeout.TimeSpan(PasscodeUtil.REQUIRE_PASSCODE_AFTER_1M.toLong()) to PasscodeUtil.REQUIRE_PASSCODE_AFTER_1M,
                null to Constants.REQUIRE_PASSCODE_INVALID,
            ).map { (input, expected) ->
                DynamicTest.dynamicTest("test that $input returns $expected") {
                    passcodeRepository.stub {
                        on { monitorPasscodeTimeOut() }.thenReturn(input.asHotFlow())
                    }
                    runTest { assertThat(underTest.getPasscodeTimeOut()).isEqualTo(expected) }
                }
            }

        @TestFactory
        fun `test that passcode timeout is mapped correctly and passed to passcode repository`() =
            mapOf(
                REQUIRE_PASSCODE_IMMEDIATE to PasscodeTimeout.Immediate,
                PasscodeUtil.REQUIRE_PASSCODE_AFTER_1M to PasscodeTimeout.TimeSpan(PasscodeUtil.REQUIRE_PASSCODE_AFTER_1M.toLong()),
                REQUIRE_PASSCODE_INVALID to PasscodeTimeout.TimeSpan(PasscodeUtil.REQUIRE_PASSCODE_AFTER_30S.toLong())
            ).map { (input, expected) ->
                DynamicTest.dynamicTest("test that $input returns $expected") {
                    runTest {
                        underTest.setPasscodeTimeOut(input)
                        verify(passcodeRepository).setPasscodeTimeOut(expected)
                    }
                }
            }

        @Test
        internal fun `test that an exception is thrown when enabling biometrics without a fallback`() =
            runTest {
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(null.asHotFlow<PasscodeType?>())
                }

                assertThrows<IllegalStateException> { underTest.setFingerprintLockEnabled(true) }
            }

        @Test
        internal fun `test that current type is wrapped in biometric type if fingerprints are enabled`() =
            runTest {
                val current = PasscodeType.Password
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(current.asHotFlow())
                }

                underTest.setFingerprintLockEnabled(true)

                verify(passcodeRepository).setPasscodeType(PasscodeType.Biometric(current))
            }

        @Test
        internal fun `test that biometric type is unwrapped if fingerprints are disabled`() =
            runTest {
                val fallback = PasscodeType.Password
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(
                        PasscodeType.Biometric(fallback).asHotFlow()
                    )
                }

                underTest.setFingerprintLockEnabled(false)

                verify(passcodeRepository).setPasscodeType(fallback)
            }

        @Test
        internal fun `test that is fingerprint enabled returns true if current type is biometric`() =
            runTest {
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(
                        PasscodeType.Biometric(
                            PasscodeType.Pin(
                                4
                            )
                        ).asHotFlow()
                    )
                }

                assertThat(underTest.isFingerPrintLockEnabled()).isTrue()
            }

        @Test
        internal fun `test that is fingerprint enabled returns false if current type is not biometric`() =
            runTest {
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(
                        PasscodeType.Pin(4).asHotFlow()
                    )
                }

                assertThat(underTest.isFingerPrintLockEnabled()).isFalse()
            }

        @Test
        internal fun `test that is fingerprint enabled returns false if current type is null`() =
            runTest {
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(
                        null.asHotFlow<PasscodeType?>()
                    )
                }

                assertThat(underTest.isFingerPrintLockEnabled()).isFalse()
            }

        @Test
        internal fun `test that repository value is set when passcode lock is enabled`() = runTest {
            val expected = true
            underTest.setPasscodeEnabled(expected)

            verify(passcodeRepository).setPasscodeEnabled(expected)
        }

        @TestFactory
        internal fun `test that passcode type string is mapped and set on passcode repository`(): List<DynamicTest> {
            passcodeRepository.stub { on { monitorPasscodeType() }.thenReturn(null.asHotFlow<PasscodeType?>()) }
            return mapOf(
                PIN_4 to PasscodeType.Pin(4),
                PIN_6 to PasscodeType.Pin(6),
                PIN_ALPHANUMERIC to PasscodeType.Password
            ).map { (input, expected) ->

                DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                    runTest {
                        underTest.setPasscodeLockType(input)

                        verify(passcodeRepository).setPasscodeType(expected)
                    }
                }
            }
        }

        @TestFactory
        internal fun `test that passcode type string is mapped as a Biometric type and set on passcode repository if biometrics are enabled`(): List<DynamicTest> {
            passcodeRepository.stub {
                on { monitorPasscodeType() }.thenReturn(
                    PasscodeType.Biometric(
                        PasscodeType.Pin(22)
                    ).asHotFlow()
                )
            }
            return mapOf(
                PIN_4 to PasscodeType.Pin(4),
                PIN_6 to PasscodeType.Pin(6),
                PIN_ALPHANUMERIC to PasscodeType.Password
            ).map { (input, expected) ->

                DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                    runTest {
                        underTest.setPasscodeLockType(input)

                        verify(passcodeRepository).setPasscodeType(PasscodeType.Biometric(expected))
                    }
                }
            }
        }

        @Test
        internal fun `test that pin 4 is set as default type if unknown string is passed`() =
            runTest {
                passcodeRepository.stub { on { monitorPasscodeType() }.thenReturn(null.asHotFlow<PasscodeType?>()) }

                underTest.setPasscodeLockType("That's no passcode!")

                verify(passcodeRepository).setPasscodeType(PasscodeType.Pin(4))
            }

        @Test
        internal fun `test that repository value is set when passcode is set`() = runTest {
            val expected = "4321"

            underTest.setPasscode(expected)

            verify(passcodeRepository).setPasscode(expected)
        }

        @Test
        internal fun `test that repository value is returned when getting failed attempts count`() =
            runTest {
                val expected = 78
                passcodeRepository.stub {
                    on { monitorFailedAttempts() }.thenReturn(expected.asHotFlow())
                }

                assertThat(underTest.getFailedAttemptsCount()).isEqualTo(expected)
            }

        @Test
        internal fun `test that 0 is returned if failed attempts are null`() = runTest {
            passcodeRepository.stub {
                on { monitorFailedAttempts() }.thenReturn(null.asHotFlow<Int?>())
            }

            assertThat(underTest.getFailedAttemptsCount()).isEqualTo(0)
        }

        @Test
        internal fun `test that repository value is set when setting failed attempts count`() =
            runTest {
                val expected = 66

                underTest.setFailedAttemptsCount(expected)

                verify(passcodeRepository).setFailedAttempts(expected)
            }

        @TestFactory
        internal fun `test that passcode type is mapped and returned as a string when fetching passcode type`() =
            mapOf(
                PasscodeType.Password to PIN_ALPHANUMERIC,
                PasscodeType.Pin(4) to PIN_4,
                PasscodeType.Pin(6) to PIN_6,
            ).map { (input, expected) ->
                DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                    passcodeRepository.stub {
                        on { monitorPasscodeType() }.thenReturn(input.asHotFlow())
                    }
                    runTest {
                        assertThat(underTest.getPasscodeType()).isEqualTo(expected)
                    }
                }
            }

        @TestFactory
        internal fun `test that biometric passcode type is mapped and returned as a string when fetching passcode type`() =
            mapOf(
                PasscodeType.Password to PIN_ALPHANUMERIC,
                PasscodeType.Pin(4) to PIN_4,
                PasscodeType.Pin(6) to PIN_6,
            ).map { (input, expected) ->
                DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                    passcodeRepository.stub {
                        on { monitorPasscodeType() }.thenReturn(
                            PasscodeType.Biometric(input).asHotFlow()
                        )
                    }
                    runTest {
                        assertThat(underTest.getPasscodeType()).isEqualTo(expected)
                    }
                }
            }

        @Test
        internal fun `test that pin 4 is returned as default when passcode type is null`() =
            runTest {
                passcodeRepository.stub {
                    on { monitorPasscodeType() }.thenReturn(null.asHotFlow<PasscodeType?>())
                }

                assertThat(underTest.getPasscodeType()).isEqualTo(PIN_4)
            }

        @Test
        internal fun `test that password is checked against the account repository`() = runTest {
            val expected = "A password, huzzah!"

            underTest.checkPassword(expected)

            verify(accountRepository).isCurrentPassword(expected)
        }
    }


    private fun resetMocks() {
        Mockito.reset(
            databaseHandler,
            megaApiGateway,
            monitorPasscodeLockPreferenceUseCase,
            passcodeRepository,
            accountRepository,
        )

        Mockito.clearInvocations(
            databaseHandler,
            megaApiGateway,
            monitorPasscodeLockPreferenceUseCase,
            passcodeRepository,
            accountRepository,
        )
    }

}