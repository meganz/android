@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.feature.photos.presentation.mediadiscovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.MediaListItem.PhotoItem
import mega.privacy.android.domain.entity.photos.MediaListItem.VideoItem
import mega.privacy.android.domain.entity.photos.MediaListMedia
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
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@HiltViewModel(assistedFactory = CloudDriveMediaDiscoveryViewModel.Factory::class)
class CloudDriveMediaDiscoveryViewModel @AssistedInject constructor(
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val getPhotosByFolderIdUseCase: GetPhotosByFolderIdUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val photoToTypedFileNodeMapper: PhotoToTypedFileNodeMapper,
    @Assisted private val folderId: Long,
    @Assisted private val folderName: String,
    @Assisted private val fromFolderLink: Boolean,
    @Assisted private val nodeSourceType: NodeSourceType,
) : ViewModel() {
    private val _state = MutableStateFlow(
        CloudDriveMediaDiscoveryUiState(
            folderName = folderName,
            fromFolderLink = fromFolderLink,
            nodeSourceType = nodeSourceType
        )
    )
    val state = _state.asStateFlow()

    init {
        monitorMediaDiscovery()
    }

    private fun monitorMediaDiscovery() {
        viewModelScope.launch {
            combine(
                monitorAccountDetailUseCase()
                    .catch { Timber.e(it) },
                monitorHiddenNodesEnabledUseCase()
                    .catch { Timber.e(it) },
                monitorShowHiddenItemsUseCase()
                    .catch { Timber.e(it) },
                monitorSubFolderMediaDiscoverySettingsUseCase()
                    .catch { Timber.e(it) },
            ) { accountDetail, isHiddenNodesEnabled, showHiddenItems, isRecursive ->
                _state.update {
                    it.copy(
                        accountType = accountDetail.levelDetail?.accountType,
                        isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        showHiddenNodes = showHiddenItems,
                    )
                }
                isRecursive
            }
                .flatMapLatest { isRecursive ->
                    getPhotosByFolderIdUseCase(
                        folderId = NodeId(folderId),
                        recursive = isRecursive,
                        isFromFolderLink = fromFolderLink
                    )
                }
                .conflate()
                .collectLatest { sourcePhotos ->
                    handleFolderPhotosAndLogic(sourcePhotos)
                }
        }
    }

    internal suspend fun handleFolderPhotosAndLogic(
        sourcePhotos: List<Photo>,
    ) {
        if (sourcePhotos.isEmpty() && isNodeInRubbishBinUseCase(NodeId(folderId))) {
            _state.update {
                it.copy(backEvent = triggered)
            }
        } else {
            handlePhotoItems(
                sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(sourcePhotos),
                sourcePhotos = sourcePhotos
            )
        }
    }

    internal suspend fun handlePhotoItems(
        sortedPhotosWithoutHandleSensitive: List<Photo>,
        sourcePhotos: List<Photo>? = null,
    ) {
        val sortedPhotos = filterNonSensitivePhotos(
            photos = sortedPhotosWithoutHandleSensitive,
            isPaid = _state.value.accountType?.isPaid,
        )
        val dayPhotos = groupPhotosByDay(sortedPhotos = sortedPhotos)
        val yearsCardList = createYearsCardList(dayPhotos = dayPhotos)
        val monthsCardList = createMonthsCardList(dayPhotos = dayPhotos)
        val daysCardList = createDaysCardList(dayPhotos = dayPhotos)
        val currentZoomLevel = _state.value.currentZoomLevel
        val mediaListItemList = mutableListOf<MediaListItem>()

        sortedPhotos.forEachIndexed { index, photo ->
            val shouldShowDate = if (index == 0)
                true
            else
                needsDateSeparator(
                    current = photo,
                    previous = sortedPhotos[index - 1],
                    currentZoomLevel = currentZoomLevel
                )
            if (shouldShowDate) {
                mediaListItemList.add(MediaListItem.Separator(photo.modificationTime))
            }
            val item = when (photo) {
                is Photo.Image -> PhotoItem(photo)
                is Photo.Video -> VideoItem(
                    photo,
                    durationInSecondsTextMapper(photo.fileTypeInfo.duration)
                )
            }
            mediaListItemList.add(item)
        }
        if (sourcePhotos == null) {
            _state.update {
                it.copy(
                    loadPhotosDone = true,
                    mediaListItemList = mediaListItemList,
                    yearsCardList = yearsCardList,
                    monthsCardList = monthsCardList,
                    daysCardList = daysCardList,
                )
            }
        } else {
            val sourceNodes = sourcePhotos.map(photoToTypedFileNodeMapper::invoke)
            _state.update {
                it.copy(
                    loadPhotosDone = true,
                    sourcePhotos = sourcePhotos,
                    sourceNodes = sourceNodes,
                    mediaListItemList = mediaListItemList,
                    yearsCardList = yearsCardList,
                    monthsCardList = monthsCardList,
                    daysCardList = daysCardList,
                )
            }
        }
    }

    internal fun sortAndFilterPhotos(sourcePhotos: List<Photo>): List<Photo> {
        val filteredPhotos = when (_state.value.currentMediaType) {
            FilterMediaType.ALL_MEDIA -> sourcePhotos
            FilterMediaType.IMAGES -> sourcePhotos.filterIsInstance<Photo.Image>()
            FilterMediaType.VIDEOS -> sourcePhotos.filterIsInstance<Photo.Video>()
        }
        return when (_state.value.currentSort) {
            Sort.NEWEST -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
            Sort.OLDEST -> filteredPhotos.sortedWith(compareBy<Photo> { it.modificationTime }.thenByDescending { it.id })
            else -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
        }
    }

    internal suspend fun filterNonSensitivePhotos(
        photos: List<Photo>,
        isPaid: Boolean?,
    ): List<Photo> {
        val showHiddenItems = _state.value.showHiddenNodes
        isPaid ?: return photos

        return if (showHiddenItems || !isPaid || getBusinessStatusUseCase() == BusinessAccountStatus.Expired) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    internal fun groupPhotosByDay(sortedPhotos: List<Photo>) =
        sortedPhotos
            .groupBy { it.modificationTime.toLocalDate().toEpochDay() }
            .map { (_, photosList) ->
                photosList.first() to photosList.size
            }
            .toMap()

    internal fun createYearsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
        dayPhotos.keys.distinctBy { it.modificationTime.year }
            .map { createYearCard(it) }.toList()


    private fun createYearCard(photo: Photo): DateCard {
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR)
            .format(photo.modificationTime)

        return DateCard.YearsCard(
            date = year,
            photo = photo,
        )
    }

    internal fun createMonthsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
        dayPhotos.keys.distinctBy { YearMonth.from(it.modificationTime) }
            .map { createMonthCard(it) }.toList()

    private fun createMonthCard(photo: Photo): DateCard {
        val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
        val month = SimpleDateFormat(DATE_FORMAT_MONTH, Locale.getDefault()).format(
            Date.from(
                photo.modificationTime.toLocalDate().atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            )
        )
        val showDate = if (sameYear) {
            month
        } else {
            SimpleDateFormat(
                "$DATE_FORMAT_MONTH $DATE_FORMAT_YEAR_WITH_MONTH",
                Locale.getDefault()
            ).format(
                Date.from(
                    photo.modificationTime.toLocalDate().atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            )
        }
        return DateCard.MonthsCard(
            date = showDate,
            photo = photo
        )
    }

    internal fun createDaysCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
        dayPhotos.map { (key, value) ->
            createDaysCard(key, value)
        }.toList()

    private fun createDaysCard(photo: Photo, photosCount: Int): DateCard {
        val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
        val showDate = DateTimeFormatter.ofPattern(
            if (sameYear) {
                "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY"
            } else {
                "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY $DATE_FORMAT_YEAR"
            }
        ).format(photo.modificationTime)
        return DateCard.DaysCard(
            date = showDate,
            photo = photo,
            photosCount = photosCount.toString()
        )
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        currentZoomLevel: ZoomLevel,
    ): Boolean {
        val currentDate = current.modificationTime.toLocalDate()
        val previousDate = previous.modificationTime.toLocalDate()
        return if (currentZoomLevel == ZoomLevel.Grid_1) {
            currentDate != previousDate
        } else {
            currentDate.month != previousDate.month || currentDate.year != previousDate.year
        }
    }

    fun updatePeriod(mediaDiscoveryPeriod: MediaDiscoveryPeriod) {
        _state.update {
            it.copy(selectedPeriod = mediaDiscoveryPeriod)
        }
    }

    fun selectPeriod(dateCard: DateCard) {
        when (dateCard) {
            is DateCard.YearsCard -> {
                updatePeriodStateAndScrollOffset(
                    MediaDiscoveryPeriod.Months,
                    _state.value.monthsCardList.indexOfFirst {
                        it.photo.modificationTime.toLocalDate() == dateCard.photo.modificationTime.toLocalDate()
                    }
                )
            }

            is DateCard.MonthsCard -> {
                updatePeriodStateAndScrollOffset(
                    MediaDiscoveryPeriod.Days,
                    _state.value.daysCardList.indexOfFirst {
                        it.photo.modificationTime.toLocalDate() == dateCard.photo.modificationTime.toLocalDate()
                    }
                )
            }

            is DateCard.DaysCard -> {
                updatePeriodStateAndScrollOffset(
                    MediaDiscoveryPeriod.All,
                    _state.value.mediaListItemList.indexOfFirst { item ->
                        when (item) {
                            is VideoItem -> item.video.id == dateCard.photo.id
                            is PhotoItem -> item.photo.id == dateCard.photo.id
                            else -> item.key == dateCard.photo.id.toString()
                        }
                    },
                )
            }
        }
    }

    private fun updatePeriodStateAndScrollOffset(
        selectedPeriod: MediaDiscoveryPeriod,
        startIndex: Int = 0,
        startOffset: Int = 0,
    ) {
        _state.update {
            it.copy(
                selectedPeriod = selectedPeriod,
                scrollStartIndex = startIndex,
                scrollStartOffset = startOffset
            )
        }
    }

    fun selectPhoto(photo: Photo) {
        val id = photo.id
        _state.update {
            val updatedIds = if (id in it.selectedPhotoIds) {
                it.selectedPhotoIds - id
            } else {
                it.selectedPhotoIds + id
            }
            it.copy(selectedPhotoIds = updatedIds)
        }
    }

    fun selectAllPhotos() {
        _state.update {
            it.copy(selectedPhotoIds = getAllPhotoIds().toSet())
        }
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotoIds = emptySet())
        }
    }

    fun setCurrentSort(sort: Sort) {
        _state.update { it.copy(currentSort = sort) }
        viewModelScope.launch {
            handlePhotoItems(
                sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(_state.value.sourcePhotos)
            )
        }
    }

    fun setCurrentMediaType(filterMediaType: FilterMediaType) {
        _state.update { it.copy(currentMediaType = filterMediaType) }
        viewModelScope.launch {
            handlePhotoItems(
                sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(_state.value.sourcePhotos)
            )
        }
    }

    fun zoomIn() {
        val currentIndex = ZoomLevel.entries.indexOf(_state.value.currentZoomLevel)
        if (currentIndex > 0) {
            _state.update { it.copy(currentZoomLevel = ZoomLevel.entries[currentIndex - 1]) }
            viewModelScope.launch {
                handlePhotoItems(
                    sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(_state.value.sourcePhotos)
                )
            }
        }
    }

    fun zoomOut() {
        val currentIndex = ZoomLevel.entries.indexOf(_state.value.currentZoomLevel)
        if (currentIndex < ZoomLevel.entries.size - 1) {
            _state.update { it.copy(currentZoomLevel = ZoomLevel.entries[currentIndex + 1]) }
            viewModelScope.launch {
                handlePhotoItems(
                    sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(_state.value.sourcePhotos)
                )
            }
        }
    }

    private fun getAllPhotoIds() = _state.value.mediaListItemList
        .filterIsInstance<MediaListMedia>()
        .map { it.mediaId }

    @AssistedFactory
    interface Factory {
        fun create(
            folderId: Long = -1,
            folderName: String = "",
            fromFolderLink: Boolean = false,
            nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        ): CloudDriveMediaDiscoveryViewModel
    }

    companion object Companion {
        private const val DATE_FORMAT_YEAR = "uuuu"
        private const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
        private const val DATE_FORMAT_MONTH = "LLLL"
        private const val DATE_FORMAT_DAY = "dd"
        private const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"
    }
}