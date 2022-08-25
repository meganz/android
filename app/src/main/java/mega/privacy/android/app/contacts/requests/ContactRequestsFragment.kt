package mega.privacy.android.app.contacts.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.ColorUtils.setElevationWithColor
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.Util

/**
 * Fragment that represents the UI showing the contact requests for the current user.
 */
@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    companion object {
        const val EXTRA_IS_OUTGOING = "isOutgoing"
        const val STATE_PAGER_POSITION = "STATE_PAGER_POSITION"
    }

    private lateinit var binding: FragmentContactRequestsBinding

    private val isOutgoing by extraNotNull(EXTRA_IS_OUTGOING, false)
    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val toolbarElevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactRequestsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()

        if (savedInstanceState?.containsKey(STATE_PAGER_POSITION) == true) {
            setViewPagerPosition(savedInstanceState.getInt(STATE_PAGER_POSITION))
        } else {
            val position = if (isOutgoing) {
                Tabs.OUTGOING.ordinal
            } else {
                Tabs.INCOMING.ordinal
            }
            setViewPagerPosition(position)

            viewModel.getDefaultPagerPosition(isOutgoing).observe(viewLifecycleOwner) { pagePosition ->
                setViewPagerPosition(pagePosition)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGER_POSITION, binding.pager.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_contact_search, menu)
        menu.findItem(R.id.action_search)?.setupSearchView { query ->
            viewModel.setQuery(query)
        }
    }

    private fun setupView() {
        binding.pager.apply {
            adapter = ContactRequestPageAdapter(this@ContactRequestsFragment)

            TabLayoutMediator(binding.tabs, this) { tab, position ->
                tab.text = if (position == Tabs.INCOMING.ordinal) {
                    getString(R.string.tab_received_requests)
                } else {
                    getString(R.string.tab_sent_requests)
                }
            }.attach()
        }
    }

    /**
     * Show toolbar elevation
     *
     * @param show  Flag to either show or hide toolbar elevation
     */
    fun showElevation(show: Boolean) {
        binding.tabs.setElevationWithColor(if (show) toolbarElevation else 0F)
        if (Util.isDarkMode(requireContext())) {
            (activity as ContactsActivity?)?.showElevation(show)
        }
    }

    /**
     * Update ViewPager current position
     *
     * @param pagerPosition     Position to be shown
     */
    private fun setViewPagerPosition(pagerPosition: Int) {
        binding.pager.post { binding.pager.currentItem = pagerPosition }
    }
}
