package mega.privacy.android.app.myAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentMyAccountUsageBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.ActiveFragment
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.businessOrProFlexiUpdate
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.update
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.updateBusinessOrProFlexi
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Fragment for my account usage
 */
@AndroidEntryPoint
class MyAccountUsageFragment : Fragment(), Scrollable {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var fileSizeStringMapper: FileSizeStringMapper

    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val viewModel: MyAccountViewModel by activityViewModels()

    private val isBusinessAccount
        get() = viewModel.state.value.isBusinessAccount

    private val isProFlexiAccount
        get() = viewModel.state.value.isProFlexiAccount

    private lateinit var binding: FragmentMyAccountUsageBinding
    private lateinit var usageBinding: MyAccountUsageContainerBinding
    private lateinit var paymentAlertBinding: MyAccountPaymentInfoContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyAccountUsageBinding.inflate(layoutInflater)
        usageBinding = binding.usageViewLayout
        paymentAlertBinding = binding.paymentAlert
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupObservers()
        binding.upgradeButton.setOnClickListener {
            megaNavigator.openUpgradeAccount(
                context = requireActivity(),
                source = UpgradeAccountSource.MY_ACCOUNT_SCREEN
            )
            viewModel.setOpenUpgradeFrom()
        }
    }

    private fun setupView() {
        viewModel.getFileVersionsOption()
        binding.usageLayout.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        usageBinding.root.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.background_usage_storage_transfer
        )

        paymentAlertBinding.renewExpiryText.setTextAppearance(
            R.style.TextAppearance_Mega_Body1_Grey87White54,
        )

        binding.rubbishSeparator.isVisible = false
        binding.previousVersionsStorageContainer.isVisible = false
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.usageLayout.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        viewLifecycleOwner.collectFlow(viewModel.state) {
            refreshVersionsInfo(it.versionsInfo, it.isFileVersioningEnabled)
        }
        viewLifecycleOwner.collectFlow(viewModel.state.map { it.storageState }
            .distinctUntilChanged()) {
            setupAccountDetails()
        }
        viewLifecycleOwner.collectFlow(viewModel.monitorMyAccountUpdate) {
            if (it.action == MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS) {
                setupAccountDetails()
            }
        }
    }

    /**
     * Updates versions info depending on if is enabled or not.
     *
     * @param versionsInfo Text to show as versions info.
     */
    private fun refreshVersionsInfo(versionsInfo: String?, isFileVersioningEnabled: Boolean) {
        if (isFileVersioningEnabled) {
            binding.rubbishSeparator.isVisible = true
            binding.previousVersionsStorageContainer.isVisible = true
            binding.previousVersionsText.text = versionsInfo
        } else {
            binding.rubbishSeparator.isVisible = false
            binding.previousVersionsStorageContainer.isVisible = false
        }
    }

    private fun setupAccountDetails() {
        if (isBusinessAccount) {
            usageBinding.updateBusinessOrProFlexi(
                requireContext(),
                viewModel.getUsedStorage(),
                viewModel.getUsedTransfer()
            )
            paymentAlertBinding.businessOrProFlexiUpdate(
                requireContext(),
                viewModel.getRenewTime(),
                viewModel.getExpirationTime(),
                viewModel.hasRenewableSubscription(),
                viewModel.hasExpirableSubscription(),
                megaApi,
                true,
                ActiveFragment.MY_ACCOUNT_USAGE,
                false
            )
            paymentAlertBinding.root.isVisible = true
            binding.upgradeButton.isVisible = false
        } else if (isProFlexiAccount) {
            usageBinding.updateBusinessOrProFlexi(
                requireContext(),
                viewModel.getUsedStorage(),
                viewModel.getUsedTransfer()
            )
            paymentAlertBinding.businessOrProFlexiUpdate(
                requireContext(),
                viewModel.getRenewTime(),
                viewModel.getExpirationTime(),
                viewModel.hasRenewableSubscription(),
                viewModel.hasExpirableSubscription(),
                megaApi,
                true,
                ActiveFragment.MY_ACCOUNT_USAGE,
                true
            )
            paymentAlertBinding.root.isVisible = true
            binding.upgradeButton.isVisible = false
        } else {
            usageBinding.update(
                context = requireContext(),
                storageState = viewModel.getStorageState(),
                isFreeAccount = viewModel.isFreeAccount(),
                totalStorage = viewModel.getTotalStorage(),
                totalTransfer = viewModel.getTotalTransfer(),
                usedStorage = viewModel.getUsedStorage(),
                usedStoragePercentage = viewModel.getUsedStoragePercentage(),
                usedTransfer = viewModel.getUsedTransfer(),
                usedTransferPercentage = viewModel.getUsedTransferPercentage(),
                usedTransferStatus = viewModel.getUsedTransferStatus(),
                themeMode = monitorThemeModeUseCase()
            )
            paymentAlertBinding.root.isVisible = paymentAlertBinding.update(
                viewModel.getRenewTime(),
                viewModel.getExpirationTime(),
                viewModel.hasRenewableSubscription(),
                viewModel.hasExpirableSubscription(),
                viewModel.isFreeAccount(),
                ActiveFragment.MY_ACCOUNT_USAGE
            )
            binding.upgradeButton.isVisible = true
        }

        binding.cloudStorageText.text = viewModel.getCloudStorage()

        binding.incomingStorageText.text = viewModel.getIncomingStorage()
        binding.rubbishStorageText.text = viewModel.getRubbishStorage()

        updateBackupStorage(viewModel.state.value.backupStorageSize)
    }

    /**
     * Update back up storage size display
     *
     * @param backupStorageSize
     */
    private fun updateBackupStorage(backupStorageSize: Long) {
        if (backupStorageSize < 1) {
            binding.backupsStorageContainer.isVisible = false
        } else {
            binding.backupsStorageContainer.isVisible = true
            binding.backupsStorageText.text = fileSizeStringMapper(backupStorageSize)
        }
    }
}
