package mega.privacy.android.app.main.adapters

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.GetUserEmailListener
import mega.privacy.android.app.listeners.GetUserEmailListener.OnUserEmailUpdateCallback
import mega.privacy.android.app.main.adapters.MegaNotificationsAdapter.ViewHolderNotifications
import mega.privacy.android.app.main.managerSections.NotificationsFragment
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.util.Locale

class MegaNotificationsAdapter(
    private val context: Context,
    private val fragment: NotificationsFragment?,
    private var notifications: List<MegaUserAlert>,
    private val recyclerView: RecyclerView,
    private val megaApi: MegaApiAndroid,
    private val outMetrics: DisplayMetrics,
) : RecyclerView.Adapter<ViewHolderNotifications>(), View.OnClickListener,
    OnUserEmailUpdateCallback {
    private var positionClicked: Int = -1

    /*private view holder class*/
    class ViewHolderNotifications(v: View) : RecyclerView.ViewHolder(v) {
        val itemLayout: LinearLayout = v.findViewById(R.id.notification_list_item_layout)
        val sectionIcon: ImageView = v.findViewById(R.id.notification_title_icon)
        val sectionText: TextView = v.findViewById(R.id.notification_title_text)
        val titleIcon: ImageView = v.findViewById(R.id.notification_first_line_icon)
        val titleText: TextView = v.findViewById(R.id.notification_first_line_text)
        val newText: TextView = v.findViewById(R.id.notification_new_label)
        val descriptionText: TextView = v.findViewById(R.id.notifications_text)
        val dateText: TextView = v.findViewById(R.id.notifications_date)
        val separator: LinearLayout = v.findViewById(R.id.notifications_separator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderNotifications {
        Timber.d("onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_list, parent, false)
        val holder = ViewHolderNotifications(v)
        holder.itemLayout.tag = holder
        holder.itemLayout.setOnClickListener(this)
        v.tag = holder
        return holder
    }
    

    /**
     * Method for getting a user's email from MegaUserAlert.
     *
     * @param pos    position in adapter
     * @param holder ViewHolderNotifications
     * @param alert  MegaUserAlert
     * @return The email
     */
    private fun getEmail(pos: Int, alert: MegaUserAlert): String? {
        val email = alert.email
        if (email != null) {
            return email
        }
        val nonContact =
            MegaApplication.getInstance().dbH.findNonContactByHandle(alert.userHandle.toString() + "")
        if (nonContact != null && nonContact.email != null) {
            return nonContact.email
        }
        megaApi.getUserEmail(alert.userHandle, GetUserEmailListener(context, this, pos))
        return context.getString(R.string.unknown_name_label)
    }

    /**
     * Method to correctly show the title or description text of a notification when the user's email needs to be displayed.
     *
     * @param position position in adapter
     * @param holder   ViewHolderNotifications
     * @param alert    MegaUserAlert
     * @param email    Email of a user
     */
    private fun setTitleOrDescriptionTextWithEmail(
        holder: ViewHolderNotifications,
        alert: MegaUserAlert,
        email: String?,
    ) {
        val textToShow: String
        when (alert.type) {
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.notification_new_contact_request,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_contact_request_notification_cancelled,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED -> {
                textToShow = StringResourcesUtils.getString(R.string.notification_new_contact,
                    ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_account_notification_deleted,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.notification_reminder_contact_request,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_contact_notification_deleted,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_contact_notification_blocked,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_outgoing_contact_request_accepted,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_outgoing_contact_request_denied,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_incoming_contact_request_ignored,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_incoming_contact_request_accepted,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED -> {
                textToShow =
                    StringResourcesUtils.getString(R.string.subtitle_incoming_contact_request_denied,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.descriptionText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_NEWSHARE -> {
                textToShow = StringResourcesUtils.getString(R.string.notification_new_shared_folder,
                    ContactUtil.getNicknameForNotificationsSection(context, email))
                holder.titleText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_DELETEDSHARE -> {
                textToShow = if (alert.getNumber(0) == 0L) {
                    val node = megaApi.getNodeByHandle(alert.nodeHandle)
                    if (node != null) {
                        StringResourcesUtils.getString(R.string.notification_left_shared_folder_with_name,
                            ContactUtil.getNicknameForNotificationsSection(context, email),
                            node.name)
                    } else {
                        StringResourcesUtils.getString(R.string.notification_left_shared_folder,
                            ContactUtil.getNicknameForNotificationsSection(context, email))
                    }
                } else {
                    StringResourcesUtils.getString(R.string.notification_deleted_shared_folder,
                        ContactUtil.getNicknameForNotificationsSection(context, email))
                }
                holder.titleText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_NEWSHAREDNODES -> {
                val numFiles = alert.getNumber(1).toInt()
                val numFolders = alert.getNumber(0).toInt()
                textToShow = if (numFolders > 0 && numFiles > 0) {
                    val numFilesString =
                        StringResourcesUtils.getQuantityString(R.plurals.num_files_with_parameter,
                            numFiles,
                            numFiles)
                    val numFoldersString =
                        StringResourcesUtils.getQuantityString(R.plurals.num_folders_with_parameter,
                            numFolders,
                            numFolders)
                    StringResourcesUtils.getString(R.string.subtitle_notification_added_folders_and_files,
                        ContactUtil.getNicknameForNotificationsSection(context, email),
                        numFoldersString,
                        numFilesString)
                } else if (numFolders > 0) {
                    StringResourcesUtils.getQuantityString(R.plurals.subtitle_notification_added_folders,
                        numFolders,
                        ContactUtil.getNicknameForNotificationsSection(context, email),
                        numFolders)
                } else {
                    StringResourcesUtils.getQuantityString(R.plurals.subtitle_notification_added_files,
                        numFiles,
                        ContactUtil.getNicknameForNotificationsSection(context, email),
                        numFiles)
                }
                holder.titleText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES -> {
                val itemCount = alert.getNumber(0).toInt()
                textToShow =
                    StringResourcesUtils.getQuantityString(R.plurals.subtitle_notification_deleted_items,
                        itemCount,
                        ContactUtil.getNicknameForNotificationsSection(context, email),
                        itemCount)
                holder.titleText.text =
                    TextUtil.replaceFormatNotificationText(context, textToShow)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolderNotifications, position: Int) {
        Timber.d("Position: %s", position)
        val alert = getItem(position) as MegaUserAlert
        val alertType = alert.type
        var section = alert.heading
        val params: LinearLayout.LayoutParams
        when (alertType) {
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST, MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {

                //New contact request
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_contact_request_notification)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED -> {

                //Contact request cancelled
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_contact_request_notification_cancelled)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED -> {

                //New contact
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_acceptance_contact_request_notification)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED -> {

                //Account deleted android100@yopmail.com
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_account_notification_deleted)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            } //Contact request reminder
            MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU -> {

                //Contact deleted you
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_contact_notification_deleted)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            }
            MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {

                //Contact blocked you
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text =
                    context.getString(R.string.title_contact_notification_blocked)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {

                //Outgoing contact request accepted by the other user
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text = context.getString(R.string.title_outgoing_contact_request)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED -> {

                //Outgoing contact request denied by the other user
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text = context.getString(R.string.title_outgoing_contact_request)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED, MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED -> {

                //Incoming contact request ignored by me
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text = context.getString(R.string.title_incoming_contact_request)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(null)
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED -> {

                //Incoming contact request accepted by me
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section =
                    context.getString(R.string.section_contacts).uppercase(Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.jade_600_jade_300))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                holder.titleText.text = context.getString(R.string.title_incoming_contact_request)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.VISIBLE

                //Description set to max, adjust title
                holder.titleText.maxLines = 1
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    holder.titleText.maxWidth =
                        Util.scaleWidthPx(MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                } else {
                    holder.descriptionText.maxWidth = Util.scaleWidthPx(
                        MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    if (!alert.seen) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            } //Incoming contact request denied by me
            MegaUserAlert.TYPE_NEWSHARE -> {

                //New shared folder
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = context.getString(R.string.title_incoming_shares_explorer).uppercase(
                    Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.orange_400_orange_300))
                holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context,
                    R.drawable.ic_y_arrow_in))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }

                //Allow navigation to the folder
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    holder.itemLayout.setOnClickListener(this)
                }
            }
            MegaUserAlert.TYPE_DELETEDSHARE -> {

                //Removed shared folder
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = context.getString(R.string.title_incoming_shares_explorer).uppercase(
                    Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.orange_400_orange_300))
                holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context,
                    R.drawable.ic_y_arrow_in))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))

                //TYPE_DELETEDSHARE (0: value 1 if access for this user was removed by the share owner, otherwise
                //value 0 if someone left the folder)
                if (alert.getNumber(0) == 0L) {
                    val node = megaApi.getNodeByHandle(alert.nodeHandle)
                    holder.itemLayout.setOnClickListener(if (node != null) this else null)
                }
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
            }
            MegaUserAlert.TYPE_NEWSHAREDNODES -> {

                //New files added to a shared folder
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = context.getString(R.string.title_incoming_shares_explorer).uppercase(
                    Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.orange_400_orange_300))
                holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context,
                    R.drawable.ic_y_arrow_in))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }

                //Allow navigation to the folder
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    holder.itemLayout.setOnClickListener(this)
                }
            }
            MegaUserAlert.TYPE_REMOVEDSHAREDNODES -> {

                //New files added to a shared folder
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = context.getString(R.string.title_incoming_shares_explorer).uppercase(
                    Locale.getDefault())
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.orange_400_orange_300))
                holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context,
                    R.drawable.ic_y_arrow_in))
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                setTitleOrDescriptionTextWithEmail(
                    holder,
                    alert,
                    getEmail(position, alert))
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }

                //Allow navigation to the folder
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    holder.itemLayout.setOnClickListener(this)
                }
            }
            MegaUserAlert.TYPE_PAYMENT_SUCCEEDED -> {
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = alert.heading.uppercase(Locale.getDefault())
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.red_600_red_300))
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                holder.titleText.text = alert.title
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_PAYMENT_FAILED -> {
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = alert.heading.uppercase(Locale.getDefault())
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.red_600_red_300))
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                holder.titleText.text = alert.title
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_PAYMENTREMINDER -> {
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = alert.heading
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.red_600_red_300))
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                holder.titleText.text = alert.title
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }
                holder.itemLayout.setOnClickListener(this)
            }
            MegaUserAlert.TYPE_TAKEDOWN -> {

                //Link takedown android100
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                section = section.uppercase(Locale.getDefault())
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.red_600_red_300))
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                val name = alert.name
                val path = alert.path
                var textToShow: String
                textToShow = if (path != null && FileUtil.isFile(path)) {
                    String.format(context.getString(R.string.subtitle_file_takedown_notification),
                        Util.toCDATA(name))
                } else {
                    String.format(context.getString(R.string.subtitle_folder_takedown_notification),
                        Util.toCDATA(name))
                }
                try {
                    textToShow = textToShow.replace("[A]", "<font color='"
                            + getColorHexString(context, R.color.grey_900_grey_100)
                            + "'>")
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace("[B]", "<font color='"
                            + getColorHexString(context, R.color.grey_500_grey_400)
                            + "'>")
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                holder.titleText.text = result
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }

                //Allow navigation to the folder
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    holder.itemLayout.setOnClickListener(this)
                }
            }
            MegaUserAlert.TYPE_TAKEDOWN_REINSTATED -> {

                //Link takedown reinstated android100
                section = section.uppercase(Locale.getDefault())
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.VISIBLE
                holder.sectionIcon.visibility = View.GONE
                holder.titleIcon.visibility = View.GONE
                holder.sectionText.setTextColor(ContextCompat.getColor(context,
                    R.color.red_600_red_300))
                holder.titleText.visibility = View.VISIBLE
                holder.titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                val name = alert.name
                val path = alert.path
                var textToShow: String
                textToShow = if (path != null && FileUtil.isFile(path)) {
                    String.format(context.getString(R.string.subtitle_file_takedown_reinstated_notification),
                        Util.toCDATA(name))
                } else {
                    String.format(context.getString(R.string.subtitle_folder_takedown_reinstated_notification),
                        Util.toCDATA(name))
                }
                try {
                    textToShow = textToShow.replace("[A]", "<font color='"
                            + getColorHexString(context, R.color.grey_900_grey_100)
                            + "'>")
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace("[B]", "<font color='"
                            + getColorHexString(context, R.color.grey_500_grey_400)
                            + "'>")
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                    Timber.e(e)
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                holder.titleText.text = result
                holder.descriptionText.visibility = View.GONE

                //Description not shown, adjust title
                holder.titleText.maxLines = 3
                if (!alert.seen) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_NEW_PORT, outMetrics)
                    }
                } else {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_LAND, outMetrics)
                    } else {
                        holder.titleText.maxWidth = Util.scaleWidthPx(
                            MAX_WIDTH_FIRST_LINE_SEEN_PORT, outMetrics)
                    }
                }

                //Allow navigation to the folder
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    holder.itemLayout.setOnClickListener(this)
                }
            }
            else -> {

                //Hide
                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                params.height = 0
                holder.itemLayout.layoutParams = params
                holder.itemLayout.visibility = View.GONE
                holder.itemLayout.setOnClickListener(null)
            }
        }
        holder.sectionText.text = section
        val date =
            TimeUtils.formatDateAndTime(context, alert.getTimestamp(0), TimeUtils.DATE_LONG_FORMAT)
        holder.dateText.text = date
        if (!alert.seen) {
            holder.newText.visibility = View.VISIBLE
            holder.itemLayout.setBackgroundColor(getThemeColor(context,
                android.R.attr.colorBackground))
            if (position < notifications.size - 1) {
                val nextAlert = getItem(position + 1) as MegaUserAlert
                if (!nextAlert.seen) {
                    val textParams = holder.separator.layoutParams as LinearLayout.LayoutParams
                    textParams.setMargins(Util.scaleWidthPx(16, outMetrics),
                        0,
                        Util.scaleWidthPx(16, outMetrics),
                        0)
                    holder.separator.layoutParams = textParams
                } else {
                    val textParams = holder.separator.layoutParams as LinearLayout.LayoutParams
                    textParams.setMargins(0, 0, 0, 0)
                    holder.separator.layoutParams = textParams
                }
            } else {
                Timber.d("Last element of the notifications")
                val textParams = holder.separator.layoutParams as LinearLayout.LayoutParams
                textParams.setMargins(0, 0, 0, 0)
                holder.separator.layoutParams = textParams
            }
        } else {
            holder.newText.visibility = View.GONE
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context,
                R.color.grey_020_grey_800))
            val textParams = holder.separator.layoutParams as LinearLayout.LayoutParams
            textParams.setMargins(Util.scaleWidthPx(16, outMetrics),
                0,
                Util.scaleWidthPx(16, outMetrics),
                0)
            holder.separator.layoutParams = textParams
        }


