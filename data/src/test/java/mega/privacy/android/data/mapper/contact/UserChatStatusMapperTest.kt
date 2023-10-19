package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserChatStatusMapperTest {

    private lateinit var underTest: UserChatStatusMapper

    @BeforeEach
    fun setup() {
        underTest = UserChatStatusMapper()
    }

    @ParameterizedTest(name = " if status is {0} the mapped result is {1}")
    @MethodSource("provideTestParameters")
    fun `test that user chat status mapper returns correctly`(
        sdkUserChatStatus: Int,
        expectedUserChatStatus: UserChatStatus,
    ) {
        Truth.assertThat(underTest(sdkUserChatStatus)).isEqualTo(expectedUserChatStatus)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaChatApi.STATUS_OFFLINE, UserChatStatus.Offline),
        Arguments.of(MegaChatApi.STATUS_AWAY, UserChatStatus.Away),
        Arguments.of(MegaChatApi.STATUS_ONLINE, UserChatStatus.Online),
        Arguments.of(MegaChatApi.STATUS_BUSY, UserChatStatus.Busy),
        Arguments.of(MegaChatApi.STATUS_INVALID, UserChatStatus.Invalid),
        Arguments.of(-100, UserChatStatus.Invalid),
    )
}