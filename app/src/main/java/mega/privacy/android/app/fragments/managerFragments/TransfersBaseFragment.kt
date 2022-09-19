package mega.privacy.android.app.fragments.managerFragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentTransfersBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.managerSections.ActiveTransfersState
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.main.managerSections.WrapContentLinearLayoutManager
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Util

/**
 * The base transfer fragment
 */
@AndroidEntryPoint
open class TransfersBaseFragment : RotatableFragment() {

    /**
     * [TransfersViewModel] instance
     */
    protected val viewModel by viewModels<TransfersViewModel>()

    /**
     * LayoutManager
     */
    protected lateinit var mLayoutManager: LinearLayoutManager

    /**
     * [ChatDividerItemDecoration]
     */
    protected lateinit var itemDecoration: ChatDividerItemDecoration

    /**
     * FragmentTransfersBinding
     */
    protected lateinit var binding: FragmentTransfersBinding


    /**
     * Initial the view
     *
     * @param inflater LayoutInflater
     * @param container ViewGroup
     *
     * @return view
     */
    protected open fun initView(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentTransfersBinding.inflate(inflater, container, false)

        itemDecoration = ChatDividerItemDecoration(requireContext())

        mLayoutManager = WrapContentLinearLayoutManager(requireContext())
        binding.transfersListView.run {
            addItemDecoration(itemDecoration)
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateElevation()
                }
            })
        }

        binding.layoutGetMoreQuotaView.getMoreQuotaUpgradeButton.setOnClickListener {
            (requireActivity() as ManagerActivity).navigateToUpgradeAccount()
        }
        setupFlow()
        setGetMoreQuotaViewVisibility()
        requireActivity().invalidateOptionsMenu()
        return binding.root
    }

    private fun setupFlow() {
        viewModel.activeState.flowWithLifecycle(
            lifecycle = viewLifecycleOwner.lifecycle,
            minActiveState = Lifecycle.State.RESUMED
        ).onEach { transfersState ->
            when (transfersState) {
                is ActiveTransfersState.GetMoreQuotaViewVisibility -> {
                    val getMoreQuotaView = binding.layoutGetMoreQuotaView.getMoreQuotaView
                    getMoreQuotaView.isVisible = transfersState.isVisible
                    if (transfersState.isVisible) {
                        if (Util.isDarkMode(requireContext())) {
                            getMoreQuotaView.setBackgroundColor(ColorUtils.getColorForElevation(
                                requireContext(),
                                DARK_MODE_ELEVATION))
                        } else {
                            getMoreQuotaView.setBackgroundResource(
                                R.drawable.white_layout_with_broder_shadow)
                        }
                    }
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /**
     * Update elevation when scrolling if needed.
     */
    open fun updateElevation() = (requireActivity() as ManagerActivity).changeAppBarElevation(
        binding.transfersListView.canScrollVertically(DEFAULT_SCROLL_DIRECTION))

    /**
     * Shows an empty view if there are not transfers
     * and the list if there are.
     *
     * @param size  the size of the list of transfers
     */
    protected open fun setEmptyView(size: Int) {
        with(binding) {
            val isEmpty = size == 0
            transfersEmptyImage.isVisible = isEmpty
            transfersEmptyText.isVisible = isEmpty
            transfersListView.isVisible = isEmpty.not()
            if (isEmpty) updateElevation()
        }
    }

    /**
     * Sets the visibility of the view "Get more quota".
     */
    fun setGetMoreQuotaViewVisibility() {
        viewModel.setGetMoreQuotaViewVisibility()
    }

    override fun getAdapter(): RotatableAdapter? = null

    override fun activateActionMode() {
    }

    override fun multipleItemClick(position: Int) {
    }

    override fun reselectUnHandledSingleItem(position: Int) {
    }

    override fun updateActionModeTitle() {
    }

    companion object {
        /**
         * The default value for scroll direction
         */
        const val DEFAULT_SCROLL_DIRECTION = -1
        private const val DARK_MODE_ELEVATION = 6F
    }
}