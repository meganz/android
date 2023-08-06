package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatInitState
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatInitStateMapperTest {
    private lateinit var underTest: ChatInitStateMapper

    @BeforeEach
    fun setup() {
        underTest = ChatInitStateMapper()
    }

    @ParameterizedTest(name = "{0} should be mapped to {1}")
    @MethodSource("provideTestParameters")
    fun `test that mapper should map mega chat constants value to correct chat init state value type`(
        input: Int,
        output: ChatInitState,
    ) {
        assertThat(underTest(input)).isEqualTo(output)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaChatApi.INIT_ERROR, ChatInitState.ERROR),
        Arguments.of(MegaChatApi.INIT_NOT_DONE, ChatInitState.NOT_DONE),
        Arguments.of(MegaChatApi.INIT_WAITING_NEW_SESSION, ChatInitState.WAITING_NEW_SESSION),
        Arguments.of(MegaChatApi.INIT_OFFLINE_SESSION, ChatInitState.OFFLINE),
        Arguments.of(MegaChatApi.INIT_ONLINE_SESSION, ChatInitState.ONLINE),
        Arguments.of(MegaChatApi.INIT_ANONYMOUS, ChatInitState.ANONYMOUS),
        Arguments.of(MegaChatApi.INIT_TERMINATED, ChatInitState.TERMINATED),
        Arguments.of(MegaChatApi.INIT_NO_CACHE, ChatInitState.NO_CACHE)
    )
}