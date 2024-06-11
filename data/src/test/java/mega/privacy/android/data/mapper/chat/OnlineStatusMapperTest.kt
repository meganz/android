package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.spy

/**
 * Test class for [OnlineStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnlineStatusMapperTest {
    private lateinit var underTest: OnlineStatusMapper

    private val userChatStatusMapper = spy(UserChatStatusMapper())

    @BeforeAll
    fun setUp() {
        underTest = OnlineStatusMapper(userChatStatusMapper)
    }

    @Test
    fun `test that the parameters are mapped into an online status`() = runTest {
        val userHandle = 123456L
        val status = MegaChatApi.STATUS_ONLINE
        val inProgress = false

        val expected = OnlineStatus(
            userHandle = userHandle,
            status = UserChatStatus.Online,
            inProgress = inProgress,
        )
        val actual = underTest(
            userHandle = userHandle,
            status = status,
            inProgress = inProgress,
        )

        assertThat(actual).isEqualTo(expected)
    }
}