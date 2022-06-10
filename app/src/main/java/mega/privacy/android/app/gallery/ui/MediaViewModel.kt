package mega.privacy.android.app.gallery.ui

import androidx.lifecycle.*
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.constant.INTENT_KEY_MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * MediaDiscovery viewModel
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    repository: MediaItemRepository,
    private val sortOrderManagement: SortOrderManagement,
    savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    fun getOrder() = sortOrderManagement.getOrderCamera()

    var currentHandle: Long? = null

    /**
     * Empty live data, used to switch to LiveData<List<PhotoNodeItem>>.
     */
    var liveDataRoot = MutableLiveData<Unit>()

    var forceUpdate = false

    // Whether a photo loading is in progress
    var loadInProgress = false

    // Whether another photo loading should be executed after current loading
    var pendingLoad = false

    private val _refreshCards = MutableLiveData(false)
    val refreshCards: LiveData<Boolean> = _refreshCards

    private var cancelToken: MegaCancelToken? = null

    /**
     * Custom condition in sub class for filter the real photos count
     */
    fun getFilterRealPhotoCountCondition(item: GalleryItem) =
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
    fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex

        if (item.type != GalleryItem.TYPE_HEADER) {
            item.indexForViewer = tempIndex++
        }

        return tempIndex
    }

    /**
     * the showing data from the UI layer, it will come from liveDataRoot
     */
    var items: LiveData<List<GalleryItem>> = liveDataRoot.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                cancelToken = initNewSearch()
                repository.getFiles(
                    cancelToken!!,
                    sortOrderManagement.getOrderCamera(),
                    mZoom,
                    currentHandle!!
                )
            }
        } else {
            repository.emitFiles()
        }

        repository.galleryItems
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
            repository.context,
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

    fun triggerDataLoad() {
        // Trigger data load.
        liveDataRoot.value = liveDataRoot.value
    }

    /**
     * Get items node handles
     *
     * @return  Node handles as LongArray
     */
    fun getItemsHandle(): LongArray =
        items.value?.map { it.node?.handle ?: MegaApiJava.INVALID_HANDLE }?.toLongArray() ?: LongArray(0)

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

    override fun onCleared() {
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { getFilterRealPhotoCountCondition(it) }
            ?.let {
                return it.size
            }
        return 0
    }

    fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    fun cancelSearch() {
        cancelToken?.cancel()
    }
}