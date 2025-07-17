package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.account.AccountBlockedTypeMapper
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.AccountConfirmationEvent
import mega.privacy.android.domain.entity.BusinessStatusEvent
import mega.privacy.android.domain.entity.ChangeToHttpsEvent
import mega.privacy.android.domain.entity.CommitDbEvent
import mega.privacy.android.domain.entity.DisconnectEvent
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.KeyModifiedEvent
import mega.privacy.android.domain.entity.MediaInfoReadyEvent
import mega.privacy.android.domain.entity.MiscFlagsReadyEvent
import mega.privacy.android.domain.entity.NodesCurrentEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.StorageSumChangedEvent
import mega.privacy.android.domain.entity.UnknownEvent
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
import kotlin.reflect.KClass

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
            storageStateMapper = storageStateMapper,
            accountBlockedTypeMapper = AccountBlockedTypeMapper(),
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
        expectedEventType: KClass<Event>,
    ) {
        whenever(storageStateMapper(any())).thenReturn(StorageState.Green)
        megaEvent.stub {
            on { type }.thenReturn(megaEventType)
        }
        val actual = underTest(megaEvent)
        assertThat(actual).isInstanceOf(expectedEventType.java)
    }

    private fun provideEventTypeParameters() = listOf(
        arrayOf(MegaEvent.EVENT_COMMIT_DB, CommitDbEvent::class),
        arrayOf(MegaEvent.EVENT_ACCOUNT_CONFIRMATION, AccountConfirmationEvent::class),
        arrayOf(MegaEvent.EVENT_CHANGE_TO_HTTPS, ChangeToHttpsEvent::class),
        arrayOf(MegaEvent.EVENT_DISCONNECT, DisconnectEvent::class),
        arrayOf(MegaEvent.EVENT_ACCOUNT_BLOCKED, AccountBlockedEvent::class),
        arrayOf(MegaEvent.EVENT_STORAGE, StorageStateEvent::class),
        arrayOf(MegaEvent.EVENT_NODES_CURRENT, NodesCurrentEvent::class),
        arrayOf(MegaEvent.EVENT_MEDIA_INFO_READY, MediaInfoReadyEvent::class),
        arrayOf(MegaEvent.EVENT_STORAGE_SUM_CHANGED, StorageSumChangedEvent::class),
        arrayOf(MegaEvent.EVENT_BUSINESS_STATUS, BusinessStatusEvent::class),
        arrayOf(MegaEvent.EVENT_KEY_MODIFIED, KeyModifiedEvent::class),
        arrayOf(MegaEvent.EVENT_MISC_FLAGS_READY, MiscFlagsReadyEvent::class),
        arrayOf(100, UnknownEvent::class),
    )

    @Test
    fun `test that StorageStateEvent instance is returned for a storage state`() {
        whenever(storageStateMapper(any())).thenReturn(StorageState.Green)
        megaEvent.stub {
            on { type }.thenReturn(MegaEvent.EVENT_STORAGE)
        }
        assertThat(underTest(megaEvent)).isInstanceOf(StorageStateEvent::class.java)
    }

    @Test
    fun `test that blocked account event with null text returns empty string`() {
        megaEvent.stub {
            on { type }.thenReturn(MegaEvent.EVENT_ACCOUNT_BLOCKED)
            on { text }.thenReturn(null)
        }
        val actual = underTest(megaEvent)
        assertThat(actual).isInstanceOf(AccountBlockedEvent::class.java)
        assertThat((actual as AccountBlockedEvent).text).isEmpty()
    }
}
