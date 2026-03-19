@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.feature.photos.presentation.mediadiscovery

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.feature.photos.presentation.mediadiscovery.model.MediaDiscoveryPeriod
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotoToTypedFileNodeMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveMediaDiscoveryViewModelTest {

    private lateinit var underTest: CloudDriveMediaDiscoveryViewModel

    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()
    private val getPhotosByFolderIdUseCase = mock<GetPhotosByFolderIdUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val photoToTypedFileNodeMapper = mock<PhotoToTypedFileNodeMapper>()

    private val testFolderId = 123L
    private val testFolderName = "Test Folder"
    private val testFromFolderLink = false

    @BeforeAll
    fun setup() {
        commonStub()
        initViewModel()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSubFolderMediaDiscoverySettingsUseCase,
            getPhotosByFolderIdUseCase,
            isNodeInRubbishBinUseCase,
            durationInSecondsTextMapper,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(
            flow { awaitCancellation() }
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flow { awaitCancellation() })
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flow { awaitCancellation() })
        whenever(monitorAccountDetailUseCase()).thenReturn(flow { awaitCancellation() })
        getBusinessStatusUseCase.stub {
            onBlocking { invoke() }.thenReturn(BusinessAccountStatus.Active)
        }
        whenever(getPhotosByFolderIdUseCase(any(), any(), any())).thenReturn(
            flowOf(emptyList())
        )
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn("0:30")
        whenever(photoToTypedFileNodeMapper(any())).thenReturn(mock())
    }

    private fun initViewModel() {
        underTest = CloudDriveMediaDiscoveryViewModel(
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
            getPhotosByFolderIdUseCase = getPhotosByFolderIdUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            photoToTypedFileNodeMapper = photoToTypedFileNodeMapper,
            folderId = testFolderId,
            folderName = testFolderName,
            fromFolderLink = testFromFolderLink,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )
    }

    @Test
    fun `test that initial state has default values`() = runTest {
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentSort).isEqualTo(Sort.NEWEST)
            assertThat(state.currentMediaType).isEqualTo(FilterMediaType.ALL_MEDIA)
            assertThat(state.backEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that back event is triggered when photos are empty and folder is in rubbish bin`() =
        runTest {
            whenever(isNodeInRubbishBinUseCase(NodeId(testFolderId))).thenReturn(true)

            underTest.handleFolderPhotosAndLogic(emptyList())

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.backEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that back event is not triggered when photos are empty and folder is not in rubbish bin`() =
        runTest {
            whenever(isNodeInRubbishBinUseCase(NodeId(testFolderId))).thenReturn(false)
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(AccountDetail(levelDetail = createAccountLevelDetail()))
            )
            whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(
                flow { awaitCancellation() }
            )

            initViewModel()

            underTest.handleFolderPhotosAndLogic(emptyList())

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.backEvent).isEqualTo(consumed)
                assertThat(state.loadPhotosDone).isTrue()
                assertThat(state.sourcePhotos).isEmpty()
            }
        }

    @Test
    fun `test that sortAndFilterPhotos returns all media sorted by newest`() = runTest {
        val photos = createTestPhotos()

        val sortedAll = underTest.sortAndFilterPhotos(photos)

        assertThat(sortedAll.first().modificationTime)
            .isGreaterThan(sortedAll.last().modificationTime)
    }

    @Test
    fun `test that groupPhotosByDay groups photos correctly`() = runTest {
        val now = LocalDateTime.of(2026, 3, 16, 10, 0)
        val photo1 = createImagePhoto(id = 1, modificationTime = now)
        val photo2 = createImagePhoto(id = 2, modificationTime = now.plusHours(1))
        val photo3 = createImagePhoto(id = 3, modificationTime = now.plusDays(1))

        val result = underTest.groupPhotosByDay(listOf(photo1, photo2, photo3))

        // photo1 and photo2 are on same day, photo3 is on next day
        assertThat(result).hasSize(2)
    }

    @Test
    fun `test that createYearsCardList creates distinct year cards`() = runTest {
        val photo2025 = createImagePhoto(
            id = 1,
            modificationTime = LocalDateTime.of(2025, 6, 15, 10, 0)
        )
        val photo2026 = createImagePhoto(
            id = 2,
            modificationTime = LocalDateTime.of(2026, 3, 16, 10, 0)
        )

        val dayPhotos = underTest.groupPhotosByDay(listOf(photo2025, photo2026))
        val yearsCards = underTest.createYearsCardList(dayPhotos)

        assertThat(yearsCards).hasSize(2)
    }

    @Test
    fun `test that createMonthsCardList creates distinct month cards`() = runTest {
        val photoJan = createImagePhoto(
            id = 1,
            modificationTime = LocalDateTime.of(2026, 1, 10, 10, 0)
        )
        val photoFeb = createImagePhoto(
            id = 2,
            modificationTime = LocalDateTime.of(2026, 2, 15, 10, 0)
        )
        val photoFeb2 = createImagePhoto(
            id = 3,
            modificationTime = LocalDateTime.of(2026, 2, 20, 10, 0)
        )

        val dayPhotos = underTest.groupPhotosByDay(listOf(photoJan, photoFeb, photoFeb2))
        val monthsCards = underTest.createMonthsCardList(dayPhotos)

        assertThat(monthsCards).hasSize(2)
    }

    @Test
    fun `test that filterNonSensitivePhotos filters sensitive photos for paid accounts`() =
        runTest {
            val sensitivePhoto = createImagePhoto(id = 1, isSensitive = true)
            val normalPhoto = createImagePhoto(id = 2, isSensitive = false)
            val photos = listOf(sensitivePhoto, normalPhoto)

            val result = underTest.filterNonSensitivePhotos(
                photos = photos,
                isPaid = true,
            )

            assertThat(result).hasSize(1)
            assertThat(result.first().id).isEqualTo(2)
        }

    @Test
    fun `test that filterNonSensitivePhotos returns all photos for free accounts`() = runTest {
        val sensitivePhoto = createImagePhoto(id = 1, isSensitive = true)
        val normalPhoto = createImagePhoto(id = 2, isSensitive = false)
        val photos = listOf(sensitivePhoto, normalPhoto)

        val result = underTest.filterNonSensitivePhotos(
            photos = photos,
            isPaid = false,
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun `test that filterNonSensitivePhotos returns all photos when showHiddenItems is true`() =
        runTest {
            val sensitivePhoto = createImagePhoto(id = 1, isSensitive = true)
            val normalPhoto = createImagePhoto(id = 2, isSensitive = false)
            val photos = listOf(sensitivePhoto, normalPhoto)

            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(AccountDetail(levelDetail = createAccountLevelDetail()))
            )
            whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(
                flowOf(false)
            )
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)

            // Re-init to pick up showHiddenItems = true
            initViewModel()

            val result = underTest.filterNonSensitivePhotos(
                photos = photos,
                isPaid = true,
            )

            assertThat(result).hasSize(2)
        }

    @Test
    fun `test that filterNonSensitivePhotos returns all photos when isPaid is null`() = runTest {
        val sensitivePhoto = createImagePhoto(id = 1, isSensitive = true)
        val normalPhoto = createImagePhoto(id = 2, isSensitive = false)
        val photos = listOf(sensitivePhoto, normalPhoto)

        val result = underTest.filterNonSensitivePhotos(
            photos = photos,
            isPaid = null,
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun `test that filterNonSensitivePhotos returns all photos when business account is expired`() =
        runTest {
            val sensitivePhoto = createImagePhoto(id = 1, isSensitive = true)
            val normalPhoto = createImagePhoto(id = 2, isSensitive = false)
            val photos = listOf(sensitivePhoto, normalPhoto)

            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

            val result = underTest.filterNonSensitivePhotos(
                photos = photos,
                isPaid = true,
            )

            assertThat(result).hasSize(2)
        }

    @Test
    fun `test that account type is set from monitorAccountDetailUseCase`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(
                AccountDetail(
                    levelDetail = createAccountLevelDetail(accountType = AccountType.PRO_I)
                )
            )
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(
            flowOf(false)
        )
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.accountType).isEqualTo(AccountType.PRO_I)
        }
    }

    @Test
    fun `test that isBusinessAccountExpired is true when business status is expired`() = runTest {
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(
                AccountDetail(
                    levelDetail = createAccountLevelDetail(accountType = AccountType.BUSINESS)
                )
            )
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(
            flowOf(false)
        )
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isBusinessAccountExpired).isTrue()
        }
    }

    @Test
    fun `test that handleFolderPhotosAndLogic triggers back event when photos empty and node in rubbish bin`() =
        runTest {
            whenever(isNodeInRubbishBinUseCase(NodeId(testFolderId))).thenReturn(true)

            underTest.handleFolderPhotosAndLogic(emptyList())

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.backEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that onTimeBarTabSelected updates selectedTimeBarTab`() = runTest {
        underTest.updatePeriod(MediaDiscoveryPeriod.Years)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPeriod).isEqualTo(MediaDiscoveryPeriod.Years)
        }
    }

    @Test
    fun `test that onCardClick with YearsCard switches to Months tab`() = runTest {
        val photo = createImagePhoto(id = 1)
        val yearsCard = DateCard.YearsCard(date = "2026", photo = photo)

        underTest.selectPeriod(yearsCard)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPeriod).isEqualTo(MediaDiscoveryPeriod.Months)
        }
    }

    @Test
    fun `test that onCardClick with MonthsCard switches to Days tab`() = runTest {
        val photo = createImagePhoto(id = 1)
        val monthsCard = DateCard.MonthsCard(date = "March", photo = photo)

        underTest.selectPeriod(monthsCard)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPeriod).isEqualTo(MediaDiscoveryPeriod.Days)
        }
    }

    @Test
    fun `test that onCardClick with DaysCard switches to All tab`() = runTest {
        val photo = createImagePhoto(id = 1)
        val daysCard = DateCard.DaysCard(date = "16 March", photo = photo, photosCount = "3")

        underTest.selectPeriod(daysCard)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPeriod).isEqualTo(MediaDiscoveryPeriod.All)
        }
    }

    @Test
    fun `test that initial state has folderName and fromFolderLink from constructor`() = runTest {
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.folderName).isEqualTo(testFolderName)
            assertThat(state.fromFolderLink).isEqualTo(testFromFolderLink)
        }
    }

    @Test
    fun `test that selectPhoto adds photo id to selectedPhotoIds`() = runTest {
        initViewModel()
        val photo = createImagePhoto(id = 10)

        underTest.selectPhoto(photo)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotoIds).contains(10L)
        }
    }

    @Test
    fun `test that selectPhoto removes photo id when already selected`() = runTest {
        initViewModel()
        val photo = createImagePhoto(id = 10)

        underTest.selectPhoto(photo)
        underTest.selectPhoto(photo)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotoIds).isEmpty()
        }
    }

    @Test
    fun `test that selectAllPhotos selects all photos from media list`() = runTest {
        val photos = createTestPhotos()
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn("")

        initViewModel()

        underTest.handleFolderPhotosAndLogic(photos)
        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotoIds).hasSize(photos.size)
            photos.forEach { photo ->
                assertThat(state.selectedPhotoIds).contains(photo.id)
            }
        }
    }

    @Test
    fun `test that clearSelectedPhotos resets selectedPhotoIds to empty`() = runTest {
        initViewModel()
        val photo1 = createImagePhoto(id = 1)
        val photo2 = createImagePhoto(id = 2)

        underTest.selectPhoto(photo1)
        underTest.selectPhoto(photo2)
        underTest.clearSelectedPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotoIds).isEmpty()
        }
    }

    @Test
    fun `test that isInSelectionMode is true when photos are selected`() = runTest {
        initViewModel()
        val photo = createImagePhoto(id = 1)

        underTest.selectPhoto(photo)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that selectedPhotosCount returns correct count`() = runTest {
        initViewModel()
        val photo1 = createImagePhoto(id = 1)
        val photo2 = createImagePhoto(id = 2)

        underTest.selectPhoto(photo1)
        underTest.selectPhoto(photo2)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotosCount).isEqualTo(2)
        }
    }

    @Test
    fun `test that isAllSelected is true when all source photos are selected`() = runTest {
        val photos = createTestPhotos()
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn("")

        initViewModel()

        underTest.handleFolderPhotosAndLogic(photos)
        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isAllSelected).isTrue()
        }
    }

    @Test
    fun `test that setCurrentSort updates currentSort in state`() = runTest {
        initViewModel()

        underTest.setCurrentSort(Sort.OLDEST)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentSort).isEqualTo(Sort.OLDEST)
        }
    }

    @Test
    fun `test that setCurrentMediaType updates currentMediaType in state`() = runTest {
        initViewModel()

        underTest.setCurrentMediaType(FilterMediaType.IMAGES)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentMediaType).isEqualTo(FilterMediaType.IMAGES)
        }
    }

    @Test
    fun `test that zoomIn decreases zoom level`() = runTest {
        initViewModel()

        // Default is Grid_3, zoom in should go to Grid_1
        underTest.zoomIn()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentZoomLevel).isEqualTo(ZoomLevel.Grid_1)
        }
    }

    @Test
    fun `test that zoomOut increases zoom level`() = runTest {
        initViewModel()

        // Default is Grid_3, zoom out should go to Grid_5
        underTest.zoomOut()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentZoomLevel).isEqualTo(ZoomLevel.Grid_5)
        }
    }

    @Test
    fun `test that zoomIn does not go below Grid_1`() = runTest {
        initViewModel()

        underTest.zoomIn() // Grid_3 -> Grid_1
        underTest.zoomIn() // Grid_1 -> should stay Grid_1

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentZoomLevel).isEqualTo(ZoomLevel.Grid_1)
        }
    }

    @Test
    fun `test that zoomOut does not go above Grid_5`() = runTest {
        initViewModel()

        underTest.zoomOut() // Grid_3 -> Grid_5
        underTest.zoomOut() // Grid_5 -> should stay Grid_5

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentZoomLevel).isEqualTo(ZoomLevel.Grid_5)
        }
    }

    private fun createAccountLevelDetail(
        accountType: AccountType = AccountType.PRO_I,
    ) = AccountLevelDetail(
        accountType = accountType,
        accountPlanDetail = AccountPlanDetail(
            accountType = accountType,
            isProPlan = true,
            expirationTime = null,
            subscriptionId = null,
            featuresList = emptyList(),
            isFreeTrial = false,
        ),
        subscriptionStatus = null,
        subscriptionRenewTime = 0L,
        proExpirationTime = 0L,
        accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
        accountSubscriptionDetailList = emptyList(),
    )

    private fun createTestPhotos(): List<Photo> {
        val now = LocalDateTime.of(2026, 3, 16, 10, 0)
        return listOf(
            createImagePhoto(id = 1, modificationTime = now),
            createImagePhoto(id = 2, modificationTime = now.minusDays(1)),
        )
    }

    private fun createImagePhoto(
        id: Long = 1,
        modificationTime: LocalDateTime = LocalDateTime.of(2026, 3, 16, 10, 0),
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Image = Photo.Image(
        id = id,
        parentId = testFolderId,
        name = "photo_$id.jpg",
        isFavourite = false,
        creationTime = modificationTime,
        modificationTime = modificationTime,
        thumbnailFilePath = null,
        previewFilePath = null,
        fileTypeInfo = StaticImageFileTypeInfo("image/jpeg", "jpg"),
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
    )

    private fun createVideoPhoto(
        id: Long = 1,
        modificationTime: LocalDateTime = LocalDateTime.of(2026, 3, 16, 10, 0),
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Video = Photo.Video(
        id = id,
        parentId = testFolderId,
        name = "video_$id.mp4",
        isFavourite = false,
        creationTime = modificationTime,
        modificationTime = modificationTime,
        thumbnailFilePath = null,
        previewFilePath = null,
        fileTypeInfo = VideoFileTypeInfo("video/mp4", "mp4", 30.seconds),
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
    )
}
