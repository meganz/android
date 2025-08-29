package mega.privacy.android.app.namecollision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityNameCollisionBinding
import mega.privacy.android.app.databinding.ViewNameCollisionOptionBinding
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.namecollision.data.NameCollisionActionResult
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.app.namecollision.data.toDomainEntity
import mega.privacy.android.app.namecollision.data.toUiEntity
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_SINGLE_COLLISION_RESULT
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for showing name collisions and resolving them as per user's choices.
 */
@AndroidEntryPoint
class NameCollisionActivity : PasscodeActivity() {
    private val viewModel: NameCollisionViewModel by viewModels()

    private lateinit var binding: ActivityNameCollisionBinding

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            @Suppress("UNCHECKED_CAST")
            val collisionsList = with(intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getSerializableExtra(INTENT_EXTRA_COLLISION_RESULTS, ArrayList::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getSerializableExtra(INTENT_EXTRA_COLLISION_RESULTS)
                } as ArrayList<NameCollisionUiEntity>?
            }

            val singleCollision = with(intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getSerializableExtra(
                        INTENT_EXTRA_SINGLE_COLLISION_RESULT,
                        NameCollisionUiEntity::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    getSerializableExtra(INTENT_EXTRA_SINGLE_COLLISION_RESULT)
                } as NameCollisionUiEntity?
            }

            when {
                collisionsList != null -> viewModel.setData(
                    collisionsList.map { it.toDomainEntity() }
                )

                singleCollision != null -> viewModel.setSingleData(
                    singleCollision.toDomainEntity()
                )

                else -> {
                    Timber.e("No collisions received")
                    finish()
                }
            }

