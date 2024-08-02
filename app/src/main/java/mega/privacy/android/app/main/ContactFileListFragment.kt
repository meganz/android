package mega.privacy.android.app.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.listeners.FabButtonListener
import mega.privacy.android.app.presentation.contact.ContactFileListViewModel
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity.Companion.createSecondaryIntent
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.StartUpload
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Stack
import javax.inject.Inject

@AndroidEntryPoint
class ContactFileListFragment : ContactFileBaseFragment() {
    private var actionMode: ActionMode? = null
    var mainLayout: CoordinatorLayout? = null
    var listView: RecyclerView? = null
    var mLayoutManager: LinearLayoutManager? = null
    var emptyImageView: ImageView? = null
    var emptyTextView: TextView? = null
    var fab: FloatingActionButton? = null
    var parentHandleStack: Stack<Long>? = Stack()
    var currNodePosition: Int = -1

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    /**
     * [MegaNavigator] injection
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private var startDownloadViewModel: StartDownloadViewModel? = null

    private var viewModel: ContactFileListViewModel? = null

    var handler: Handler? = null

    override fun activateActionMode() {
        Timber.d("activateActionMode")
        if (!adapter.isMultipleSelect) {
            adapter.isMultipleSelect = true
            adapter.notifyDataSetChanged()
            actionMode = (context as AppCompatActivity).startSupportActionMode(ActionBarCallBack())
        }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val documents = adapter.selectedNodes

            val itemId = item.itemId
            if (itemId == R.id.cab_menu_download) {
                (context as ContactFileListActivity).downloadFile(documents)
            } else if (itemId == R.id.cab_menu_copy) {
                val handleList = ArrayList<Long>()
                for (i in documents.indices) {
                    handleList.add(documents[i].handle)
                }

                (context as ContactFileListActivity).showCopy(handleList)
            } else if (itemId == R.id.cab_menu_select_all) {
                selectAll()
            } else if (itemId == R.id.cab_menu_unselect_all) {
                clearSelections()
            } else if (itemId == R.id.cab_menu_leave_multiple_share) {
                val handleList = ArrayList<Long>()
                for (i in documents.indices) {
                    handleList.add(documents[i].handle)
                }

                megaNodeUtilWrapper.showConfirmationLeaveIncomingShares(
                    requireActivity(),
                    (requireActivity() as SnackbarShower), handleList
                )
            } else if (itemId == R.id.cab_menu_trash) {
                val handleList = ArrayList<Long>()
                for (i in documents.indices) {
                    handleList.add(documents[i].handle)
                }
                (context as ContactFileListActivity).askConfirmationMoveToRubbish(handleList)
            } else if (itemId == R.id.cab_menu_rename) {
                val node = documents[0]
                showRenameNodeDialog(
                    context, node, activity as SnackbarShower?,
                    activity as ActionNodeCallback?
                )
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.file_browser_action, menu)
            fab!!.hide()
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode")
            clearSelections()
            adapter.isMultipleSelect = false
            adapter.notifyDataSetChanged()
            setFabVisibility(megaApi.getNodeByHandle(_parentHandle))
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selected = adapter.selectedNodes

            menu.findItem(R.id.cab_menu_share_link)
                .setTitle(
                    resources.getQuantityString(
                        mega.privacy.android.shared.resources.R.plurals.label_share_links,
                        selected.size
                    )
                )

            val areAllNotTakenDown = selected.areAllNotTakenDown()
            var showRename = false
            var showMove = false
            var showTrash = false

            // Rename
            if (selected.size == 1) {
                if ((megaApi.checkAccessErrorExtended(
                        selected[0],
                        MegaShare.ACCESS_FULL
                    ).errorCode == MegaError.API_OK) || (megaApi.checkAccessErrorExtended(
                        selected[0], MegaShare.ACCESS_READWRITE
                    ).errorCode == MegaError.API_OK)
                ) {
                    showRename = true
                }
            }

            if (selected.size > 0) {
                if ((megaApi.checkAccessErrorExtended(
                        selected[0],
                        MegaShare.ACCESS_FULL
                    ).errorCode == MegaError.API_OK) || (megaApi.checkAccessErrorExtended(
                        selected[0], MegaShare.ACCESS_READWRITE
                    ).errorCode == MegaError.API_OK)
                ) {
                    showMove = true
                }
            }

            if (selected.size != 0) {
                showMove = false
                // Rename
                if (selected.size == 1) {
                    if ((megaApi.checkAccessErrorExtended(
                            selected[0],
                            MegaShare.ACCESS_FULL
                        ).errorCode == MegaError.API_OK)
                    ) {
                        showMove = true
                        showRename = true
                    } else if (megaApi.checkAccessErrorExtended(
                            selected[0],
                            MegaShare.ACCESS_READWRITE
                        ).errorCode == MegaError.API_OK
                    ) {
                        showMove = false
                        showRename = false
                    }
                } else {
                    showRename = false
                    showMove = false
                }

                for (i in selected.indices) {
                    if (megaApi.checkMoveErrorExtended(
                            selected[i],
                            megaApi.rubbishNode
                        ).errorCode != MegaError.API_OK
                    ) {
                        showMove = false
                        break
                    }
                }

                if (!(context as ContactFileListActivity).isEmptyParentHandleStack) {
                    showTrash = true
                }
                for (i in selected.indices) {
                    if ((megaApi.checkAccessErrorExtended(
                            selected[i],
                            MegaShare.ACCESS_FULL
                        ).errorCode != MegaError.API_OK)
                    ) {
                        showTrash = false
                        break
                    }
                }

                if (selected.size == adapter.itemCount) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false)
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true)
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true)
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true)
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false)
            }

            if (areAllNotTakenDown) {
                menu.findItem(R.id.cab_menu_download).setVisible(true)
                menu.findItem(R.id.cab_menu_download)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true)
                menu.findItem(R.id.cab_menu_leave_multiple_share)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.findItem(R.id.cab_menu_copy).setVisible(true)
            }

            menu.findItem(R.id.cab_menu_rename).setVisible(showRename)
            menu.findItem(R.id.cab_menu_move).setVisible(showMove)
            menu.findItem(R.id.cab_menu_share_link).setVisible(false)
            menu.findItem(R.id.cab_menu_trash).setVisible(showTrash)

            return false
        }
    }

    private fun List<MegaNode?>.areAllNotTakenDown(): Boolean {
        for (node in this) {
            if (node?.isTakenDown == true) {
                return false
            }
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PARENT_HANDLE_STACK, parentHandleStack)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            parentHandleStack =
                savedInstanceState.getSerializable(PARENT_HANDLE_STACK) as Stack<Long>?
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")
        var v: View? = null
        handler = Handler()
        if (userEmail != null) {
            v = inflater.inflate(R.layout.fragment_contact_file_list, container, false)

            mainLayout =
                v.findViewById<View>(R.id.contact_file_list_coordinator_layout) as CoordinatorLayout

            fab =
                v.findViewById<View>(R.id.floating_button_contact_file_list) as FloatingActionButton
            fab!!.setOnClickListener(FabButtonListener(context))
            fab!!.hide()

            contact = megaApi.getContact(userEmail)
            if (contact == null) {
                return null
            }

            _parentHandle = (context as ContactFileListActivity).getParentHandle()
            if (_parentHandle != -1L) {
                val parentNode = megaApi.getNodeByHandle(_parentHandle)
                contactNodes =
                    megaApi.getChildren(parentNode, sortOrderIntMapper.invoke(orderGetChildren))
                (context as ContactFileListActivity).setTitleActionBar(parentNode!!.name)
            } else {
                contactNodes = megaApi.getInShares(contact)
            }

            listView = v.findViewById<View>(R.id.contact_file_list_view_browser) as RecyclerView
            listView!!.addItemDecoration(SimpleDividerItemDecoration(context))
            mLayoutManager = LinearLayoutManager(context)
            listView!!.layoutManager = mLayoutManager
            listView!!.itemAnimator = Util.noChangeRecyclerViewItemAnimator()

            val res = resources
            val valuePaddingTop =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, res.displayMetrics)
                    .toInt()
            val valuePaddingBottom =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88f, res.displayMetrics)
                    .toInt()

            listView!!.clipToPadding = false
            listView!!.setPadding(0, valuePaddingTop, 0, valuePaddingBottom)
            listView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })

            emptyImageView = v.findViewById<View>(R.id.contact_file_list_empty_image) as ImageView
            emptyTextView = v.findViewById<View>(R.id.contact_file_list_empty_text) as TextView
            if (contactNodes.size != 0) {
                emptyImageView!!.visibility = View.GONE
                emptyTextView!!.visibility = View.GONE
                listView!!.visibility = View.VISIBLE
            } else {
                emptyImageView!!.visibility = View.VISIBLE
                emptyTextView!!.visibility = View.VISIBLE
                listView!!.visibility = View.GONE

                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView!!.setImageResource(R.drawable.incoming_empty_landscape)
                } else {
                    emptyImageView!!.setImageResource(R.drawable.incoming_shares_empty)
                }
                var textToShow = String.format(context.getString(R.string.context_empty_incoming))
                try {
                    textToShow = textToShow.replace(
                        "[A]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                + "\'>"
                    ).replace("[/A]", "</font>").replace(
                        "[B]", "<font color=\'"
                                + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                + "\'>"
                    ).replace("[/B]", "</font>")
                } catch (e: Exception) {
                }
                val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                emptyTextView!!.text = result
            }

            if (adapter == null) {
                adapter = MegaNodeAdapter(
                    context, this, contactNodes, _parentHandle,
                    listView, Constants.CONTACT_FILE_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                )
            } else {
                adapter.setNodes(contactNodes)
                adapter.parentHandle = _parentHandle
            }

            adapter.isMultipleSelect = false

            listView!!.adapter = adapter
        }
        if (currNodePosition != -1 && _parentHandle == -1L) {
            itemClick(currNodePosition)
        }
        showFabButton(megaApi.getNodeByHandle(_parentHandle))
        addStartDownloadTransferView(v)
        return v
    }

    private fun addStartDownloadTransferView(root: View?) {
        if (root is ViewGroup && activity != null) {
            startDownloadViewModel = ViewModelProvider(requireActivity()).get(
                StartDownloadViewModel::class.java
            )
            viewModel = ViewModelProvider(requireActivity()).get(
                ContactFileListViewModel::class.java
            )
            root.addView(
                createStartTransferView(
                    requireActivity(),
                    startDownloadViewModel!!.state,
                    {
                        if ((startDownloadViewModel!!.state.value as StateEventWithContentTriggered<TransferTriggerEvent?>).content is StartUpload) {
                            viewModel!!.consumeUploadEvent()
                        }
                        startDownloadViewModel!!.consumeDownloadEvent()
                        Unit
                    },
                    { StartTransferEvent: StartTransferEvent? -> Unit }
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeDragSupportEvents(
            viewLifecycleOwner,
            listView,
            Constants.VIEWER_FROM_CONTACT_FILE_LIST
        )
    }

    fun checkScroll() {
        val withElevation =
            (listView != null && listView!!.canScrollVertically(-1) && listView!!.visibility == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect)
        val abL = requireActivity().findViewById<AppBarLayout>(R.id.app_bar_layout)
        Util.changeActionBarElevation(requireActivity(), abL, withElevation)
    }

    fun showOptionsPanel(sNode: MegaNode) {
        Timber.d("Node handle: %s", sNode.handle)
        (context as ContactFileListActivity).showOptionsPanel(sNode)
    }

    fun setNodes(parentHandle: Long) {
        if (megaApi.getNodeByHandle(parentHandle) != null) {
            this._parentHandle = parentHandle
            (context as ContactFileListActivity).setParentHandle(parentHandle)
            adapter.parentHandle = parentHandle
            setNodes(
                megaApi.getChildren(
                    megaApi.getNodeByHandle(parentHandle),
                    sortOrderIntMapper.invoke(orderGetChildren)
                )
            )
        }
    }

    fun setNodes(nodes: ArrayList<MegaNode?>?) {
        this.contactNodes = nodes
        if (adapter != null) {
            adapter.setNodes(contactNodes)
            if (adapter.itemCount == 0) {
                listView!!.visibility = View.GONE
                emptyImageView!!.visibility = View.VISIBLE
                emptyTextView!!.visibility = View.VISIBLE
                if (megaApi.rootNode!!.handle == _parentHandle) {
                    emptyImageView!!.setImageResource(R.drawable.ic_empty_cloud_drive)
                    emptyTextView!!.setText(R.string.file_browser_empty_cloud_drive)
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView!!.setImageResource(R.drawable.incoming_empty_landscape)
                    } else {
                        emptyImageView!!.setImageResource(R.drawable.incoming_shares_empty)
                    }
                    var textToShow =
                        String.format(context.getString(R.string.context_empty_incoming))
                    try {
                        textToShow = textToShow.replace(
                            "[A]", "<font color=\'"
                                    + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                    + "\'>"
                        ).replace("[/A]", "</font>").replace(
                            "[B]", "<font color=\'"
                                    + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                    + "\'>"
                        ).replace("[/B]", "</font>")
                    } catch (e: Exception) {
                    }
                    val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                    emptyTextView!!.text = result
                }
            } else {
                listView!!.visibility = View.VISIBLE
                emptyImageView!!.visibility = View.GONE
                emptyTextView!!.visibility = View.GONE
            }
        }
    }

    fun itemClick(position: Int) {
        if (adapter.isMultipleSelect) {
            Timber.d("Multiselect ON")
            adapter.toggleSelection(position)

            val selectedNodes = adapter.selectedNodes
            if (selectedNodes.size > 0) {
                updateActionModeTitle()
            }
        } else {
            if (contactNodes[position].isFolder) {
                navigateToFolder(contactNodes[position])
            } else {
                if (typeForName(contactNodes[position].name).isImage) {
                    val anchorNode = contactNodes[position]
                    val anchorNodeHandle = anchorNode.handle
                    val parentNode = megaApi.getParentNode(anchorNode) ?: return

                    val parentNodeHandle = parentNode.handle

                    val previewParams: MutableMap<String, Any> = HashMap()
                    previewParams[SharedItemsImageNodeFetcher.PARENT_ID] = parentNodeHandle

                    val intent = createSecondaryIntent(
                        requireContext(),
                        ImagePreviewFetcherSource.SHARED_ITEMS,
                        ImagePreviewMenuSource.SHARED_ITEMS,
                        anchorNodeHandle,
                        previewParams,
                        false
                    )
                    startActivity(intent)
                } else if (typeForName(contactNodes[position].name).isVideoMimeType || typeForName(
                        contactNodes[position].name
                    ).isAudio
                ) {
                    viewModel?.let {
                        viewLifecycleOwner.lifecycleScope.launch {
                            runCatching {
                                val megaNode = contactNodes[position]
                                val contentUri = it.getNodeContentUri(megaNode.handle)
                                val localPath = FileUtil.getLocalFile(megaNode)
                                if (localPath != null) {
                                    val file = File(localPath)
                                    megaNavigator.openMediaPlayerActivityByLocalFile(
                                        context = requireContext(),
                                        localFile = file,
                                        handle = megaNode.handle,
                                        parentId = megaNode.parentHandle,
                                        viewType = CONTACT_FILE_ADAPTER,
                                        sortOrder = orderGetChildren,
                                    )
                                } else {
                                    megaNavigator.openMediaPlayerActivity(
                                        context = requireContext(),
                                        contentUri = contentUri,
                                        name = megaNode.name,
                                        handle = megaNode.handle,
                                        parentId = megaNode.parentHandle,
                                        viewType = CONTACT_FILE_ADAPTER,
                                        sortOrder = orderGetChildren
                                    )
                                }
                            }.onFailure { exception ->
                                Timber.e(exception)
                                (context as ContactFileListActivity).showSnackbar(
                                    Constants.SNACKBAR_TYPE, context.resources.getString(
                                        R.string.intent_not_available
                                    )
                                )
                                adapter.notifyDataSetChanged()
                                (context as ContactFileListActivity).downloadFile(
                                    listOf(contactNodes[position])
                                )
                            }
                        }
                    }
                } else if (typeForName(contactNodes[position].name).isPdf) {
                    val file = contactNodes[position]

                    val mimeType = typeForName(file.name).type
                    Timber.d("NODE HANDLE: %d, TYPE: %s", file.handle, mimeType)

                    val pdfIntent = Intent(context, PdfViewerActivity::class.java)
                    pdfIntent.putExtra("inside", true)
                    pdfIntent.putExtra("adapterType", Constants.CONTACT_FILE_ADAPTER)

                    val localPath = FileUtil.getLocalFile(file)
                    if (localPath != null) {
                        val mediaFile = File(localPath)
                        if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                            pdfIntent.setDataAndType(
                                FileProvider.getUriForFile(
                                    context,
                                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                    mediaFile
                                ), typeForName(file.name).type
                            )
                        } else {
                            pdfIntent.setDataAndType(
                                Uri.fromFile(mediaFile),
                                typeForName(file.name).type
                            )
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            pdfIntent.putExtra(
                                Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                                true
                            )
                        }

                        val url = megaApi.httpServerGetLocalLink(file)
                        pdfIntent.setDataAndType(Uri.parse(url), mimeType)
                    }
                    pdfIntent.putExtra("HANDLE", file.handle)
                    putThumbnailLocation(
                        pdfIntent,
                        listView,
                        position,
                        Constants.VIEWER_FROM_CONTACT_FILE_LIST,
                        adapter
                    )
                    if (MegaApiUtils.isIntentAvailable(context, pdfIntent)) {
                        startActivity(pdfIntent)
                    } else {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG
                        ).show()

                        (context as ContactFileListActivity).downloadFile(
                            listOf(contactNodes[position])
                        )
                    }
                    (context as ContactFileListActivity).overridePendingTransition(0, 0)
                } else if (typeForName(contactNodes[position].name).isURL) {
                    megaNodeUtilWrapper.manageURLNode(context, megaApi, contactNodes[position])
                } else if (typeForName(contactNodes[position].name).isOpenableTextFile(
                        contactNodes[position].size
                    )
                ) {
                    megaNodeUtilWrapper.manageTextFileIntent(
                        requireContext(),
                        contactNodes[position],
                        Constants.CONTACT_FILE_ADAPTER
                    )
                } else {
                    adapter.notifyDataSetChanged()
                    (context as ContactFileListActivity).downloadFile(
                        listOf(contactNodes[position])
                    )
                }
            }
        }
    }

    /**
     * Navigates to a child folder.
     *
     * @param node The folder node.
     */
    fun navigateToFolder(node: MegaNode) {
        var lastFirstVisiblePosition = 0

        lastFirstVisiblePosition = mLayoutManager!!.findFirstCompletelyVisibleItemPosition()

        Timber.d("Push to stack %d position", lastFirstVisiblePosition)
        lastPositionStack.push(lastFirstVisiblePosition)

        (context as ContactFileListActivity).setTitleActionBar(node.name)
        (context as ContactFileListActivity).supportInvalidateOptionsMenu()

        parentHandleStack!!.push(_parentHandle)
        _parentHandle = node.handle
        adapter.parentHandle = _parentHandle
        (context as ContactFileListActivity).setParentHandle(_parentHandle)

        contactNodes = megaApi.getChildren(node)
        adapter.setNodes(contactNodes)
        listView!!.scrollToPosition(0)

        // If folder has no files
        if (adapter.itemCount == 0) {
            listView!!.visibility = View.GONE
            emptyImageView!!.visibility = View.VISIBLE
            emptyTextView!!.visibility = View.VISIBLE

            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                emptyImageView!!.setImageResource(R.drawable.incoming_empty_landscape)
            } else {
                emptyImageView!!.setImageResource(R.drawable.incoming_shares_empty)
            }
            var textToShow = String.format(context.getString(R.string.context_empty_incoming))
            try {
                textToShow = textToShow.replace(
                    "[A]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                            + "\'>"
                ).replace("[/A]", "</font>").replace(
                    "[B]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                            + "\'>"
                ).replace("[/B]", "</font>")
            } catch (e: Exception) {
            }
            val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
            emptyTextView!!.text = result
        } else {
            listView!!.visibility = View.VISIBLE
            emptyImageView!!.visibility = View.GONE
            emptyTextView!!.visibility = View.GONE
        }

