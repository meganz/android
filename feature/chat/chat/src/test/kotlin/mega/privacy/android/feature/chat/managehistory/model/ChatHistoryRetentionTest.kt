package mega.privacy.android.feature.chat.managehistory.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Periods are passed as whole seconds rather than as [kotlin.time.Duration]: Duration is an inline
 * value class, so it erases to `long` in the test method signature and JUnit's parameter resolver
 * cannot convert the boxed argument back.
 */
class ChatHistoryRetentionTest {

    @ParameterizedTest(name = "{1} seconds is {0}")
    @MethodSource("presetSeconds")
    fun `test that a preset carries the period it stands for`(
        retention: ChatHistoryRetention,
        expectedSeconds: Long,
    ) {
        assertThat(retention.duration).isEqualTo(expectedSeconds.seconds)
    }

    @ParameterizedTest(name = "{0} seconds resolves to {1}")
    @MethodSource("secondsPresets")
    fun `test that from returns the preset a period stands for`(
        seconds: Long,
        expected: ChatHistoryRetention,
    ) {
        assertThat(ChatHistoryRetention.from(seconds.seconds)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} seconds is custom")
    @MethodSource("customSeconds")
    fun `test that from returns Custom carrying the period when it matches no preset`(
        seconds: Long,
    ) {
        val retention = ChatHistoryRetention.from(seconds.seconds)

        assertThat(retention).isInstanceOf(ChatHistoryRetention.Custom::class.java)
        assertThat(retention.duration).isEqualTo(seconds.seconds)
    }

    @Test
    fun `test that a zero period is Disabled rather than a custom period of zero`() {
        assertThat(ChatHistoryRetention.from(Duration.ZERO)).isEqualTo(ChatHistoryRetention.Disabled)
    }

    @ParameterizedTest(name = "{0} converts to {1} seconds for the use case")
    @MethodSource("presetSeconds")
    fun `test that the period converts to the seconds the use case expects`(
        retention: ChatHistoryRetention,
        expectedSeconds: Long,
    ) {
        assertThat(retention.duration.inWholeSeconds).isEqualTo(expectedSeconds)
    }

    companion object {

        @JvmStatic
        fun presetSeconds(): Stream<Arguments> = Stream.of(
            Arguments.of(ChatHistoryRetention.Disabled, 0L),
            Arguments.of(ChatHistoryRetention.OneDay, 86_400L),
            Arguments.of(ChatHistoryRetention.OneWeek, 604_800L),
            Arguments.of(ChatHistoryRetention.OneMonth, 2_592_000L),
        )

        @JvmStatic
        fun secondsPresets(): Stream<Arguments> = Stream.of(
            Arguments.of(0L, ChatHistoryRetention.Disabled),
            Arguments.of(86_400L, ChatHistoryRetention.OneDay),
            Arguments.of(604_800L, ChatHistoryRetention.OneWeek),
            Arguments.of(2_592_000L, ChatHistoryRetention.OneMonth),
        )

        @JvmStatic
        fun customSeconds(): Stream<Arguments> = Stream.of(
            Arguments.of(3_600L),
            Arguments.of(172_800L),
            Arguments.of(31_536_000L),
        )
    }
}
