package mega.privacy.android.app.modalbottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.databinding.BottomSheetQrCodeBinding
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollision.Upload.Companion.getUploadCollision
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.bottomsheet.QrCodeSaveBottomSheetDialogViewModel
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeFragment
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException.ChildDoesNotExistsException
import mega.privacy.android.app.usecase.exception.MegaNodeException.ParentDoesNotExistException
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CacheFolderManager.buildQrFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import mega.privacy.android.domain.entity.StorageState
import timber.log.Timber
import javax.inject.Inject

/**
 * Bottom sheet dialog to save QR code
 */
@AndroidEntryPoint
class QRCodeSaveBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    /**
     * check Name collision use case
     */
    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    /**
     * upload use case
     */
    @Inject
    lateinit var uploadUseCase: UploadUseCase

    private val viewModel: QrCodeSaveBottomSheetDialogViewModel by viewModels()

    private var _binding: BottomSheetQrCodeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetQrCodeBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.qrCodeSaveToCloudLayout.setOnClickListener {
            saveToCloudDrive()
            setStateBottomSheetBehaviorHidden()
        }
        binding.qrCodeSaveToFileSystemLayout.setOnClickListener {
            saveToFileSystem()
            setStateBottomSheetBehaviorHidden()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveToCloudDrive() {
        val parentNode = megaApi.rootNode
        val myEmail = megaApi.myUser?.email ?: return
        val qrFile =
            buildQrFile(requireActivity(), myEmail + MyCodeFragment.QR_IMAGE_FILE_NAME)
                ?.takeIf { FileUtil.isFileAvailable(it) }
                ?: run {
                    Util.showSnackbar(requireActivity(), getString(R.string.error_upload_qr))
                    return
                }
        if (viewModel.getStorageState() === StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        val checkNameCollisionDisposable =
            checkNameCollisionUseCase.check(qrFile.name, parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { handle: Long ->
                        val list = ArrayList<NameCollision>()
                        list.add(
                            getUploadCollision(handle, qrFile, parentNode?.handle)
                        )
                        (requireActivity() as QRCodeActivity).nameCollisionActivityContract?.launch(
                            list
                        )
                    }
                ) { throwable: Throwable? ->
                    if (throwable is ParentDoesNotExistException) {
                        Util.showSnackbar(requireActivity(), getString(R.string.error_upload_qr))
                    } else if (throwable is ChildDoesNotExistsException) {
                        val info = ShareInfo.infoFromFile(qrFile)
                        if (info == null) {
                            Util.showSnackbar(
                                requireActivity(),
                                getString(R.string.error_upload_qr)
                            )
                            return@subscribe
                        }
                        checkNotificationsPermission(requireActivity())
                        val text = getString(R.string.save_qr_cloud_drive, qrFile.name)
                        val uploadDisposable =
                            uploadUseCase.upload(requireActivity(), info, null, parentNode?.handle)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Util.showSnackbar(
                                        requireActivity(),
                                        text
                                    )
                                }) { t: Throwable? -> Timber.e(t) }
                    }
                }
    }

    private fun saveToFileSystem() {
        val intent = Intent(requireActivity(), FileStorageActivity::class.java).apply {
            putExtra(
                FileStorageActivity.PICK_FOLDER_TYPE,
                FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.folderType
            )
            action = FileStorageActivity.Mode.PICK_FOLDER.action
        }
        (requireActivity() as QRCodeActivity).startActivityForResult(
            intent,
            Constants.REQUEST_DOWNLOAD_FOLDER
        )
    }
}