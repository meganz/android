package mega.privacy.android.app.getLink

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentGetSeveralLinksBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.getLink.adapter.LinksAdapter
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ALPHA_VIEW_DISABLED
import mega.privacy.android.app.utils.Constants.ALPHA_VIEW_ENABLED
import mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.TextUtil.copyToClipboard
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import java.util.UUID
import javax.inject.Inject

/**
 * Fragment of [GetLinkActivity] to allow the creation of multiple links.
 */
@AndroidEntryPoint
class GetSeveralLinksFragment : Fragment() {

    private val viewModel: GetSeveralLinksViewModel by activityViewModels()

    private lateinit var binding: FragmentGetSeveralLinksBinding
    private val chatLauncher = registerForActivityResult(SendToChatActivityContract()) {
        if (it != null) {
            viewModel.sendToChat(it, viewModel.getLinksList())
        }
    }
    private var menu: Menu? = null

    private val linksAdapter by lazy { LinksAdapter() }

    private val handles: LongArray? by lazy {
        activity?.intent?.getLongArrayExtra(Constants.HANDLE_LIST)
    }

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGetSeveralLinksBinding.inflate(layoutInflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initialize()
        viewLifecycleOwner.collectFlow(viewModel.sendLinkToChatResult) {
            it?.let { sendLinkToChatResult ->
                (activity as? SnackbarShower)?.showSnackbarWithChat(
                    resources.getQuantityString(
                        R.plurals.links_sent,
                        1
                    ),
                    sendLinkToChatResult.chatId
                )
                viewModel.onShareLinkResultHandled()
            }
        }
    }

    private fun initialize() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes) && !viewModel.isInitialized()) {
                checkSensitiveItems()
            } else {
                initNodes()

                setupView()
                setupObservers()
            }
        }
    }

    private fun checkSensitiveItems() {
        val handles = handles ?: run {
            activity?.finish()
            return
        }

        viewModel.hasSensitiveItemsFlow
            .filterNotNull()
            .onEach { hasSensitiveItems ->
                if (hasSensitiveItems) {
                    showSharingSensitiveItemsWarningDialog()
                } else {
                    initNodes()

                    setupView()
                    setupObservers()
                }

                viewModel.clearSensitiveItemCheck()
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.checkSensitiveItems(handles.toList())
    }

    private fun showSharingSensitiveItemsWarningDialog() {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(sharedR.string.hidden_items))
            .setMessage(getString(sharedR.string.share_hidden_item_links_description))
            .setCancelable(false)
            .setPositiveButton(R.string.button_continue) { _, _ ->
                initNodes()

                setupView()
                setupObservers()
            }
            .setNegativeButton(R.string.general_cancel) { _, _ -> activity?.finish() }
            .show()
    }

    private fun initNodes() {
        val context = context ?: return
        val handles = handles ?: run {
            activity?.finish()
            return
        }

        if (viewModel.isInitialized()) return
        viewModel.initNodes(handles, context)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            R.id.action_share -> {
                val uniqueId = UUID.randomUUID()
                startActivity(
                    Intent(Intent.ACTION_SEND)
                        .setType(TYPE_TEXT_PLAIN)
                        .putExtra(Intent.EXTRA_SUBJECT, "${uniqueId}.url")
                        .putExtra(Intent.EXTRA_TEXT, viewModel.getLinksString())
                )
            }

            R.id.action_chat -> {
                chatLauncher.launch(longArrayOf())
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.activity_get_link, menu)
        this.menu = menu

        refreshMenuOptionsVisibility()

        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        menu.toggleAllMenuItemsVisibility(!viewModel.isExportingNodes())
    }

    private fun setupView() {
        binding.linksList.apply {
            adapter = linksAdapter
            setHasFixedSize(true)
            addItemDecoration(
                PositionDividerItemDecoration(
                    requireContext(),
                    resources.displayMetrics,
                    0
                )
            )
        }

        binding.copyButton.apply {
            isEnabled = false
            setOnClickListener {
                copyLinks(
                    links = viewModel.getLinksString(),
                    isForFirstTime = false
                )
            }
        }
    }

    private fun setupObservers() {
        viewModel.getLinkItems().observe(viewLifecycleOwner, ::showLinks)
        viewModel.getExportingNodes().observe(viewLifecycleOwner, ::updateUI)
    }

    /**
     * Shows the links in the list.
     *
     * @param links List of [LinkItem]s to show.
     */
    private fun showLinks(links: List<LinkItem>) {
        linksAdapter.submitList(links)
        if (viewModel.getLinksString().isNotBlank()) {
            copyLinks(links = viewModel.getLinksString(), isForFirstTime = true)
        }
    }

    /**
     * Updates the UI depending on if is exporting nodes yet or not.
     *
     * @param isExportingNodes True if is exporting nodes, false otherwise.
     */
    private fun updateUI(isExportingNodes: Boolean) {
        binding.copyButton.apply {
            isEnabled = !isExportingNodes
            alpha = if (isExportingNodes) ALPHA_VIEW_DISABLED else ALPHA_VIEW_ENABLED
        }
        refreshMenuOptionsVisibility()
    }

    /**
     * Copies to the clipboard all the links.
     *
     * @param links String containing all the links to copy.
     */
    private fun copyLinks(links: String, isForFirstTime: Boolean) {
        copyToClipboard(requireActivity(), links)
        (requireActivity() as SnackbarShower).showSnackbar(
            if (isForFirstTime) {
                requireContext().resources.getQuantityString(
                    R.plurals.general_snackbar_link_created_and_copied,
                    viewModel.getLinksNumber()
                )
            } else {
                requireContext().resources.getQuantityString(
                    R.plurals.links_copied_clipboard,
                    viewModel.getLinksNumber()
                )
            }
        )
    }
}