package mega.privacy.android.app.activities.editProfile

import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.AppBarStateChangeListener
import mega.privacy.android.app.databinding.ActivityEditProfileBinding
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDominantColor
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_APPBAR_LAND
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_APPBAR_PORT
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaChatApi
import java.util.*

class EditProfileActivity : PasscodeActivity() {

    companion object {
        private const val PADDING_BOTTOM_APP_BAR = 19F
        private const val PADDING_COLLAPSED_BOTTOM_APP_BAR = 9F
        private const val NAME_SIZE = 16F
        private const val EMAIL_SIZE = 12F
        private const val PADDING_LEFT_STATE = 8F
    }

    private val viewModel by viewModels<EditProfileViewModel>()

    private lateinit var binding: ActivityEditProfileBinding

    private var maxWidth = 0
    private var firstLineTextMaxWidthExpanded = 0
    private var stateToolbar = AppBarStateChangeListener.State.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setUpView() {
        setUpActionBar()
        setUpAvatar()
        setUpHeader()
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding.headerLayout.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null

//        val params = binding.headerLayout.appBar.layoutParams as CoordinatorLayout.LayoutParams
//        val behaviour = params.behavior
//
//        behaviour?.onNestedPreScroll(
//            binding.containerLayout,
//            binding.headerLayout.appBar,
//            binding.headerLayout.appBar,
//            0,
//            50,
//            intArrayOf(0, 0),
//            TYPE_TOUCH
//        )

        var drawableArrow = ContextCompat.getDrawable(
            applicationContext,
            R.drawable.ic_arrow_back_white
        )

        val contactStateIcon =
            if (Util.isDarkMode(this)) R.drawable.ic_offline_dark_standard
            else R.drawable.ic_offline_light

        drawableArrow = drawableArrow?.mutate()

        val statusBarColor =
            getColorForElevation(this, resources.getDimension(R.dimen.toolbar_elevation))

        binding.headerLayout.collapseToolbar.apply {
            if (Util.isDarkMode(this@EditProfileActivity)) {
                setContentScrimColor(statusBarColor)
            }

            setStatusBarScrimColor(statusBarColor)
        }

        val white = ContextCompat.getColor(this, R.color.white_alpha_087)
        val black = ContextCompat.getColor(this, R.color.grey_087_white_087)

        binding.headerLayout.appBar.addOnOffsetChangedListener(object :
            AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                stateToolbar = state

                if (state == State.EXPANDED || state == State.COLLAPSED) {
                    val color: Int = if (state == State.EXPANDED) white else black

                    binding.headerLayout.firstLineToolbar.setTextColor(color)
                    binding.headerLayout.secondLineToolbar.setTextColor(color)

                    drawableArrow?.colorFilter =
                        PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

                    supportActionBar?.setHomeAsUpIndicator(drawableArrow)
                    visibilityStateIcon()
                }

                binding.headerLayout.secondLineToolbar.setPadding(0, 0, 0,
                    dp2px(if (state == State.COLLAPSED) PADDING_COLLAPSED_BOTTOM_APP_BAR else PADDING_BOTTOM_APP_BAR)
                )
            }
        })
    }

    private fun visibilityStateIcon() {
        val status = megaChatApi.onlineStatus
        val showStatus = stateToolbar == AppBarStateChangeListener.State.EXPANDED
                && (status == MegaChatApi.STATUS_ONLINE || status == MegaChatApi.STATUS_AWAY
                || status == MegaChatApi.STATUS_BUSY || status == MegaChatApi.STATUS_OFFLINE)

        binding.headerLayout.firstLineToolbar.apply {
            maxLines =
                if (showStatus || stateToolbar == AppBarStateChangeListener.State.EXPANDED) 2
                else 1

            if (showStatus) {
                setTrailingIcon(
                    ChatUtil.getIconResourceIdByLocation(
                        this@EditProfileActivity,
                        status,
                        StatusIconLocation.STANDARD
                    ), dp2px(PADDING_LEFT_STATE)
                )
            }

            updateMaxWidthAndIconVisibility(
                if (stateToolbar == AppBarStateChangeListener.State.EXPANDED) firstLineTextMaxWidthExpanded else maxWidth,
                showStatus
            )
        }
    }

    private fun setUpAvatar() {
        val avatar = buildAvatarFile(this, megaApi.myEmail + FileUtil.JPG_EXTENSION)
        var avatarBitmap: Bitmap? = null

        if (isFileAvailable(avatar)) {
            avatarBitmap = BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())

            if (avatarBitmap != null) {
                binding.headerLayout.toolbarImage.setImageBitmap(avatarBitmap)

                if (!avatarBitmap.isRecycled) {
                    binding.headerLayout.imageLayout.setBackgroundColor(
                        getDominantColor(
                            avatarBitmap
                        )
                    )
                }
            }
        }

        if (avatarBitmap == null) {
            binding.headerLayout.imageLayout.setBackgroundColor(getColorAvatar(megaApi.myUser))
        }
    }

    private fun setUpHeader() {

        firstLineTextMaxWidthExpanded = outMetrics.widthPixels - dp2px(108f, outMetrics)
        binding.headerLayout.firstLineToolbar.apply {
            setMaxWidthEmojis(firstLineTextMaxWidthExpanded)
            text = viewModel.getName()?.toUpperCase(Locale.getDefault())
            textSize = NAME_SIZE
        }

        maxWidth = dp2px(
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) MAX_WIDTH_APPBAR_PORT
            else MAX_WIDTH_APPBAR_LAND,
            outMetrics
        )

        binding.headerLayout.secondLineToolbar.apply {
            maxWidth = maxWidth
            text = viewModel.getEmail()
            textSize = EMAIL_SIZE
        }
    }
}