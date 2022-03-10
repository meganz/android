package mega.privacy.android.app.namecollision

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FILE_VERSIONS
import mega.privacy.android.app.databinding.ActivityNameCollisionBinding
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getSizeString

/**
 * Activity for showing name collisions and resolving them as per user's choices.
 */
class NameCollisionActivity : PasscodeActivity() {

    companion object {

        @JvmStatic
        fun getIntentForList(
            context: Context,
            collisions: ArrayList<NameCollision>
        ): Intent =
            Intent(context, NameCollisionActivity::class.java).apply {
                putExtra(INTENT_EXTRA_COLLISION_RESULTS, collisions)
            }

        @JvmStatic
        fun getIntentForSingleItem(
            context: Context,
            collision: NameCollision
        ): Intent =
            Intent(context, NameCollisionActivity::class.java).apply {
                putExtra(INTENT_EXTRA_COLLISION_RESULTS, collision)
            }
    }

    private val viewModel: NameCollisionViewModel by viewModels()

    private lateinit var binding: ActivityNameCollisionBinding

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val elevationColor by lazy {
        ContextCompat.getColor(this, R.color.action_mode_background)
    }
    private val noElevationColor by lazy { ContextCompat.getColor(this, R.color.dark_grey) }

    private val updateFileVersionsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_UPDATE_FILE_VERSIONS != intent.action) return

            viewModel.fileVersioningUpdated()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameCollisionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            @Suppress("UNCHECKED_CAST")
            viewModel.setData(
                intent.getSerializableExtra(INTENT_EXTRA_COLLISION_RESULTS) as ArrayList<NameCollision>?,
                intent.getSerializableExtra(INTENT_EXTRA_COLLISION_RESULTS) as NameCollision?
            )
        }

        setupView()
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateFileVersionsReceiver)
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.scrollView.setOnScrollChangeListener { v, _, _, _, _ ->
            val showElevation = v.canScrollVertically(RecyclerView.NO_POSITION)

            binding.toolbar.elevation = if (showElevation) elevation else 0F

            if (Util.isDarkMode(this@NameCollisionActivity)) {
                val color = if (showElevation) elevationColor else noElevationColor
                window.statusBarColor = color
                binding.toolbar.setBackgroundColor(color)
            }
        }

        binding.replaceUpdateMergeButton.setOnClickListener { }
        binding.cancelButton.setOnClickListener { }
        binding.renameButton.setOnClickListener { }
    }

    private fun setupObservers() {
        viewModel.getCurrentCollision().observe(this, ::showCollision)
        viewModel.getFileVersioningInfo().observe(this, ::updateFileVersioningData)

        registerReceiver(
            updateFileVersionsReceiver,
            IntentFilter(ACTION_UPDATE_FILE_VERSIONS)
        )
    }

    private fun showCollision(collision: NameCollision) {
        val isFile = collision.isFile
        val name = collision.name

        binding.alreadyExistsText.text = StringResourcesUtils.getString(
            if (isFile) R.string.file_already_exists_in_location
            else R.string.folder_already_exists_in_location, name
        )

        binding.selectText.text = StringResourcesUtils.getString(
            if (isFile) R.string.choose_file
            else R.string.choose_folder
        )

        binding.replaceUpdateMergeView.apply {
            thumbnail.setActualImageResource(
                if (isFile) MimeTypeList.typeForName(name).iconResourceId
                else R.drawable.ic_folder_list
            )
            this.name.text = name
            size.text = getSizeString(collision.size)
            date.text = formatLongDateTime(collision.lastModified)
        }

        binding.cancelInfo.text = StringResourcesUtils.getString(
            if (isFile) R.string.skip_file
            else R.string.skip_folder
        )

        val cancelButtonId: Int
        val renameInfoId: Int
        val renameButtonId: Int

        when (collision) {
            is NameCollision.Upload -> {
                cancelButtonId = R.string.do_not_upload
                renameInfoId = R.string.warning_upload_and_rename
                renameButtonId = R.string.upload_and_rename
            }
            is NameCollision.Copy -> {
                cancelButtonId = R.string.do_not_copy
                renameInfoId = R.string.warning_copy_and_rename
                renameButtonId = R.string.copy_and_rename
            }
            is NameCollision.Movement -> {
                cancelButtonId = R.string.do_not_move
                renameInfoId = R.string.warning_move_and_rename
                renameButtonId = R.string.move_and_rename
            }
        }

        binding.cancelButton.text = StringResourcesUtils.getString(cancelButtonId)

        binding.cancelSeparator.isVisible = !isFile
        binding.renameInfo.isVisible = !isFile
        binding.renameView.optionView.isVisible = !isFile
        binding.renameButton.isVisible = !isFile

        if (isFile) {
            binding.renameInfo.text = StringResourcesUtils.getString(renameInfoId)
            binding.renameView.apply {
                thumbnail.setActualImageResource(MimeTypeList.typeForName(name).iconResourceId)
                this.name.text = name
                size.isVisible = false
                date.isVisible = false
            }
            binding.renameButton.text = StringResourcesUtils.getString(renameButtonId)
        }

        binding.applyForAllCheck.isVisible = viewModel.getPendingCollisions() > 1
    }

    private fun updateFileVersioningData(fileVersioningInfo: Triple<Boolean, NameCollisionViewModel.NameCollisionType, Boolean>) {
        val isFileVersioningEnabled = fileVersioningInfo.first
        val isFile = fileVersioningInfo.third

        binding.learnMore.isVisible = isFileVersioningEnabled

        val replaceUpdateMergeInfoId: Int
        val replaceUpdateMergeButtonId: Int

        when (fileVersioningInfo.second) {
            NameCollisionViewModel.NameCollisionType.UPLOAD -> {
                replaceUpdateMergeInfoId = when {
                    !isFile -> R.string.warning_upload_and_merge
                    isFileVersioningEnabled -> R.string.warning_versioning_upload_and_update
                    else -> R.string.warning_upload_and_replace
                }
                replaceUpdateMergeButtonId = when {
                    !isFile -> R.string.upload_and_merge
                    isFileVersioningEnabled -> R.string.upload_and_update
                    else -> R.string.upload_and_replace
                }
            }
            NameCollisionViewModel.NameCollisionType.COPY -> {
                replaceUpdateMergeInfoId =
                    if (isFile) R.string.warning_copy_and_replace
                    else R.string.warning_copy_and_merge
                replaceUpdateMergeButtonId =
                    if (isFile) R.string.copy_and_replace
                    else R.string.copy_and_merge
            }
            NameCollisionViewModel.NameCollisionType.MOVEMENT -> {
                replaceUpdateMergeInfoId =
                    if (isFile) R.string.warning_move_and_replace
                    else R.string.warning_move_and_merge
                replaceUpdateMergeButtonId =
                    if (isFile) R.string.move_and_replace
                    else R.string.move_and_merge
            }
        }

        binding.replaceUpdateMergeInfo.text =
            StringResourcesUtils.getString(replaceUpdateMergeInfoId)
        binding.replaceUpdateMergeButton.text =
            StringResourcesUtils.getString(replaceUpdateMergeButtonId)
    }
}