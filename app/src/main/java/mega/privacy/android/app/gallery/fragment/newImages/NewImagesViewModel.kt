package mega.privacy.android.app.gallery.fragment.newImages

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.fragments.managerFragments.cu.CUCard
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Util.fromEpoch
import mega.privacy.android.app.viewmodel.ZoomViewModel
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*

class NewImagesViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository,
    private val zoomViewModel: ZoomViewModel
) : ViewModel() {

    private var _query = MutableLiveData<String>()

    var searchMode = false
    var searchQuery = ""
    var skipNextAutoScroll = false

    private var forceUpdate = false

    // Whether a photo loading is in progress
    private var loadInProgress = false

    // Whether another photo loading should be executed after current loading
    private var pendingLoad = false

    private val _refreshCards = MutableLiveData(false)
    val refreshCards : LiveData<Boolean> = _refreshCards

    fun refreshing() {
        _refreshCards.value = false
    }

    val items: LiveData<List<PhotoNodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(
                    FILE_TYPE_PHOTO,
                    ORDER_MODIFICATION_DESC,
                    zoomViewModel.zoom.value!!
                )
            }
        } else {
            repository.emitFiles()
        }

        repository.fileNodeItems
    }.map { it ->
        @Suppress("UNCHECKED_CAST")
        val items = it as List<PhotoNodeItem>
        var index = 0
        var photoIndex = 0
        var filteredNodes = items

        if (searchMode) {
            filteredNodes = filteredNodes.filter {
                it.type == PhotoNodeItem.TYPE_PHOTO
            }
        }

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = items.filter {
                it.node?.name?.contains(
                    _query.value!!,
                    true
                ) ?: false
            }
        }

        filteredNodes.forEach {
            it.index = index++
            if (it.type == PhotoNodeItem.TYPE_PHOTO) it.photoIndex = photoIndex++
        }

        filteredNodes
    }

    val dateCards: LiveData<List<List<CUCard>>> = items.map {
        val days = ArrayList<CUCard>()
        val months = ArrayList<CUCard>()
        val years = ArrayList<CUCard>()

        val nodesWithoutPreview = mutableMapOf<MegaNode, String>()
        var lastDayDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastYearDate: LocalDate? = null

        it.forEach foreach@{ photoItem ->
            if (photoItem.node == null) return@foreach

            val node = photoItem.node!!

            var shouldGetPreview = false
            val preview = File(
                PreviewUtils.getPreviewFolder(repository.context),
                node.base64Handle + FileUtil.JPG_EXTENSION
            )

            val modifyDate = fromEpoch(node.modificationTime)
            val day = ofPattern("dd").format(modifyDate)
            val month = ofPattern("MMMM").format(modifyDate)
            val year = ofPattern("uuuu").format(modifyDate)
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)

            if (lastDayDate == null || lastDayDate!!.dayOfYear != modifyDate.dayOfYear) {
                shouldGetPreview = true
                lastDayDate = modifyDate
                val date =
                    ofPattern(if (sameYear) "dd MMMM" else "dd MMMM uuuu").format(lastDayDate)
                days.add(
                    CUCard(
                        node, if (preview.exists()) preview else null, day, month,
                        if (sameYear) null else year, date, modifyDate, 0
                    )
                )
            } else if (days.isNotEmpty()) {
                days[days.size - 1].incrementNumItems()
            }

            if (lastMonthDate == null || YearMonth.from(lastMonthDate) != YearMonth.from(modifyDate)
            ) {
                shouldGetPreview = true
                lastMonthDate = modifyDate
                val date = if (sameYear) month else ofPattern("MMMM yyyy").format(modifyDate)
                months.add(
                    CUCard(
                        node, if (preview.exists()) preview else null, null, month,
                        if (sameYear) null else year, date, modifyDate, 0
                    )
                )
            }

            if (lastYearDate == null || Year.from(lastYearDate) != Year.from(modifyDate)) {
                shouldGetPreview = true
                lastYearDate = modifyDate
                years.add(
                    CUCard(
                        node, if (preview.exists()) preview else null, null, null,
                        year, year, modifyDate, 0
                    )
                )
            }

            if (shouldGetPreview && !preview.exists()) {
                nodesWithoutPreview[node] = preview.absolutePath
            }
        }

        viewModelScope.launch {
            repository.getPreviews(nodesWithoutPreview) {
                _refreshCards.value = true
            }
        }

        listOf(days, months, years)
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked year card.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    fun yearClicked(position: Int, card: CUCard): Int {
        val yearCard = getClickedCard(position, card.node.handle, dateCards.value?.get(2)) ?: return 0
        val monthCards = dateCards.value?.get(1) ?: return 0

        val cardYear = yearCard.localDate.year
        val currentMonth = LocalDate.now().monthValue
        for (i in monthCards.indices) {
            val nextLocalDate = monthCards[i].localDate
            val nextMonth = nextLocalDate.monthValue
            if (nextLocalDate.year == cardYear && nextMonth <= currentMonth) {
                //Year clicked, current month. If not exists, the closest month behind the current.
                if (i == 0 || nextMonth == currentMonth || monthCards[i - 1].localDate.year != cardYear) {
                    return i
                }
                val previousMonth = monthCards[i - 1].localDate.monthValue.toLong()

                //The closest month to the current
                return if (previousMonth - currentMonth <= currentMonth - nextMonth) i - 1 else i
            }
        }

        //No month equal or behind the current found, then return the latest month.
        return monthCards.size - 1
    }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(position: Int, card: CUCard): Int {
        val monthCard = getClickedCard(position, card.node.handle, dateCards.value?.get(1)) ?: return 0
        val dayCards = dateCards.value?.get(0) ?: return 0

        val cardLocalDate = monthCard.localDate
        val cardMonth = cardLocalDate.monthValue
        val cardYear = cardLocalDate.year
        val currentDay = LocalDate.now().dayOfMonth
        var dayPosition = 0
        for (i in dayCards.indices) {
            val nextLocalDate = dayCards[i].localDate
            val nextDay = nextLocalDate.dayOfMonth
            val nextMonth = nextLocalDate.monthValue
            val nextYear = nextLocalDate.year
            if (nextYear == cardYear && nextMonth == cardMonth) {
                dayPosition = if (nextDay <= currentDay) {
                    //Month of year clicked, current day. If not exists, the closest day behind the current.
                    if (i == 0 || nextDay == currentDay || dayCards[i - 1].localDate.monthValue != cardMonth) {
                        return i
                    }
                    val previousDay = dayCards[i - 1].localDate.dayOfMonth

                    //The closest day to the current
                    return if (previousDay - currentDay <= currentDay - nextDay) i - 1 else i
                } else {
                    //Save the closest day above the current in case there is no day of month behind the current.
                    i
                }
            }
        }
        return dayPosition
    }

