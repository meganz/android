package mega.privacy.android.feature.chat.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class ReturnToCallBannerFormatTest {

    @Test
    fun `test that formatElapsed renders seconds under a minute as m ss`() {
        assertThat(5.seconds.formatElapsed()).isEqualTo("0:05")
    }

    @Test
    fun `test that formatElapsed renders minutes and seconds as m ss`() {
        assertThat(125.seconds.formatElapsed()).isEqualTo("2:05")
    }

    @Test
    fun `test that formatElapsed renders an hour or more as h mm ss`() {
        assertThat(3725.seconds.formatElapsed()).isEqualTo("1:02:05")
    }

    @Test
    fun `test that formatElapsed renders zero as m ss`() {
        assertThat(0.seconds.formatElapsed()).isEqualTo("0:00")
    }
}
