package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.call.CallNotificationType
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallNotificationMapperTest {
    private val underTest: CallNotificationMapper = CallNotificationMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(CallNotificationType::class)
    fun `test mapping is not null`(expected: CallNotificationType) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: CallNotificationType, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(CallNotificationType.Invalid, MegaChatCall.NOTIFICATION_TYPE_INVALID),
        Arguments.of(CallNotificationType.SFUError, MegaChatCall.NOTIFICATION_TYPE_SFU_ERROR),
        Arguments.of(CallNotificationType.SFUDeny, MegaChatCall.NOTIFICATION_TYPE_SFU_DENY),
    )
}