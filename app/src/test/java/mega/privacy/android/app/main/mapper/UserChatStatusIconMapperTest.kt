package mega.privacy.android.app.main.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserChatStatusIconMapperTest {
    private val underTest = UserChatStatusIconMapper()

    @ParameterizedTest
    @MethodSource("provideUserChatStatusAndTheme")
    fun `test that mapper returns correct icon for given status and theme`(
        status: UserChatStatus,
        isDarkTheme: Boolean,
        expectedIcon: Int,
    ) {
        val result = underTest(status, isDarkTheme)
        Truth.assertThat(result).isEqualTo(expectedIcon)
    }

    private fun provideUserChatStatusAndTheme() = Stream.of(
        Arguments.of(UserChatStatus.Offline, true, R.drawable.ic_offline_dark_drawer),
        Arguments.of(UserChatStatus.Offline, false, R.drawable.ic_offline_light),
        Arguments.of(UserChatStatus.Away, true, R.drawable.ic_away_dark_drawer),
        Arguments.of(UserChatStatus.Away, false, R.drawable.ic_away_light),
        Arguments.of(UserChatStatus.Online, true, R.drawable.ic_online_dark_drawer),
        Arguments.of(UserChatStatus.Online, false, R.drawable.ic_online_light),
        Arguments.of(UserChatStatus.Busy, true, R.drawable.ic_busy_dark_drawer),
        Arguments.of(UserChatStatus.Busy, false, R.drawable.ic_busy_light),
        Arguments.of(UserChatStatus.Invalid, true, 0),
        Arguments.of(UserChatStatus.Invalid, false, 0)
    )
}