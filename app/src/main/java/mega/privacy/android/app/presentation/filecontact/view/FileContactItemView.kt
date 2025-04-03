package mega.privacy.android.app.presentation.filecontact.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.dispose
import coil.load
import coil.transform.CircleCropTransformation
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient

/**
 * File contact item view
 *
 * @param context
 * @param attrs
 * @param defStyleAttr
 */
class FileContactItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val avatarLayout: RelativeLayout
    private val thumbnail: RoundedImageView
    private val verifiedIcon: ImageView
    private val threeDotsLayout: RelativeLayout
    private val nameText: EmojiTextView
    private val permissionsText: TextView
    private val stateIcon: ImageView
    private var imageLoader: RoundedImageView.() -> Unit = { }
    private var _isSelected: Boolean = false

    /**
     * Initialize the view
     */
    init {
        LayoutInflater.from(context).inflate(R.layout.item_shared_folder, this, true)

        avatarLayout = findViewById(R.id.shared_folder_contact_relative_layout_avatar)
        thumbnail = findViewById(R.id.shared_folder_contact_thumbnail)
        verifiedIcon = findViewById(R.id.verified_icon)
        threeDotsLayout = findViewById(R.id.shared_folder_three_dots_layout)
        nameText = findViewById(R.id.shared_folder_contact_name)
        permissionsText = findViewById(R.id.shared_folder_contact_permissions)
        stateIcon = findViewById(R.id.shared_folder_state_icon)
        nameText.setTypeEllipsize(TextUtils.TruncateAt.MIDDLE)
    }

    /**
     * Bind data
     *
     * @param recipient
     * @param isDarkMode
     * @param onClick
     * @param onLongClick
     * @param onOptionsClick
     * @param isSelected
     */
    fun bindData(
        recipient: ShareRecipient,
        isDarkMode: Boolean,
        onClick: (ShareRecipient) -> Unit,
        onLongClick: (ShareRecipient) -> Unit,
        onOptionsClick: (ShareRecipient) -> Unit,
        isSelected: Boolean,
    ) {
        _isSelected = isSelected
        setOnClickListener { onClick(recipient) }
        setOnLongClickListener {
            onLongClick(recipient)
            true
        }
        threeDotsLayout.setOnClickListener { onOptionsClick(recipient) }

        when (recipient) {
            is ShareRecipient.Contact -> {
                verifiedIcon.visibility = if (recipient.isVerified) {
                    VISIBLE
                } else {
                    GONE
                }
                nameText.text = recipient.contactData.alias ?: recipient.contactData.fullName
                stateIcon.setImageResource(userChatStatusIconMapper(recipient.status, isDarkMode))
                imageLoader = {
                    load(recipient.contactData.avatarUri) {
                        transformations(CircleCropTransformation())
                        placeholder(getImagePlaceholder(recipient, context))
                        listener(onError = { _, _ ->
                            thumbnail.setImageDrawable(
                                getImagePlaceholder(recipient, context)
                            )
                        }
                        )
                    }
                }

                if (_isSelected) {
                    thumbnail.setImageResource(R.drawable.ic_chat_avatar_select);
                } else {
                    thumbnail.imageLoader()
                }
            }

            is ShareRecipient.NonContact -> {
                verifiedIcon.visibility = GONE
                nameText.text = recipient.email
                stateIcon.visibility = GONE
                imageLoader = { load(getImagePlaceholder(recipient, context)) }
                if (_isSelected) {
                    thumbnail.setImageResource(R.drawable.ic_chat_avatar_select);
                } else {
                    thumbnail.imageLoader()
                }
            }
        }

        when (recipient.permission) {
            AccessPermission.OWNER,
            AccessPermission.FULL,
                -> permissionsText.text =
                context.getString(R.string.file_properties_shared_folder_full_access)

            AccessPermission.READ -> permissionsText.text =
                context.getString(R.string.file_properties_shared_folder_read_only)

            AccessPermission.READWRITE -> permissionsText.text =
                context.getString(R.string.file_properties_shared_folder_read_write)

            AccessPermission.UNKNOWN -> permissionsText.text = ""
        }

        if (recipient.isPending) {
            permissionsText.append(" " + context.getString(R.string.pending_outshare_indicator))
        }
    }

    /**
     * Clear fields
     */
    fun clear() {
        thumbnail.dispose()
        verifiedIcon.visibility = GONE
        nameText.text = null
        permissionsText.text = null
        stateIcon.visibility = GONE
        imageLoader = { }
    }

    /**
     * Set the item as selected
     */
    fun setItemSelected(isSelected: Boolean) {
        if (_isSelected == isSelected) return
        _isSelected = isSelected

        val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
        flipAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                if (_isSelected) {
                    thumbnail.setImageResource(R.drawable.ic_chat_avatar_select)
                } else {
                    thumbnail.imageLoader()
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        }
        )
        thumbnail.startAnimation(flipAnimation)
    }


    private fun userChatStatusIconMapper(status: UserChatStatus, isDarkTheme: Boolean): Int =
        when (status) {
            UserChatStatus.Offline -> if (isDarkTheme) R.drawable.ic_offline_dark_drawer else R.drawable.ic_offline_light
            UserChatStatus.Away -> if (isDarkTheme) R.drawable.ic_away_dark_drawer else R.drawable.ic_away_light
            UserChatStatus.Online -> if (isDarkTheme) R.drawable.ic_online_dark_drawer else R.drawable.ic_online_light
            UserChatStatus.Busy -> if (isDarkTheme) R.drawable.ic_busy_dark_drawer else R.drawable.ic_busy_light
            UserChatStatus.Invalid -> 0
        }

    private fun getImagePlaceholder(recipient: ShareRecipient, context: Context): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .height(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
            .textColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(
                AvatarUtil.getFirstLetter(recipient.getAvatarFirstLetter()),
                (recipient as? ShareRecipient.Contact)?.defaultAvatarColor
                    ?: ContextCompat.getColor(
                        context,
                        R.color.red_600_red_300
                    ),
            )
}