//    /**
//     * Checks and gets the clicked day card.
//     *
//     * @param position Clicked position in the list.
//     * @param card     Clicked day card.
//     * @return The checked day card.
//     */
//    fun dayClicked(position: Int, card: CUCard): CUCard? {
//        return getClickedCard(position, card.node.handle, dateCards.value?.get(0))
//    }

    /**
     * Checks a clicked card, if it is in the provided list and if is in right position.
     *
     * @param position Clicked position in the list.
     * @param handle   Identifier of the card node.
     * @param cards    List of cards to check.
     * @return The checked card if found, null otherwise.
     */
    private fun getClickedCard(position: Int, handle: Long, cards: List<CUCard>?): CUCard? {
        if (cards == null) {
            return null
        }

        var card: CUCard? = cards[position]
        if (handle != card!!.node.handle) {
            card = null
            for (c in cards) {
                if (c.node.handle == handle) {
                    card = c
                    break
                }
            }
        }

        return card
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            loadPhotos(true)
        } else {
            refreshUi()
        }
    }

    private val loadFinishedObserver = Observer<List<PhotoNodeItem>> {
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
     * , false if filter current nodes by searchQuery
     */
    fun loadPhotos(forceUpdate: Boolean = false) {
        this.forceUpdate = forceUpdate

        if (loadInProgress) {
            pendingLoad = true
        } else {
            pendingLoad = false
            loadInProgress = true
            _query.value = searchQuery
        }
    }

    /**
     * Make the list adapter to rebind all item views with data since
     * the underlying meta data of items may have been changed.
     */
    fun refreshUi() {
        items.value?.forEach { item ->
            item.uiDirty = true
        }
        loadPhotos()
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { it.type == PhotoNodeItem.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getItemPositionByHandle(handle: Long) =
        items.value?.find { it.node?.handle == handle }?.index ?: INVALID_POSITION

    fun getHandlesOfPhotos(): LongArray? {
        val list = items.value?.filter {
            it.type == PhotoNodeItem.TYPE_PHOTO
        }?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    override fun onCleared() {
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
    }

    /**********zoom*************/
    val zoomManager = ZoomManager()
    fun setZoom(currentZoom:Int) = zoomManager.setZoom(currentZoom)
    fun getZoom() = zoomManager.zoom
}