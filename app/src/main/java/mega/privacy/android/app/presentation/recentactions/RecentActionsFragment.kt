package mega.privacy.android.app.presentation.recentactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.brandongogetap.stickyheaders.StickyLayoutManager
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.HeaderItemDecoration
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.FragmentRecentsBinding
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.fragments.recent.SelectedBucketViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForSingleNode
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.SharedPreferenceConstants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * Recent actions page
 */
@AndroidEntryPoint
class RecentActionsFragment : Fragment(), StickyHeaderHandler {

    private var _binding: FragmentRecentsBinding? = null
    private val binding get() = _binding!!

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private val visibleContacts = ArrayList<MegaContactAdapter>()
    private var buckets: List<MegaRecentActionBucket> = listOf()
    private var recentActionsItems: List<RecentActionItemType?> = listOf()

    private var adapter: RecentsAdapter? = null
    private lateinit var emptyLayout: ScrollView
    private lateinit var emptyText: TextView
    private lateinit var showActivityButton: Button
    private lateinit var emptySpanned: Spanned
    private lateinit var activityHiddenSpanned: Spanned
    private lateinit var stickyLayoutManager: StickyLayoutManager
    private lateinit var listView: RecyclerView
    private lateinit var fastScroller: FastScroller

    val selectedBucketModel: SelectedBucketViewModel by activityViewModels()

    private val nodeChangeObserver = Observer { forceUpdate: Boolean ->
        if (forceUpdate) {
            buckets = megaApi.recentActions ?: emptyList()
            reloadItems(buckets)
            refreshRecentActions()
            setVisibleContacts()
            setRecentView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecentsBinding.inflate(inflater, container, false)

        emptyLayout = binding.emptyStateRecents
        emptyText = binding.emptyTextRecents
        showActivityButton = binding.showActivityButton
        showActivityButton.setOnClickListener { showRecentActivity() }
        emptySpanned = TextUtil.formatEmptyScreenText(requireContext(),
            StringResourcesUtils.getString(R.string.context_empty_recents))
        activityHiddenSpanned = TextUtil.formatEmptyScreenText(requireContext(),
            StringResourcesUtils.getString(R.string.recents_activity_hidden))
        listView = binding.listViewRecents
        fastScroller = binding.fastscroll
        stickyLayoutManager = TopSnappedStickyLayoutManager(requireContext(), this)
        listView.layoutManager = stickyLayoutManager
        listView.clipToPadding = false
        listView.itemAnimator = DefaultItemAnimator()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buckets = megaApi.recentActions ?: emptyList()

        fillRecentItems(buckets)
        setRecentView()

        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodeChangeObserver)
        LiveEventBus.get(EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
            .observe(viewLifecycleOwner) { hideRecentActivity: Boolean ->
                this.setRecentView(hideRecentActivity)
            }

        observeDragSupportEvents(viewLifecycleOwner, listView, Constants.VIEWER_FROM_RECETS)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodeChangeObserver)
        _binding = null
    }

    override fun getAdapterData(): List<RecentActionItemType?> = recentActionsItems

    private fun fillRecentItems(buckets: List<MegaRecentActionBucket>) {
        this.buckets = buckets
        reloadItems(buckets)
        adapter = RecentsAdapter(
            requireActivity(),
            this,
            recentActionsItems)
        listView.adapter = adapter
        listView.addItemDecoration(HeaderItemDecoration(requireContext()))
        setVisibleContacts()
    }

    private fun reloadItems(buckets: List<MegaRecentActionBucket>) {
        val recentItemList = arrayListOf<RecentActionItemType>()
        var previousDate: Long? = null
        var currentDate: Long
        for (i in buckets.indices) {
            val item =
                RecentActionItemType.Item(buckets[i])
            if (i == 0) {
                currentDate = item.timestamp
                previousDate = currentDate
                recentItemList.add(RecentActionItemType.Header(currentDate))
            } else {
                currentDate = item.timestamp
                if (currentDate != previousDate) {
                    recentItemList.add(RecentActionItemType.Header(currentDate))
                    previousDate = currentDate
                }
            }
            recentItemList.add(item)
        }
        recentActionsItems = recentItemList
    }

    private fun refreshRecentActions() {
        adapter?.setItems(recentActionsItems)
        setRecentView()
    }

