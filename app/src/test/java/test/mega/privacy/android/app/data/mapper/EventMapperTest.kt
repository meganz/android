package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.mapper.toEvent
import mega.privacy.android.domain.entity.EventType
import nz.mega.sdk.MegaEvent
import org.junit.Test
import org.mockito.kotlin.mock

class EventMapperTest {

    @Test
    fun `test that event type can be mapped correctly`() {
        val expectedResults = HashMap<Int, EventType>().apply {
            put(MegaEvent.EVENT_COMMIT_DB, EventType.CommitDb)
            put(MegaEvent.EVENT_ACCOUNT_CONFIRMATION, EventType.AccountConfirmation)
            put(MegaEvent.EVENT_CHANGE_TO_HTTPS, EventType.ChangeToHttps)
            put(MegaEvent.EVENT_DISCONNECT, EventType.Disconnect)
            put(MegaEvent.EVENT_ACCOUNT_BLOCKED, EventType.AccountBlocked)
            put(MegaEvent.EVENT_STORAGE, EventType.Storage)
            put(MegaEvent.EVENT_NODES_CURRENT, EventType.NodesCurrent)
            put(MegaEvent.EVENT_MEDIA_INFO_READY, EventType.MediaInfoReady)
            put(MegaEvent.EVENT_STORAGE_SUM_CHANGED, EventType.StorageSumChanged)
            put(MegaEvent.EVENT_BUSINESS_STATUS, EventType.BusinessStatus)
            put(MegaEvent.EVENT_KEY_MODIFIED, EventType.KeyModified)
            put(MegaEvent.EVENT_MISC_FLAGS_READY, EventType.MiscFlagsReady)
            put(100, EventType.Unknown)  // invalid value
        }

        expectedResults.forEach { (key, value) ->
            val megaEvent = mock<MegaEvent> {
                on { handle }.thenReturn(12L)
                on { eventString }.thenReturn("eventString")
                on { number }.thenReturn(1L)
                on { text }.thenReturn("text")
                on { type }.thenReturn(key)
            }
            val actual = toEvent(megaEvent)
            assertThat(actual.type).isEqualTo(value)
        }

    }
}