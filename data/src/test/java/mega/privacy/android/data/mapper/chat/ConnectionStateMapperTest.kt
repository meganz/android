package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ConnectionState
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ConnectionStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConnectionStateMapperTest {
    private lateinit var underTest: ConnectionStateMapper

    @BeforeAll
    fun setUp() {
        underTest = ConnectionStateMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatApi.CONNECTED to ConnectionState.Connected,
        MegaChatApi.CONNECTING to ConnectionState.Connecting,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that an unspecified value is mapped to a disconnected connection state`() = runTest {
        // -1 is unspecified in the Mapper
        assertThat(underTest(-1)).isEqualTo(ConnectionState.Disconnected)
    }
}