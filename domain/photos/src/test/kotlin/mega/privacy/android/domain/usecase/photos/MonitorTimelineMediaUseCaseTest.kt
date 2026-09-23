package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTimelineMediaUseCaseTest {

    private lateinit var underTest: MonitorTimelineMediaUseCase
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase = mock()
    private val filterCloudDriveMediaUseCase: FilterCloudDriveMediaUseCase = mock()
    private val filterCameraUploadMediaUseCase: FilterCameraUploadMediaUseCase = mock()
    private val monitorMediaTypedNodesUseCase: MonitorMediaTypedNodesUseCase = mock()

    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        runTest {
            whenever(getTimelineFilterPreferencesUseCase()) doReturn null
            whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)
        }

        underTest = MonitorTimelineMediaUseCase(
            defaultDispatcher = dispatcher,
            ioDispatcher = dispatcher,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            filterCloudDriveMediaUseCase = filterCloudDriveMediaUseCase,
            filterCameraUploadMediaUseCase = filterCameraUploadMediaUseCase,
            monitorMediaTypedNodesUseCase = monitorMediaTypedNodesUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            getTimelineFilterPreferencesUseCase,
            filterCloudDriveMediaUseCase,
            filterCameraUploadMediaUseCase,
            monitorMediaTypedNodesUseCase
        )
    }

    @Test
    fun `test that timeline media are successfully retrieved`() = runTest(dispatcher) {
        val request = TimelinePhotosRequest()
        val now = ZonedDateTime.now()
        val imageFileType = mock<StaticImageFileTypeInfo>()
        val image1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(1L)
            on { type } doReturn imageFileType
            on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val image2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(2L)
            on { type } doReturn imageFileType
            on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
            on { isMarkedSensitive } doReturn true
            on { isSensitiveInherited } doReturn false
        }
        val videoFileType = mock<VideoFileTypeInfo>()
        val video = mock<TypedFileNode> {
            on { id } doReturn NodeId(3L)
            on { type } doReturn videoFileType
            on { modificationTime } doReturn now.minusDays(3).toEpochSecond()
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn true
        }
        val allMediaList = listOf(image1, image2, video)
        whenever(monitorMediaTypedNodesUseCase()) doReturn flowOf(allMediaList)
        whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)

        underTest(request = request).test {
            assertThat(expectMostRecentItem().size).isEqualTo(3)
        }
    }

    @Test
    fun `test that the media is not marked as sensitive when hidden nodes flag is not active`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest()
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image = mock<TypedFileNode> {
                on { id } doReturn NodeId(1L)
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val allMediaList = listOf(image)
            whenever(monitorMediaTypedNodesUseCase()) doReturn flowOf(
                allMediaList
            )
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)

            underTest(request = request).test {
                val item = expectMostRecentItem()
                assertThat(item.none { it.isMarkedSensitive }).isTrue()
                assertThat(item.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that sensitive media are successfully filtered when hidden nodes flag is active`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest()
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image1Id = NodeId(1L)
            val image1 = mock<TypedFileNode> {
                on { id } doReturn image1Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val image2Id = NodeId(2L)
            val image2 = mock<TypedFileNode> {
                on { id } doReturn image2Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
                on { isMarkedSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val videoFileType = mock<VideoFileTypeInfo>()
            val videoId = NodeId(3L)
            val video = mock<TypedFileNode> {
                on { id } doReturn videoId
                on { type } doReturn videoFileType
                on { modificationTime } doReturn now.minusDays(3).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allMediaList = listOf(image1, image2, video)
            whenever(monitorMediaTypedNodesUseCase()) doReturn flowOf(
                allMediaList
            )
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(false)
            whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)

            underTest(request = request).test {
                val item = expectMostRecentItem()
                assertThat(item.find { it.id == image1Id }!!.isMarkedSensitive).isFalse()
                assertThat(item.size).isEqualTo(1)
                assertThat(item.first().id).isEqualTo(image1Id)
            }
        }

    @Test
    fun `test that sensitive media are returned when should show hidden items`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest()
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image1Id = NodeId(1L)
            val image1 = mock<TypedFileNode> {
                on { id } doReturn image1Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val image2Id = NodeId(2L)
            val image2 = mock<TypedFileNode> {
                on { id } doReturn image2Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
                on { isMarkedSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val videoFileType = mock<VideoFileTypeInfo>()
            val videoId = NodeId(3L)
            val video = mock<TypedFileNode> {
                on { id } doReturn videoId
                on { type } doReturn videoFileType
                on { modificationTime } doReturn now.minusDays(3).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allMediaList = listOf(image1, image2, video)
            whenever(monitorMediaTypedNodesUseCase()) doReturn flowOf(
                allMediaList
            )
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)

            underTest(request = request).test {
                assertThat(expectMostRecentItem().size).isEqualTo(3)
            }
        }

    @Test
    fun `test that the list of media is successfully sorted by the modification time in descending`() =
        runTest {
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image1Id = NodeId(1L)
            val image1 = mock<TypedFileNode> {
                on { id } doReturn image1Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val image2Id = NodeId(2L)
            val image2 = mock<TypedFileNode> {
                on { id } doReturn image2Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
                on { isMarkedSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val unsortedMedia = listOf(image2, image1)

            val actual = underTest.sortMedia(
                nodes = unsortedMedia,
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )

            assertThat(actual[0].id).isEqualTo(image1Id)
            assertThat(actual[1].id).isEqualTo(image2Id)
        }

    @Test
    fun `test that the list of media is successfully sorted by the modification time in ascending`() =
        runTest {
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image1Id = NodeId(1L)
            val image1 = mock<TypedFileNode> {
                on { id } doReturn image1Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val image2Id = NodeId(2L)
            val image2 = mock<TypedFileNode> {
                on { id } doReturn image2Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
                on { isMarkedSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val unsortedMedia = listOf(image2, image1)

            val actual = underTest.sortMedia(
                nodes = unsortedMedia,
                sortOrder = SortOrder.ORDER_MODIFICATION_ASC
            )

            assertThat(actual[0].id).isEqualTo(image2Id)
            assertThat(actual[1].id).isEqualTo(image1Id)
        }

    @Test
    fun `test that sensitive media are successfully filtered with the new filter when hidden nodes flag is active`() =
        runTest(dispatcher) {
            val isRemembered = Random.nextBoolean()
            val mediaType = "images"
            val mediaSource = "cloudDrive"
            val newFilter = mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to isRemembered.toString(),
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType,
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource,
            )
            whenever(getTimelineFilterPreferencesUseCase()) doReturn mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "false",
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType,
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource,
            )
            val request = TimelinePhotosRequest(
                selectedFilterFlow = flowOf(newFilter)
            )
            val now = ZonedDateTime.now()
            val imageFileType = mock<StaticImageFileTypeInfo>()
            val image1Id = NodeId(1L)
            val image1 = mock<TypedFileNode> {
                on { id } doReturn image1Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val image2Id = NodeId(2L)
            val image2 = mock<TypedFileNode> {
                on { id } doReturn image2Id
                on { type } doReturn imageFileType
                on { modificationTime } doReturn now.minusDays(2).toEpochSecond()
                on { isMarkedSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val videoFileType = mock<VideoFileTypeInfo>()
            val videoId = NodeId(3L)
            val video = mock<TypedFileNode> {
                on { id } doReturn videoId
                on { type } doReturn videoFileType
                on { modificationTime } doReturn now.minusDays(3).toEpochSecond()
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allMediaList = listOf(image1, image2, video)
            whenever(monitorMediaTypedNodesUseCase()) doReturn flowOf(
                allMediaList
            )
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(false)
            whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf(true)

            underTest(request = request).test { cancelAndConsumeRemainingEvents() }
            verify(filterCloudDriveMediaUseCase).invoke(source = allMediaList)
        }
}