//		holder.imageButtonThreeDots.setTag(holder);
//		holder.imageButtonThreeDots.setOnClickListener(this);
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun getItem(position: Int): Any {
        Timber.d("Position: %s", position)
        return notifications[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getPositionClicked(): Int {
        return positionClicked
    }

    fun setPositionClicked(p: Int) {
        Timber.d("Position: %s", p)
        positionClicked = p
        notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        val holder = v.tag as ViewHolderNotifications
        val currentPosition = holder.absoluteAdapterPosition
        try {
            when (v.id) {
                R.id.notification_list_item_layout -> {
                    Timber.d("notification_list_item_layout")
                    fragment?.itemClick(currentPosition)
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Timber.e(e)
        }
    }

    fun setNotifications(notifications: List<MegaUserAlert>) {
        Timber.d("setNotifications")
        this.notifications = notifications
        positionClicked = -1
        notifyDataSetChanged()
    }

    override fun onUserEmailUpdate(email: String?, handler: Long, position: Int) {
        val view =
            recyclerView.findViewHolderForLayoutPosition(position) as ViewHolderNotifications?
        if (view != null) {
            val alert = getItem(position) as MegaUserAlert
            if (handler != alert.userHandle) {
                return
            }
            setTitleOrDescriptionTextWithEmail( view, alert, email)
        }
    }

    companion object {
        var MAX_WIDTH_FIRST_LINE_NEW_LAND = 306
        var MAX_WIDTH_FIRST_LINE_SEEN_LAND = 336
        var MAX_WIDTH_FIRST_LINE_NEW_PORT = 276
        var MAX_WIDTH_FIRST_LINE_SEEN_PORT = 328
    }

}