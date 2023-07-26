package mega.privacy.android.app.presentation.transfers.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentTransferPageBinding
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.TransfersPageAdapter
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment
import mega.privacy.android.app.main.managerSections.TransfersFragment
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

@AndroidEntryPoint
internal class TransferPageFragment : Fragment() {
    @Inject
    lateinit var transfersManagement: TransfersManagement
    private var _binding: FragmentTransferPageBinding? = null
    val binding: FragmentTransferPageBinding
        get() = _binding!!

    private val transferTabsAdapter: TransfersPageAdapter by lazy {
        TransfersPageAdapter(
            childFragmentManager,
            requireContext()
        )
    }

    private val transfersManagementViewModel by activityViewModels<TransfersManagementViewModel>()
    private val transfersViewModel: TransfersViewModel by viewModels()
    private val viewModel by activityViewModels<TransferPageViewModel>()

    private val completedTransfersFragment: CompletedTransfersFragment?
        get() = childFragmentManager.findFragmentByTag(COMPLETED_TRANSFERS_TAG) as? CompletedTransfersFragment

    private val transfersFragment: TransfersFragment?
        get() = childFragmentManager.findFragmentByTag(TRANSFERS_TAG) as? TransfersFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FragmentTransferPageBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpPager()
        registerListener()
        observer()
    }

    private fun observer() {
        transfersManagementViewModel.onGetShouldCompletedTab().observe(
            viewLifecycleOwner
        ) { showCompleted: Boolean -> updateTransfersTab(showCompleted) }
    }

    private fun setUpPager() {
        binding.transfersTabsPager.adapter = transferTabsAdapter
        binding.slidingTabsTransfers.setupWithViewPager(binding.transfersTabsPager)
    }

    private fun registerListener() {
        binding.transfersTabsPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                val selectedTab: TransfersTab = TransfersTab.fromPosition(position)
                transfersViewModel.setCurrentSelectedTab(selectedTab)
                viewModel.setTransfersTab(selectedTab)
                requireActivity().invalidateOptionsMenu()
                updateElevation()
                if (selectedTab === TransfersTab.PENDING_TAB) {
                    transfersFragment?.setGetMoreQuotaViewVisibility()
                } else if (selectedTab === TransfersTab.COMPLETED_TAB) {
                    completedTransfersFragment?.setGetMoreQuotaViewVisibility()
                    transfersFragment?.destroyActionModeIfNeed()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun updateElevation() {
        if (viewModel.transferTab == TransfersTab.PENDING_TAB) {
            transfersFragment?.updateElevation()
        } else if (viewModel.transferTab == TransfersTab.COMPLETED_TAB) {
            completedTransfersFragment?.updateElevation()
        }
    }

    /**
     * Updates the Transfers tab index.
     *
     * @param showCompleted True if should show the Completed tab, false otherwise.
     */
    private fun updateTransfersTab(showCompleted: Boolean) {
        viewModel.setTransfersTab(if (transfersManagement.getAreFailedTransfers() || showCompleted) TransfersTab.COMPLETED_TAB else TransfersTab.PENDING_TAB)
        when (viewModel.state.value.transfersTab) {
            TransfersTab.COMPLETED_TAB -> {
                binding.transfersTabsPager.currentItem = TransfersTab.COMPLETED_TAB.position
            }

            else -> {
                binding.transfersTabsPager.currentItem = TransfersTab.PENDING_TAB.position
                if (transfersManagement.shouldShowNetworkWarning) {
                    (activity as BaseActivity).showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        binding.root,
                        getString(R.string.error_server_connection_problem),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
            }
        }
        transferTabsAdapter.notifyDataSetChanged()
        viewModel.setTransfersTab(TransfersTab.fromPosition(binding.transfersTabsPager.currentItem))
        (activity as? ManagerActivity)?.setToolbarTitle()
    }

    fun destroyActionModeIfNeeded() {
        transfersFragment?.destroyActionModeIfNeed()
    }

    fun destroyActionMode() {
        transfersFragment?.destroyActionMode()
    }

    fun activateActionMode() {
        transfersFragment?.activateActionMode()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private val TRANSFERS_TAG = "android:switcher:${R.id.transfers_tabs_pager}:0"
        private val COMPLETED_TRANSFERS_TAG = "android:switcher:${R.id.transfers_tabs_pager}:1"

        fun newInstance(): TransferPageFragment = TransferPageFragment()
    }
}