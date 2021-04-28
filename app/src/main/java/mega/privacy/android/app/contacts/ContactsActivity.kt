package mega.privacy.android.app.contacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.contacts.adapter.ContactsAdapter
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ActivityContactsBinding
import mega.privacy.android.app.lollipop.AddContactActivityLollipop

@AndroidEntryPoint
class ContactsActivity : PasscodeActivity() {

    private lateinit var binding: ActivityContactsBinding

    private val viewModel by viewModels<ContactsViewModel>()
    private val adapter: ContactsAdapter by lazy {
        ContactsAdapter(::onContactClick, ::onContactMoreInfoClick)
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
        binding.listContacts.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.getContacts().observe(this, ::showContacts)
    }

    private fun showContacts(items: List<ContactItem>) {
        adapter.submitList(items)
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
