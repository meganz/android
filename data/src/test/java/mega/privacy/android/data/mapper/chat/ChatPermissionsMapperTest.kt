package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatPermissionsMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatPermissionsMapperTest {
    private lateinit var underTest: ChatPermissionsMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatPermissionsMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatRoom.PRIV_RM to ChatRoomPermission.Removed,
        MegaChatRoom.PRIV_RO to ChatRoomPermission.ReadOnly,
        MegaChatRoom.PRIV_STANDARD to ChatRoomPermission.Standard,
        MegaChatRoom.PRIV_MODERATOR to ChatRoomPermission.Moderator,
        MegaChatRoom.PRIV_UNKNOWN to ChatRoomPermission.Unknown,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that an unspecified value is mapped to an unknown chat room permission`() = runTest {
        // 50 is unspecified in the Mapper
        assertThat(underTest(50)).isEqualTo(ChatRoomPermission.Unknown)
    }
}