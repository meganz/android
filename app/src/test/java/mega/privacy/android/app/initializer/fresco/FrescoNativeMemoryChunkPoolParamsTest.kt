package mega.privacy.android.app.initializer.fresco

import com.facebook.common.util.ByteConstants
import com.google.common.truth.Truth
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrescoNativeMemoryChunkPoolParamsTest {

    @ParameterizedTest(name = "availableMemory {0}MB - maxMemory {1}MB")
    @MethodSource("provideTestParameters")
    fun `test that softCap is equal or smaller than hardCap when different values of memory are provided`(
        availableMemory: Float,
        maxMemory: Float,
    ) {
        val maxSizeCap = getMaxSizeCapPair(
            availableMemory = (availableMemory * ByteConstants.MB).toLong(),
            maxMemory = (maxMemory * ByteConstants.MB).toLong()
        )
        println("soft: ${maxSizeCap.soft.toFloat() / ByteConstants.MB} hard: ${maxSizeCap.hard.toFloat() / ByteConstants.MB}")
        Truth.assertThat(maxSizeCap.soft).isAtMost(maxSizeCap.hard)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1.5f, 1.5f),
        Arguments.of(1.5f, 3f),
        Arguments.of(3f, 3f),
        Arguments.of(3f, 32f),
        Arguments.of(6f, 6f),
        Arguments.of(8f, 8f),
        Arguments.of(8f, 16f),
        Arguments.of(16f, 16f),
        Arguments.of(16f, 32f),
        Arguments.of(32f, 32f),
        Arguments.of(128f, 128f),
    )
}
