package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.audios.TypedAudioNodeMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.AudioSectionRepository
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioSectionRepositoryImplTest {
    private lateinit var underTest: AudioSectionRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val fileNodeMapper = mock<FileNodeMapper>()
    private val typedAudioNodeMapper = mock<TypedAudioNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()

    @BeforeAll
    fun setUp() {
        underTest = AudioSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            fileNodeMapper = fileNodeMapper,
            typedAudioNodeMapper = typedAudioNodeMapper,
            cancelTokenProvider = cancelTokenProvider,
            megaLocalRoomGateway = megaLocalRoomGateway,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            sortOrderIntMapper,
            fileNodeMapper,
            typedAudioNodeMapper,
            megaLocalRoomGateway
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get all audios returns successfully`() = runTest {
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(mock())
        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(MegaApiJava.ORDER_DEFAULT_DESC)
        whenever(megaApiGateway.searchByType(any(), any(), any(), any()))
            .thenReturn(listOf(mock(), mock()))
        whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(emptyList())
        whenever(typedAudioNodeMapper(any(), any())).thenReturn(mock())

        val actual = underTest.getAllAudios(SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual.size).isEqualTo(2)
    }
}