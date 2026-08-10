package mega.privacy.android.app.presentation.login.mapper

import android.content.Context
import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountBlockedTypeStringMapperTest {

    private lateinit var underTest: AccountBlockedTypeStringMapper

    private val context: Context = mock()

    @BeforeAll
    fun setUp() {
        underTest = AccountBlockedTypeStringMapper(context = context)
    }

    @ParameterizedTest(name = "when type is {0}")
    @MethodSource("provideMappedTypes")
    fun `test that mapper returns context string for known blocked types`(
        type: AccountBlockedType,
        @StringRes stringRes: Int,
        localizedMessage: String,
    ) {
        whenever(context.getString(stringRes)).thenReturn(localizedMessage)
        val event = AccountBlockedEvent(
            handle = 1L,
            type = type,
            text = "ignored server text",
        )

        val actual = underTest(event)

        assertThat(actual).isEqualTo(localizedMessage)
    }

    @ParameterizedTest(name = "when type is {0}")
    @MethodSource("provideFallbackTypes")
    fun `test that mapper returns event text for types without string resource mapping`(
        type: AccountBlockedType,
        eventText: String,
    ) {
        val event = AccountBlockedEvent(
            handle = -1L,
            type = type,
            text = eventText,
        )

        val actual = underTest(event)

        assertThat(actual).isEqualTo(eventText)
    }

    private fun provideMappedTypes() = Stream.of(
        Arguments.of(
            AccountBlockedType.TOS_COPYRIGHT,
            sharedR.string.dialog_account_suspended_ToS_copyright_message,
            "ToS copyright",
        ),
        Arguments.of(
            AccountBlockedType.TOS_NON_COPYRIGHT,
            sharedR.string.dialog_account_suspended_ToS_non_copyright_message,
            "ToS non-copyright",
        ),
        Arguments.of(
            AccountBlockedType.SUBUSER_DISABLED,
            sharedR.string.error_business_disabled,
            "Business disabled",
        ),
        Arguments.of(
            AccountBlockedType.VERIFICATION_EMAIL,
            sharedR.string.login_account_suspension_email_verification_message,
            "Verify email",
        ),
    )

    private fun provideFallbackTypes() = Stream.of(
        Arguments.of(AccountBlockedType.NOT_BLOCKED, ""),
        Arguments.of(AccountBlockedType.SUBUSER_REMOVED, "removed message"),
        Arguments.of(AccountBlockedType.VERIFICATION_SMS, "sms verification"),
    )
}
