package mega.privacy.android.app.presentation.transfers.page

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentTransferPageBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.TransfersPageAdapter
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment
import mega.privacy.android.app.main.managerSections.LegacyTransfersFragment
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class TransferPageFragment : Fragment() {

    @Inject
    lateinit var areTransfersPausedUseCase: AreTransfersPausedUseCase

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

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
    private val transfersViewModel: TransfersViewModel by activityViewModels()
    private val viewModel by activityViewModels<TransferPageViewModel>()

    private val completedTransfersFragment: CompletedTransfersFragment?
        get() = childFragmentManager.findFragmentByTag(COMPLETED_TRANSFERS_TAG) as? CompletedTransfersFragment

    private val legacyTransfersFragment: LegacyTransfersFragment?
        get() = childFragmentManager.findFragmentByTag(TRANSFERS_TAG) as? LegacyTransfersFragment

    private var cancelAllTransfersMenuItem: MenuItem? = null
    private var playTransfersMenuIcon: MenuItem? = null
    private var pauseTransfersMenuIcon: MenuItem? = null
    private var retryTransfers: MenuItem? = null
    private var clearCompletedTransfers: MenuItem? = null

    private var confirmationTransfersDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FragmentTransferPageBinding.inflate(inflater, container, false).also {
            addStartDownloadTransferView(it.root)
            _binding = it
        }.root
    }

    private fun addStartDownloadTransferView(root: ViewGroup) {
        root.addView(
            createStartTransferView(
                activity = requireActivity(),
                transferEventState = transfersViewModel.uiState.map { it.startEvent },
                onConsumeEvent = transfersViewModel::consumeRetry,
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        requireActivity(),
                        storageTargetPreference
                    )
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpPager()
        registerListener()
        observer()
        setupMenu()
    }

    override fun onDestroyView() {
        _binding = null
        confirmationTransfersDialog?.dismiss()
        super.onDestroyView()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_transfer_tab, menu)
                cancelAllTransfersMenuItem = menu.findItem(R.id.action_menu_cancel_all_transfers)
                clearCompletedTransfers = menu.findItem(R.id.action_menu_clear_completed_transfers)
                retryTransfers = menu.findItem(R.id.action_menu_retry_transfers)
                playTransfersMenuIcon = menu.findItem(R.id.action_play)
                pauseTransfersMenuIcon = menu.findItem(R.id.action_pause)
                handleMenuOptionsVisible(viewModel.transferTab)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem) {
                    pauseTransfersMenuIcon -> {
                        Timber.d("Pause all transfers")
                        viewModel.pauseOrResumeTransfers(true)
                        return true
                    }

                    playTransfersMenuIcon -> {
                        Timber.d("Resume all transfers")
                        viewModel.pauseOrResumeTransfers(false)
                        return true
                    }

                    cancelAllTransfersMenuItem -> {
                        Timber.d("Cancel all transfers")
                        showConfirmationCancelAllTransfers()
                        return true
                    }

                    clearCompletedTransfers -> {
                        Timber.d("Clear all completed transfers")
                        showConfirmationClearCompletedTransfers()
                        return true
                    }

                    retryTransfers -> {
                        Timber.d("Retry all transfers")
                        retryAllTransfers()
                        return true
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observer() {
        transfersManagementViewModel.onGetShouldCompletedTab().observe(
            viewLifecycleOwner
        ) { showCompleted: Boolean -> updateTransfersTab(showCompleted) }

        viewLifecycleOwner.collectFlow(transfersManagementViewModel.state) {
            if (it.transfersInfo.status == TransfersStatus.Completed) {
                pauseTransfersMenuIcon?.isVisible = false
                playTransfersMenuIcon?.isVisible = false
                cancelAllTransfersMenuItem?.isVisible = false
            }
        }
        viewLifecycleOwner.collectFlow(viewModel.state) { uiState ->
            val pauseOrResultResult = uiState.pauseOrResultResult
            if (pauseOrResultResult?.isSuccess == true) {
                val isPause = pauseOrResultResult.getOrThrow()
                playTransfersMenuIcon?.isVisible =
                    isPause && uiState.transfersTab == TransfersTab.PENDING_TAB
                pauseTransfersMenuIcon?.isVisible =
                    !isPause && uiState.transfersTab == TransfersTab.PENDING_TAB
                legacyTransfersFragment?.refresh()
                viewModel.markPauseOrResultResultConsumed()
            }

            if (uiState.cancelTransfersResult != null) {
                handleAllTransfersCanceled(uiState.cancelTransfersResult)
            }

            uiState.deleteFailedOrCancelledTransfersResult?.let {
                handleDeleteFailedOrCancelledTransfersResult(it.getOrThrow())
                viewModel.markDeleteFailedOrCancelledTransferResultConsumed()
            }

            uiState.deleteAllCompletedTransfersResult?.let {
                clearCompletedTransfers?.isVisible = false
                viewModel.markDeleteAllCompletedTransfersResultConsumed()
            }
        }
        viewLifecycleOwner.collectFlow(transfersViewModel.completedTransfers) { completedTransfers ->
            if (viewModel.transferTab == TransfersTab.COMPLETED_TAB) {
                clearCompletedTransfers?.isVisible = completedTransfers.isNotEmpty()
            }
        }
        viewLifecycleOwner.collectFlow(transfersViewModel.uiState) { uiState ->
            with(uiState) {
                readRetryError?.let {
                    (activity as? BaseActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        resources.getString(
                            if (it == 1) {
                                sharedR.string.transfers_completed_one_read_error_retrying
                            } else {
                                sharedR.string.transfers_completed_some_read_error_retrying
                            }
                        ),
                        -1
                    )
                    transfersViewModel.onConsumeRetryReadError()
                }
            }
        }
    }

    private fun handleDeleteFailedOrCancelledTransfersResult(transfers: List<CompletedTransfer>) {
        for (transfer in transfers) {
            // still call to activity, we will refactor later
            retryTransfer(transfer)
        }
    }

    private fun handleAllTransfersCanceled(cancelTransfersResult: Result<Unit>) {
        viewModel.onCancelTransfersResultConsumed()
        if (cancelTransfersResult.isSuccess) {
            pauseTransfersMenuIcon?.isVisible = false
            playTransfersMenuIcon?.isVisible = false
            cancelAllTransfersMenuItem?.isVisible = false
        } else {
            (activity as? BaseActivity)?.showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_general_nodes),
                -1
            )
        }
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
                    legacyTransfersFragment?.setGetMoreQuotaViewVisibility()
                } else if (selectedTab === TransfersTab.COMPLETED_TAB) {
                    completedTransfersFragment?.setGetMoreQuotaViewVisibility()
                    legacyTransfersFragment?.destroyActionModeIfNeed()
                }
                handleMenuOptionsVisible(selectedTab)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun updateElevation() {
        if (viewModel.transferTab == TransfersTab.PENDING_TAB) {
            legacyTransfersFragment?.updateElevation()
        } else if (viewModel.transferTab == TransfersTab.COMPLETED_TAB) {
            completedTransfersFragment?.updateElevation()
        }
    }

    private fun handleMenuOptionsVisible(transferTab: TransfersTab) {
        if (transferTab == TransfersTab.PENDING_TAB
            && transfersViewModel.getActiveTransfers().isNotEmpty()
        ) {
            if (areTransfersPausedUseCase()) {
                playTransfersMenuIcon?.isVisible = true
            } else {
                pauseTransfersMenuIcon?.isVisible = true
            }
            cancelAllTransfersMenuItem?.isVisible = true
        } else if (transferTab == TransfersTab.COMPLETED_TAB
            && transfersViewModel.getCompletedTransfers().isNotEmpty()
        ) {
            clearCompletedTransfers?.isVisible = true
            retryTransfers?.isVisible = transfersViewModel.hasFailedOrCancelledTransfer()
        }
    }

    /**
     * Updates the Transfers tab index.
     *
     * @param showCompleted True if should show the Completed tab, false otherwise.
     */
    private fun updateTransfersTab(showCompleted: Boolean) {
        viewModel.setTransfersTab(if (transfersManagementViewModel.shouldCheckTransferError() || showCompleted) TransfersTab.COMPLETED_TAB else TransfersTab.PENDING_TAB)
        when (viewModel.state.value.transfersTab) {
            TransfersTab.COMPLETED_TAB -> {
                binding.transfersTabsPager.currentItem = TransfersTab.COMPLETED_TAB.position
            }

            else -> {
                binding.transfersTabsPager.currentItem = TransfersTab.PENDING_TAB.position
                if (!transfersManagementViewModel.state.value.isOnline) {
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
        legacyTransfersFragment?.destroyActionModeIfNeed()
    }

    fun destroyActionMode() {
        legacyTransfersFragment?.destroyActionMode()
    }

    fun activateActionMode() {
        legacyTransfersFragment?.activateActionMode()
    }

    private fun showConfirmationCancelAllTransfers() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setMessage(resources.getString(R.string.cancel_all_transfer_confirmation))
            .setPositiveButton(
                R.string.cancel_all_action
            ) { _: DialogInterface?, _: Int ->
                viewModel.cancelAllTransfers()
                viewModel.stopCameraUploads()
            }
            .setNegativeButton(R.string.general_dismiss, null)
        confirmationTransfersDialog = builder.create()
        setConfirmationTransfersDialogNotCancellableAndShow()
    }

    /**
     * Shows a warning to ensure if it is sure of remove all completed transfers.
     */
    private fun showConfirmationClearCompletedTransfers() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setMessage(R.string.confirmation_to_clear_completed_transfers)
            .setPositiveButton(R.string.general_clear) { _: DialogInterface?, _: Int ->
                transfersViewModel.deleteFailedOrCancelledTransferCacheFiles()
                viewModel.deleteAllCompletedTransfers()
            }
            .setNegativeButton(R.string.general_dismiss, null)
        confirmationTransfersDialog = builder.create()
        setConfirmationTransfersDialogNotCancellableAndShow()
    }

    /**
     * Retries all the failed and cancelled transfers.
     */
    private fun retryAllTransfers() {
        transfersViewModel.retryAllTransfers()
    }

    private fun setConfirmationTransfersDialogNotCancellableAndShow() {
        if (confirmationTransfersDialog != null) {
            confirmationTransfersDialog?.setCancelable(false)
            confirmationTransfersDialog?.setCanceledOnTouchOutside(false)
            confirmationTransfersDialog?.show()
        }
    }


    /**
     * Retries a transfer that finished wrongly.
     *
     * @param transfer the transfer to retry
     */
    fun retryTransfer(transfer: CompletedTransfer) {
        when {
            transfer.type.isUploadType() || transfer.type.isDownloadType() -> {
                lifecycleScope.launch {
                    transfersViewModel.retryTransfer(transfer)
                }
            }

            else -> {
                Timber.d("Unable to retrieve transfer type value")
            }
        }
    }

    /**
     * Hide or show the tab.
     */
    fun hideTab(hide: Boolean) {
        binding.slidingTabsTransfers.isGone = hide
        binding.transfersTabsPager.disableSwipe(hide)
    }

    companion object {
        private val TRANSFERS_TAG = "android:switcher:${R.id.transfers_tabs_pager}:0"
        private val COMPLETED_TRANSFERS_TAG = "android:switcher:${R.id.transfers_tabs_pager}:1"

        fun newInstance(): TransferPageFragment = TransferPageFragment()
    }
}
