@file:Suppress("DEPRECATION")

package mega.privacy.android.app.main.managerSections

import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaNotificationsAdapter
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ContactUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsFragment : Fragment(), View.OnClickListener {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    var adapterList: MegaNotificationsAdapter? = null
    var mainRelativeLayout: RelativeLayout? = null
    var listView: RecyclerView? = null
    var mLayoutManager: LinearLayoutManager? = null
    val notifications = mutableListOf<MegaUserAlert>()
    var numberOfClicks = 0

    //Empty screen
    var emptyTextView: TextView? = null
    var emptyLayout: RelativeLayout? = null
    var emptyImageView: ImageView? = null
    var density = 0f
    val outMetrics: DisplayMetrics by lazy {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        metrics
    }

    fun checkScroll() {
        if (listView?.canScrollVertically(-1) == true) {
            (activity as ManagerActivity?)?.changeAppBarElevation(true)
        } else {
            (activity as ManagerActivity?)?.changeAppBarElevation(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")
        density = resources.displayMetrics.density
        val v = inflater.inflate(R.layout.notifications_fragment, container, false)
        listView = v.findViewById<View>(R.id.notifications_list_view) as RecyclerView
        listView?.clipToPadding = false
        mLayoutManager = LinearLayoutManager(context)
        listView?.setHasFixedSize(true)
        listView?.itemAnimator = DefaultItemAnimator()
        listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (context is ManagerActivity) {
                    checkScroll()
                }
            }
        })
        listView?.layoutManager = mLayoutManager
        emptyLayout = v.findViewById<View>(R.id.empty_layout_notifications) as RelativeLayout
        emptyTextView = v.findViewById<View>(R.id.empty_text_notifications) as TextView
        emptyImageView = v.findViewById<View>(R.id.empty_image_view_notifications) as ImageView
        mainRelativeLayout =
            v.findViewById<View>(R.id.main_relative_layout_notifications) as RelativeLayout
        setNotifications()
        return v
    }

    fun setNotifications() {
        Timber.d("setNotifications")
        notifications.clear()
        notifications.addAll(megaApi.userAlerts.reversed())
        if (isAdded) {
            if (adapterList == null) {
                Timber.w("adapterList is NULL")
                val localListView = listView
                if (context != null && localListView != null) {
                    adapterList = MegaNotificationsAdapter(
                        context = requireContext(),
                        fragment = this,
                        notifications = notifications,
                        recyclerView = localListView,
                        megaApi = megaApi,
                        outMetrics
                    )
                }

            } else {
                adapterList?.setNotifications(notifications)
            }
            listView?.adapter = adapterList
            if (notifications.isEmpty()) {
                listView?.visibility = View.GONE
                emptyImageView?.visibility = View.VISIBLE
                emptyTextView?.visibility = View.VISIBLE
                if (context?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView?.setImageResource(R.drawable.empty_notification_landscape)
                } else {
                    emptyImageView?.setImageResource(R.drawable.empty_notification_portrait)
                }
                var textToShow = String.format(getString(R.string.context_empty_notifications))

                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                            + "\'>")
                    textToShow = textToShow.replace("[/A]", "</font>")
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                            + "\'>")
                    textToShow = textToShow.replace("[/B]", "</font>")
                } catch (e: Exception) {
                }
                val result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
                emptyTextView?.text = result
            } else {
                Timber.d("Number of notifications: %s", notifications.size)
                listView?.visibility = View.VISIBLE
                emptyLayout?.visibility = View.GONE
            }
            (activity as ManagerActivity?)?.markNotificationsSeen(false)
        }
    }

    fun addNotification(newAlert: MegaUserAlert) {
        Timber.d("addNotification")
        //Check scroll position
        var shouldScroll = false
        if (listView?.canScrollVertically(-1) != true) {
            shouldScroll = true
        }
        notifications.add(0, newAlert)
        adapterList?.notifyItemInserted(0)

        //Before scrolling be sure it was on the first
        if (shouldScroll) {
            listView?.smoothScrollToPosition(0)
        }
        (activity as ManagerActivity?)?.markNotificationsSeen(false)
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        when (v.id) {
            R.id.empty_image_view_chat -> {
                numberOfClicks++
                Timber.d("Number of clicks: %s", numberOfClicks)
                if (numberOfClicks >= 5) {
                    numberOfClicks = 0
                }
            }
        }
    }

    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)
        val alert = notifications[position]
        when (alert.type) {
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST, MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED, MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED, MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER -> {
                val contact = megaApi.getContact(alert.email)
                if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                    Timber.d("Go to contact info")
                    ContactUtil.openContactInfoActivity(context, alert.email)
                } else {
                    val contacts = megaApi.incomingContactRequests
                    if (contacts != null) {
                        var i = 0
                        while (i < contacts.size) {
                            val c = contacts[i]
                            if (c.sourceEmail == alert.email) {
                                Timber.d("Go to Received requests")
                                (activity as ManagerActivity?)?.navigateToContactRequests()
                                break
                            }
                            i++
                        }
                    }
                }
                Timber.w("Request not found")
            }
            MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED -> {
                val contact = megaApi.getContact(alert.email)
                if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                    Timber.d("Go to contact info")
                    ContactUtil.openContactInfoActivity(context, alert.email)
                }
            }
            MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED, MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED, MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED, MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED, MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU, MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED, MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU -> {
                Timber.d("Do not navigate")
            }
            MegaUserAlert.TYPE_PAYMENT_SUCCEEDED, MegaUserAlert.TYPE_PAYMENT_FAILED, MegaUserAlert.TYPE_PAYMENTREMINDER -> {
                Timber.d("Go to My Account")
                (activity as ManagerActivity?)?.navigateToMyAccount()
            }
            MegaUserAlert.TYPE_TAKEDOWN, MegaUserAlert.TYPE_TAKEDOWN_REINSTATED -> {
                if (alert.nodeHandle != -1L) {
                    val node = megaApi.getNodeByHandle(alert.nodeHandle)
                    if (node != null) {
                        if (node.isFile) {
                            (activity as ManagerActivity?)?.openLocation(node.parentHandle, null)
                        } else {
                            (activity as ManagerActivity?)?.openLocation(alert.nodeHandle, null)
                        }
                    }
                }
            }
            MegaUserAlert.TYPE_NEWSHARE, MegaUserAlert.TYPE_REMOVEDSHAREDNODES, MegaUserAlert.TYPE_DELETEDSHARE -> {
                Timber.d("Go to open corresponding location")
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    (activity as ManagerActivity?)?.openLocation(alert.nodeHandle, null)
                }
            }
            MegaUserAlert.TYPE_NEWSHAREDNODES -> {
                Timber.d("Go to open corresponding location")
                val numOfFolders = alert.getNumber(0).toInt()
                val numOfFiles = alert.getNumber(1).toInt()
                val childNodeHandleCount = numOfFolders + numOfFiles
                val childNodeHandleList = LongArray(childNodeHandleCount)
                var i = 0
                while (i < childNodeHandleCount) {
                    childNodeHandleList[i] = alert.getHandle(i.toLong())
                    i++
                }
                if (alert.nodeHandle != -1L && megaApi.getNodeByHandle(alert.nodeHandle) != null) {
                    (activity as ManagerActivity?)?.openLocation(alert.nodeHandle,
                        childNodeHandleList)
                }
            }
        }
    }

    fun updateNotifications(updatedUserAlerts: List<MegaUserAlert>) {
        Timber.d("updateNotifications")
        if (!isAdded) {
            Timber.d("return!")
            return
        }
        for (i in updatedUserAlerts.indices) {
            if (updatedUserAlerts[i].isOwnChange) {
                Timber.d("isOwnChange")
                continue
            }
            Timber.d("User alert type: %s", updatedUserAlerts[i].type)
            val idToUpdate = updatedUserAlerts[i].id
            var indexToReplace = -1
            val itrReplace: ListIterator<MegaUserAlert?> = notifications.listIterator()
            while (itrReplace.hasNext()) {
                val notification = itrReplace.next()
                if (notification != null) {
                    if (notification.id == idToUpdate) {
                        indexToReplace = itrReplace.nextIndex() - 1
                        break
                    }
                } else {
                    break
                }
            }
            if (indexToReplace != -1) {
                Timber.d("Index to replace: %s", indexToReplace)
                notifications[indexToReplace] = updatedUserAlerts[i]
                if (adapterList != null) {
                    adapterList?.notifyItemChanged(indexToReplace)
                }
                (activity as ManagerActivity?)?.markNotificationsSeen(false)
            } else {
                addNotification(updatedUserAlerts[i])
            }
        }
    }

    val itemCount: Int
        get() = adapterList?.itemCount ?: 0

    companion object {
        @JvmStatic
        fun newInstance(): NotificationsFragment {
            Timber.d("newInstance")
            return NotificationsFragment()
        }
    }
}