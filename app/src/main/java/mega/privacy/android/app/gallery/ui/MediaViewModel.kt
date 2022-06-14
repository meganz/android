package mega.privacy.android.app.gallery.ui

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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.GetCameraSortOrder
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.constant.INTENT_KEY_MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * MediaDiscovery viewModel
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repository: MediaItemRepository,
    private val getCameraSortOrder: GetCameraSortOrder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        const val ALL_VIEW = 0
        const val DAYS_VIEW = 1
        const val MONTHS_VIEW = 2
        const val YEARS_VIEW = 3

        const val SPAN_CARD_PORTRAIT = 1
        const val SPAN_CARD_LANDSCAPE = 2

        const val DAYS_INDEX = 0
        const val MONTHS_INDEX = 1
        const val YEARS_INDEX = 2

        const val VIEW_TYPE = "VIEW_TYPE"
    }

    var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    fun getOrder() = runBlocking { getCameraSortOrder() }

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

    /**
     * Custom condition in sub class for filter the real photos count
     */
    fun getFilterRealPhotoCountCondition(item: GalleryItem) =
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
    fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex

        if (item.type != MediaCardType.Header) {
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
                repository.getFiles(
                    order = getCameraSortOrder(),
                    zoom = mZoom,
                    handle = currentHandle ?: return@liveData)
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

    private fun getClickedCard(
        handle: Long,
        cards: List<GalleryCard>?,
    ) = cards?.find { it.id == handle }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(card: GalleryCard) = monthClicked(
        card,
        dateCards.value?.get(DAYS_INDEX),
        dateCards.value?.get(MONTHS_INDEX)
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
        currentHandle = savedStateHandle.get<Long>(INTENT_KEY_MEDIA_HANDLE)

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
}