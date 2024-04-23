package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatPresenceConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatPresenceConfigMapperTest {

    private lateinit var userChatStatusMapper: UserChatStatusMapper

    private lateinit var underTest: ChatPresenceConfigMapper

    @BeforeEach
    fun setUp() {
        userChatStatusMapper = UserChatStatusMapper()
        underTest = ChatPresenceConfigMapper(
            userChatStatusMapper = userChatStatusMapper
        )
    }

    @ParameterizedTest
    @ValueSource(
        ints = [
            MegaChatApi.STATUS_OFFLINE,
            MegaChatApi.STATUS_AWAY,
            MegaChatApi.STATUS_ONLINE,
            MegaChatApi.STATUS_BUSY,
            MegaChatApi.STATUS_INVALID
        ]
    )
    fun `test that the mega chat presence config is successfully mapped to the chat presence config domain entity`(
        megaOnlineStatus: Int,
    ) {
        val megaChatPresenceConfig = mock<MegaChatPresenceConfig> {
            on { onlineStatus } doReturn megaOnlineStatus
            on { isAutoawayEnabled } doReturn true
            on { autoawayTimeout } doReturn 1L
            on { isPersist } doReturn true
            on { isPending } doReturn true
            on { isLastGreenVisible } doReturn true
        }

        val actual = underTest(megaChatPresenceConfig)

        val expected = ChatPresenceConfig(
            onlineStatus = userChatStatusMapper(megaChatPresenceConfig.onlineStatus),
            isAutoAwayEnabled = megaChatPresenceConfig.isAutoawayEnabled,
            autoAwayTimeout = megaChatPresenceConfig.autoawayTimeout,
            isPersist = megaChatPresenceConfig.isPersist,
            isPending = megaChatPresenceConfig.isPending,
            isLastGreenVisible = megaChatPresenceConfig.isLastGreenVisible
        )
        assertThat(actual).isEqualTo(expected)
    }
}
