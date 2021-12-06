package mega.privacy.android.app.gallery.ui

import android.content.Context
import android.os.Environment
import android.util.Pair
import androidx.collection.LongSparseArray
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.getClickedCard
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.monthClicked
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.yearClicked
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.fragments.managerFragments.cu.BaseZoomFragment
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.Constants.GET_THUMBNAIL_THROTTLE_MS
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop
import mega.privacy.android.app.utils.Util.fromEpoch
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.*
import java.io.File
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

abstract class GalleryViewModel constructor(
    @MegaApi private val mMegaApi: MegaApiAndroid,
    private val mRepo: MegaNodeRepo,
    private val mAppContext: Context
) : BaseRxViewModel() {

    private val dayCards = MutableLiveData<List<GalleryCard>>()
    private val monthCards = MutableLiveData<List<GalleryCard>>()
    private val yearCards = MutableLiveData<List<GalleryCard>>()
    private val mCuNodes = MutableLiveData<List<GalleryItem>?>()
    private val mNodeToOpen = MutableLiveData<Pair<Int, GalleryItem?>>()
    private val mNodeToAnimate = MutableLiveData<Pair<Int, GalleryItem>>()
    private val mActionBarTitle = MutableLiveData<String>()
    private val mActionMode = MutableLiveData(false)

    private val mOpenNodeAction: Subject<Pair<Int, GalleryItem?>> = PublishSubject.create()
    private val mCreatingThumbnailFinished: Subject<Any> = PublishSubject.create()
    private val mCreatingPreviewFinished: Subject<Any> = PublishSubject.create()
    private val mCreatePreviewForCardFinished: Subject<Any> = PublishSubject.create()

    private var mCreateThumbnailRequest: MegaRequestListenerInterface? = null
    private var createPreviewRequest: MegaRequestListenerInterface? = null
    private var mCreatePreviewForCardRequest: MegaRequestListenerInterface? = null
    private val mSelectedNodes = LongSparseArray<MegaNode>(5)
    private var mSelecting = false
    private var mRealNodeCount = 0
    private var enableCUShown = false

    private val thumbnailHandle = HashSet<Long>()
    private val previewHandle = HashSet<Long>()
    private var mZoom = 0
    var selectedViewType = BaseZoomFragment.ALL_VIEW

    init {
        mCreateThumbnailRequest = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _: MegaRequest?, e: MegaError ->
                if (e.errorCode == MegaError.API_OK) {
                    mCreatingThumbnailFinished.onNext(true)
                }
            })

        createPreviewRequest = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _: MegaRequest?, e: MegaError ->
                if (e.errorCode == MegaError.API_OK) {
                    mCreatingPreviewFinished.onNext(true)
                }
            })

        mCreatePreviewForCardRequest = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _: MegaRequest?, e: MegaError ->
                if (e.errorCode == MegaError.API_OK) {
                    mCreatePreviewForCardFinished.onNext(true)
                }
            })

        loadNodes()
        getCards()

        add(
            mOpenNodeAction.throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mNodeToOpen.setValue(
                        it
                    )
                }, logErr("openNodeAction"))
        )

        add(
            mCreatingThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe({ loadNodes() }, logErr("creatingThumbnailFinished"))
        )

        add(
            mCreatingPreviewFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe({ loadNodes() }, logErr("creatingPreviewFinished"))
        )

        add(
            mCreatePreviewForCardFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe({ getCards() }, logErr("creatingPreviewFinished"))
        )
    }

    fun getDayCardsData(): LiveData<List<GalleryCard>> {
        return dayCards
    }

    fun getMonthCardsData(): LiveData<List<GalleryCard>> {
        return monthCards
    }

    fun getYearCardsData(): LiveData<List<GalleryCard>> {
        return yearCards
    }

    fun getDayCards() = dayCards.value!!

    fun getMonthCards() = monthCards.value!!

    fun getYearCards() = yearCards.value!!

    fun cuNodes() = mCuNodes

    fun getCUNodes() = mCuNodes.value

    fun nodeToOpen() = mNodeToOpen

    fun nodeToAnimate() = mNodeToAnimate

    fun actionBarTitle(): LiveData<String?> {
        return mActionBarTitle
    }

    fun actionMode() = mActionMode

    fun isSelecting() = mSelecting

    fun resetOpenedNode() {
        with(mNodeToOpen) {
            setValue(
                Pair.create(
                    INVALID_POSITION,
                    null
                )
            )
        }
    }

    /**
     * Handle node click & long click event.
     *
     *
     * In selection mode, we need need animate the selection icon, so we don't
     * trigger nodes update through `cuNodes.setValue(nodes); `, we only
     * update node's selected property here, for consistency.
     *
     * @param position clicked node position in RV
     * @param node     clicked node
     */
    fun onNodeClicked(position: Int, node: GalleryItem) {
        if (mSelecting) {
            val nodes = mCuNodes.value
            if (nodes == null || position < 0 || position >= nodes.size || nodes[position].node == null || nodes[position].node!!.handle != node.node!!.handle) {
                return
            }
            nodes[position].selected = !nodes[position].selected
            if (nodes[position].selected) {
                mSelectedNodes.put(node.node!!.handle, node.node)
            } else {
                mSelectedNodes.remove(node.node!!.handle)
            }
            mSelecting = !mSelectedNodes.isEmpty
            mActionMode.value = mSelecting
            mNodeToAnimate.setValue(Pair(position, node))
        } else {
            mOpenNodeAction.onNext(Pair(position, node))
        }
    }

    fun onNodeLongClicked(position: Int, node: GalleryItem) {
        mSelecting = true
        onNodeClicked(position, node)
    }

    fun getSelectedNodes(): List<MegaNode> {
        val nodes: MutableList<MegaNode> = ArrayList()
        var i = 0
        val n = mSelectedNodes.size()
        while (i < n) {
            nodes.add(mSelectedNodes.valueAt(i))
            i++
        }
        return nodes
    }

    fun getSelectedNodesCount(): Int {
        return mSelectedNodes.size()
    }

    fun getRealNodesCount(): Int {
        return mRealNodeCount
    }

    fun selectAll() {
        val nodes = mCuNodes.value
        if (nodes == null || nodes.isEmpty()) {
            return
        }
        for (i in nodes.indices) {
            val node = nodes[i]
            if (node.node != null) {
                if (!node.selected) {
                    mNodeToAnimate.value = Pair.create(i, node)
                }
                node.selected = true
                mSelectedNodes.put(node.node!!.handle, node.node)
            }
        }
        mSelecting = true
        mCuNodes.value = nodes
        mActionMode.value = true
    }

    fun clearSelection() {
        if (mSelecting) {
            mSelecting = false
            mActionMode.value = false
            val nodes = mCuNodes.value
            if (nodes == null || nodes.isEmpty()) {
                return
            }
            for (i in nodes.indices) {
                val node = nodes[i]
                if (node.selected) {
                    mNodeToAnimate.value = Pair.create(i, node)
                }
                node.selected = false
            }
            mSelectedNodes.clear()
            mCuNodes.value = nodes
        }
    }

    fun loadNodes() {
        add(Single.fromCallable { getCuNodes() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ value: List<GalleryItem>? ->
                mCuNodes.setValue(
                    value
                )
            }, logErr("loadCuNodes")))
    }

    /**
     * Gets CU and MU content as CUNode objects.
     * Content means images and videos which are located on CU and MU folders.
     *
     *
     * Also requests needed thumbnails to show in holders if not exist yet.
     *
     * @return The list of CUNode objects.
     */
    private fun getCuNodes(): List<GalleryItem> {
        val nodes: MutableList<GalleryItem> = ArrayList()
        val nodesWithoutThumbnail: MutableList<MegaNode> = ArrayList()
        val nodesWithoutPreview: MutableList<MegaNode> = ArrayList()
        var lastYearDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastDayDate: LocalDate? = null
        val realNodes: List<Pair<Int, MegaNode>> = getRealMegaNodes()
        for (pair in realNodes) {
            val node = pair.second
            val thumbnail = File(
                ThumbnailUtilsLollipop.getThumbFolder(mAppContext),
                node.base64Handle + FileUtil.JPG_EXTENSION
            )
            val preview = File(
                PreviewUtils.getPreviewFolder(mAppContext),
                node.base64Handle + FileUtil.JPG_EXTENSION
            )
            val modifyDate: LocalDate = fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMMM yyyy").format(modifyDate)
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)
            val cuNode = GalleryItem(
                node,
                pair.first,
                INVALID_POSITION,
                if (thumbnail.exists()) thumbnail else null,
                if (FileUtil.isVideoFile(node.name)) GalleryItem.TYPE_VIDEO else GalleryItem.TYPE_IMAGE,
                dateString,
                null,
                null,
                mSelectedNodes.containsKey(node.handle),
                false
            )
            if (mZoom == ZoomUtil.ZOOM_OUT_2X) {
                if (lastYearDate == null || Year.from(lastYearDate) != Year.from(modifyDate)) {
                    lastYearDate = modifyDate
                    val date = DateTimeFormatter.ofPattern("yyyy").format(modifyDate)
                    nodes.add(GalleryItem(date, Pair(date, "")))
                }
            } else if (mZoom == ZoomUtil.ZOOM_IN_1X) {
                if (lastDayDate == null || lastDayDate.dayOfYear != modifyDate.dayOfYear) {
                    lastDayDate = modifyDate
                    nodes.add(
                        GalleryItem(
                            dateString, Pair(
                                DateTimeFormatter.ofPattern("dd MMMM").format(modifyDate),
                                if (sameYear) "" else DateTimeFormatter.ofPattern("yyyy")
                                    .format(modifyDate)
                            )
                        )
                    )
                }
                // For zoom in 1X, use preview file as thumbnail to avoid blur.
                cuNode.thumbnail = preview
            } else {
                if (lastMonthDate == null
                    || YearMonth.from(lastMonthDate) != YearMonth.from(modifyDate)
                ) {
                    lastMonthDate = modifyDate
                    nodes.add(
                        GalleryItem(
                            dateString, Pair(
                                DateTimeFormatter.ofPattern("MMMM").format(modifyDate),
                                if (sameYear) "" else DateTimeFormatter.ofPattern("yyyy")
                                    .format(modifyDate)
                            )
                        )
                    )
                }
            }
            nodes.add(cuNode)
            if (!thumbnail.exists()) {
                nodesWithoutThumbnail.add(node)
            }
            if (!preview.exists() && mZoom == ZoomUtil.ZOOM_IN_1X) {
                nodesWithoutPreview.add(node)
            }
        }
        mRealNodeCount = realNodes.size

        // Fetch thumbnails of nodes in computation thread, fetching each in 50ms interval
        add(
            Observable.fromIterable(nodesWithoutThumbnail)
                .zipWith(
                    Observable.interval(
                        GET_THUMBNAIL_THROTTLE_MS,
                        TimeUnit.MILLISECONDS
                    ),
                    { node: MegaNode, _: Long? -> node })
                .observeOn(Schedulers.computation())
                .subscribe(
                    { node: MegaNode ->
                        if (!thumbnailHandle.contains(node.handle)) {
                            thumbnailHandle.add(node.handle)
                            val thumbnail = File(
                                ThumbnailUtilsLollipop.getThumbFolder(mAppContext),
                                node.base64Handle + FileUtil.JPG_EXTENSION
                            )
                            if (!thumbnail.exists()) {
                                mMegaApi.getThumbnail(
                                    node,
                                    thumbnail.absolutePath,
                                    mCreateThumbnailRequest
                                )
                            }
                        }
                    },
                    logErr("mega.privacy.android.app.fragments.managerFragments.cu.CuViewModel getThumbnail")
                )
        )

        // Fetch previews of nodes in computation thread, fetching each in 50ms interval
        add(
            Observable.fromIterable(nodesWithoutPreview)
                .zipWith(
                    Observable.interval(
                        GET_THUMBNAIL_THROTTLE_MS,
                        TimeUnit.MILLISECONDS
                    ),
                    { node: MegaNode, _ -> node })
                .observeOn(Schedulers.computation())
                .subscribe(
                    { node: MegaNode ->
                        if (!previewHandle.contains(node.handle)) {
                            previewHandle.add(node.handle)
                            val preview = File(
                                PreviewUtils.getPreviewFolder(mAppContext),
                                node.base64Handle + FileUtil.JPG_EXTENSION
                            )
                            if (!preview.exists()) {
                                mMegaApi.getPreview(
                                    node,
                                    preview.absolutePath,
                                    createPreviewRequest
                                )
                            }
                        }
                    },
                    logErr("mega.privacy.android.app.fragments.managerFragments.cu.CuViewModel getPreview")
                )
        )
        return nodes
    }

    /**
     * Gets three different lists of cards, organizing CU and MU content in three different ways:
     * - Day cards:   Content organized by days.
     * - Month cards: Content organized by months.
     * - Year cards:  Content organized by years.
     *
     *
     * Also requests needed previews to show in cards if not exist yet.
     */
    fun getCards() {
        val cardsProvider = DateCardsProvider()
        val cardNodes: List<MegaNode> =
            mRepo.getFilteredCuChildren(MegaApiJava.ORDER_MODIFICATION_DESC)
        cardsProvider.extractCardsFromNodeList(mAppContext, cardNodes)
        add(
            Observable.fromIterable(cardsProvider.getNodesWithoutPreview().keys)
                .zipWith(
                    Observable.interval(
                        GET_THUMBNAIL_THROTTLE_MS,
                        TimeUnit.MILLISECONDS
                    ),
                    { node: MegaNode, _ -> node })
                .observeOn(Schedulers.computation())
                .subscribe(
                    { node: MegaNode ->
                        val preview = File(
                            PreviewUtils.getPreviewFolder(mAppContext),
                            node.base64Handle + FileUtil.JPG_EXTENSION
                        )
                        if (!preview.exists()) {
                            mMegaApi.getPreview(
                                node,
                                preview.absolutePath,
                                mCreatePreviewForCardRequest
                            )
                        }
                    },
                    logErr("mega.privacy.android.app.fragments.managerFragments.cu.CuViewModel getPreview")
                )
        )
        dayCards.postValue(cardsProvider.getDays())
        monthCards.postValue(cardsProvider.getMonths())
        yearCards.postValue(cardsProvider.getYears())
    }

    /**
     * Checks and gets the clicked day card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked day card.
     * @return The checked day card.
     */
    fun dayClicked(position: Int, card: GalleryCard): GalleryCard? {
        return getClickedCard(position, card.node.handle, getDayCards())
    }

    /**
     * Checks the clicked month card and gets the day card to show after click on a month card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked month card.
     * @return A day card corresponding to the month of the year clicked, current day. If not exists,
     * the closest day to the current.
     */
    fun monthClicked(position: Int, card: GalleryCard?): Int {
        return monthClicked(position, card!!, getDayCards(), getMonthCards())
    }

    /**
     * Checks the clicked year card and gets the month card to show after click on a year card.
     *
     * @param position Clicked position in the list.
     * @param card     Clicked year card.
     * @return A month card corresponding to the year clicked, current month. If not exists,
     * the closest month to the current.
     */
    fun yearClicked(position: Int, card: GalleryCard?): Int {
        return yearClicked(position, card!!, getMonthCards(), getYearCards())
    }

    fun setZoom(zoom: Int) {
        mZoom = zoom
    }

    /**
     * Get the real mega nodes from the strategy by subclass. Eg,from mega api directly or a folder handle
     *
     * @return the real mega node in a pair <position, MegaNode>
     */
    abstract fun getRealMegaNodes(n: MegaNode? = null): List<Pair<Int, MegaNode>>
}