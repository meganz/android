package mega.privacy.android.app.main.qrcode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.CodeScanner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.Result
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.DialogAcceptContactBinding
import mega.privacy.android.app.databinding.DialogInviteBinding
import mega.privacy.android.app.databinding.FragmentScanCodeBinding
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * ScanCodeFragment
 */
@AndroidEntryPoint
class ScanCodeFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentScanCodeBinding? = null
    private val binding get() = _binding!!

    private var _dialogInviteBinding: DialogInviteBinding? = null
    private val dialogInviteBinding get() = _dialogInviteBinding!!

    private var _dialogAcceptContactBinding: DialogAcceptContactBinding? = null
    private val dialogAcceptContactBinding get() = _dialogAcceptContactBinding!!

    private var mStartPreviewRetried = 0
    private var codeScanner: CodeScanner? = null
    private var inviteAlertDialog: AlertDialog? = null
    private var requestedAlertDialog: AlertDialog? = null

    @JvmField
    var myEmail: String? = null
    var handler: Handler? = null
    var handle: Long = -1
    var handleContactLink: Long = -1
    private var success = true
    private var printEmail = false
    private var inviteShown = false
    private var dialogshown = false

    @JvmField
    var dialogTitleContent = -1

    @JvmField
    var dialogTextContent = -1
    private var contactNameContent: String? = null
    private var isContact = false
    private val avatarSave: Bitmap? = null
    private val initialLetterSave: String? = null
    private var contentAvatar = false
    private var userQuery: MegaUser? = null

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isContact = savedInstanceState.getBoolean("isContact", false)
            inviteShown = savedInstanceState.getBoolean("inviteShown", false)
            dialogshown = savedInstanceState.getBoolean("dialogshown", false)
            dialogTitleContent = savedInstanceState.getInt("dialogTitleContent", -1)
            dialogTextContent = savedInstanceState.getInt("dialogTextContent", -1)
            contactNameContent = savedInstanceState.getString("contactNameContent")
            myEmail = savedInstanceState.getString("myEmail")
            success = savedInstanceState.getBoolean("success", true)
            printEmail = savedInstanceState.getBoolean(PRINT_EMAIL, false)
            handleContactLink = savedInstanceState.getLong("handleContactLink", 0)
        }
        handler = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentScanCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (inviteShown) {
            outState.putBoolean("inviteShown", true)
            outState.putString("contactNameContent", contactNameContent)
            outState.putBoolean("isContact", isContact)
        }
        if (dialogshown) {
            outState.putBoolean("dialogshown", true)
            outState.putInt("dialogTitleContent", dialogTitleContent)
            outState.putInt("dialogTextContent", dialogTextContent)
        }
        if (dialogshown || inviteShown) {
            outState.putString("myEmail", myEmail)
            outState.putBoolean("success", success)
            outState.putBoolean(PRINT_EMAIL, printEmail)
            outState.putLong("handleContactLink", handleContactLink)
        }
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        codeScanner?.startPreview()
    }

    override fun onPause() {
        Timber.d("onPause")
        super.onPause()
        codeScanner?.releaseResources()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogInviteBinding = null
        _dialogAcceptContactBinding = null
    }

    private fun setupView() {
        val aB = (activity as AppCompatActivity).supportActionBar
        aB?.apply {
            title = requireContext().getFormattedStringOrDefault(R.string.section_qr_code)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        codeScanner = CodeScanner(requireContext(), binding.scannerView)
        codeScanner?.apply {
            setDecodeCallback { result ->
                if (!inviteShown && !dialogshown) {
                    activity?.runOnUiThread {
                        invite(result)
                    }
                }
            }

            setErrorCallback { error: Exception ->
                Timber.w("Start preview error:%s, retry:%d",
                    error.message,
                    mStartPreviewRetried + 1)

                if (mStartPreviewRetried++ < START_PREVIEW_RETRY) {
                    handler?.postDelayed({ codeScanner?.startPreview() },
                        START_PREVIEW_DELAY.toLong())
                } else {
                    Timber.e("Start preview failed")
                }
            }
        }

        binding.invalidCodeText.visibility = View.GONE
        binding.scannerView.setOnClickListener {
            codeScanner?.startPreview()
            if (binding.invalidCodeText.isVisible) {
                binding.invalidCodeText.visibility = View.GONE
            }
        }

        if (inviteShown) {
            showInviteDialog()
        } else if (dialogshown) {
            showAlertDialog(dialogTitleContent, dialogTextContent, success, printEmail)
        }
    }

    /**
     * Method to display an alert dialog just after scan QR code and send the contact invitation to
     * communicate the operation result to the user.
     *
     * @param title      String resource ID of the dialog title.
     * @param text       String resource ID of the dialog message.
     * @param success    Flag to indicate if the operation finished with success or not.
     * @param printEmail Flag to indicate if the dialog message includes contact email or not.
     */
    fun showAlertDialog(title: Int, text: Int, success: Boolean, printEmail: Boolean) {
        if (requestedAlertDialog == null) {
            _dialogInviteBinding = DialogInviteBinding.inflate(layoutInflater)

            requestedAlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogInviteBinding.root)
                .create().apply {
                    setOnDismissListener {
                        _dialogInviteBinding = null
                        requestedAlertDialog = null

                        if (success) {
                            dialogshown = false
                            codeScanner?.releaseResources()
                            activity?.finish()
                        } else {
                            codeScanner?.startPreview()
                        }
                    }
                }

            dialogInviteBinding.dialogInviteButton.setOnClickListener(this)
        }
        this.success = success
        this.printEmail = printEmail

        if (dialogTitleContent == -1) {
            dialogTitleContent = title
        }

        if (dialogTextContent == -1) {
            dialogTextContent = text
        }

        dialogInviteBinding.dialogInviteTitle.text =
            requireContext().getFormattedStringOrDefault(dialogTitleContent)
        if (printEmail) {
            dialogInviteBinding.dialogInviteText.text =
                requireContext().getFormattedStringOrDefault(dialogTextContent, myEmail)
        } else {
            dialogInviteBinding.dialogInviteText.text =
                requireContext().getFormattedStringOrDefault(dialogTextContent)
        }

        dialogshown = true
        requestedAlertDialog?.show()
    }

    private fun invite(rawResult: Result) {
        val contactLink = rawResult.text
        val s = contactLink.split("C!").toTypedArray()

        codeScanner?.startPreview()

        if (s.size <= 1) {
            binding.invalidCodeText.visibility = View.VISIBLE
        } else if (s[0] != "https://mega.nz/") {
            binding.invalidCodeText.visibility = View.VISIBLE
        } else {
            binding.invalidCodeText.visibility = View.GONE

            handle = MegaApiAndroid.base64ToHandle(s[1].trim { it <= ' ' })
            Timber.d("Contact link: %s s[1]: %s handle: %d", contactLink, s[1], handle)
            megaApi.contactLinkQuery(handle, activity as QRCodeActivity?)

            _dialogAcceptContactBinding = DialogAcceptContactBinding.inflate(layoutInflater)

            inviteAlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogAcceptContactBinding.root)
                .create().apply {
                    setOnDismissListener {
                        Timber.d("onDismiss")
                        _dialogAcceptContactBinding = null
                        inviteAlertDialog = null
                        inviteShown = false
                        codeScanner?.startPreview()
                    }
                }

            dialogAcceptContactBinding.acceptContactInvite.setOnClickListener(this)
            dialogAcceptContactBinding.viewContact.setOnClickListener(this)
        }
    }

    /**
     * Global click listener for all the views in the fragment
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.accept_contact_invite -> {
                inviteShown = false
                sendInvitation()
                if (inviteAlertDialog != null) {
                    inviteAlertDialog?.dismiss()
                }
            }
            R.id.dialog_invite_button -> {
                dialogshown = false
                codeScanner?.releaseResources()
                requestedAlertDialog?.dismiss()
                if (success) {
                    activity?.finish()
                } else {
                    codeScanner?.startPreview()
                }
            }
            R.id.view_contact -> {
                inviteShown = false
                codeScanner?.releaseResources()
                if (inviteAlertDialog != null) {
                    inviteAlertDialog?.dismiss()
                }
                ContactUtil.openContactInfoActivity(context, myEmail)
                activity?.finish()
            }
        }
    }

    private fun sendInvitation() {
        Timber.d("sendInvitation")
        megaApi.inviteContact(myEmail,
            null,
            MegaContactRequest.INVITE_ACTION_ADD,
            handleContactLink,
            activity as QRCodeActivity?)
    }

    /**
     * Method to set scanned contact avatar in the dialog
     */
    fun setAvatar() {
        Timber.d("updateAvatar")
        if (!isContact) {
            Timber.d("Is not Contact")
            setDefaultAvatar()
        } else {
            Timber.d("Is Contact")
            val avatar: File? = if (context != null) {
                Timber.d("Context is not null")
                buildAvatarFile(requireContext(), "$myEmail.jpg")
            } else {
                Timber.w("Context is null!!!")
                if (activity != null) {
                    Timber.d("getActivity is not null")
                    buildAvatarFile(requireActivity(), "$myEmail.jpg")
                } else {
                    Timber.w("getActivity is ALSO null")
                    return
                }
            }
            if (avatar != null) {
                setProfileAvatar(avatar)
            } else {
                setDefaultAvatar()
            }
        }
    }

    private fun setProfileAvatar(avatar: File) {
        Timber.d("setProfileAvatar")
        if (avatar.exists()) {
            Timber.d("Avatar path: ${avatar.absolutePath}")
            if (avatar.length() > 0) {
                Timber.d("My avatar exists!")
                val imBitmap =
                    BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())
                if (imBitmap == null) {
                    avatar.delete()
                    Timber.d("Call to getUserAvatar")
                    setDefaultAvatar()
                } else {
                    Timber.d("Show my avatar")
                    dialogAcceptContactBinding.acceptContactAvatar.setImageBitmap(imBitmap)
                    dialogAcceptContactBinding.acceptContactInitialLetter.visibility = View.GONE
                    contentAvatar = true
                }
            }
        } else {
            Timber.d("My avatar NOT exists!")
            Timber.d("Call to getUserAvatar")
            Timber.d("DO NOT Retry!")
            megaApi.getUserAvatar(myEmail, avatar.path, activity as QRCodeActivity?)
        }
    }

    /**
     * Method to set a default avatar
     */
    fun setDefaultAvatar() {
        Timber.d("setDefaultAvatar")
        val defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,
            DEFAULT_AVATAR_WIDTH_HEIGHT,
            Bitmap.Config.ARGB_8888)
        val c = Canvas(defaultAvatar)
        val p = Paint()
        p.isAntiAlias = true
        if (isContact && userQuery != null) {
            val color = megaApi.getUserAvatarColor(userQuery)
            if (color != null) {
                Timber.d("The color to set the avatar is %s", color)
                p.color = Color.parseColor(color)
            } else {
                Timber.d("Default color to the avatar")
                p.color = ContextCompat.getColor(requireContext(), R.color.red_600_red_300)
            }
        } else {
            p.color = ContextCompat.getColor(requireContext(), R.color.red_600_red_300)
        }
        val radius: Int =
            if (defaultAvatar.width < defaultAvatar.height) defaultAvatar.width / 2 else defaultAvatar.height / 2
        c.drawCircle((defaultAvatar.width / 2).toFloat(),
            (defaultAvatar.height / 2).toFloat(),
            radius.toFloat(),
            p)
        dialogAcceptContactBinding.acceptContactAvatar.setImageBitmap(defaultAvatar)
        val density = resources.displayMetrics.density
        val avatarTextSize = getAvatarTextSize(density)
        Timber.d("DENSITY: %s:::: %d", density, avatarTextSize)
        val fullName: String? = if (contactNameContent != null) {
            contactNameContent
        } else {
            //No name, ask for it and later refresh!!
            myEmail
        }
        if (fullName != null && fullName.isNotEmpty()) {
            var firstLetter = fullName[0].toString() + ""
            firstLetter = firstLetter.uppercase(Locale.getDefault())
            dialogAcceptContactBinding.acceptContactInitialLetter.text = firstLetter
            dialogAcceptContactBinding.acceptContactInitialLetter.textSize = 30f
            dialogAcceptContactBinding.acceptContactInitialLetter.setTextColor(Color.WHITE)
            dialogAcceptContactBinding.acceptContactInitialLetter.visibility = View.VISIBLE
            contentAvatar = false
        }
    }

    private fun getAvatarTextSize(density: Float): Int {
        val textSize: Float = if (density > 3.0) {
            density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f)
        } else if (density > 2.0) {
            density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f)
        } else if (density > 1.5) {
            density * (DisplayMetrics.DENSITY_XHIGH / 72.0f)
        } else if (density > 1.0) {
            density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f)
        } else if (density > 0.75) {
            density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f)
        } else {
            density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f)
        }
        return textSize.toInt()
    }

    private fun showInviteDialog() {
        if (inviteAlertDialog != null) {
            dialogAcceptContactBinding.acceptContactName.text = contactNameContent
            if (isContact) {
                dialogAcceptContactBinding.acceptContactMail.text =
                    resources.getString(R.string.context_contact_already_exists, myEmail)
                dialogAcceptContactBinding.acceptContactInvite.visibility = View.GONE
                dialogAcceptContactBinding.viewContact.visibility = View.VISIBLE
            } else {
                dialogAcceptContactBinding.acceptContactMail.text = myEmail
                dialogAcceptContactBinding.acceptContactInvite.visibility = View.VISIBLE
                dialogAcceptContactBinding.viewContact.visibility = View.GONE
            }
            setAvatar()
        } else {
            val builder = MaterialAlertDialogBuilder(requireContext())
            _dialogAcceptContactBinding = DialogAcceptContactBinding.inflate(layoutInflater)
            builder.setView(dialogAcceptContactBinding.root)
            dialogAcceptContactBinding.acceptContactInvite.setOnClickListener(this)
            dialogAcceptContactBinding.viewContact.setOnClickListener(this)
            if (avatarSave != null) {
                dialogAcceptContactBinding.acceptContactAvatar.setImageBitmap(avatarSave)
                if (contentAvatar) {
                    dialogAcceptContactBinding.acceptContactInitialLetter.visibility = View.GONE
                } else {
                    if (initialLetterSave != null) {
                        dialogAcceptContactBinding.acceptContactInitialLetter.text =
                            initialLetterSave
                        dialogAcceptContactBinding.acceptContactInitialLetter.textSize = 30f
                        dialogAcceptContactBinding.acceptContactInitialLetter.setTextColor(Color.WHITE)
                        dialogAcceptContactBinding.acceptContactInitialLetter.isVisible = true
                    } else {
                        setAvatar()
                    }
                }
            } else {
                setAvatar()
            }
            if (isContact) {
                dialogAcceptContactBinding.acceptContactMail.text =
                    resources.getString(R.string.context_contact_already_exists, myEmail)
                dialogAcceptContactBinding.acceptContactInvite.visibility = View.GONE
                dialogAcceptContactBinding.viewContact.visibility = View.VISIBLE
            } else {
                dialogAcceptContactBinding.acceptContactMail.text = myEmail
                dialogAcceptContactBinding.acceptContactInvite.visibility = View.VISIBLE
                dialogAcceptContactBinding.viewContact.visibility = View.GONE
            }
            inviteAlertDialog = builder.create()
            inviteAlertDialog?.setOnDismissListener {
                Timber.d("onDismiss")
                _dialogAcceptContactBinding = null
                inviteAlertDialog = null
                inviteShown = false
                codeScanner?.startPreview()
            }
            dialogAcceptContactBinding.acceptContactName.text = contactNameContent
        }
        inviteAlertDialog?.show()
        inviteShown = true
    }

    private fun queryIfIsContact(): MegaUser? {
        val contacts = megaApi.contacts
        for (i in contacts.indices) {
            if (contacts[i].visibility == MegaUser.VISIBILITY_VISIBLE) {
                Timber.d("Contact mail[i]=%d:%s contact mail request: %s",
                    i,
                    contacts[i].email,
                    myEmail)
                if (contacts[i].email == myEmail) {
                    isContact = true
                    return contacts[i]
                }
            }
        }
        isContact = false
        return null
    }

    /**
     * Show appropriate dialog based on request type
     *
     * @param request   object containing information of the sdk request
     * @param e         error object of the sdk request
     */
    fun initDialogInvite(request: MegaRequest, e: MegaError) {
        when (e.errorCode) {
            MegaError.API_OK -> {
                Timber.d("Contact link query %d_%s_%s_%s_%s",
                    request.nodeHandle,
                    MegaApiAndroid.handleToBase64(request.nodeHandle),
                    request.email,
                    request.name,
                    request.text)
                handleContactLink = request.nodeHandle
                contactNameContent = request.name + " " + request.text
                myEmail = request.email
                userQuery = queryIfIsContact()
                showInviteDialog()
            }
            MegaError.API_EEXIST -> {
                dialogTitleContent = R.string.invite_not_sent
                dialogTextContent = R.string.invite_not_sent_text_already_contact
                showAlertDialog(dialogTitleContent,
                    dialogTextContent,
                    success = true,
                    printEmail = true)
            }
            else -> {
                dialogTitleContent = R.string.invite_not_sent
                dialogTextContent = R.string.invite_not_sent_text
                showAlertDialog(dialogTitleContent,
                    dialogTextContent,
                    success = false,
                    printEmail = false)
            }
        }
    }

    companion object {
        private var DEFAULT_AVATAR_WIDTH_HEIGHT = 150
        private var PRINT_EMAIL = "PRINT_EMAIL"

        // Bug #14988: disableLocalCamera() may hasn't completely released the camera resource as
        // the megaChatApi.disableVideo() is async call. A simply way to solve the issue is
        // setErrorCallback for CodeScanner. If error occurs, retry in 300ms. Retry 5 times max.
        private const val START_PREVIEW_RETRY = 5
        private const val START_PREVIEW_DELAY = 300

        /**
         * New instance of ScanCodeFragment
         */
        @JvmStatic
        fun newInstance(): ScanCodeFragment {
            Timber.d("newInstance")
            return ScanCodeFragment()
        }
    }
}