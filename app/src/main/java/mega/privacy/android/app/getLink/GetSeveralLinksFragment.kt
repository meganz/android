package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentGetSeveralLinksBinding
import mega.privacy.android.app.getLink.adapter.LinksAdapter
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.Constants.ALPHA_VIEW_DISABLED
import mega.privacy.android.app.utils.Constants.ALPHA_VIEW_ENABLED
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.copyToClipboard

class GetSeveralLinksFragment : Fragment() {

    private val viewModel: GetSeveralLinksViewModel by activityViewModels()

    private lateinit var binding: FragmentGetSeveralLinksBinding

    private val linksAdapter by lazy { LinksAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetSeveralLinksBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
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
        viewModel.isExportingNodes().observe(viewLifecycleOwner, ::updateUI)
    }

    private fun showLinks(links: List<LinkItem>) {
        linksAdapter.submitList(links)
    }

    private fun updateUI(isExportingNodes: Boolean) {
        binding.copyButton.apply {
            isEnabled = !isExportingNodes
            alpha = if (isExportingNodes) ALPHA_VIEW_DISABLED else ALPHA_VIEW_ENABLED
        }
    }

    private fun copyLinks(links: String) {
        copyToClipboard(requireActivity(), links)
        (requireActivity() as SnackbarShower).showSnackbar(
            StringResourcesUtils.getQuantityString(R.plurals.links_copied_clipboard, 2)
        )
    }
}