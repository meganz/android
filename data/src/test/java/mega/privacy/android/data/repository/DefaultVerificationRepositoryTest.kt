package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.domain.entity.VerifiedPhoneNumber
import mega.privacy.android.domain.exception.SMSVerificationException
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaStringListMap
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultVerificationRepositoryTest {

    private lateinit var underTest: VerificationRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val countryCallingCodeMapper = mock<CountryCallingCodeMapper>()

    @Before
    fun setUp() {
        underTest = DefaultVerificationRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            appEventGateway = mock(),
            telephonyGateway = mock(),
            countryCallingCodeMapper = countryCallingCodeMapper,
            appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        )
    }

    @Test
    fun `test that get Country calling returns a successful result`() = runTest {
        val countryCode = "AD"
        val callingCode = "376"
        val expected = listOf("$countryCode:$callingCode,")

        val listMap = mock<MegaStringListMap>()
        whenever(countryCallingCodeMapper.invoke(listMap)).thenReturn(expected)

        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_COUNTRY_CALLING_CODES)
            on { megaStringListMap }.thenReturn(listMap)
        }

        whenever(megaApiGateway.getCountryCallingCodes(listener = any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }
        val actual = underTest.getCountryCallingCodes()
        Truth.assertThat(actual).isEqualTo(expected)
    }


    @Test(expected = SMSVerificationException.LimitReached::class)
    fun `test that send sms verification code finishes with limit reached exception when api returns API_ETEMPUNAVAIL error code`() =
        runTest {
            val phoneNumber = "12345678"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ETEMPUNAVAIL)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE)
            }

            whenever(
                megaApiGateway.sendSMSVerificationCode(
                    eq(phoneNumber),
                    eq(false),
                    listener = any(),
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            underTest.sendSMSVerificationCode(phoneNumber)
        }

    @Test(expected = SMSVerificationException.AlreadyVerified::class)
    fun `test that send sms verification code finishes with already verified exception when api returns API_EACCESS error code`() =
        runTest {
            val phoneNumber = "12345678"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EACCESS)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE)
            }

            whenever(
                megaApiGateway.sendSMSVerificationCode(
                    eq(phoneNumber),
                    eq(false),
                    listener = any(),
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            underTest.sendSMSVerificationCode(phoneNumber)
        }

    @Test(expected = SMSVerificationException.InvalidPhoneNumber::class)
    fun `test that send sms verification code finishes with invalid phone number exception when api returns API_EARGS error code`() =
        runTest {
            val phoneNumber = "12345678"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EARGS)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE)
            }

            whenever(
                megaApiGateway.sendSMSVerificationCode(
                    eq(phoneNumber),
                    eq(false),
                    listener = any(),
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            underTest.sendSMSVerificationCode(phoneNumber)
        }

    @Test(expected = SMSVerificationException.AlreadyExists::class)
    fun `test that send sms verification code finishes with already exists exception when api returns API_EEXIST error code`() =
        runTest {
            val phoneNumber = "12345678"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE)
            }

            whenever(
                megaApiGateway.sendSMSVerificationCode(
                    eq(phoneNumber),
                    eq(false),
                    listener = any(),
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            underTest.sendSMSVerificationCode(phoneNumber)
        }

    @Test
    fun `test that initial value is returned when monitoring verified phone number`() = runTest {
        val verifiedPhoneNumber = "123"
        megaApiGateway.stub {
            onBlocking { getVerifiedPhoneNumber() }.thenReturn(verifiedPhoneNumber)
        }
        underTest.monitorVerifiedPhoneNumber().test {
            assertThat(awaitItem()).isEqualTo(VerifiedPhoneNumber.PhoneNumber(verifiedPhoneNumber))
        }
    }

    @Test
    fun `test that verified phone number is updated when reset call returns`() = runTest {
        val verifiedPhoneNumber = "123"
        val okResponse = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        megaApiGateway.stub {
            onBlocking { getVerifiedPhoneNumber() }.thenReturn(verifiedPhoneNumber, null)
            on { resetSmsVerifiedPhoneNumber(any()) }.thenAnswer {
                (it.arguments[0] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    okResponse
                )
            }
        }

        underTest.monitorVerifiedPhoneNumber().test {
            assertThat(awaitItem()).isEqualTo(VerifiedPhoneNumber.PhoneNumber(verifiedPhoneNumber))
            underTest.resetSMSVerifiedPhoneNumber()
            assertThat(awaitItem()).isEqualTo(VerifiedPhoneNumber.NoVerifiedPhoneNumber)
        }
    }

    @Test
    fun `test that verified phone number is updated when verify phone number call returns`() =
        runTest {
            val verifiedPhoneNumber = "123"
            val okResponse = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
            megaApiGateway.stub {
                onBlocking { getVerifiedPhoneNumber() }.thenReturn(null, verifiedPhoneNumber)
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        okResponse
                    )
                }
            }

            underTest.monitorVerifiedPhoneNumber().test {
                assertThat(awaitItem()).isEqualTo(VerifiedPhoneNumber.NoVerifiedPhoneNumber)
                underTest.verifyPhoneNumber("12345")
                assertThat(awaitItem()).isEqualTo(
                    VerifiedPhoneNumber.PhoneNumber(
                        verifiedPhoneNumber
                    )
                )
            }
        }


    @Test(expected = SMSVerificationException.AlreadyVerified::class)
    fun `test that verify phone number finishes with AlreadyVerified exception if api returns code API_EEXPIRED`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXPIRED)
            }

            megaApiGateway.stub {
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        megaError
                    )
                }
            }

            underTest.verifyPhoneNumber("1234")

        }

    @Test(expected = SMSVerificationException.LimitReached::class)
    fun `test that verify phone number finishes with LimitReached exception if api returns code API_EACCESS`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EACCESS)
            }

            megaApiGateway.stub {
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        megaError
                    )
                }
            }

            underTest.verifyPhoneNumber("1234")

        }

    @Test(expected = SMSVerificationException.AlreadyExists::class)
    fun `test that verify phone number finishes with AlreadyExists exception if api returns code API_EEXIST`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EEXIST)
            }

            megaApiGateway.stub {
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        megaError
                    )
                }
            }

            underTest.verifyPhoneNumber("1234")

        }

    @Test(expected = SMSVerificationException.VerificationCodeDoesNotMatch::class)
    fun `test that verify phone number finishes with VerificationCodeDoesNotMatch exception if api returns code API_EFAILED`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            megaApiGateway.stub {
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        megaError
                    )
                }
            }

            underTest.verifyPhoneNumber("1234")

        }

    @Test(expected = SMSVerificationException.Unknown::class)
    fun `test that verify phone number finishes with Unknown exception if api returns unhandled code `() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_ETOOMANYCONNECTIONS)
            }

            megaApiGateway.stub {
                on { verifyPhoneNumber(any(), any()) }.thenAnswer {
                    (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                        mock(),
                        mock(),
                        megaError
                    )
                }
            }

            underTest.verifyPhoneNumber("1234")
        }

}
