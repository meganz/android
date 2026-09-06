package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatRoomItem
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

    @Test
    fun `test that getHeaderTimeFormatted returns the past header for the first non-pending meeting`() {
        val current = meeting(scheduledStart = null, isPending = false)
        val previous = meeting(scheduledStart = pendingStart, isPending = true)

        val actual = underTest.getHeaderTimeFormatted(current, previous)

        assertThat(actual).isEqualTo("Past meetings")
    }

    @Test
    fun `test that getHeaderTimeFormatted returns the date header for a pending meeting on a new day`() {
        val current = meeting(scheduledStart = pendingStart, isPending = true)

        val actual = underTest.getHeaderTimeFormatted(current, previousItem = null)

        assertThat(actual).isEqualTo("Sunday, 1 May")
    }

    @Test
    fun `test that getHeaderTimeFormatted returns today for a pending meeting scheduled today`() {
        val todayStart = Instant.now().epochSecond
        val current = meeting(scheduledStart = todayStart, isPending = true)

        val actual = underTest.getHeaderTimeFormatted(current, previousItem = null)

        assertThat(actual).isEqualTo("Today")
    }

    @Test
    fun `test that getHeaderTimeFormatted returns null for consecutive pending meetings on the same day`() {
        val current = meeting(scheduledStart = pendingStart + 3600, isPending = true)
        val previous = meeting(scheduledStart = pendingStart, isPending = true)

        val actual = underTest.getHeaderTimeFormatted(current, previous)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that getHeaderTimeFormatted returns null for a pending meeting with no scheduled start`() {
        val current = meeting(scheduledStart = null, isPending = true)

        val actual = underTest.getHeaderTimeFormatted(current, previousItem = null)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that getHeaderTimeFormatted returns null for consecutive non-pending meetings`() {
        val current = meeting(scheduledStart = null, isPending = false)
        val previous = meeting(scheduledStart = null, isPending = false)

        val actual = underTest.getHeaderTimeFormatted(current, previous)

        assertThat(actual).isNull()
    }

    private fun meeting(
        scheduledStart: Long?,
        isPending: Boolean,
    ): ChatRoomItem = ChatRoomItem.MeetingChatRoomItem(
        chatId = 1L,
        title = "Meeting",
        isPending = isPending,
        scheduledStartTimestamp = scheduledStart,
    )

    private companion object {
        val pendingStart: Long = Instant.parse("2022-05-01T10:00:00Z").epochSecond
    }
}
