package test.mega.privacy.android.app.smsVerification.model.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapperImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SMSVerificationTextMapperImplTest {

    private lateinit var underTest: SMSVerificationTextMapper

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        underTest = SMSVerificationTextMapperImpl(context = context)
    }

    @Test
    fun `test that when user is locked info text is correctly mapped`() =
        runTest {
            assertThat(
                underTest(SMSVerificationUIState(isUserLocked = true)).infoText
            ).isEqualTo(context.getString(R.string.verify_account_helper_locked))
        }

    @Test
    fun `test that when user is not locked info text is correctly mapped`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationUIState(
                        isUserLocked = false,
                    )
                ).infoText
            ).isEqualTo(
                context.getString(R.string.sms_add_phone_number_dialog_msg_non_achievement_user)
            )
        }

    @Test
    fun `test that when user is locked header text is correctly mapped`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationUIState(
                        isUserLocked = true,
                    )
                ).headerText
            ).isEqualTo(
                context.getString(R.string.verify_account_title)
            )
        }

    @Test
    fun `test that when user is not locked header text is correctly mapped`() =
        runTest {
            assertThat(
                underTest(
                    SMSVerificationUIState(
                        isUserLocked = false,
                    )
                ).headerText
            ).isEqualTo(
                context.getString(R.string.add_phone_number_label)
            )
        }

    @Test
    fun `test that when when country codes are selected country code text is correctly mapped`() =
        runTest {
            val countryName = "A"
            val countryDialCode = "+1"
            val countryCode = "a"
            assertThat(
                underTest(
                    SMSVerificationUIState(
                        selectedCountryCode = countryCode,
                        selectedCountryName = countryName,
                        selectedDialCode = countryDialCode
                    )
                ).countryCodeText
            ).isEqualTo(
                "$countryName ($countryDialCode)"
            )
        }

    @Test
    fun `test that when when country codes are not selected country code text is correctly mapped`() =
        runTest {
            val countryName = "A"
            val countryCode = "+1"
            assertThat(
                underTest(
                    SMSVerificationUIState(
                        selectedCountryName = countryName,
                        selectedDialCode = countryCode
                    )
                ).countryCodeText
            ).isEqualTo(
                context.getString(R.string.sms_region_label)
            )
        }
}
