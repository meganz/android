package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserChatStatusToIntMapperTest {

    private lateinit var underTest: UserStatusToIntMapper

    @BeforeAll
    fun setup() {
        underTest = UserStatusToIntMapper()
    }

    @ParameterizedTest(name = "test that UserStatusToIntMapper {0} returns {1}")
    @MethodSource("provideUserStatusParameter")
    fun `test that mapper returns correctly`(
        status: UserChatStatus,
        value: Int,
    ) {
        Truth.assertThat(underTest(status)).isEqualTo(value)
    }

    private fun provideUserStatusParameter() = Stream.of(
        Arguments.of(UserChatStatus.Offline, MegaChatApi.STATUS_OFFLINE),
        Arguments.of(UserChatStatus.Away, MegaChatApi.STATUS_AWAY),
        Arguments.of(UserChatStatus.Online, MegaChatApi.STATUS_ONLINE),
        Arguments.of(UserChatStatus.Invalid, MegaChatApi.STATUS_INVALID),
        Arguments.of(UserChatStatus.Busy, MegaChatApi.STATUS_BUSY),
    )
}