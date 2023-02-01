package mega.privacy.android.app.main.qrcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMycodeBinding
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CacheFolderManager.buildQrFile
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * The UI of showing current user's QR Code.
 */
@AndroidEntryPoint
class MyCodeFragment : Fragment(), View.OnClickListener {

    @Inject
    lateinit var getCurrentUserFullName: GetCurrentUserFullName

    @Inject
    lateinit var dbH: DatabaseHandler

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private var _binding: FragmentMycodeBinding? = null
    private val binding get() = _binding!!

    var myUser: MegaUser? = null
    var myEmail: String? = null

    var handle: Long = -1
    var contactLink: String? = null
    private var aB: ActionBar? = null
    private var abL: AppBarLayout? = null

    private var processingDialog: AlertDialog? = null

    private var qrCodeBitmap: Bitmap? = null
    private var qrFile: File? = null
    private var copyLink = true
    private var createQR = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        myEmail = megaApi.myUser.email
        myUser = megaApi.myUser
        if (savedInstanceState != null) {
            handle = savedInstanceState.getLong("handle")
            contactLink = savedInstanceState.getString("contactLink")
        }

        //remove QR image in old format
        buildQrFile(
            requireContext(),
            myEmail + QR_IMAGE_FILE_NAME_OLD
        )?.delete()
    }

    fun queryIfQRExists(): File? {
        Timber.d("queryIfQRExists")
        qrFile = buildQrFile(requireContext(), myEmail + QR_IMAGE_FILE_NAME)

        return qrFile?.takeIf { it.exists() }
    }

    private fun setImageQR() {
        Timber.d("setImageQR")
        qrFile?.let {
            if (it.exists() && it.length() > 0) {
                @Suppress("DEPRECATION")
                val bOpts = BitmapFactory.Options().apply {
                    inPurgeable = true
                    inInputShareable = true
                }

                qrCodeBitmap = BitmapFactory.decodeFile(it.absolutePath, bOpts)
                binding.qrCodeImage.setImageBitmap(qrCodeBitmap)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("handle", handle)
        outState.putString("contactLink", contactLink)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")

        _binding = FragmentMycodeBinding.inflate(inflater, container, false)

        if (aB == null) {
            aB = (requireActivity() as AppCompatActivity?)?.supportActionBar
        }
        aB?.run {
            title = StringResourcesUtils.getString(R.string.section_qr_code)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        abL = requireActivity().findViewById(R.id.app_bar_layout)

        copyLink = true
        createQR = false

        with(binding.qrCodeButtonCopyLink) {
            text = resources.getString(R.string.button_copy_link)
            isEnabled = false
            setOnClickListener(this@MyCodeFragment)
            contactLink?.let {
                binding.qrCodeLink.text = it
                isEnabled = true
            }
        }

        binding.myCodeScrollview.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            checkScroll()
        }
        val configuration = resources.configuration
        val width = getDP(RELATIVE_WIDTH)
        val params: LinearLayout.LayoutParams
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params = LinearLayout.LayoutParams(width - 80, width - 80)
            params.gravity = Gravity.CENTER
            params.setMargins(0, 0, 0, getDP(20))
            binding.qrCodeRelativeContainer.layoutParams = params
            binding.qrCodeRelativeContainer.setPadding(0, -40, 0, 0)
        } else {
            params = LinearLayout.LayoutParams(width, width)
            params.gravity = Gravity.CENTER
            params.setMargins(0, getDP(55), 0, getDP(58))
            binding.qrCodeRelativeContainer.layoutParams = params
        }
        createLink()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkScroll() {
        val withElevation = binding.myCodeScrollview.canScrollVertically(-1)
        abL?.elevation =
            if (withElevation) resources.getDimension(R.dimen.toolbar_elevation) else 0f
        changeStatusBarColorForElevation(requireActivity(), withElevation)
    }

    private fun getDP(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics).toInt()
    }

    private fun createQRCode(qr: Bitmap, avatar: Bitmap): Bitmap {
        Timber.d("createQRCode")
        val qrCode = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888)
        val width = AVATAR_WIDTH
        val offset = (width / 2).toFloat()
        val c = Canvas(qrCode)
        val paint = Paint()
        paint.isAntiAlias = true
        // Avatar border's color
        paint.color = ContextCompat.getColor(requireContext(),
            R.color.white_dark_grey)
        val scaledAvatar = Bitmap.createScaledBitmap(avatar, width, width, false)
        c.drawBitmap(qr, 0f, 0f, null)
        c.drawCircle(AVATAR_LEFT + offset,
            AVATAR_LEFT + offset,
            offset + Util.dp2px(BORDER_WIDTH.toFloat()),
            paint)
        c.drawBitmap(scaledAvatar, AVATAR_LEFT.toFloat(), AVATAR_LEFT.toFloat(), null)
        return qrCode
    }

    /**
     * Generate the QR code bitmap.
     *
     * @return QR code bitmap. Return null if there is any error.
     */
    private fun queryQR(): Bitmap? {
        val hints: MutableMap<EncodeHintType, ErrorCorrectionLevel?> = HashMap()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val bitMatrix: BitMatrix = try {
            MultiFormatWriter().encode(contactLink, BarcodeFormat.QR_CODE, WIDTH, WIDTH, hints)
        } catch (e: WriterException) {
            Timber.e(e)
            return null
        }
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        val color = ContextCompat.getColor(requireContext(), R.color.dark_grey)
        val bitmap = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = ContextCompat.getColor(requireContext(), R.color.white_grey_700)
        c.drawRect(0f, 0f, WIDTH.toFloat(), WIDTH.toFloat(), paint)
        paint.color = color
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (bitMatrix[x, y]) color else
                    ContextCompat.getColor(requireContext(), R.color.white_grey_700)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }


    private fun setUserAvatar(): Bitmap {
        Timber.d("setUserAvatar")
        val avatar = buildAvatarFile(requireContext(), "$myEmail.jpg")

        return avatar?.takeIf { avatar.exists() && avatar.length() > 0 }?.let { avatarFile ->
            @Suppress("DEPRECATION")
            val bOpts = BitmapFactory.Options().apply {
                inPurgeable = true
                inInputShareable = true
            }

            BitmapFactory.decodeFile(avatarFile.absolutePath, bOpts)?.let {
                getCircleBitmap(it)
            } ?: createDefaultAvatar()
        } ?: run {
            createDefaultAvatar()
        }
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        Timber.d("getCircleBitmap")
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        bitmap.recycle()
        return output
    }

    private fun createDefaultAvatar(): Bitmap {
        Timber.d("createDefaultAvatar()")
        val fullName: String?

        val credentials = dbH.credentials

        fullName = credentials?.firstName ?: credentials?.lastName
                ?: runBlocking {
            // temporary remove reference to MyAccountInfo.full name, it will be refactor when we create MyQRCodeViewModel
            getCurrentUserFullName(
                false,
                getString(R.string.first_name_text),
                getString(R.string.lastname_text)
            )
        }

        return AvatarUtil.getDefaultAvatar(AvatarUtil.getColorAvatar(myUser),
            fullName,
            Constants.AVATAR_SIZE,
            true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("onConfigurationChanged")
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Timber.d("Changed to LANDSCAPE")
        } else {
            Timber.d("Changed to PORTRAIT")
        }
    }

    override fun onAttach(context: Context) {
        Timber.d("onAttach context")
        super.onAttach(context)
        aB = (requireActivity() as AppCompatActivity?)?.supportActionBar

    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        when (v.id) {
            R.id.qr_code_button_copy_link -> {
                if (copyLink) {
                    copyLink()
                } else {
                    createLink()
                }
            }
        }
    }

    private fun copyLink() {
        Timber.d("copyLink")
        val clipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", contactLink)
        clipboardManager.setPrimaryClip(clip)
        (requireActivity() as QRCodeActivity?)?.showSnackbar(binding.root,
            getString(R.string.qrcode_link_copied))
    }

    private fun createLink() {
        qrFile = queryIfQRExists()
        qrFile?.takeIf { it.exists() }?.let {
            setImageQR()
            megaApi.contactLinkCreate(false, requireActivity() as QRCodeActivity?)
            return
        } ?: run {
            megaApi.contactLinkCreate(false, requireActivity() as QRCodeActivity?)
            var temp: AlertDialog? = null
            try {
                temp = createProgressDialog(requireContext(), getString(R.string.generatin_qr))
                temp.show()
            } catch (e: Exception) {
                Timber.e(e)
            }
            processingDialog = temp
        }
    }

    fun initCreateQR(request: MegaRequest, e: MegaError) {
        var reset = false
        if (handle != -1L && handle != request.nodeHandle && copyLink) {
            reset = true
        }
        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Contact link create LONG: %s", request.nodeHandle)
            Timber.d("Contact link create BASE64: https://mega.nz/C!%s",
                MegaApiAndroid.handleToBase64(request.nodeHandle))
            handle = request.nodeHandle
            contactLink = "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.nodeHandle)
            binding.qrCodeLink.text = contactLink
            val qrBitmap = queryQR() ?: return
            qrCodeBitmap = createQRCode(qrBitmap, setUserAvatar())
            val qrCodeFile = buildQrFile(requireContext(), myEmail + QR_IMAGE_FILE_NAME)
            qrCodeFile?.let { file ->
                try {
                    val out = FileOutputStream(file, false)
                    qrCodeBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                } catch (e1: FileNotFoundException) {
                    e1.printStackTrace()
                }
            }
            binding.qrCodeImage.setImageBitmap(qrCodeBitmap)
            binding.qrCodeButtonCopyLink.isEnabled = true
            if (reset) {
                (requireActivity() as QRCodeActivity?)?.resetSuccessfully(true)
            }
            if (createQR) {
                (requireActivity() as QRCodeActivity?)?.showSnackbar(binding.root,
                    resources.getString(R.string.qrcode_create_successfully))
                (requireActivity() as QRCodeActivity?)?.createSuccessfully()
                binding.qrCodeButtonCopyLink.text = resources.getString(R.string.button_copy_link)
                createQR = false
                copyLink = true
            }

            processingDialog?.dismiss()

        } else {
            if (reset) {
                (requireActivity() as QRCodeActivity?)?.resetSuccessfully(false)
            }
        }
    }

    fun initDeleteQR(request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Contact link delete:%d_%d_%s",
                e.errorCode,
                request.nodeHandle,
                MegaApiAndroid.handleToBase64(request.nodeHandle))
            val qrCodeFile = buildQrFile(requireContext(), myEmail + QR_IMAGE_FILE_NAME)
            qrCodeFile?.takeIf { it.exists() }?.delete()
            (requireActivity() as QRCodeActivity?)?.showSnackbar(binding.root,
                resources.getString(R.string.qrcode_delete_successfully))
            binding.qrCodeImage.setImageBitmap(Bitmap.createBitmap(WIDTH,
                WIDTH,
                Bitmap.Config.ARGB_8888))
            binding.qrCodeButtonCopyLink.text = resources.getString(R.string.button_create_qr)
            copyLink = false
            createQR = true
            binding.qrCodeLink.text = ""
            (requireActivity() as QRCodeActivity?)?.deleteSuccessfully()
        } else {
            (requireActivity() as QRCodeActivity?)?.showSnackbar(binding.root,
                resources.getString(R.string.qrcode_delete_not_successfully))
        }
    }

    fun resetQRCode() {
        Timber.d("resetQRCode")
        megaApi.contactLinkCreate(true, requireActivity() as QRCodeActivity?)
    }

    fun deleteQRCode() {
        Timber.d("deleteQRCode")
        megaApi.contactLinkDelete(handle, requireActivity() as QRCodeActivity?)
    }

    companion object {
        const val QR_IMAGE_FILE_NAME_OLD = "QRcode.jpg"
        const val QR_IMAGE_FILE_NAME = "QR_code_image.jpg"

        private const val RELATIVE_WIDTH = 280
        private const val WIDTH = 500
        private const val AVATAR_LEFT = 182
        private const val AVATAR_WIDTH = 135

        /**
         * Avatar's border width
         */
        const val BORDER_WIDTH = 3

        @JvmStatic
        fun newInstance(): MyCodeFragment {
            Timber.d("newInstance")
            return MyCodeFragment()
        }
    }
}