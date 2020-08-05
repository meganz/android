package mega.privacy.android.app.fragments.offline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.NewHeaderItemDecoration
import mega.privacy.android.app.databinding.FragmentOfflineBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.autoCleared

@AndroidEntryPoint
class OfflineFragment : Fragment() {
    private val args: OfflineFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineBinding>()
    private val viewModel: OfflineViewModel by viewModels()

    private var managerActivity: ManagerActivityLollipop? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: OfflineAdapter? = null
    private var itemDecoration: NewHeaderItemDecoration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        managerActivity = requireActivity() as ManagerActivityLollipop

        if (!args.rootFolderOnly) {
            // TODO: workaround for navigation with ManagerActivity
            managerActivity?.fullscreenOfflineFragmentOpened(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!args.rootFolderOnly) {
            // TODO: workaround for navigation with ManagerActivity
            managerActivity?.fullscreenOfflineFragmentClosed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOfflineBinding.inflate(inflater, container, false)

        if (isList()) {
            binding.offlineBrowserList.isVisible = true
        } else {
            binding.offlineBrowserGrid.isVisible = true
        }

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setDisplayParam(
            args.rootFolderOnly, isList(),
            if (isList()) 0 else binding.offlineBrowserGrid.spanCount, args.path
        )
        setupRecyclerView()
        observeLiveData()
    }

    private fun setupRecyclerView() {
        val rv = if (isList()) {
            binding.offlineBrowserList.layoutManager = LinearLayoutManager(context)
            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid
        }

        adapter = OfflineAdapter(isList(), object : OfflineAdapterListener {
            override fun onNodeClicked(position: Int, node: OfflineNode) {
                viewModel.onNodeClicked(position, node)
            }

            override fun onNodeLongClicked(position: Int, node: OfflineNode) {
                viewModel.onNodeLongClicked(position, node)
            }
        })
        rv.adapter = adapter
        rv.setPadding(0, 0, 0, scaleHeightPx(85, resources.displayMetrics))
        rv.clipToPadding = false
        rv.setHasFixedSize(true)
        rv.itemAnimator = DefaultItemAnimator()
        rv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })

        val decor = NewHeaderItemDecoration(context)
        if (isList()) {
            decor.setType(ITEM_VIEW_TYPE_LIST)
        } else {
            decor.setType(ITEM_VIEW_TYPE_GRID)
        }
        rv.addItemDecoration(decor)

        recyclerView = rv
        itemDecoration = decor
    }

    private fun observeLiveData() {
        viewModel.nodes.observe(viewLifecycleOwner) {
            adapter?.setNodes(it)

            val isList = isList()
            itemDecoration?.setKeys(
                viewModel.buildSectionTitle(
                    it, isList, if (isList) 0 else binding.offlineBrowserGrid.spanCount
                )
            )

            if (!args.rootFolderOnly) {
                managerActivity?.updateFullscreenOfflineFragmentOptionMenu()
            }
        }

        viewModel.actionBarTitle.observe(viewLifecycleOwner) {
            if (viewModel.selecting) {

            } else {
                managerActivity?.setToolbarTitleFromFullscreenOfflineFragment(
                    it, viewModel.path == "/"
                )
            }
        }

        viewModel.pathLiveData.observe(viewLifecycleOwner) {
            managerActivity?.pathNavigationOffline = it
        }
        viewModel.openFolderFullscreen.observe(viewLifecycleOwner) {
            managerActivity?.openFullscreenOfflineFragment(it)
        }
    }

    private fun isList(): Boolean {
        return managerActivity?.isList ?: true || args.rootFolderOnly
    }

    fun checkScroll() {
        val rv = recyclerView
        if (rv != null) {
            managerActivity?.changeActionBarElevation(
                rv.canScrollVertically(-1) || viewModel.selecting
            )
        }
    }

    fun setOrder(order: Int) {
        viewModel.setOrder(order)
    }

    fun onBackPressed(): Int {
        return viewModel.navigateOut()
    }

    fun getItemCount(): Int {
        return adapter?.itemCount ?: 0
    }

    companion object {
        fun setArgs(fragment: OfflineFragment, rootFolderOnly: Boolean) {
            fragment.arguments = OfflineFragmentArgs("/", rootFolderOnly).toBundle()
        }
    }
}