        showFabButton(node)
    }

    fun onBackPressed(): Int {
        Timber.d("onBackPressed")

        _parentHandle = adapter.parentHandle
        (context as ContactFileListActivity).setParentHandle(_parentHandle)
        //If from ContactInfoActivity embedded list, return to ContactInfoActivity directly.
        if (currNodePosition != -1 && parentHandleStack!!.size == 1) {
            return 0
        }
        if (parentHandleStack!!.isEmpty()) {
            Timber.d("return 0")
            fab!!.hide()
            return 0
        } else {
            _parentHandle = parentHandleStack!!.pop()
            setFabVisibility(megaApi.getNodeByHandle(_parentHandle))
            listView!!.visibility = View.VISIBLE
            emptyImageView!!.visibility = View.GONE
            emptyTextView!!.visibility = View.GONE
            if (_parentHandle == -1L) {
                contactNodes = megaApi.getInShares(contact)
                (context as ContactFileListActivity).setTitleActionBar(null)
                (context as ContactFileListActivity).supportInvalidateOptionsMenu()
                adapter.setNodes(contactNodes)
                var lastVisiblePosition = 0
                if (!lastPositionStack.empty()) {
                    lastVisiblePosition = lastPositionStack.pop()
                    Timber.d("Pop of the stack %d position", lastVisiblePosition)
                }
                Timber.d("Scroll to %d position", lastVisiblePosition)

                if (lastVisiblePosition >= 0) {
                    mLayoutManager!!.scrollToPositionWithOffset(lastVisiblePosition, 0)
                }
                (context as ContactFileListActivity).setParentHandle(_parentHandle)
                (context as ContactFileListActivity).supportInvalidateOptionsMenu()
                adapter.parentHandle = _parentHandle
                Timber.d("return 2")
                return 2
            } else {
                contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(_parentHandle))
                (context as ContactFileListActivity).setTitleActionBar(
                    megaApi.getNodeByHandle(_parentHandle)!!.name
                )
                (context as ContactFileListActivity).supportInvalidateOptionsMenu()
                adapter.setNodes(contactNodes)
                var lastVisiblePosition = 0
                if (!lastPositionStack.empty()) {
                    lastVisiblePosition = lastPositionStack.pop()
                    Timber.d("Pop of the stack %d position", lastVisiblePosition)
                }
                Timber.d("Scroll to %d position", lastVisiblePosition)

                if (lastVisiblePosition >= 0) {
                    mLayoutManager!!.scrollToPositionWithOffset(lastVisiblePosition, 0)
                }
                (context as ContactFileListActivity).setParentHandle(_parentHandle)
                adapter.parentHandle = _parentHandle
                showFabButton(megaApi.getNodeByHandle(_parentHandle))
                Timber.d("return 3")
                return 3
            }
        }
    }

    fun setNodes() {
        contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(_parentHandle))
        adapter.setNodes(contactNodes)
        listView!!.invalidate()
    }

    fun selectAll() {
        if (adapter != null) {
            if (adapter.isMultipleSelect) {
                adapter.selectAll()
            } else {
                adapter.isMultipleSelect = true
                adapter.selectAll()

                actionMode =
                    (context as AppCompatActivity).startSupportActionMode(ActionBarCallBack())
            }

            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    override fun updateActionModeTitle() {
        if (actionMode == null) {
            return
        }
        val documents = adapter.selectedNodes
        var files = 0
        var folders = 0
        for (document in documents) {
            if (document.isFile) {
                files++
            } else if (document.isFolder) {
                folders++
            }
        }
        val res = resources
        val title: String
        val sum = files + folders

        title = if (files == 0 && folders == 0) {
            sum.toString()
        } else if (files == 0) {
            folders.toString()
        } else if (folders == 0) {
            files.toString()
        } else {
            sum.toString()
        }
        actionMode!!.title = title
        try {
            actionMode!!.invalidate()
        } catch (e: NullPointerException) {
            Timber.e(e, "Invalidate error")
            e.printStackTrace()
        }
        // actionMode.
    }

    fun clearSelections() {
        if (adapter != null && adapter.isMultipleSelect) {
            adapter.clearSelections()
        }
    }

    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect")
        adapter.isMultipleSelect = false
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    fun notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged()
        }
    }

    fun showFabButton(node: MegaNode?) {
        setFabVisibility(node)
        (context as ContactFileListActivity).invalidateOptionsMenu()
    }

    val fabVisibility: Int
        get() = fab!!.visibility

    var parentHandle: Long
        get() = _parentHandle
        set(parentHandle) {
            this._parentHandle = parentHandle
            if (adapter != null) {
                adapter.parentHandle = parentHandle
            }
        }

    val isEmptyParentHandleStack: Boolean
        get() = parentHandleStack!!.isEmpty()

    private fun setFabVisibility(node: MegaNode?) {
        if (megaApi.getAccess(node) == MegaShare.ACCESS_READ || node == null) {
            fab!!.hide()
        } else {
            fab!!.show()
        }
    }

    companion object {
        private const val PARENT_HANDLE_STACK = "parentHandleStack"
    }
}