            viewModel.isFolderUploadContext = UPLOAD_FOLDER_CONTEXT == intent.action
        }

        if (!viewModel.isCopyToOrigin) {
            setTheme(R.style.Theme_Mega)
            enableEdgeToEdge()
            binding = ActivityNameCollisionBinding.inflate(layoutInflater)
            consumeInsetsWithToolbar(customToolbar = binding.toolbar)
            setContentView(binding.root)
            setupView()
        }
        setupObservers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupView() {
        addStartUploadTransferView()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = ""
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.scrollView.setOnScrollChangeListener { v, _, _, _, _ ->
            val showElevation = v.canScrollVertically(RecyclerView.NO_POSITION)

            binding.toolbar.elevation = if (showElevation) elevation else 0F
        }

        binding.learnMore.setOnClickListener {
            this.launchUrl(LEARN_MORE_URI)
        }
        binding.replaceUpdateMergeButton.setOnClickListener {
            viewModel.replaceUpdateOrMerge(binding.applyForAllCheck.isChecked)
        }
        binding.cancelButton.setOnClickListener {
            viewModel.cancel(binding.applyForAllCheck.isChecked)
        }
        binding.renameButton.setOnClickListener {
            viewModel.rename(binding.applyForAllCheck.isChecked)
        }
    }

    private fun addStartUploadTransferView() {
        binding.root.addView(
            createStartTransferView(
                activity = this,
                transferEventState = viewModel.uiState.map { it.uploadEvent },
                onConsumeEvent = { },
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        this,
                        storageTargetPreference
                    )
                }
            ) { transferEvent ->
                ((transferEvent as StartTransferEvent.FinishUploadProcessing).triggerEvent as TransferTriggerEvent.StartUpload.CollidedFiles).let {
                    setResult(
                        NameCollisionActionResult(
                            message = resources.getQuantityString(
                                R.plurals.upload_began,
                                it.pathsAndNames.size,
                                it.pathsAndNames.size,
                            ),
                            shouldFinish = viewModel.shouldFinish()
                        )
                    )
                    viewModel.consumeUploadEvent()
                }
            }
        )
    }

    private fun setupObservers() {
        viewModel.updateCurrentCollision().observe(this, ::showCollision)
        viewModel.getFileVersioningInfo().observe(this, ::updateFileVersioningData)
        viewModel.onActionResult().observe(this, this::setResult)
        viewModel.onExceptionThrown().observe(this) { error ->
            if (!manageCopyMoveException(error) && error is MegaException) {
                showSnackbar(error.message!!)
            }
        }
        viewModel.getCollisionsResolution().observe(this, ::manageCollisionsResolution)
    }

    private fun setResult(result: NameCollisionActionResult) {
        if (result.isForeignNode) {
            showForeignStorageOverQuotaWarningDialog(this)
            return
        }

        if (result.shouldFinish) {
            setResult(RESULT_OK, Intent().putExtra(MESSAGE_RESULT, result.message))
            finish()
        }
    }

    /**
     * Shows the current collision.
     *
     * @param nodeCollisionResult   Object containing all the required info to present a collision.
     */
    private fun showCollision(nodeCollisionResult: NodeNameCollisionResult?) {
        val collisionResult = nodeCollisionResult?.toUiEntity()
        if (collisionResult == null) {
            Timber.e("Cannot show any collision. Finishing...")
            finish()
            return
        }

        // Don't populate UI if copying to the original folder as it will automatically replicate the file
        if (viewModel.isCopyToOrigin)
            return

        supportActionBar?.title = getString(R.string.title_duplicated_items)
        binding.progressBarContainer.visibility = View.GONE

        val collision = collisionResult.nameCollision
        val isFile = collision.isFile
        val name = collision.name

        binding.alreadyExistsText.text = getString(
            if (isFile) R.string.file_already_exists_in_location
            else R.string.folder_already_exists_in_location, name
        ).formatColorTag(this, 'B', R.color.grey_900_grey_100)
            .toSpannedHtmlText()

        binding.selectText.text = getString(
            if (isFile) R.string.choose_file
            else R.string.choose_folder
        )

        binding.replaceUpdateMergeView.apply {
            val hasThumbnail = collisionResult.thumbnail != null
            thumbnail.isVisible = hasThumbnail
            thumbnailIcon.isVisible = !hasThumbnail
            when {
                hasThumbnail -> {
                    thumbnail.load(collisionResult.thumbnail) {
                        transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.thumbnail_corner_radius)))
                    }
                }

                else -> {
                    thumbnailIcon.setImageResource(
                        if (isFile) MimeTypeList.typeForName(name).iconResourceId
                        else IconPackR.drawable.ic_folder_medium_solid
                    )

                    if (isFile && collisionResult.nameCollision is NameCollisionUiEntity.Upload) {
                        requestFileThumbnail(collisionResult.nameCollision.absolutePath)
                    }
                }
            }
            this.name.text = name
            size.text = if (isFile) getSizeString(
                collision.size ?: 0,
                this@NameCollisionActivity
            ) else getFolderContentString(collision)
            date.text = formatLongDateTime(
                if (collision is NameCollisionUiEntity.Upload) collision.lastModified / 1000
                else collision.lastModified
            )

            val thumbnailView = if (hasThumbnail) R.id.thumbnail else R.id.thumbnail_icon

            ConstraintSet().apply {
                clone(root)
                connect(R.id.name, ConstraintSet.TOP, thumbnailView, ConstraintSet.TOP)
                applyTo(root)
            }
        }

        binding.cancelInfo.text = getString(
            if (isFile) R.string.skip_file
            else R.string.skip_folder
        )

        val cancelButtonId: Int
        val renameInfoId: Int
        val renameButtonId: Int

        when (collision) {
            is NameCollisionUiEntity.Upload -> {
                cancelButtonId = R.string.do_not_upload
                renameInfoId = R.string.warning_upload_and_rename
                renameButtonId = R.string.upload_and_rename
            }

            is NameCollisionUiEntity.Copy, is NameCollisionUiEntity.Import -> {
                cancelButtonId = R.string.do_not_copy
                renameInfoId = R.string.warning_copy_and_rename
                renameButtonId = R.string.copy_and_rename
            }

            is NameCollisionUiEntity.Movement -> {
                cancelButtonId = R.string.do_not_move
                renameInfoId = R.string.warning_move_and_rename
                renameButtonId = R.string.move_and_rename
            }
        }

        binding.cancelView.apply {
            val hasThumbnail = collisionResult.thumbnail != null
            thumbnail.isVisible = hasThumbnail
            thumbnailIcon.isVisible = !hasThumbnail
            if (hasThumbnail) {
                thumbnail.load(collisionResult.thumbnail) {
                    transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.thumbnail_corner_radius)))
                }
            } else {
                thumbnailIcon.setImageResource(
                    if (isFile) MimeTypeList.typeForName(name).iconResourceId
                    else IconPackR.drawable.ic_folder_medium_solid
                )

                if (collisionResult.nameCollision is NameCollisionUiEntity.Upload) {
                    requestFileThumbnail(collisionResult.nameCollision.absolutePath)
                }
            }
            this.name.text = collisionResult.collisionName
            size.text =
                if (isFile) getSizeString(
                    collisionResult.collisionSize ?: 0,
                    this@NameCollisionActivity
                )
                else nodeCollisionResult.collisionFolderContent?.let { getFolderContentString(it) }
            date.text = formatLongDateTime(collisionResult.collisionLastModified ?: 0)

            val thumbnailView = if (hasThumbnail) R.id.thumbnail else R.id.thumbnail_icon

            ConstraintSet().apply {
                clone(root)
                connect(R.id.name, ConstraintSet.TOP, thumbnailView, ConstraintSet.TOP)
                applyTo(root)
            }
        }

        binding.cancelButton.text = getString(cancelButtonId)

        binding.renameSeparator.isVisible = isFile
        binding.renameInfo.isVisible = isFile
        binding.renameView.optionView.isVisible = isFile
        binding.renameButton.isVisible = isFile

        if (isFile) {
            binding.renameInfo.text = getString(renameInfoId)
            binding.renameView.apply {
                val hasThumbnail = collisionResult.thumbnail != null
                thumbnail.isVisible = hasThumbnail
                thumbnailIcon.isVisible = !hasThumbnail
                when {
                    hasThumbnail -> {
                        thumbnail.load(collisionResult.thumbnail) {
                            transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.thumbnail_corner_radius)))
                        }
                    }

                    else -> {
                        thumbnailIcon.setImageResource(MimeTypeList.typeForName(name).iconResourceId)

                        if (collisionResult.nameCollision is NameCollisionUiEntity.Upload) {
                            requestFileThumbnail(collisionResult.nameCollision.absolutePath)
                        }
                    }
                }
                this.name.text = collisionResult.nameCollision.renameName
                size.isVisible = false
                date.isVisible = false

                val thumbnailView = if (hasThumbnail) R.id.thumbnail else R.id.thumbnail_icon

                ConstraintSet().apply {
                    clone(root)
                    connect(
                        thumbnailView,
                        ConstraintSet.BOTTOM,
                        root.id,
                        ConstraintSet.BOTTOM
                    )
                    centerVertically(R.id.name, thumbnailView)
                    applyTo(root)
                }
            }
            binding.renameButton.text = getString(renameButtonId)
        }

        val pendingCollisions =
            if (isFile) viewModel.pendingFileCollisions
            else viewModel.pendingFolderCollisions

        binding.applyForAllCheckLayout.isVisible = pendingCollisions > 0

        if (pendingCollisions > 0) {
            binding.applyForAllCheckMsg.text = this.resources.getQuantityString(
                R.plurals.general_name_collision_file_apply_for_all,
                pendingCollisions, pendingCollisions,
            )
        }
    }

    private fun getFolderContentString(collision: FolderTreeInfo) =
        TextUtil.getFolderInfo(collision.numberOfFolders, collision.numberOfFiles, this)

    private fun getFolderContentString(collision: NameCollisionUiEntity) =
        TextUtil.getFolderInfo(collision.childFolderCount, collision.childFileCount, this)

    /**
     * Requests the thumbnail of a file through Fresco controller and updates the UI if get.
     *
     * @param absolutePath The path from which the thumbnail will be requested.
     */
    private fun ViewNameCollisionOptionBinding.requestFileThumbnail(absolutePath: String) {
        with(thumbnail) {
            isVisible = true
            load(absolutePath) {
                transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.thumbnail_corner_radius)))
                listener(
                    onSuccess = { _, _ -> finishThumbnailRequest(true) },
                    onError = { _, _ -> finishThumbnailRequest(false) }
                )
            }
        }
    }

    /**
     * Updates the UI according to the thumbnail request.
     *
     * @param success True if the thumbnail was set, false otherwise.
     */
    private fun ViewNameCollisionOptionBinding.finishThumbnailRequest(success: Boolean) {
        runOnUiThread {
            val thumbnailView = if (success) {
                thumbnailIcon.isVisible = false
                thumbnail.id
            } else {
                thumbnail.isVisible = false
                thumbnailIcon.id
            }

            root.post {
                ConstraintSet().apply {
                    clone(root)

                    if (root.id == R.id.rename_view) {
                        connect(
                            thumbnailView,
                            ConstraintSet.BOTTOM,
                            root.id,
                            ConstraintSet.BOTTOM
                        )
                        centerVertically(R.id.name, thumbnailView)
                    } else {
                        connect(R.id.name, ConstraintSet.TOP, thumbnailView, ConstraintSet.TOP)
                    }

                    applyTo(root)
                }
            }
        }
    }

    /**
     * Updates the UI related to file versioning.
     *
     * @param fileVersioningInfo Triple with the following info:
     *                              - First:    True if file versioning is enabled, false otherwise.
     *                              - Second:   Collision type: upload, movement of copy.
     *                              - Third:    True if the collision is related to a file, false if is to a folder.
     */
    private fun updateFileVersioningData(fileVersioningInfo: Triple<Boolean, NameCollisionType, Boolean>) {
        if (viewModel.isCopyToOrigin)
            return

        val isFileVersioningEnabled = fileVersioningInfo.first
        val nameCollisionType = fileVersioningInfo.second
        val isFile = fileVersioningInfo.third

        binding.learnMore.isVisible =
            isFileVersioningEnabled && nameCollisionType == NameCollisionType.UPLOAD

        val replaceUpdateMergeInfoId: Int
        val replaceUpdateMergeButtonId: Int

        when (nameCollisionType) {
            NameCollisionType.UPLOAD -> {
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

            NameCollisionType.COPY -> {
                replaceUpdateMergeInfoId =
                    if (isFile) R.string.warning_copy_and_replace
                    else R.string.warning_copy_and_merge
                replaceUpdateMergeButtonId =
                    if (isFile) R.string.copy_and_replace
                    else R.string.copy_and_merge
            }

            NameCollisionType.MOVE -> {
                replaceUpdateMergeInfoId =
                    if (isFile) R.string.warning_move_and_replace
                    else R.string.warning_move_and_merge
                replaceUpdateMergeButtonId =
                    if (isFile) R.string.move_and_replace
                    else R.string.move_and_merge
            }
        }

        binding.replaceUpdateMergeInfo.text =
            getString(replaceUpdateMergeInfoId)
        binding.replaceUpdateMergeButton.text =
            getString(replaceUpdateMergeButtonId)
    }

    private fun manageCollisionsResolution(collisionsResolution: ArrayList<NodeNameCollisionResult>) {
        setResult(
            Activity.RESULT_OK,
            Intent().putParcelableArrayListExtra(
                INTENT_EXTRA_COLLISION_RESULTS,
                collisionsResolution.map { it.toUiEntity() }.toCollection(ArrayList())
            )
        )
        finish()
    }


    companion object {
        private const val LEARN_MORE_URI =
            "https://help.mega.io/files-folders/restore-delete/file-version-history"

        private const val UPLOAD_FOLDER_CONTEXT = "UPLOAD_FOLDER_CONTEXT"
        const val MESSAGE_RESULT = "MESSAGE_RESULT"

        @JvmStatic
        fun getIntentForList(
            context: Context,
            collisions: ArrayList<NameCollision>,
        ): Intent = Intent(context, NameCollisionActivity::class.java).apply {
            putExtra(
                INTENT_EXTRA_COLLISION_RESULTS,
                collisions.map { it.toUiEntity() }.toCollection(ArrayList())
            )
        }

        @JvmStatic
        fun getIntentForFolderUpload(
            context: Context,
            collisions: ArrayList<NameCollision>,
        ): Intent = getIntentForList(
            context,
            collisions
        ).apply { action = UPLOAD_FOLDER_CONTEXT }

        @JvmStatic
        fun getIntentForSingleItem(
            context: Context,
            collision: NameCollision,
        ): Intent = Intent(context, NameCollisionActivity::class.java).apply {
            putExtra(INTENT_EXTRA_SINGLE_COLLISION_RESULT, collision.toUiEntity())
        }
    }
}