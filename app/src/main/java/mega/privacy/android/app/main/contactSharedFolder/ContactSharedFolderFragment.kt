package mega.privacy.android.app.main.contactSharedFolder

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentContactSharedFolderListBinding
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.ContactFileBaseFragment
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.view.createStartDownloadView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Fragment for Contact shared Folder
 */
class ContactSharedFolderFragment : ContactFileBaseFragment() {

    companion object {
        private const val MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED = 5
    }

    private var _binding: FragmentContactSharedFolderListBinding? = null
    private val binding: FragmentContactSharedFolderListBinding
        get() = _binding!!
    private lateinit var moreButton: Button
    private lateinit var listView: RecyclerView

    private val handler = Handler(Looper.getMainLooper())
    private val viewModel by activityViewModels<ContactInfoViewModel>()
    private val startDownloadViewModel by activityViewModels<StartDownloadViewModel>()
    private val contactInfoActivity: ContactInfoActivity
        get() = (requireActivity() as ContactInfoActivity)

    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        return if (userEmail.isNullOrBlank().not()) {
            _binding = FragmentContactSharedFolderListBinding.inflate(inflater, container, false)
            contact = megaApi.getContact(userEmail)
            val fullList = megaApi.getInShares(contact)
            contactNodes = getNodeListToBeDisplayed(fullList)
            moreButton = binding.moreButton
            listView = binding.contactSharedFolderListView
            setupMoreButtonText(fullList.size)
            if (adapter == null) {
                adapter = MegaNodeAdapter(
                    requireActivity(),
                    this,
                    contactNodes,
                    -1,
                    listView,
                    Constants.CONTACT_SHARED_FOLDER_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                )
            } else {
                adapter.setNodes(contactNodes)
                adapter.parentHandle = -1
            }
            adapter.isMultipleSelect = false
            listView.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
                itemAnimator = Util.noChangeRecyclerViewItemAnimator()
                adapter = this@ContactSharedFolderFragment.adapter
            }
            moreButton.setOnClickListener {
                val i = Intent(getContext(), ContactFileListActivity::class.java)
                i.putExtra(Constants.NAME, userEmail)
                startActivity(i)
            }
            addStartDownloadView(binding.root)
            binding.root
        } else {
            null
        }
    }

    private fun addStartDownloadView(rootView: ViewGroup) {
        activity?.let { activity ->
            rootView.addView(
                createStartDownloadView(
                    activity,
                    startDownloadViewModel.state,
                    startDownloadViewModel::consumeDownloadEvent
                )
            )
        }
    }

    /**
     * Generate a List of [MegaNode] which needs to be displayed
     * @param fullList list of [MegaNode]
     * @return list of [MegaNode] needs to be displayed
     */
    private fun getNodeListToBeDisplayed(fullList: ArrayList<MegaNode>): ArrayList<MegaNode> {
        return if (fullList.size > MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED) {
            ArrayList(fullList.take(MAX_SHARED_FOLDER_NUMBER_TO_BE_DISPLAYED))
        } else {
            fullList
        }
    }

    /**
     * Show optopns pane base on [MegaNode]
     * @param sNode [MegaNode]
     */
    fun showOptionsPanel(sNode: MegaNode) {
        Timber.d("Node handle: ${sNode.handle}")
        contactInfoActivity.showOptionsPanel(sNode)
    }

    /**
     * Clears the item selection from [MegaNodeAdapter]
     */
    fun clearSelections() {
        adapter?.let {
            if (it.isMultipleSelect) it.clearSelections()
        }
    }

    /**
     * Set up More button text and visibility
     * @param fullListLength size of list
     */
    private fun setupMoreButtonText(fullListLength: Int) {
        val foldersInvisible = fullListLength - contactNodes.size
        if (foldersInvisible == 0) {
            moreButton.visibility = View.GONE
            return
        } else {
            moreButton.visibility = View.VISIBLE
        }
        moreButton.text = "$foldersInvisible ${getString(R.string.contact_info_button_more)}"
    }

    /**
     * Hide multiple select
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect")
        adapter.isMultipleSelect = false
        actionMode?.finish()
    }

    /**
     * Set Nodes based on [parentHandle]
     * @param parentHandle Long
     */
    fun setNodes(parentHandle: Long) {
        var handle = parentHandle
        if (megaApi.getNodeByHandle(parentHandle) == null) {
            handle = -1
            this.parentHandle = -1
            viewModel.setParentHandle(handle)
            adapter.parentHandle = handle
            val fullList = megaApi.getInShares(contact)
            setNodes(getNodeListToBeDisplayed(fullList))
            setupMoreButtonText(fullList.size)
        }
    }


    /**
     * Set nodes to [MegaNodeAdapter]
     * @param nodes nodes to set to [MegaNodeAdapter]
     */
    private fun setNodes(nodes: ArrayList<MegaNode>) {
        this.contactNodes = nodes
        adapter?.setNodes(contactNodes)

    }

    /**
     * Set nodes to [MegaNodeAdapter]
     */
    fun setNodes() {
        contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle))
        adapter?.setNodes(contactNodes)
        listView.invalidate()
    }

    /**
     * Handle item click of [MegaNodeAdapter]
     */
    fun itemClick(position: Int) {
        if (adapter?.isMultipleSelect == false) {
            val i = Intent(requireContext(), ContactFileListActivity::class.java)
            i.putExtra(Constants.NAME, userEmail)
            i.putExtra("node_position", position)
            startActivity(i)
        } else {
            Timber.d("Multiselect ON")
            adapter.toggleSelection(position)

            val selectedNodes = adapter?.selectedNodes
            if ((selectedNodes?.size ?: 0) > 0) {
                updateActionModeTitle()
            }
        }
    }

    /**
     * Selects all items from [MegaNodeAdapter]
     */
    fun selectAll() {
        adapter?.let {
            if (it.isMultipleSelect) {
                it.selectAll()
            } else {
                it.isMultipleSelect = true
                it.selectAll()
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    ActionBarCallBack()
                )
            }
            Handler(Looper.getMainLooper()).post { updateActionModeTitle() }
        }
    }

    /**
     * update the title of ActionBar
     */
    override fun updateActionModeTitle() {
        actionMode?.let {
            val files = adapter?.selectedNodes?.count { it.isFile } ?: 0
            val folders = adapter?.selectedNodes?.count { it.isFolder } ?: 0

            actionMode?.title = when {
                (files == 0 && folders == 0) -> 0.toString()
                files == 0 -> folders.toString()
                folders == 0 -> files.toString()
                else -> (files + folders).toString()
            }

            runCatching {
                actionMode?.invalidate()
            }.getOrElse {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.file_browser_action, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val selected = adapter?.selectedNodes ?: listOf()
            menu?.apply {
                findItem(R.id.cab_menu_share_link)?.title =
                    resources.getQuantityString(R.plurals.get_links, selected.size)

                var showRename = false
                var showMove = false
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

                if (selected.isNotEmpty()) {
                    if ((megaApi.checkAccessErrorExtended(selected[0], MegaShare.ACCESS_FULL)
                            .errorCode == MegaError.API_OK) || (megaApi.checkAccessErrorExtended(
                            selected[0],
                            MegaShare.ACCESS_READWRITE
                        ).errorCode == MegaError.API_OK)
                    ) {
                        showMove = true
                    }
                }
                if (selected.isNotEmpty()) {
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

                    selected.forEach {
                        if (megaApi.checkMoveErrorExtended(
                                it,
                                megaApi.rubbishNode
                            ).errorCode != MegaError.API_OK
                        ) {
                            showMove = false
                            return@forEach
                        }
                    }

                    if (selected.size == adapter?.itemCount) {
                        findItem(R.id.cab_menu_select_all).isVisible = false
                        findItem(R.id.cab_menu_unselect_all).isVisible = true
                    } else {
                        findItem(R.id.cab_menu_select_all).isVisible = true
                        findItem(R.id.cab_menu_unselect_all).isVisible = true
                    }
                } else {
                    findItem(R.id.cab_menu_select_all).isVisible = true
                    findItem(R.id.cab_menu_unselect_all).isVisible = false
                }

                findItem(R.id.cab_menu_download).isVisible = true
                findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                findItem(R.id.cab_menu_leave_multiple_share).isVisible = true
                findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                findItem(R.id.cab_menu_rename).isVisible = showRename
                findItem(R.id.cab_menu_copy).isVisible = true

                findItem(R.id.cab_menu_move).isVisible = showMove
                findItem(R.id.cab_menu_share_link).isVisible = false
            }

            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val documents = adapter?.selectedNodes ?: listOf()
            when (item?.itemId) {
                R.id.cab_menu_download -> {
                    contactInfoActivity.downloadFile(documents)
                }

                R.id.cab_menu_copy -> {
                    val handleList = arrayListOf<Long>()
                    documents.forEach {
                        handleList.add(it.handle)
                    }
                    contactInfoActivity.showCopy(handleList)
                }

                R.id.cab_menu_select_all -> {
                    selectAll()
                }

                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                }

                R.id.cab_menu_leave_multiple_share -> {
                    val handleList = arrayListOf<Long>()
                    documents.forEach {
                        handleList.add(it.handle)
                    }
                    MegaNodeUtil.showConfirmationLeaveIncomingShares(
                        requireActivity(),
                        (requireActivity() as SnackbarShower), handleList
                    )
                }

                R.id.cab_menu_rename -> {
                    if (documents.isNotEmpty()) {
                        val node = documents[0]
                        MegaNodeDialogUtil.showRenameNodeDialog(
                            context, node, (requireActivity() as SnackbarShower),
                            (requireActivity() as ActionNodeCallback)
                        )
                    }
                }

                else -> {}
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            clearSelections()
            adapter?.isMultipleSelect = false
        }

    }

    override fun activateActionMode() {
        Timber.d("activateActionMode")
        if (adapter?.isMultipleSelect == false) {
            adapter?.isMultipleSelect = true
            actionMode =
                (requireActivity() as AppCompatActivity).startSupportActionMode(ActionBarCallBack())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}