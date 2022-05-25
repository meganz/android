package mega.privacy.android.app.gallery.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.constant.INTENT_KEY_MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.GalleryItemRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

abstract class GalleryViewModel constructor(
    private val galleryItemRepository: GalleryItemRepository,
    private val sortOrderManagement: SortOrderManagement,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle? = null,
) : ViewModel(), SearchCallback.Data {

    companion object {
        const val DAYS_INDEX = 0
        const val MONTHS_INDEX = 1
        const val YEARS_INDEX = 2
    }

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

    abstract var mZoom: Int

    /**
     * Custom condition in sub class for filter the real photos count
     */
    open fun getFilterRealPhotoCountCondition(item: GalleryItem) =
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
    open fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
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
        liveData(viewModelScope.coroutineContext) {
            if (forceUpdate) {
                cancelToken = initNewSearch()
                galleryItemRepository.getFiles(cancelToken ?: return@liveData,
                    sortOrderManagement.getOrderCamera(),
                    mZoom,
                    currentHandle)
            } else {
                galleryItemRepository.emitFiles()
            }
            emit(galleryItemRepository.galleryItems.value ?: return@liveData)
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
            val cardsProvider = DateCardsProvider()
            cardsProvider.extractCardsFromNodeList(
                context = galleryItemRepository.context,
                nodes = galleryItems.mapNotNull { item -> item.node }
                    .sortedWith(compareByDescending<MegaNode> { node -> node.modificationTime }
                        .thenByDescending { node -> node.name })
            )

            emit(listOf(cardsProvider.getDays(), cardsProvider.getMonths(), cardsProvider.getYears()))

            galleryItemRepository.getPreviews(cardsProvider.getNodesWithoutPreview()) {
                _refreshCards.value = true
            }
        }
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
        dateCards.value?.get(MONTHS_INDEX),
        dateCards.value?.get(YEARS_INDEX)
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
        dateCards.value?.get(DAYS_INDEX),
        dateCards.value?.get(MONTHS_INDEX)
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
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
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
        items.value?.map { it.node?.handle ?: INVALID_HANDLE }?.toLongArray() ?: LongArray(0)

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
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
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

    override fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun cancelSearch() {
        cancelToken?.cancel()
    }
}