package mega.privacy.android.app.presentation.rubbishbin

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentRubbishbingridBinding
import mega.privacy.android.app.databinding.FragmentRubbishbinlistBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.ViewUtils.isVisible
import mega.privacy.android.data.mapper.sortOrderToInt
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.Stack
import javax.inject.Inject

/**
 * Fragment is for Rubbish Bin
 */
@AndroidEntryPoint
class RubbishBinFragment : Fragment() {

    companion object {
        /**
         * Returns the instance of RubbishBinFragment
         */
        @JvmStatic
        fun newInstance() = RubbishBinFragment()
    }

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val lastPositionStack = Stack<Int>()

    private val managerViewModel: ManagerViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private val rubbishBinViewModel: RubbishBinViewModel by activityViewModels()

    private var nodes: List<MegaNode> = mutableListOf()

    private var recyclerView: RecyclerView? = null
    private lateinit var emptyImageView: ImageView
    private lateinit var emptyTextView: TextView

    //Bindings
    private var _listBinding: FragmentRubbishbinlistBinding? = null
    private val listBinding: FragmentRubbishbinlistBinding
        get() = _listBinding!!

    private var _gridBinding: FragmentRubbishbingridBinding? = null
    private val gridBinding: FragmentRubbishbingridBinding
        get() = _gridBinding!!


    private var adapter: MegaNodeAdapter? = null
    private lateinit var gridLayoutManager: CustomizedGridLayoutManager
    private val outMetrics: DisplayMetrics by lazy {
        DisplayMetrics()
    }

    private lateinit var layoutManager: LinearLayoutManager

    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")

