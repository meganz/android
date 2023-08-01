package mega.privacy.android.app.main.managerSections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.main.adapters.MegaCompletedTransfersAdapter
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
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
        viewLifecycleOwner.collectFlow(viewModel.failedTransfer) { isFailed ->
            if (isFailed) {
                requireActivity().invalidateOptionsMenu()
            }
        }
        viewLifecycleOwner.collectFlow(viewModel.completedTransfers) { completedTransfers ->
            adapter.setCompletedTransfers(completedTransfers)
            setEmptyView(completedTransfers.size)
        }
    }

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
