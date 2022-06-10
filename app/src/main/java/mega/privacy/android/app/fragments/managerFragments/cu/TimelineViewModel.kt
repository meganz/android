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
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.constant.INTENT_KEY_MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.gallery.repository.PhotosItemRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RxUtil
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * TimelineViewModel works with TimelineFragment
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PhotosItemRepository,
    private val mDbHandler: DatabaseHandler,
    private val sortOrderManagement: SortOrderManagement,
    private val jobUtilWrapper: JobUtilWrapper,
    savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {

    private val composite = CompositeDisposable()

    var mZoom = PHOTO_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = sortOrderManagement.getOrderCamera()

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
                    sortOrderManagement.getOrderCamera(),
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

    var dateCards: LiveData<List<List<GalleryCard>>> = items.map {
        val cardsProvider = DateCardsProvider()
        cardsProvider.extractCardsFromNodeList(
            context,
            it.mapNotNull { item -> item.node }
                // Sort by modification time and name desc.
                .sortedWith(compareByDescending<MegaNode> { node -> node.modificationTime }.thenByDescending { node -> node.name })
        )

        viewModelScope.launch {
            repository.getPreviews(cardsProvider.getNodesWithoutPreview()) {
                _refreshCards.value = true
            }
        }

        listOf(cardsProvider.getDays(), cardsProvider.getMonths(), cardsProvider.getYears())
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
    }


    /**
     * Custom condition in sub class for filter the real photos count
     */
    private fun getFilterRealPhotoCountCondition(item: GalleryItem) =
        item.type != GalleryItem.TYPE_HEADER

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

        if (item.type != GalleryItem.TYPE_HEADER) {
            item.indexForViewer = tempIndex++
        }

        return tempIndex
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked year card.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    fun yearClicked(position: Int, card: GalleryCard) = CardClickHandler.yearClicked(
        position,
        card,
        dateCards.value?.get(BaseZoomFragment.MONTHS_INDEX),
        dateCards.value?.get(BaseZoomFragment.YEARS_INDEX)
    )

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(position: Int, card: GalleryCard) = CardClickHandler.monthClicked(
        position,
        card,
        dateCards.value?.get(BaseZoomFragment.DAYS_INDEX),
        dateCards.value?.get(BaseZoomFragment.MONTHS_INDEX)
    )

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
            triggerDataLoad()
        }
    }

    private fun triggerDataLoad() {
        // Trigger data load.
        liveDataRoot.value = liveDataRoot.value
    }

    /**
     * Get items node handles
     *
     * @return  Node handles as LongArray
     */
    fun getItemsHandle(): LongArray =
        items.value?.map { it.node?.handle ?: MegaApiJava.INVALID_HANDLE }?.toLongArray()
            ?: LongArray(0)

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
    fun isEnableCUShown(): Boolean {
        return enableCUShown
    }

    /**
     * set enable CU shown UI
     */
    fun setEnableCUShown(shown: Boolean) {
        enableCUShown = shown
    }

    /**
     * Check is CU enabled
     */
    fun isCUEnabled(): Boolean {
        return if (camSyncEnabled.value != null) camSyncEnabled.value!! else false
    }

    /**
     * User enabled Camera Upload, so a periodic job should be scheduled if not already running
     */
    fun startCameraUploadJob() {
        Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
        jobUtilWrapper.fireCameraUploadJob(context, false)
    }

    /**
     * Set Initial Preferences
     */
    fun setInitialPreferences() {
        add(Completable.fromCallable {
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
            true
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setInitialPreferences")))
    }

    /**
     * Set CamSync Enabled to db
     */
    fun setCamSyncEnabled(enabled: Boolean) {
        add(Completable.fromCallable {
            mDbHandler.setCamSyncEnabled(enabled)
            enabled
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setCamSyncEnabled")))
    }

    fun enableCu(enableCellularSync: Boolean, syncVideo: Boolean) {
        viewModelScope.launch(IO) {
            runCatching {
                enableCUSettingForDb(enableCellularSync, syncVideo)
                startCameraUploadJob()
            }.onFailure {
                Timber.e("enableCu" + it.message)
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
        mDbHandler.setCamSyncLocalPath(localFile?.absolutePath)
        mDbHandler.setCameraFolderExternalSDCard(false)
        mDbHandler.setCamSyncWifi(!enableCellularSync)
        mDbHandler.setCamSyncFileUpload(
            if (syncVideo) MegaPreferences.PHOTOS_AND_VIDEOS else MegaPreferences.ONLY_PHOTOS
        )
        mDbHandler.setCameraUploadVideoQuality(SettingsConstants.VIDEO_QUALITY_ORIGINAL)
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

    fun checkAndUpdateCamSyncEnabledStatus() {
        viewModelScope.launch(IO) {
            camSyncEnabled.postValue(mDbHandler.preferences?.camSyncEnabled.toBoolean())
        }
    }

    private fun add(disposable: Disposable) {
        composite.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
        composite.clear()
    }
}




