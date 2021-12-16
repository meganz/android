package mega.privacy.android.app.gallery.ui

import androidx.lifecycle.*
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment.Companion.ALL_VIEW
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment.Companion.DAYS_INDEX
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment.Companion.MONTHS_INDEX
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment.Companion.YEARS_INDEX
import mega.privacy.android.app.gallery.repository.GalleryItemRepository
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION

abstract class GalleryViewModel constructor(
        private val repository: GalleryItemRepository
) : BaseRxViewModel() {

    /**
     * Empty live data, used to switch to LiveData<List<PhotoNodeItem>>.
     */
    var liveDataRoot = MutableLiveData<Unit>()

    var selectedViewTypeImages = ALL_VIEW
    var selectedViewTypePhotos = ALL_VIEW
    var selectedViewTypeMedia = ALL_VIEW

    var forceUpdate = false

    // Whether a photo loading is in progress
    var loadInProgress = false

    // Whether another photo loading should be executed after current loading
    var pendingLoad = false

    private val _refreshCards = MutableLiveData(false)
    val refreshCards: LiveData<Boolean> = _refreshCards

    abstract var mZoom: Int

    abstract var mOrder: Int

    abstract fun isAutoGetItem(): Boolean

    abstract fun getFilterRealPhotoCountCondition(item: GalleryItem): Boolean

    fun setZoom(zoom: Int) {
        mZoom = zoom
    }

    fun setOrder(order: Int) {
        mOrder = order
    }

    /**
     * Indicate refreshing cards has finished.
     */
    fun refreshCompleted() {
        _refreshCards.value = false
    }

    var items: LiveData<List<GalleryItem>> = getAndFilterFiles()

    protected fun getAndFilterFiles(): LiveData<List<GalleryItem>> {
        if (!isAutoGetItem())
            return MutableLiveData()
        else return liveDataRoot.switchMap {
            if (forceUpdate) {
                viewModelScope.launch {
                    repository.getFiles(mOrder, mZoom)
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
    }

    /**
     * Custom node index and assign it to node.
     *
     * @return node index
     */
    abstract fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int

    val dateCards: LiveData<List<List<GalleryCard>>> = items.map {
        val cardsProvider = DateCardsProvider()
        cardsProvider.extractCardsFromNodeList(
                repository.context,
                it.mapNotNull { item -> item.node })

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
            liveDataRoot.value = liveDataRoot.value
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

    fun getItemPositionByHandle(handle: Long) =
            items.value?.find { it.node?.handle == handle }?.index ?: INVALID_POSITION

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
}