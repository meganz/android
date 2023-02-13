package mega.privacy.android.app.main.qrcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.ViewPager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityQrCodeBinding
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeViewModel
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel
import mega.privacy.android.app.presentation.settings.SettingsActivity.Companion.getIntent
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.utils.CacheFolderManager.buildQrFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Activity for user's QR code
 */
@AndroidEntryPoint
class QRCodeActivity : PasscodeActivity() {

    private val scanCodeViewModel: ScanCodeViewModel by viewModels()
    private val qrCodeViewModel: MyCodeViewModel by viewModels()

    private lateinit var binding: ActivityQrCodeBinding

    private var shareMenuItem: MenuItem? = null
    private var saveMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private var resetQRMenuItem: MenuItem? = null
    private var deleteQRMenuItem: MenuItem? = null

    private var myCodeFragment: MyCodeFragment? = null

    private var qrCodePageAdapter: QRCodePageAdapter? = null
    private var qrCodeFragmentPos = QR_CODE_PAGE_INDEX

    private var inviteContacts = false
    private var showScanQrView = false

    private var qrCodeSaveBottomSheetDialogFragment: QRCodeSaveBottomSheetDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        binding = ActivityQrCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            showScanQrView = it.getBoolean(Constants.OPEN_SCAN_QR, false)
            inviteContacts = it.getBoolean(Constants.INVITE_CONTACT, false)
        } ?: run {
            showScanQrView = intent.getBooleanExtra(Constants.OPEN_SCAN_QR, false)
            inviteContacts = intent.getBooleanExtra(Constants.INVITE_CONTACT, false)
        }

        scanCodeViewModel.updateFinishActivityOnScanComplete(inviteContacts)

        with(binding.toolbar) {
            visibility = View.VISIBLE
            title = getString(R.string.section_qr_code)
            setSupportActionBar(this)
        }

        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        qrCodePageAdapter = QRCodePageAdapter(supportFragmentManager, this)

        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
            requestPermission(
                this,
                MY_PERMISSIONS_REQUEST_CAMERA,
                Manifest.permission.CAMERA
            )
        } else {
            initActivity()
        }
        setupFlow()
    }

    private fun setupFlow() {
        collectFlow(qrCodeViewModel.uiState, Lifecycle.State.RESUMED) { uiState ->
            if (qrCodeFragmentPos != QR_CODE_PAGE_INDEX) return@collectFlow

            with(uiState) {
                val hasContactLink = contactLink != null
                shareMenuItem?.isVisible = hasContactLink
                saveMenuItem?.isVisible = hasContactLink
                deleteQRMenuItem?.isVisible = hasContactLink
                resetQRMenuItem?.isVisible = true
                settingsMenuItem?.isVisible = true

                localQRCodeFile?.let {
                    shareQR()
                    qrCodeViewModel.finishSharing()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initActivity()
            } else {
                finish()
            }
        }
    }

    private fun initActivity() {
        binding.qrCodeTabsPager.addOnPageChangeListener(
            object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                }

                override fun onPageSelected(position: Int) {
                    qrCodeFragmentPos = position
                    invalidateOptionsMenu()
                }

                override fun onPageScrollStateChanged(state: Int) {}
            }
        )

        binding.qrCodeTabsPager.apply {
            adapter = qrCodePageAdapter
            binding.slidingTabsQrCode.setupWithViewPager(this)
            currentItem = if (showScanQrView || inviteContacts) 1 else 0
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(Constants.OPEN_SCAN_QR, showScanQrView)
        outState.putBoolean(Constants.INVITE_CONTACT, inviteContacts)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        menuInflater.inflate(R.menu.activity_qr_code, menu)
        shareMenuItem = menu.findItem(R.id.qr_code_share)
        saveMenuItem = menu.findItem(R.id.qr_code_save)
        settingsMenuItem = menu.findItem(R.id.qr_code_settings)
        resetQRMenuItem = menu.findItem(R.id.qr_code_reset)
        deleteQRMenuItem = menu.findItem(R.id.qr_code_delete)



        when (qrCodeFragmentPos) {
            QR_CODE_PAGE_INDEX -> {
                val hasContactLink = qrCodeViewModel.uiState.value.contactLink != null
                shareMenuItem?.isVisible = hasContactLink
                saveMenuItem?.isVisible = hasContactLink
                settingsMenuItem?.isVisible = true
                resetQRMenuItem?.isVisible = true
                deleteQRMenuItem?.isVisible = hasContactLink
            }
            SCAN_CODE_PAGE_INDEX -> {
                shareMenuItem?.isVisible = false
                saveMenuItem?.isVisible = false
                settingsMenuItem?.isVisible = false
                resetQRMenuItem?.isVisible = false
                deleteQRMenuItem?.isVisible = false
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.qr_code_share -> {
                qrCodeViewModel.startSharing()
            }
            R.id.qr_code_save -> {
                if (!qrCodeSaveBottomSheetDialogFragment.isBottomSheetDialogShown()) {
                    qrCodeSaveBottomSheetDialogFragment =
                        QRCodeSaveBottomSheetDialogFragment().also {
                            it.show(supportFragmentManager, it.tag)
                        }
                }
            }
            R.id.qr_code_settings -> {
                val settingsIntent = getIntent(this, TargetPreference.QR).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(settingsIntent)
                finish()
            }
            R.id.qr_code_reset -> {
                resetQR()
            }
            R.id.qr_code_delete -> {
                deleteQR()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        intent?.takeIf { requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK }
            ?.getStringExtra(FileStorageActivity.EXTRA_PATH)
            ?.let { parentPath ->
                val myEmail = megaApi.myEmail
                val qrFile: File? = buildQrFile(
                    context = this,
                    fileName = myEmail + MyCodeFragment.QR_IMAGE_FILE_NAME
                )

                if (qrFile == null) {
                    showSnackbar(binding.rootLevelLayout, getString(R.string.general_error))
                    return
                }

                if (!qrFile.exists()) {
                    showSnackbar(binding.rootLevelLayout, getString(R.string.error_download_qr))
                    return
                }

                val hasStoragePermission =
                    hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (!hasStoragePermission) {
                    requestPermission(
                        this,
                        Constants.REQUEST_WRITE_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }

                val availableFreeSpace = try {
                    StatFs(parentPath).run {
                        availableBlocks.toDouble() * blockSize.toDouble()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    Double.MAX_VALUE
                }

                if (availableFreeSpace < qrFile.length()) {
                    showSnackbar(
                        binding.rootLevelLayout,
                        getString(R.string.error_not_enough_free_space)
                    )
                    return
                }

                val newQrFile =
                    File(parentPath, "$myEmail${MyCodeFragment.QR_IMAGE_FILE_NAME}")

                try {
                    newQrFile.createNewFile()
                    val src = FileInputStream(qrFile).channel
                    val dst = FileOutputStream(newQrFile, false).channel
                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    showSnackbar(
                        binding.rootLevelLayout,
                        getString(R.string.success_download_qr, parentPath)
                    )
                } catch (e: IOException) {
                    Timber.e(e)
                    showSnackbar(
                        binding.rootLevelLayout,
                        getString(R.string.general_error)
                    )
                }
            }
    }

    private fun shareQR() {
        Timber.d("shareQR")
        ensureMyCodeFragment()
        myCodeFragment?.takeIf { it.isAdded }?.run {
            qrCodeViewModel.uiState.value.localQRCodeFile?.let { qrCodeFile ->
                Timber.d("Use provider to share")

                val uri = FileProvider.getUriForFile(
                    this@QRCodeActivity,
                    AUTHORITY_STRING_FILE_PROVIDER,
                    qrCodeFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share)))
            } ?: run {
                showSnackbar(binding.rootLevelLayout, getString(R.string.error_share_qr))
            }
        }
    }

    private fun resetQR() {
        Timber.d("resetQRCode")
        val penColor = ContextCompat.getColor(this, R.color.dark_grey)
        val bgColor = ContextCompat.getColor(this, R.color.white_grey_700)
        val avatarBorderColor = ContextCompat.getColor(this, R.color.white_dark_grey)
        qrCodeViewModel.resetQRCode(
            width = MyCodeFragment.QRCODE_WIDTH,
            height = MyCodeFragment.QRCODE_WIDTH,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = MyCodeFragment.AVATAR_WIDTH,
            avatarBorderWidth = MyCodeFragment.AVATAR_BORDER_WIDTH,
            avatarBorderColor = avatarBorderColor,
        )
    }

    private fun deleteQR() {
        Timber.d("deleteQR")
        ensureMyCodeFragment()
        myCodeFragment?.takeIf { it.isAdded }?.run { qrCodeViewModel.deleteQR() }
    }

    override fun showSnackbar(view: View, s: String) =
        showSnackbar(Constants.SNACKBAR_TYPE, view, s)

    private fun ensureMyCodeFragment() {
        if (myCodeFragment == null) {
            Timber.w("MyCodeFragment is NULL")
            myCodeFragment =
                qrCodePageAdapter?.instantiateItem(
                    binding.qrCodeTabsPager,
                    QR_CODE_PAGE_INDEX
                ) as? MyCodeFragment
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1010
        private const val QR_CODE_PAGE_INDEX = 0
        private const val SCAN_CODE_PAGE_INDEX = 1
    }
}