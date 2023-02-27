package test.mega.privacy.android.app.smsVerification.model.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapperImpl
import mega.privacy.android.domain.exception.SMSVerificationException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class SmsVerificationTextErrorMapperImplTest {
    private lateinit var underTest: SmsVerificationTextErrorMapper

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        underTest = SmsVerificationTextErrorMapperImpl(context = context)
    }


    @Test
    fun `test that SMSVerificationException LimitReached maps to verify_account_error_invalid_code`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationException.LimitReached(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.verify_account_error_reach_limit))
        }

    @Test
    fun `test that SMSVerificationException AlreadyVerified maps to verify_account_error_reach_limit`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationException.AlreadyVerified(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.verify_account_error_code_verified))
        }

    @Test
    fun `test that SMSVerificationException AlreadyExists maps to verify_account_error_code_verified`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationException.AlreadyExists(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.verify_account_error_phone_number_register))
        }

    @Test
    fun `test that SMSVerificationException VerificationCodeDoesNotMatch maps to verify_account_error_phone_number_register`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationException.VerificationCodeDoesNotMatch(
                        1,
                        null
                    )
                )
            ).isEqualTo(
                context.getString(
                    R.string.verify_account_error_wrong_code
                )
            )
        }

    @Test
    fun `test that SMSVerificationException Unknown maps to verify_account_error_wrong_code`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationException.Unknown(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.verify_account_error_invalid_code))
        }
}