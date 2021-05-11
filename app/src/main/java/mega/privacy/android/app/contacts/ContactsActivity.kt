package mega.privacy.android.app.contacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.contacts.adapter.ContactsAdapter
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ActivityContactsBinding
import mega.privacy.android.app.lollipop.AddContactActivityLollipop

@AndroidEntryPoint
class ContactsActivity : PasscodeActivity() {

    private lateinit var binding: ActivityContactsBinding

    private val viewModel by viewModels<ContactsViewModel>()
    private val recentlyAddedAdapter: ContactsAdapter by lazy {
        ContactsAdapter(::onContactClick, ::onContactMoreInfoClick, false)
    }
    private val contactsAdapter: ContactsAdapter by lazy {
        ContactsAdapter(::onContactClick, ::onContactMoreInfoClick, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupObservers()
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAddContact.setOnClickListener { openAddContactScreen() }

        val adapterConfig = ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.listContacts.adapter = ConcatAdapter(adapterConfig, recentlyAddedAdapter, contactsAdapter)
        binding.listContacts.setHasFixedSize(true)
        binding.listContacts.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )
    }

    private fun setupObservers() {
        viewModel.getContacts().observe(this, ::showContacts)
        viewModel.getRecentlyAddedContacts().observe(this, ::showRecentlyAddedContacts)
    }

    private fun showContacts(items: List<ContactItem>) {
        contactsAdapter.submitList(items)
    }

    private fun showRecentlyAddedContacts(items: List<ContactItem>) {
        binding.txtContacts.isVisible = items.isNotEmpty()
        recentlyAddedAdapter.submitList(items)
    }

    private fun onContactClick(userHandle: Long) {
        // Do something
    }

    private fun onContactMoreInfoClick(userHandle: Long) {
        // Do something
    }

    private fun openAddContactScreen() {
        startActivity(Intent(this, AddContactActivityLollipop::class.java))
    }
}
