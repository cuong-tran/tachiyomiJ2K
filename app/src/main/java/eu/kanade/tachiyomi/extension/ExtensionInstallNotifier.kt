package eu.kanade.tachiyomi.extension

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager

class ExtensionInstallNotifier(private val context: Context) {

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelExtensionUpdatePendingBroadcast(context)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_UPDATES_TO_EXTS) {
            setContentTitle(context.getString(R.string.app_name))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setLargeIcon(notificationBitmap)
            setContentTitle(context.getString(R.string.updating_extensions))
            setProgress(0, 0, true)
            setOngoing(true)
            setSilent(true)
            setOnlyAlertOnce(true)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            addAction(R.drawable.ic_close_24dp, context.getString(android.R.string.cancel), cancelIntent)
        }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(progress: Int, max: Int) {
        context.notificationManager.notify(
            Notifications.ID_EXTENSION_PROGRESS,
            progressNotificationBuilder
                .setContentTitle(context.getString(R.string.updating_extensions))
                .setProgress(max, progress, progress == 0)
                .build()
        )
    }
}
