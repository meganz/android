package mega.privacy.android.app.namecollision

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityNameCollisionBinding
import mega.privacy.android.app.domain.entity.NameCollision
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util

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

        binding.cancelInfo.text = StringResourcesUtils.getString(
            if (isFile) R.string.skip_file
            else R.string.skip_folder
        )

        val cancelButtonId: Int
        val renameInfoId: Int
        val renameButtonId: Int

        when (collision.type) {
            NameCollision.Type.UPLOAD -> {
                cancelButtonId = R.string.do_not_upload
                renameInfoId = R.string.warning_upload_and_rename
                renameButtonId = R.string.upload_and_rename
            }
            NameCollision.Type.COPY -> {
                cancelButtonId = R.string.do_not_copy
                renameInfoId = R.string.warning_copy_and_rename
                renameButtonId = R.string.copy_and_rename
            }
            NameCollision.Type.MOVE -> {
                cancelButtonId = R.string.do_not_move
                renameInfoId = R.string.warning_move_and_rename
                renameButtonId = R.string.move_and_rename
            }
        }

        binding.cancelButton.text = StringResourcesUtils.getString(cancelButtonId)

        if (isFile) {
            binding.renameInfo.text = StringResourcesUtils.getString(renameInfoId)
            binding.renameButton.text = StringResourcesUtils.getString(renameButtonId)
        }

        binding.cancelSeparator.isVisible = !isFile
        binding.renameInfo.isVisible = !isFile
        binding.renameView.optionView.isVisible = !isFile
        binding.renameButton.isVisible = !isFile

        binding.applyForAllCheck.isVisible = viewModel.getPendingCollisions() > 1
    }
}