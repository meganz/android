package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.NormalEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import nz.mega.sdk.MegaEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventMapperTest {

    private lateinit var underTest: EventMapper

    private val storageStateMapper = mock<StorageStateMapper>()
    private val megaEvent = mock<MegaEvent> {
        on { handle }.thenReturn(12L)
        on { eventString }.thenReturn("eventString")
        on { number }.thenReturn(1L)
        on { text }.thenReturn("text")
    }

    @BeforeAll
    fun setup() {
        underTest = EventMapper(
            storageStateMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(storageStateMapper)
    }

    @ParameterizedTest(name = "when Mega event type from SDK is {0}, EventType is {1}")
    @MethodSource("provideEventTypeParameters")
    fun `test that event type can be mapped correctly`(
        megaEventType: Int,
        expectedEventType: EventType,
    ) {
        whenever(storageStateMapper(any())).thenReturn(StorageState.Green)
        megaEvent.stub {
            on { type }.thenReturn(megaEventType)
        }
        val actual = underTest(megaEvent)
        assertThat(actual.type).isEqualTo(expectedEventType)
    }

    private fun provideEventTypeParameters() = listOf(
        arrayOf(MegaEvent.EVENT_COMMIT_DB, EventType.CommitDb),
        arrayOf(MegaEvent.EVENT_ACCOUNT_CONFIRMATION, EventType.AccountConfirmation),
        arrayOf(MegaEvent.EVENT_CHANGE_TO_HTTPS, EventType.ChangeToHttps),
        arrayOf(MegaEvent.EVENT_DISCONNECT, EventType.Disconnect),
        arrayOf(MegaEvent.EVENT_ACCOUNT_BLOCKED, EventType.AccountBlocked),
        arrayOf(MegaEvent.EVENT_STORAGE, EventType.Storage),
        arrayOf(MegaEvent.EVENT_NODES_CURRENT, EventType.NodesCurrent),
        arrayOf(MegaEvent.EVENT_MEDIA_INFO_READY, EventType.MediaInfoReady),
        arrayOf(MegaEvent.EVENT_STORAGE_SUM_CHANGED, EventType.StorageSumChanged),
        arrayOf(MegaEvent.EVENT_BUSINESS_STATUS, EventType.BusinessStatus),
        arrayOf(MegaEvent.EVENT_KEY_MODIFIED, EventType.KeyModified),
        arrayOf(MegaEvent.EVENT_MISC_FLAGS_READY, EventType.MiscFlagsReady),
        arrayOf(100, EventType.Unknown),
    )

    @Test
    fun `test that StorageStateEvent instance is returned for a storage state`() {
        whenever(storageStateMapper(any())).thenReturn(StorageState.Green)
        megaEvent.stub {
            on { type }.thenReturn(MegaEvent.EVENT_STORAGE)
        }
        assertThat(underTest(megaEvent)).isInstanceOf(StorageStateEvent::class.java)
    }

    @ParameterizedTest(name = "when non-storage state Mega event is {0}")
    @MethodSource("provideNonStorageStateEvents")
    fun `test that NormalEvent instance is returned for non storage state events`(
        megaEventType: Int,
    ) {
        megaEvent.stub {
            on { type }.thenReturn(megaEventType)
        }
        assertThat(underTest(megaEvent)).isInstanceOf(NormalEvent::class.java)
    }

    private fun provideNonStorageStateEvents() = listOf(
        arrayOf(MegaEvent.EVENT_COMMIT_DB),
        arrayOf(MegaEvent.EVENT_ACCOUNT_CONFIRMATION),
        arrayOf(MegaEvent.EVENT_CHANGE_TO_HTTPS),
        arrayOf(MegaEvent.EVENT_DISCONNECT),
        arrayOf(MegaEvent.EVENT_ACCOUNT_BLOCKED),
        arrayOf(MegaEvent.EVENT_NODES_CURRENT),
        arrayOf(MegaEvent.EVENT_MEDIA_INFO_READY),
        arrayOf(MegaEvent.EVENT_STORAGE_SUM_CHANGED),
        arrayOf(MegaEvent.EVENT_BUSINESS_STATUS),
        arrayOf(MegaEvent.EVENT_KEY_MODIFIED),
        arrayOf(MegaEvent.EVENT_MISC_FLAGS_READY),
    )

    @Test
    fun `test that event mapping is successful when text in MegaNode is null`() {
        megaEvent.stub {
            on { text }.thenReturn(null)
        }
        assertThat(underTest(megaEvent)).isInstanceOf(NormalEvent::class.java)
        assertThat(underTest(megaEvent).text).isEqualTo("")
    }

    @Test
    fun `test that event mapping is successful when eventString in MegaNode is null`() {
        megaEvent.stub {
            on { eventString }.thenReturn(null)
        }
        assertThat(underTest(megaEvent)).isInstanceOf(NormalEvent::class.java)
        assertThat(underTest(megaEvent).eventString).isEqualTo("")

    }


}