package mega.privacy.android.app.getLink

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.databinding.FragmentGetSeveralLinksBinding
import mega.privacy.android.app.getLink.adapter.LinksAdapter
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.copyToClipboard

class GetSeveralLinksFragment : Fragment() {

    private val viewModel: GetSeveralLinksViewModel by activityViewModels()

    private lateinit var binding: FragmentGetSeveralLinksBinding
    private var menu: Menu? = null

    private val linksAdapter by lazy { LinksAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetSeveralLinksBinding.inflate(layoutInflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
            R.id.action_share -> {
                startActivity(
                    Intent(Intent.ACTION_SEND)
                        .setType(PLAIN_TEXT_SHARE_TYPE)
                        .putExtra(Intent.EXTRA_TEXT, viewModel.getLinksString())
                )
            }
            R.id.action_chat -> {
                startActivityForResult(
                    Intent(requireContext(), ChatExplorerActivity::class.java),
                    REQUEST_CODE_SEND_SEVERAL_LINKS
                )
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == BaseActivity.RESULT_OK && requestCode == REQUEST_CODE_SEND_SEVERAL_LINKS) {
            data?.putStringArrayListExtra(EXTRA_SEVERAL_LINKS, viewModel.getLinksList())

            MegaAttacher(requireActivity() as ActivityLauncher).handleActivityResult(
                REQUEST_CODE_SELECT_CHAT,
                BaseActivity.RESULT_OK,
                data,
                requireActivity() as SnackbarShower
            )
        }
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
            addItemDecoration(SimpleDividerItemDecoration(requireContext()))
        }

        binding.copyButton.apply {
            isEnabled = false
            setOnClickListener { copyLinks(viewModel.getLinksString()) }
        }
    }

    private fun setupObservers() {
        viewModel.getLinkItems().observe(viewLifecycleOwner, ::showLinks)
        viewModel.getExportingNodes().observe(viewLifecycleOwner, ::updateUI)
    }

    private fun showLinks(links: List<LinkItem>) {
        linksAdapter.submitList(links)
    }

    private fun updateUI(isExportingNodes: Boolean) {
        binding.copyButton.apply {
            isEnabled = !isExportingNodes
            alpha = if (isExportingNodes) ALPHA_VIEW_DISABLED else ALPHA_VIEW_ENABLED
        }

        refreshMenuOptionsVisibility()
    }

    private fun copyLinks(links: String) {
        copyToClipboard(requireActivity(), links)
        (requireActivity() as SnackbarShower).showSnackbar(
            StringResourcesUtils.getQuantityString(R.plurals.links_copied_clipboard, 2)
        )
    }
}