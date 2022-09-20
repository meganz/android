package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.constant.INTENT_KEY_MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.gallery.repository.PhotosItemRepository
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] associated with [TimelineFragment]
 *
 * @param repository Photos Repository
 * @param mDbHandler Database
 * @param getCameraSortOrder Get camera sort order use case
 * @param jobUtilWrapper Job util wrapper
 * @param ioDispatcher Coroutine dispatcher for the [ViewModel]
 * @param savedStateHandle Saved state handle
 * @param monitorNodeUpdates Monitor global node updates
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: PhotosItemRepository,
    private val mDbHandler: DatabaseHandler,
    private val getCameraSortOrder: GetCameraSortOrder,
    private val jobUtilWrapper: JobUtilWrapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle? = null,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    companion object {
        const val ALL_VIEW = 0
        const val DAYS_VIEW = 1
        const val MONTHS_VIEW = 2
        const val YEARS_VIEW = 3

        const val FILTER_ALL_PHOTOS = 0
        const val FILTER_CLOUD_DRIVE = 1
        const val FILTER_CAMERA_UPLOADS = 2
        const val FILTER_VIDEOS_ONLY = 3

        const val SORT_DESC = 0
        const val SORT_ASC = 1

        const val SPAN_CARD_PORTRAIT = 1
        const val SPAN_CARD_LANDSCAPE = 2

        const val DAYS_INDEX = 0
        const val MONTHS_INDEX = 1
        const val YEARS_INDEX = 2

        const val VIEW_TYPE = "VIEW_TYPE"
    }

    private val composite = CompositeDisposable()

    var mZoom = PHOTO_ZOOM_LEVEL

    private var currentFilter: Int = FILTER_ALL_PHOTOS
    private var currentSort: Int = SORT_DESC

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = runBlocking { getCameraSortOrder() }

    private val camSyncEnabled = MutableLiveData<Boolean>()
    private var enableCUShown = false

    private var currentHandle: Long? = null

    /**
     * Empty live data, used to switch to LiveData<List<PhotoNodeItem>>.
     */
    private var liveDataRoot = MutableLiveData<Unit>()

    private var forceUpdate = false

    // Whether a photo loading is in progress
    private var loadInProgress = false

    // Whether another photo loading should be executed after current loading
    private var pendingLoad = false

    private val _refreshCards = MutableLiveData(false)
    val refreshCards: LiveData<Boolean> = _refreshCards

    private var cancelToken: MegaCancelToken? = null

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            loadPhotos(true)
        } else {
            refreshUi()
        }
    }

    private val loadFinishedObserver = Observer<List<GalleryItem>> {
        loadInProgress = false

        if (pendingLoad) {
            loadPhotos(true)
        }
    }

    /**
     * the showing data from the UI layer, it will come from liveDataRoot
     */
    var items: LiveData<List<GalleryItem>> = liveDataRoot.switchMap {
        liveData(viewModelScope.coroutineContext) {
            if (forceUpdate) {
                cancelToken = initNewSearch()
                repository.getFiles(cancelToken ?: return@liveData,
                    getCameraSortOrder(),
                    mZoom)
            } else {
                repository.emitFiles()
            }
            emit(repository.galleryItems.value ?: return@liveData)
        }
    }.map {
        var index = 0
        var photoIndex = 0

        it.forEach { item ->
            item.index = index++
            photoIndex = initMediaIndex(item, photoIndex)
        }

        it
    }

    var dateCards: LiveData<List<List<GalleryCard>>> = items.switchMap { galleryItems ->
        liveData(context = ioDispatcher) {
            val previewFolder = repository.previewFolder
                ?: return@liveData
            val cardsProvider = DateCardsProvider(
                previewFolder = previewFolder,
            )
            cardsProvider.processGalleryItems(
                nodes = galleryItems
            )

            val days = cardsProvider.getDays()


            emit(
                listOf(
                    days.sortDescending(),
                    cardsProvider.getMonths().sortDescending(),
                    cardsProvider.getYears().sortDescending())
            )

            fetchMissingPreviews(days)
        }
    }

    fun List<GalleryCard>.sortDescending() = sortedWith(
        compareByDescending<GalleryCard> { it.localDate }
            .thenByDescending { it.name }
    )

    private suspend fun fetchMissingPreviews(days: List<GalleryCard>) {
        repository.getPreviews(days) {
            _refreshCards.value = true
        }
    }

    init {
        currentHandle = savedStateHandle?.get<Long>(INTENT_KEY_MEDIA_HANDLE)

        items.observeForever(loadFinishedObserver)
        // Calling ObserveForever() here instead of calling observe()
        // in the PhotosFragment, for fear that an nodes update event would be missed if
        // emitted accidentally between the Fragment's onDestroy and onCreate when rotating screen.
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodesChangeObserver)
        loadPhotos(true)

        viewModelScope.launch {
            // Whenever the app detects MegaNode updates, load the photos
            monitorNodeUpdates().collect {
                loadPhotos(true)
            }
        }
    }


    /**
     * Custom condition in sub class for filter the real photos count
     */
    private fun getFilterRealPhotoCountCondition(item: GalleryItem) =
        item.type != MediaCardType.Header

    /**
     * Indicate refreshing cards has finished.
     */
    fun refreshCompleted() {
        _refreshCards.value = false
    }

    /**
     * Custom node index and assign it to node.
     *
     * @return node index
     */
    private fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex

        if (item.type != MediaCardType.Header) {
            item.indexForViewer = tempIndex++
        }

        return tempIndex
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param card     Clicked year card.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    fun yearClicked(card: GalleryCard) = yearClicked(
        card = card,
        months = dateCards.value?.get(MONTHS_INDEX),
        years = dateCards.value?.get(YEARS_INDEX),
    )

    private fun yearClicked(
        card: GalleryCard,
        months: List<GalleryCard>?,
        years: List<GalleryCard>?,
    ): Int {
        val yearCard = getClickedCard(card.id, years) ?: return 0
        val monthCards = months ?: return 0

        val cardYear = yearCard.localDate.year

        for (i in monthCards.indices) {
            val nextLocalDate = monthCards[i].localDate
            if (nextLocalDate.year == cardYear) {
                //Year clicked, current month. If not exists, the closest month behind the current.
                if (i == 0 || monthCards[i - 1].localDate.year != cardYear) {
                    return i
                }
            }
        }

        //No month equal or behind the current found, then return the latest month.
        return monthCards.size - 1
    }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(card: GalleryCard) = monthClicked(
        card = card,
        days = dateCards.value?.get(DAYS_INDEX),
        months = dateCards.value?.get(MONTHS_INDEX),
    )

    private fun monthClicked(
        card: GalleryCard,
        days: List<GalleryCard>?,
        months: List<GalleryCard>?,
    ): Int {
        val monthCard = getClickedCard(card.id, months) ?: return 0
        val dayCards = days ?: return 0

        val cardLocalDate = monthCard.localDate
        val cardMonth = cardLocalDate.monthValue
        val cardYear = cardLocalDate.year

        for (i in dayCards.indices) {
            val nextLocalDate = dayCards[i].localDate
            val nextMonth = nextLocalDate.monthValue
            val nextYear = nextLocalDate.year

            if (nextYear == cardYear && nextMonth == cardMonth) {
                //Month of year clicked, current day. If not exists, the closest day behind the current.
                if (i == 0 || dayCards[i - 1].localDate.monthValue != cardMonth) {
                    return i
                }
            }
        }
        return dayCards.size - 1
    }

    private fun getClickedCard(
        handle: Long,
        cards: List<GalleryCard>?,
    ) = cards?.find { it.id == handle }

    /**
     * Load photos by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     */
    fun loadPhotos(forceUpdate: Boolean = false) {
        this.forceUpdate = forceUpdate

        if (loadInProgress) {
            pendingLoad = true
        } else {
            pendingLoad = false
            loadInProgress = true
            // Trigger data load.
            liveDataRoot.value = Unit
        }
    }

    /**
     * Make the list adapter to rebind all item views with data since
     * the underlying meta data of items may have been changed.
     */
    private fun refreshUi() {
        items.value?.forEach { item ->
            item.uiDirty = true
        }
        loadPhotos()
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { getFilterRealPhotoCountCondition(it) }
            ?.let {
                return it.size
            }
        return 0
    }

    private fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    fun cancelSearch() {
        cancelToken?.cancel()
    }


    /**
     * Check is enable CU shown UI
     */
    fun isEnableCUShown(): Boolean = enableCUShown

    /**
     * set enable CU shown UI
     */
    fun setEnableCUShown(shown: Boolean) {
        enableCUShown = shown
    }

    /**
     * Check is CU enabled
     */
    fun isCUEnabled(): Boolean = camSyncEnabled.value ?: false

    /**
     * User enabled Camera Upload, so a periodic job should be scheduled if not already running
     */
    fun startCameraUploadJob(context: Context) {
        Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
        jobUtilWrapper.fireCameraUploadJob(context, false)
    }

    /**
     * Set Initial Preferences
     */
    fun setInitialPreferences() = viewModelScope.launch(ioDispatcher) {
        Timber.d("setInitialPreferences")
        mDbHandler.setFirstTime(false)
        mDbHandler.setStorageAskAlways(true)
        val defaultDownloadLocation =
            repository.buildDefaultDownloadDir()
        defaultDownloadLocation.mkdirs()
        mDbHandler.setStorageDownloadLocation(
            defaultDownloadLocation.absolutePath
        )
        mDbHandler.isPasscodeLockEnabled = false
        mDbHandler.passcodeLockCode = ""
        val nodeLinks: ArrayList<MegaNode> = repository.getPublicLinks()
        if (nodeLinks.size == 0) {
            Timber.d("No public links: showCopyright set true")
            mDbHandler.setShowCopyright(true)
        } else {
            Timber.d("Already public links: showCopyright set false")
            mDbHandler.setShowCopyright(false)
        }
    }

    /**
     * Set CamSync Enabled to db
     */
    fun setCamSyncEnabled(enabled: Boolean) = viewModelScope.launch(ioDispatcher) {
        mDbHandler.setCamSyncEnabled(enabled)
    }

    fun enableCu(enableCellularSync: Boolean, syncVideo: Boolean, context: Context) {
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                enableCUSettingForDb(enableCellularSync, syncVideo)
                startCameraUploadJob(context)
            }.onFailure {
                Timber.e("enableCu${it.message}")
            }
        }
    }

    /**
     * Enable CU
     */
    @Suppress("deprecation", "RedundantSuspendModifier")
    private suspend fun enableCUSettingForDb(enableCellularSync: Boolean, syncVideo: Boolean) {
        val localFile = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        )
        mDbHandler.setCamSyncLocalPath(localFile.absolutePath)
        mDbHandler.setCameraFolderExternalSDCard(false)
        mDbHandler.setCamSyncWifi(!enableCellularSync)
        mDbHandler.setCamSyncFileUpload(
            if (syncVideo) MegaPreferences.PHOTOS_AND_VIDEOS else MegaPreferences.ONLY_PHOTOS
        )
        mDbHandler.setCameraUploadVideoQuality(VideoQuality.ORIGINAL.value)
        mDbHandler.setConversionOnCharging(true)
        mDbHandler.setChargingOnSize(SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE)
        // After target and local folder setup, then enable CU.
        mDbHandler.setCamSyncEnabled(true)
        camSyncEnabled.postValue(true)
    }

    /**
     * Check camSync Enabled
     *
     * @return camSyncEnabled livedata
     */
    fun camSyncEnabled(): LiveData<Boolean> {
        return camSyncEnabled
    }

    fun checkAndUpdateCamSyncEnabledStatus() = viewModelScope.launch(ioDispatcher) {
        camSyncEnabled.postValue(mDbHandler.preferences?.camSyncEnabled.toBoolean())
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
        composite.clear()
    }

    /**
     * Return the current filter set as string
     */
    fun getCurrentFilterAsString(): String {
        return when (currentFilter) {
            FILTER_ALL_PHOTOS -> getString(R.string.photos_filter_all_photos)
            FILTER_CLOUD_DRIVE -> getString(R.string.photos_filter_cloud_drive)
            FILTER_CAMERA_UPLOADS -> getString(R.string.photos_filter_camera_uploads)
            FILTER_VIDEOS_ONLY -> getString(R.string.photos_filter_videos_only)
            else -> ""
        }
    }

    /**
     * Return the current filter
     */
    fun getCurrentFilter(): Int {
        return currentFilter
    }

    /**
     * Set the current selected filter
     */
    fun setCurrentFilter(filter: Int) {
        currentFilter = filter
    }

    /**
     * Get the current sort
     */
    fun getCurrentSort(): Int {
        return currentSort
    }

    /**
     * Set the current sort
     */
    fun setCurrentSort(sort: Int) {
        currentSort = sort
    }
}




