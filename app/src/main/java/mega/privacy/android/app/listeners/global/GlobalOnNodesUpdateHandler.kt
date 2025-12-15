package mega.privacy.android.app.listeners.global

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.RingtoneManager
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.pdf.CheckIfShouldDeleteLastPageViewedInPdfUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

class GlobalOnNodesUpdateHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val checkIfShouldDeleteLastPageViewedInPdfUseCase: CheckIfShouldDeleteLastPageViewedInPdfUseCase,
    @MegaApi private val megaApi: MegaApiAndroid,
) {
    operator fun invoke(nodeList: ArrayList<MegaNode>?) {
        nodeList?.toList()?.forEach { node ->
            when {
                node.isInShare && node.hasChanged(MegaNode.CHANGE_TYPE_INSHARE.toLong()) -> {
                    showSharedFolderNotification(node)
                }

                node.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong()) && node.publicLink != null -> {
                    // when activated share, will show rating if it matches the condition
                    RatingHandlerImpl(appContext).showRatingBaseOnSharing()
                }

                node.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong()) && node.isFile -> {
                    applicationScope.launch {
                        runCatching {
                            checkIfShouldDeleteLastPageViewedInPdfUseCase(
                                nodeHandle = node.handle,
                                fileName = node.name,
                                isOfflineRemoval = false,
                            )
                        }.onFailure { Timber.e(it) }
                    }
                }
            }
        }
    }

    private fun showSharedFolderNotification(n: MegaNode) {
        Timber.d("showSharedFolderNotification")
        try {
            val sharesIncoming = megaApi.inSharesList ?: return

            val notificationTitle = appContext.getString(
                if (n.hasChanged(MegaNode.CHANGE_TYPE_NEW.toLong()))
                    R.string.title_incoming_folder_notification
                else R.string.context_permissions_changed
            )

            val userName = sharesIncoming.firstOrNull { it.nodeHandle == n.handle }?.let {
                val user = megaApi.getContact(it.user)
                ContactUtil.getMegaUserNameDB(user) ?: ""
            } ?: ""

            val folderName = if (n.isNodeKeyDecrypted) n.name else notificationTitle

            val source =
                "<b>$folderName</b> " +
                        appContext.getString(R.string.incoming_folder_notification) +
                        " " +
                        Util.toCDATA(userName)
            val notificationContent = HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val notificationChannelId = Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID
            val intent: Intent = Intent(appContext, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivity(
                appContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
            val icon: Bitmap? = getBitmapFromVectorDrawable(
                mega.privacy.android.icon.pack.R.drawable.ic_folder_incoming_medium_solid
            )
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(appContext, notificationChannelId)
                    .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(appContext, R.color.red_600_red_300))
                    .setLargeIcon(icon)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
            notificationManager.notify(
                Constants.NOTIFICATION_PUSH_CLOUD_DRIVE,
                notificationBuilder.build()
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun getBitmapFromVectorDrawable(
        @DrawableRes drawableId: Int,
    ): Bitmap? {
        val drawable = ContextCompat.getDrawable(appContext, drawableId) ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}