    private fun setRecentView() {
        val hideRecentActivity = requireContext()
            .getSharedPreferences(SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
                Context.MODE_PRIVATE)
            .getBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false)
        setRecentView(hideRecentActivity)
        (requireActivity() as ManagerActivity).setToolbarTitle()
    }

    /**
     * Sets the recent view. Hide it if the setting to hide it is enabled, and shows it if the
     * setting is disabled.
     *
     * @param hideRecentActivity True if the setting to hide the recent activity is enabled,
     * false otherwise.
     */
    private fun setRecentView(hideRecentActivity: Boolean) {
        if (hideRecentActivity) {
            hideRecentActivity()
        } else {
            showActivity()
        }
    }

    /**
     * Shows the recent activity.
     */
    private fun showActivity() {
        if (buckets.isEmpty()) {
            emptyLayout.visibility = View.VISIBLE
            listView.visibility = View.GONE
            fastScroller.visibility = View.GONE
            showActivityButton.visibility = View.GONE
            emptyText.text = emptySpanned
        } else {
            emptyLayout.visibility = View.GONE
            listView.visibility = View.VISIBLE
            fastScroller.setRecyclerView(listView)
            if (buckets.size < Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller.visibility = View.GONE
            } else {
                fastScroller.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Hides the recent activity.
     */
    private fun hideRecentActivity() {
        emptyLayout.visibility = View.VISIBLE
        listView.visibility = View.GONE
        fastScroller.visibility = View.GONE
        showActivityButton.visibility = View.VISIBLE
        emptyText.text = activityHiddenSpanned
    }

    /**
     * Disables the setting to hide recent activity and updates the UI by showing it.
     */
    private fun showRecentActivity() {
        LiveEventBus.get(EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
            .post(false)
        requireContext().getSharedPreferences(SharedPreferenceConstants.USER_INTERFACE_PREFERENCES,
            Context.MODE_PRIVATE)
            .edit().putBoolean(SharedPreferenceConstants.HIDE_RECENT_ACTIVITY, false).apply()
    }

    fun findUserName(mail: String): String =
        visibleContacts.find { mail == it.megaUser.email }?.fullName.orEmpty()

    private fun setVisibleContacts() {
        visibleContacts.clear()
        megaApi.contacts
            .asSequence()
            .filter { it.visibility == MegaUser.VISIBILITY_VISIBLE }
            .forEach {
                val contactDB = ContactUtil.getContactDB(it.handle) ?: return@forEach
                val fullName = ContactUtil.getContactNameDB(contactDB) ?: it.email
                visibleContacts.add(MegaContactAdapter(contactDB, it, fullName))
            }
    }

    fun openFile(index: Int, node: MegaNode) {
        val intent: Intent
        if (MimeTypeList.typeForName(node.name).isImage) {
            intent = getIntentForSingleNode(
                requireContext(),
                node.handle,
                false
            )
            putThumbnailLocation(intent, listView, index, Constants.VIEWER_FROM_RECETS, adapter)
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
            return
        }
        val localPath = FileUtil.getLocalFile(node)
        val paramsSetSuccessfully: Boolean
        if (FileUtil.isAudioOrVideo(node)) {
            intent = if (FileUtil.isInternalIntent(node)) {
                Util.getMediaIntent(requireContext(), node.name)
            } else {
                Intent(Intent.ACTION_VIEW)
            }
            intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.RECENTS_ADAPTER)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, node.name)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
            paramsSetSuccessfully = if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(requireContext(), node, intent, localPath,
                    false, requireActivity() as ManagerActivity)
            } else {
                FileUtil.setStreamingIntentParams(requireContext(), node, megaApi, intent,
                    requireActivity() as ManagerActivity)
            }
            if (paramsSetSuccessfully && FileUtil.isOpusFile(node)) {
                intent.setDataAndType(intent.data, "audio/*")
            }
            launchIntent(intent, paramsSetSuccessfully, node, index)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireActivity(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.RECENTS_ADAPTER)
            paramsSetSuccessfully = if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(requireContext(), node, intent, localPath,
                    false, requireActivity() as ManagerActivity)
            } else {
                FileUtil.setStreamingIntentParams(requireContext(), node, megaApi, intent,
                    requireActivity() as ManagerActivity)
            }
            launchIntent(intent, paramsSetSuccessfully, node, index)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            manageTextFileIntent(requireContext(), node, Constants.RECENTS_ADAPTER)
        } else {
            Timber.d("itemClick:isFile:otherOption")
            onNodeTapped(requireContext(),
                node,
                { n: MegaNode? -> (requireActivity() as ManagerActivity).saveNodeByTap(n) },
                (requireActivity() as ManagerActivity),
                (requireActivity() as ManagerActivity))
        }
    }

    /**
     * Launch corresponding intent to open the file based on its type.
     *
     * @param intent                Intent to launch activity.
     * @param paramsSetSuccessfully true, if the param is set for the intent successfully; false, otherwise.
     * @param node                  The node to open.
     * @param position              Thumbnail's position in the list.
     */
    private fun launchIntent(
        intent: Intent?,
        paramsSetSuccessfully: Boolean,
        node: MegaNode,
        position: Int,
    ) {
        if (intent != null && !MegaApiUtils.isIntentAvailable(requireContext(), intent)) {
            (requireActivity() as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                -1)
            return
        }
        if (intent != null && paramsSetSuccessfully) {
            intent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            putThumbnailLocation(intent,
                listView,
                position,
                Constants.VIEWER_FROM_RECETS,
                adapter)
            requireActivity().startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }
}