package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class ChatRoomTimestampMapperTest {

    private lateinit var underTest: ChatRoomTimestampMapper

    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        underTest = ChatRoomTimestampMapper(
            context = ApplicationProvider.getApplicationContext<Context>(),
        )
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `test that getMeetingTimeFormatted returns the meeting hour range`() {
        val start = Instant.parse("2022-05-01T10:00:00Z").epochSecond
        val end = Instant.parse("2022-05-01T11:30:00Z").epochSecond

        val actual = underTest.getMeetingTimeFormatted(start, end)

        assertThat(actual).isEqualTo("10:00am - 11:30am")
    }

    @Test
    fun `test that getLastTimeFormatted includes the date when older than a week`() {
        val timestamp = Instant.parse("2022-05-01T10:00:00Z").epochSecond

        val actual = underTest.getLastTimeFormatted(timestamp)

        assertThat(actual).contains("2022")
    }

    @Test
    fun `test that getLastTimeFormatted returns a relative label for a recent timestamp`() {
        val timestamp = Instant.now().epochSecond

        val actual = underTest.getLastTimeFormatted(timestamp)

        assertThat(actual).isNotEmpty()
        assertThat(actual).doesNotContain("2022")
    }
}
