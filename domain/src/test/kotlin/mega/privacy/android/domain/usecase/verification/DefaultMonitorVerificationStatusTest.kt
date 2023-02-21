package mega.privacy.android.domain.usecase.verification

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorVerificationStatusTest {
    private lateinit var underTest: MonitorVerificationStatus
    private val monitorVerifiedPhoneNumber = mock<MonitorVerifiedPhoneNumber>()
    private val monitorStorageStateEvent = mock<MonitorStorageStateEvent> {
        on { invoke() }.thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 123L,
                    eventString = "",
                    number = 123L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.Unknown
                )
            )
        )
    }

    private val verificationRepository = mock<VerificationRepository> {
        onBlocking { getSmsPermissions() }.thenReturn(
            emptyList()
        )
    }

    @Before
    fun setUp() {
        underTest = DefaultMonitorVerificationStatus(
            monitorVerifiedPhoneNumber = monitorVerifiedPhoneNumber,
            monitorStorageStateEvent = monitorStorageStateEvent,
            verificationRepository = verificationRepository,
        )
    }

    @Test
    fun `test that verified number is returned if found`() = runTest {
        val expectedPhoneNumber = VerifiedPhoneNumber.PhoneNumber("1234")
        monitorVerifiedPhoneNumber.stub {
            on { invoke() }.thenReturn(flow {
                emit(expectedPhoneNumber)
                awaitCancellation()
            })
        }

        underTest().test {
            assertThat(awaitItem().phoneNumber).isEqualTo(expectedPhoneNumber)
        }
    }

    @Test
    fun `test that state is unverified if no phone number returned`() = runTest {
        val expectedPhoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
        monitorVerifiedPhoneNumber.stub {
            on { invoke() }.thenReturn(flow {
                emit(expectedPhoneNumber)
                awaitCancellation()
            })
        }

        underTest().test {
            assertThat(awaitItem()).isInstanceOf(UnVerified::class.java)
        }
    }

    @Test
    fun `test that opt in and unblock are false if current storage state is paywall`() =
        runTest {
            val expectedPhoneNumber = VerifiedPhoneNumber.PhoneNumber("1234")
            monitorVerifiedPhoneNumber.stub {
                on { invoke() }.thenReturn(flow {
                    emit(expectedPhoneNumber)
                    awaitCancellation()
                })
            }

            monitorStorageStateEvent.stub {
                on { invoke() }.thenReturn(
                    MutableStateFlow(
                        StorageStateEvent(
                            handle = 123L,
                            eventString = "",
                            number = 123L,
                            text = "",
                            type = EventType.AccountBlocked,
                            storageState = StorageState.PayWall
                        )

                    )
                )
            }

            underTest().test {
                val status = awaitItem()
                assertThat(status.canRequestOptInVerification).isFalse()
                assertThat(status.canRequestUnblockSms).isFalse()
            }
        }


    @Test
    fun `test that canRequestUnblockSms is true if unblock permission is returned`() = runTest {
        val expectedPhoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
        monitorVerifiedPhoneNumber.stub {
            on { invoke() }.thenReturn(flow {
                emit(expectedPhoneNumber)
                awaitCancellation()
            })
        }

        verificationRepository.stub {
            onBlocking { getSmsPermissions() }.thenReturn(listOf(Unblock))
        }

        underTest().test {
            assertThat(awaitItem().canRequestUnblockSms).isTrue()
        }
    }

    @Test
    fun `test that canRequestUnblockSms is false if unblock permission is not returned`() =
        runTest {
            val expectedPhoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
            monitorVerifiedPhoneNumber.stub {
                on { invoke() }.thenReturn(flow {
                    emit(expectedPhoneNumber)
                    awaitCancellation()
                })
            }

            verificationRepository.stub {
                onBlocking { getSmsPermissions() }.thenReturn(emptyList())
            }

            underTest().test {
                assertThat(awaitItem().canRequestUnblockSms).isFalse()
            }
        }

    @Test
    fun `test that canRequestOptInVerification is true if verify permission is returned`() =
        runTest {
            val expectedPhoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
            monitorVerifiedPhoneNumber.stub {
                on { invoke() }.thenReturn(flow {
                    emit(expectedPhoneNumber)
                    awaitCancellation()
                })
            }

            verificationRepository.stub {
                onBlocking { getSmsPermissions() }.thenReturn(listOf(OptInVerification))
            }

            underTest().test {
                assertThat(awaitItem().canRequestOptInVerification).isTrue()
            }
        }

    @Test
    fun `test that canRequestOptInVerification is false if verify permission is not returned`() =
        runTest {
            val expectedPhoneNumber = VerifiedPhoneNumber.NoVerifiedPhoneNumber
            monitorVerifiedPhoneNumber.stub {
                on { invoke() }.thenReturn(flow {
                    emit(expectedPhoneNumber)
                    awaitCancellation()
                })
            }

            verificationRepository.stub {
                onBlocking { getSmsPermissions() }.thenReturn(emptyList())
            }

            underTest().test {
                assertThat(awaitItem().canRequestOptInVerification).isFalse()
            }
        }

    @Test
    fun `test that a new phone number updates the status`() = runTest {
        val unVerified = VerifiedPhoneNumber.NoVerifiedPhoneNumber
        val verified = VerifiedPhoneNumber.PhoneNumber("1234")
        val phoneNumberFlow = MutableStateFlow<VerifiedPhoneNumber>(unVerified)
        monitorVerifiedPhoneNumber.stub {
            on { invoke() }.thenReturn(
                phoneNumberFlow
            )
        }

        println("Before run")
        underTest().test {
            assertThat(awaitItem()).isInstanceOf(UnVerified::class.java)
            phoneNumberFlow.emit(verified)
            assertThat(awaitItem()).isInstanceOf(Verified::class.java)
        }


    }
}