        if (megaApi.rootNode == null) {
            return null
        }
        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
            EventObserver { showSortByPanel() })
        rubbishBinViewModel.updateRubbishBinNodes.observe(viewLifecycleOwner,
            EventObserver { nodes ->
                hideMultipleSelect()
                setNodes(nodes)
                recyclerView?.invalidate()
            })
        requireActivity().display?.getMetrics(outMetrics)

        if (rubbishBinViewModel.state.value.rubbishBinHandle == -1L ||
            rubbishBinViewModel.state.value.rubbishBinHandle == megaApi.rubbishNode.handle
        ) {
            Timber.d("Parent is the Rubbish: %s",
                rubbishBinViewModel.state.value.rubbishBinHandle)
            nodes = megaApi.getChildren(megaApi.rubbishNode,
                sortOrderToInt(managerViewModel.getOrder()))
        } else {
            val parentNode =
                megaApi.getNodeByHandle(rubbishBinViewModel.state.value.rubbishBinHandle)
            parentNode?.let {
                Timber.d("The parent node is: %s", parentNode.handle)
                nodes = megaApi.getChildren(parentNode, sortOrderToInt(managerViewModel.getOrder()))
                (requireActivity() as ManagerActivity).supportInvalidateOptionsMenu()
            }
            nodes = megaApi.getChildren(parentNode, sortOrderToInt(managerViewModel.getOrder()))
        }

        (requireActivity() as ManagerActivity).setToolbarTitle()
        (requireActivity() as ManagerActivity).supportInvalidateOptionsMenu()

        return if ((requireActivity() as ManagerActivity).isList) {
            Timber.d("List View")
            _listBinding = FragmentRubbishbinlistBinding.inflate(inflater, container, false)
            recyclerView = listBinding.rubbishbinListView
            emptyImageView = listBinding.rubbishbinListEmptyImage
            emptyTextView = listBinding.rubbishbinListEmptyTextFirst

            adapter?.let {
                it.parentHandle = rubbishBinViewModel.state.value.rubbishBinHandle
                it.setListFragment(recyclerView)
                it.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
            } ?: run {
                adapter = MegaNodeAdapter(requireActivity(),
                    this@RubbishBinFragment,
                    nodes,
                    rubbishBinViewModel.state.value.rubbishBinHandle,
                    recyclerView,
                    Constants.RUBBISH_BIN_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                    sortByHeaderViewModel)
            }

            adapter?.isMultipleSelect = false

            recyclerView?.apply {
                this@RubbishBinFragment.layoutManager = LinearLayoutManager(requireActivity())
                layoutManager = this@RubbishBinFragment.layoutManager
                setPadding(0, 0, 0, scaleHeightPx(85, outMetrics))
                clipToPadding = false
                addItemDecoration(PositionDividerItemDecoration(requireActivity(),
                    resources.displayMetrics))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
                adapter = this@RubbishBinFragment.adapter
            }

            setNodes(nodes)
            checkAndConfigureAdapter(
                textRubbishBinParentHandle = getString(R.string.context_empty_rubbish_bin),
                textGeneric = getString(R.string.file_browser_empty_folder_new),
                colorPrimary = R.color.grey_900_grey_100,
                colorSecondary = R.color.grey_300_grey_600
            )

            listBinding.root
        } else {
            Timber.d("Grid View")
            _gridBinding = FragmentRubbishbingridBinding.inflate(inflater, container, false)
            recyclerView = gridBinding.rubbishbinGridView
            emptyImageView = gridBinding.rubbishbinGridEmptyImage
            emptyTextView = gridBinding.rubbishbinGridEmptyTextFirst

            gridLayoutManager = recyclerView?.layoutManager as CustomizedGridLayoutManager

            adapter?.let {
                it.parentHandle = rubbishBinViewModel.state.value.rubbishBinHandle
                it.setListFragment(recyclerView)
                it.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
            } ?: run {
                MegaNodeAdapter(requireActivity(),
                    this@RubbishBinFragment,
                    nodes,
                    rubbishBinViewModel.state.value.rubbishBinHandle,
                    recyclerView,
                    Constants.RUBBISH_BIN_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                    sortByHeaderViewModel)
            }
            adapter?.isMultipleSelect = false

            recyclerView?.apply {
                setHasFixedSize(true)
                itemAnimator = DefaultItemAnimator()
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        checkScroll()
                    }
                })
                adapter = this@RubbishBinFragment.adapter
            }

            gridLayoutManager.spanSizeLookup =
                adapter?.getSpanSizeLookup(gridLayoutManager.spanCount)

            setNodes(nodes)
            checkAndConfigureAdapter(
                textRubbishBinParentHandle = getString(R.string.context_empty_rubbish_bin),
                textGeneric = getString(R.string.file_browser_empty_folder_new),
                colorPrimary = R.color.grey_900_grey_100,
                colorSecondary = R.color.grey_300_grey_600
            )
            gridBinding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DragToExitSupport.observeDragSupportEvents(viewLifecycleOwner,
            recyclerView,
            Constants.VIEWER_FROM_RUBBISH_BIN)
    }

    /**
     * This method checks scroll of recycler view
     */
    fun checkScroll() {
        recyclerView?.let {
            if ((it.canScrollVertically(-1) && it.isVisible()) ||
                (adapter?.isMultipleSelect == true)
            ) {
                (requireActivity() as ManagerActivity).changeAppBarElevation(true)
            } else {
                (requireActivity() as ManagerActivity).changeAppBarElevation(false)
            }
        }
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    /**
     * This method will format text to be displayed on fragment when we need to show empty message
     * @param text Text to be formatted and displayed
     * @param colorResPrimary Primary color for the text to be highlighted
     * @param colorResSecondary Secondary color for the text to be displayed
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun formatRequiredText(
        text: String,
        @ColorRes colorResPrimary: Int,
        @ColorRes colorResSecondary: Int,
    ): String {
        return runCatching {
            var textToShow = text
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + getColorHexString(requireActivity(), colorResPrimary)
                    + "\'>")
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace("[B]", "<font color=\'"
                    + getColorHexString(requireActivity(), colorResSecondary)
                    + "\'>")
            textToShow = textToShow.replace("[/B]", "</font>")
            textToShow
        }.getOrElse {
            throw it
        }
    }

    /**
     * Action to be performed based on adapter's items
     * @param textRubbishBinParentHandle text when rubbishBinParentHandle rubbish node and rubbishBinParentHandle are same and no items are present in adapter
     * @param textGeneric generic text to be displayed when adapter has no count
     * @param colorPrimary Primary color for the text to be highlighted
     * @param colorSecondary Secondary color for the text to be displayed
     */
    private fun checkAndConfigureAdapter(
        textRubbishBinParentHandle: String,
        textGeneric: String,
        @ColorRes colorPrimary: Int,
        @ColorRes colorSecondary: Int,
    ) {
        if (adapter?.itemCount == 0) {
            recyclerView?.visibility = View.GONE
            emptyImageView.visibility = View.VISIBLE
            emptyTextView.visibility = View.VISIBLE

            if (megaApi.rubbishNode.handle == rubbishBinViewModel.state.value.rubbishBinHandle ||
                rubbishBinViewModel.state.value.rubbishBinHandle == -1L
            ) {
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_portrait)
                }
                runCatching {
                    emptyTextView.text = Html.fromHtml(
                        formatRequiredText(
                            text = textRubbishBinParentHandle,
                            colorResPrimary = colorPrimary,
                            colorResSecondary = colorSecondary
                        ), Html.FROM_HTML_MODE_LEGACY)
                }.getOrElse {

                }
            } else {
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_folder_landscape)
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_folder_portrait)
                }
                runCatching {
                    emptyTextView.text = Html.fromHtml(
                        formatRequiredText(
                            text = textGeneric,
                            colorResPrimary = colorPrimary,
                            colorResSecondary = colorSecondary
                        ), Html.FROM_HTML_MODE_LEGACY)

                }.getOrElse {

                }
            }
        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyImageView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }
    }

    /**
     * With this method, when adapter's multiselect is off,
     * turn it on and activate action mode
     */
    fun activateActionMode() {
        Timber.d("activateActionMode")
        if (adapter?.isMultipleSelect == false) {
            adapter?.isMultipleSelect = true
            actionMode =
                (requireActivity() as AppCompatActivity).startSupportActionMode(ActionBarCallback())
        }
    }

    private inner class ActionBarCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.rubbish_bin_action, menu)
            checkScroll()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menu?.findItem(R.id.cab_menu_select_all)?.isVisible =
                adapter!!.selectedItemCount < adapter!!.itemCount - adapter!!.placeholderCount

            var isRestoreVisible = true
            val documents = adapter?.selectedNodes
            documents?.forEach { node ->
                val restoreHandle = node.restoreHandle
                if (restoreHandle == INVALID_HANDLE) return@forEach
                val restoreNode = megaApi.getNodeByHandle(restoreHandle)
                if (restoreNode == null || megaApi.isInRubbish(restoreNode) || megaApi.isInInbox(
                        restoreNode)
                ) {
                    isRestoreVisible = false
                    return@forEach
                }
            }

            menu?.findItem(R.id.cab_menu_restore_from_rubbish)?.isVisible = isRestoreVisible

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (!managerViewModel.isConnected) {
                ((requireActivity()) as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
                return false
            }
            val documents = adapter?.selectedNodes
            when (item?.itemId) {
                R.id.cab_menu_restore_from_rubbish -> {
                    ((requireActivity()) as ManagerActivity).restoreFromRubbish(documents)
                    clearSelections()
                    hideMultipleSelect()
                }
                R.id.cab_menu_delete -> {
                    val handleList = arrayListOf<Long>()
                    documents?.forEach {
                        handleList.add(it.handle)
                    }

                    ((requireActivity()) as ManagerActivity).askConfirmationMoveToRubbish(handleList)
                }
                R.id.cab_menu_select_all -> selectAll()
                R.id.cab_menu_clear_selection -> {
                    clearSelections()
                    hideMultipleSelect()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Timber.d("onDestroyActionMode")
            clearSelections()
            adapter?.isMultipleSelect = false
            checkScroll()
        }

    }

    /**
     * Select all items from adapter
     */
    fun selectAll() {
        adapter?.let {
            if (it.isMultipleSelect) {
                it.selectAll()

            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    ActionBarCallback())
            }

        }
        Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
    }

    /**
     * To show select menu item
     * @return if adapter's multiselect is on or off
     */
    fun showSelectMenuItem() = adapter?.isMultipleSelect ?: false

    /**
     * When an item clicked from adapter it calls below method
     * @param position Position of item which is clicked
     */
    fun itemClick(position: Int) {
        Timber.d("Position:$position")
        if (adapter?.isMultipleSelect == true) {
            Timber.d("Multiselect ON")
            adapter?.toggleSelection(position)

            val selectedNodes = adapter?.selectedNodes
            if (selectedNodes.isNullOrEmpty().not()) {
                updateActionModeTitle()
            }
        } else {
            if (nodes[position].isFolder) {
                val n = nodes[position]
                val lastFirstVisiblePosition = if ((requireActivity() as ManagerActivity).isList) {
                    layoutManager.findFirstCompletelyVisibleItemPosition()
                } else {
                    val pos =
                        (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
                    if (pos == -1) {
                        Timber.w("Completely -1 then find just visible position")
                        (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()
                    }
                    pos
                }
                Timber.d("Push to stack %d position", lastFirstVisiblePosition)
                lastPositionStack.push(lastFirstVisiblePosition)
                rubbishBinViewModel.setRubbishBinHandle(n.handle)

                (requireActivity() as ManagerActivity).setToolbarTitle()
                (requireActivity() as ManagerActivity).supportInvalidateOptionsMenu()

                adapter?.parentHandle = rubbishBinViewModel.state.value.rubbishBinHandle
                nodes = megaApi.getChildren(nodes[position],
                    sortOrderToInt(managerViewModel.getOrder()))
                adapter?.setNodes(nodes)
                recyclerView?.scrollToPosition(0)

                //If folder has no files
                checkAndConfigureAdapter(
                    textRubbishBinParentHandle = getString(R.string.context_empty_rubbish_bin),
                    textGeneric = getString(R.string.file_browser_empty_folder_new),
                    colorPrimary = R.color.grey_900_grey_100,
                    colorSecondary = R.color.grey_300_grey_600
                )
                checkScroll()
            } else {
                //Is FILE
                if (MimeTypeList.typeForName(nodes[position].name).isImage) {
                    val intent = ImageViewerActivity.getIntentForParentNode(
                        requireActivity(),
                        megaApi.getParentNode(nodes[position]).handle,
                        managerViewModel.getOrder(),
                        nodes[position].handle
                    )
                    DragToExitSupport.putThumbnailLocation(intent,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_RUBBISH_BIN,
                        adapter)
                    startActivity(intent)
                    ((requireActivity()) as ManagerActivity).overridePendingTransition(0, 0)
                } else if (MimeTypeList.typeForName(nodes[position].name).isVideoReproducible ||
                    MimeTypeList.typeForName(nodes[position].name).isAudio
                ) {
                    val file = nodes[position]
                    val mimeType = MimeTypeList.typeForName(file.name).type
                    Timber.d("FILE HANDLE: ${file.handle}, TYPE: $mimeType")
                    var opusFile = false
                    val intentInternalIntentPair =
                        if (MimeTypeList.typeForName(file.name).isVideoNotSupported ||
                            MimeTypeList.typeForName(file.name).isAudioNotSupported
                        ) {
                            val s = file.name.split("\\.".toRegex())
                            if (s.size > 1 && s[s.size - 1] == "opus") {
                                opusFile = true
                            }
                            Pair(Intent(Intent.ACTION_VIEW), false)
                        } else {
                            Pair(getMediaIntent(context, nodes[position].name), true)
                        }

                    intentInternalIntentPair.first.putExtra("placeholder",
                        adapter?.placeholderCount)
                    DragToExitSupport.putThumbnailLocation(intentInternalIntentPair.first,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_RUBBISH_BIN,
                        adapter)
                    intentInternalIntentPair.first.apply {
                        putExtra("FILENAME", file.name)
                        putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER)
                        if (megaApi.getParentNode(nodes[position]).type == MegaNode.TYPE_RUBBISH) {
                            putExtra("parentNodeHandle", -1L)
                        } else {
                            putExtra("parentNodeHandle",
                                megaApi.getParentNode(nodes[position]).handle)
                        }
                    }
                    val localPath = FileUtil.getLocalFile(file)
                    localPath?.let {
                        val mediaFile = File(it)
                        if (it.contains(Environment.getExternalStorageDirectory().path)) {
                            intentInternalIntentPair.first.setDataAndType(
                                FileProvider.getUriForFile(requireActivity(),
                                    "mega.privacy.android.app.providers.fileprovider",
                                    mediaFile),
                                MimeTypeList.typeForName(file.name).type)
                        } else {
                            intentInternalIntentPair.first.setDataAndType(
                                Uri.fromFile(mediaFile),
                                MimeTypeList.typeForName(file.name).type)
                        }
                        intentInternalIntentPair.first.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } ?: run {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            intentInternalIntentPair.first.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                                true)
                        }

                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(mi)

                        if (mi.totalMem > Constants.BUFFER_COMP) {
                            Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                        } else {
                            Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                        }

                        val url = megaApi.httpServerGetLocalLink(file)
                        intentInternalIntentPair.first.setDataAndType(Uri.parse(url), mimeType)
                    }
                    intentInternalIntentPair.first.putExtra("HANDLE", file.handle)
                    if (opusFile) {
                        intentInternalIntentPair.first.setDataAndType(intentInternalIntentPair.first.data,
                            "audio/*")
                    }
                    if (intentInternalIntentPair.second) {
                        startActivity(intentInternalIntentPair.first)
                    } else {
                        if (MegaApiUtils.isIntentAvailable(context,
                                intentInternalIntentPair.first)
                        ) {
                            startActivity(intentInternalIntentPair.first)
                        } else {
                            (requireActivity() as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                                getString(R.string.intent_not_available),
                                -1)
                            adapter?.notifyDataSetChanged()
                            (requireActivity() as ManagerActivity).saveNodesToDevice(
                                Collections.singletonList(nodes[position]),
                                true, false, false, false)
                        }
                    }
                    (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
                } else if (MimeTypeList.typeForName(nodes[position].name).isPdf) {

                    val file = nodes[position]

                    val mimeType = MimeTypeList.typeForName(file.name).type
                    Timber.d("FILE HANDLE: ${file.handle}, TYPE: $mimeType")

                    val pdfIntent = Intent(requireActivity(), PdfViewerActivity::class.java)
                    pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    pdfIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

                    pdfIntent.putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER)
                    pdfIntent.putExtra("inside", true)
                    pdfIntent.putExtra("APP", true)

                    val localPath = FileUtil.getLocalFile(file)

                    localPath?.let {
                        val mediaFile = File(it)
                        if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                            pdfIntent.setDataAndType(
                                FileProvider.getUriForFile(
                                    requireActivity(),
                                    "mega.privacy.android.app.providers.fileprovider",
                                    mediaFile
                                ),
                                MimeTypeList.typeForName(file.name).type)
                        } else {
                            pdfIntent.setDataAndType(Uri.fromFile(mediaFile),
                                MimeTypeList.typeForName(file.name).type)
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } ?: run {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                                true)
                        }

                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(mi)

                        if (mi.totalMem > Constants.BUFFER_COMP) {
                            Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                        } else {
                            Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                        }

                        val url = megaApi.httpServerGetLocalLink(file)
                        pdfIntent.setDataAndType(Uri.parse(url), mimeType)
                    }
                    pdfIntent.putExtra("HANDLE", file.handle)
                    DragToExitSupport.putThumbnailLocation(pdfIntent,
                        recyclerView,
                        position,
                        Constants.VIEWER_FROM_RUBBISH_BIN,
                        adapter)
                    if (MegaApiUtils.isIntentAvailable(context, pdfIntent)) {
                        startActivity(pdfIntent)
                    } else {
                        Toast.makeText(context,
                            getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG).show()

                        (requireActivity() as ManagerActivity).saveNodesToDevice(
                            Collections.singletonList(nodes[position]),
                            true, false, false, false)
                    }
                    (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
                } else if (MimeTypeList.typeForName(nodes[position].name).isURL) {
                    MegaNodeUtil.manageURLNode(requireActivity(), megaApi, nodes[position])
                } else if (MimeTypeList.typeForName(nodes[position].name)
                        .isOpenableTextFile(nodes[position].size)
                ) {
                    MegaNodeUtil.manageTextFileIntent(requireActivity(),
                        nodes[position],
                        Constants.RUBBISH_BIN_ADAPTER)
                } else {
                    adapter?.notifyDataSetChanged()
                    MegaNodeUtil.onNodeTapped(requireActivity(),
                        nodes[position],
                        ((requireActivity()) as ManagerActivity)::saveNodeByTap,
                        requireActivity() as ManagerActivity,
                        requireActivity() as ManagerActivity)
                }
            }
        }
    }

    private fun updateActionModeTitle() {
        actionMode?.let {
            val documents = adapter?.selectedNodes
            var files = 0
            var folders = 0
            documents?.forEach {
                if (it.isFile) files++
                else if (it.isFolder) folders++
            }
            val sum = files + folders
            val title = if (sum == 0) "0"
            else if (files == 0) "0"
            else if (folders == 0) "0"
            else "$sum"
            actionMode?.title = title

            runCatching {
                actionMode?.invalidate()
            }.getOrElse {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }

    /*
    * Clear all selected item
    */
    private fun clearSelections() {
        if (adapter?.isMultipleSelect == true) {
            adapter?.clearSelections()
        }
    }

    /**
     * Hides multi select option
     */
    fun hideMultipleSelect() {
        adapter?.isMultipleSelect = false
        actionMode?.finish()
    }

    /**
     * On back pressed clicked on activity
     */
    fun onBackPressed(): Int {
        return adapter?.let {
            with(requireActivity() as ManagerActivity) {
                if (comesFromNotifications && comesFromNotificationHandle == rubbishBinViewModel.state.value.rubbishBinHandle) {
                    comesFromNotifications = false
                    comesFromNotificationHandle = -1
                    selectDrawerItem(DrawerItem.NOTIFICATIONS)
                    rubbishBinViewModel.setRubbishBinHandle(comesFromNotificationHandleSaved)
                    comesFromNotificationHandleSaved = -1
                    2
                } else {
                    val parentNode =
                        megaApi.getParentNode(megaApi.getNodeByHandle(rubbishBinViewModel.state.value.rubbishBinHandle))
                    parentNode?.let {
                        recyclerView?.visibility = View.VISIBLE
                        emptyImageView.visibility = View.GONE
                        emptyTextView.visibility = View.GONE
                        supportInvalidateOptionsMenu()
                        rubbishBinViewModel.setRubbishBinHandle(parentNode.handle)
                        setToolbarTitle()
                        nodes = megaApi.getChildren(parentNode,
                            sortOrderToInt(managerViewModel.getOrder()))
                        adapter?.setNodes(nodes)
                        val lastVisiblePosition = if (!lastPositionStack.empty()) {
                            Timber.d("Pop of the stack ${lastPositionStack.pop()} position")
                            lastPositionStack.pop()
                        } else 0

                        Timber.d("Scroll to $lastVisiblePosition position")
                        if (lastVisiblePosition >= 0) {
                            if (isList) {
                                layoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                            } else {
                                gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition,
                                    0)
                            }
                        }
                        2
                    } ?: run {
                        0
                    }
                }
            }
        } ?: run {
            0
        }
    }

    /**
     * Get current recyclerview
     * @return RecyclerView
     */
    fun getRecyclerView() = recyclerView

    /**
     * This method set nodes and updates the adapter
     * @param nodes List of Mega Nodes
     */
    fun setNodes(nodes: List<MegaNode>) {
        Timber.d("setNodes")
        this.nodes = nodes

        if (megaApi.rubbishNode == null) {
            Timber.e("megaApi.getRubbishNode() is NULL")
            return
        }
        adapter?.let {
            it.setNodes(nodes)
            checkAndConfigureAdapter(
                textRubbishBinParentHandle = getString(R.string.context_empty_rubbish_bin),
                textGeneric = getString(R.string.file_browser_empty_folder_new),
                colorPrimary = R.color.grey_900_grey_100,
                colorSecondary = R.color.grey_300_grey_600
            )
        }
    }

    /**
     * Updates the adapter items
     */
    fun notifyDataSetChanged() = adapter?.notifyDataSetChanged()

    /**
     * If adapter's multiple select is on or off
     */
    fun isMultipleselect() = adapter?.isMultipleSelect ?: false

    /**
     * Gets total number of items in an adapter
     */
    fun getItemCount() = adapter?.itemCount ?: -1

    override fun onDestroyView() {
        super.onDestroyView()
        _listBinding = null
        _gridBinding = null
    }
}
