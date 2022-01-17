package mega.privacy.android.app.upload

import android.os.Bundle
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.interfaces.Scrollable

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
class UploadFolderActivity : PasscodeActivity(), Scrollable {

    private val viewModel: UploadFolderViewModel by viewModels()
    private lateinit var binding: ActivityUploadFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            intent.data?.let { uri ->
                DocumentFile.fromSingleUri(this@UploadFolderActivity, uri)?.let { documentFile ->
                    viewModel.retrieveFolderContent(documentFile)
                }
            }
        }
        binding = ActivityUploadFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        setupObservers()
    }

    fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
    }

    fun setupObservers() {
        viewModel.getCurrentFolder().observe(this, ::showCurrentFolder)
    }

    private fun showCurrentFolder(currentFolder: String) {
        supportActionBar?.title = currentFolder
    }

    override fun checkScroll() {
        TODO("Not yet implemented")
    }
}