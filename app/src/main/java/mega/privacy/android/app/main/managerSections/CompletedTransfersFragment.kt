package mega.privacy.android.app.main.managerSections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.main.adapters.MegaCompletedTransfersAdapter
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * The Fragment is used for displaying the finished items of transfer.
 */
@AndroidEntryPoint
class CompletedTransfersFragment : TransfersBaseFragment() {
    /**
     * MegaApiAndroid injection
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private lateinit var adapter: MegaCompletedTransfersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return initView(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transfersEmptyImage.setImageResource(
            if (Util.isScreenInPortrait(requireContext())) {
                R.drawable.empty_transfer_portrait
            } else R.drawable.empty_transfer_landscape)

        binding.transfersEmptyText.text = TextUtil.formatEmptyScreenText(requireContext(),
            getString(R.string.completed_transfers_empty_new))

        adapter = MegaCompletedTransfersAdapter(
            context = requireActivity(),
            transfers = viewModel.getCompletedTransfers(),
            megaApi = megaApi
        )
        setupFlow()
        binding.transfersListView.adapter = adapter
    }

    private fun setupFlow() {
        viewModel.completedState.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED
        ).onEach { transfersState ->
            when (transfersState) {
                is CompletedTransfersState.TransfersUpdated -> {
                    setEmptyView(transfersState.newTransfers.size)
                }
                is CompletedTransfersState.TransferFinishUpdated -> {
                    setEmptyView(transfersState.newTransfers.size)
                    adapter.setCompletedTransfers(transfersState.newTransfers)
                    requireActivity().invalidateOptionsMenu()
                }
                is CompletedTransfersState.TransferRemovedUpdated -> {
                    adapter.removeItemData(transfersState.index, transfersState.newTransfers)
                }
                is CompletedTransfersState.ClearTransfersUpdated -> {
                    adapter.setCompletedTransfers(emptyList())
                    setEmptyView(0)
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        collectFlow(viewModel.failedTransfer) { isFailed ->
            if (isFailed) {
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    /**
     * Checks if there is any completed transfer.
     *
     * @return True if there is any completed transfer, false otherwise.
     */
    fun isAnyTransferCompleted(): Boolean = viewModel.getCompletedTransfers().isNotEmpty()

    /**
     * Removes a completed transfer.
     *
     * @param transfer transfer to remove
     * @param isRemovedCache If ture, remove cache file, otherwise doesn't remove cache file
     */
    fun transferRemoved(transfer: CompletedTransfer, isRemovedCache: Boolean) =
        viewModel.completedTransferRemoved(transfer, isRemovedCache)

    /**
     * Removes all completed transfers.
     */
    fun clearCompletedTransfers() = viewModel.clearCompletedTransfers()

    companion object {

        /**
         * Generate a new instance for [CompletedTransfersFragment]
         *
         * @return new [CompletedTransfersFragment] instance
         */
        @JvmStatic
        fun newInstance(): CompletedTransfersFragment = CompletedTransfersFragment()
    }
}
