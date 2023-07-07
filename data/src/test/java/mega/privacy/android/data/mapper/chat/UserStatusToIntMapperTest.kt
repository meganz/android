package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.UserStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserStatusToIntMapperTest {

    private lateinit var underTest: UserStatusToIntMapper

    @BeforeAll
    fun setup() {
        underTest = UserStatusToIntMapper()
    }

    @ParameterizedTest(name = "test that UserStatusToIntMapper {0} returns {1}")
    @MethodSource("provideUserStatusParameter")
    fun `test that mapper returns correctly`(
        status: UserStatus,
        value: Int,
    ) {
        Truth.assertThat(underTest(status)).isEqualTo(value)
    }

    private fun provideUserStatusParameter() = Stream.of(
        Arguments.of(UserStatus.Offline, MegaChatApi.STATUS_OFFLINE),
        Arguments.of(UserStatus.Away, MegaChatApi.STATUS_AWAY),
        Arguments.of(UserStatus.Online, MegaChatApi.STATUS_ONLINE),
        Arguments.of(UserStatus.Invalid, MegaChatApi.STATUS_INVALID),
        Arguments.of(UserStatus.Busy, MegaChatApi.STATUS_BUSY),
    )
}