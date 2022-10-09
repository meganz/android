package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.NormalEvent
import mega.privacy.android.domain.entity.StorageStateEvent
import nz.mega.sdk.MegaEvent
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class EventMapperTest {

    private val megaEvent = mock<MegaEvent> {
        on { handle }.thenReturn(12L)
        on { eventString }.thenReturn("eventString")
        on { number }.thenReturn(1L)
        on { text }.thenReturn("text")
    }

    @Test
    fun `test that event type can be mapped correctly`() {
        val unknownEventType = 100
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
            put(unknownEventType, EventType.Unknown)  // invalid value
        }

        expectedResults.forEach { (key, value) ->
            megaEvent.stub {
                on { type }.thenReturn(key)
            }
            val actual = toEvent(megaEvent)
            assertThat(actual.type).isEqualTo(value)
        }
    }

    @Test
    fun `test that StorageStateEvent instance is returned for a storage state`() {
        megaEvent.stub {
            on { type }.thenReturn(MegaEvent.EVENT_STORAGE)
        }
        assertThat(toEvent(megaEvent)).isInstanceOf(StorageStateEvent::class.java)
    }

    @Test
    fun `test that NormalEvent instance is returned for non storage state events`() {
        // list of event types, excluding Storage
        val expectedTypes = listOf(
            MegaEvent.EVENT_COMMIT_DB,
            MegaEvent.EVENT_ACCOUNT_CONFIRMATION,
            MegaEvent.EVENT_CHANGE_TO_HTTPS,
            MegaEvent.EVENT_DISCONNECT,
            MegaEvent.EVENT_ACCOUNT_BLOCKED,
            MegaEvent.EVENT_NODES_CURRENT,
            MegaEvent.EVENT_MEDIA_INFO_READY,
            MegaEvent.EVENT_STORAGE_SUM_CHANGED,
            MegaEvent.EVENT_BUSINESS_STATUS,
            MegaEvent.EVENT_KEY_MODIFIED,
            MegaEvent.EVENT_MISC_FLAGS_READY,
        )

        expectedTypes.forEach { t ->
            megaEvent.stub {
                on { type }.thenReturn(t)
            }
            assertThat(toEvent(megaEvent)).isInstanceOf(NormalEvent::class.java)
        }
    }

    @Test
    fun `test that event mapping is successful when text in MegaNode is null`() {
        megaEvent.stub {
            on { text }.thenReturn(null)
        }
        assertThat(toEvent(megaEvent)).isInstanceOf(NormalEvent::class.java)
        assertThat(toEvent(megaEvent).text).isEqualTo("")
    }

    @Test
    fun `test that event mapping is successful when eventString in MegaNode is null`() {
        megaEvent.stub {
            on { eventString }.thenReturn(null)
        }
        assertThat(toEvent(megaEvent)).isInstanceOf(NormalEvent::class.java)
        assertThat(toEvent(megaEvent).eventString).isEqualTo("")

    }


}