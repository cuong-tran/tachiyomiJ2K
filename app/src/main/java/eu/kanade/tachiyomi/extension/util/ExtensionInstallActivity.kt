package eu.kanade.tachiyomi.extension.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.injectLazy

/**
 * Activity used to install extensions, because we can only receive the result of the installation
 * with [startActivityForResult], which we need to update the UI.
 */
class ExtensionInstallActivity : Activity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (PACKAGE_INSTALLED_ACTION == intent.action) {
                packageInstallStep(intent)
                return
            }

            val downloadId = intent.extras!!.getLong(ExtensionInstaller.EXTRA_DOWNLOAD_ID)
            val packageInstaller = packageManager.packageInstaller
            val data = UniFile.fromUri(this, intent.data).openInputStream()

            val params = SessionParams(
                SessionParams.MODE_FULL_INSTALL
            )
            // TODO: Add once compiling via SDK 31
//            if (Build.VERSION.SDK_INT >= 31) {
            if (Build.VERSION.PREVIEW_SDK_INT + Build.VERSION.SDK_INT >= 31) {
                params.setRequireUserAction(USER_ACTION_NOT_REQUIRED)
            }
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            session.openWrite("package", 0, -1).use { packageInSession ->
                data.copyTo(packageInSession)
            }

            val newIntent = Intent(this, ExtensionInstallActivity::class.java)
                .setAction(PACKAGE_INSTALLED_ACTION)
                .putExtra(ExtensionInstaller.EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_SESSION_ID, sessionId)

            val pendingIntent = PendingIntent.getActivity(this, downloadId.hashCode(), newIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val statusReceiver = pendingIntent.intentSender
            session.commit(statusReceiver)
            val extensionManager: ExtensionManager by injectLazy()
            extensionManager.setInstalling(downloadId, sessionId)
            data.close()
        } catch (error: Exception) {
            // Either install package can't be found (probably bots) or there's a security exception
            // with the download manager. Nothing we can workaround.
            toast(error.message)
        }
        finish()
    }

    private fun packageInstallStep(intent: Intent) {
        val extras = intent.extras ?: return
        if (PACKAGE_INSTALLED_ACTION == intent.action) {
            val downloadId = extras.getLong(ExtensionInstaller.EXTRA_DOWNLOAD_ID)
            val extensionManager: ExtensionManager by injectLazy()
            when (val status = extras.getInt(PackageInstaller.EXTRA_STATUS)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = extras[Intent.EXTRA_INTENT] as? Intent
                    startActivityForResult(confirmIntent, INSTALL_REQUEST_CODE)
                    finish()
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    extensionManager.setInstallationResult(downloadId, true)
                    finish()
                }
                PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_ABORTED, PackageInstaller.STATUS_FAILURE_BLOCKED, PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_INVALID, PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    extensionManager.setInstallationResult(downloadId, false)
                    if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                        if (MiuiUtil.isMiui()) {
                            toast(R.string.extensions_miui_warning, Toast.LENGTH_LONG)
                        } else {
                            toast(R.string.could_not_install_extension)
                        }
                    }
                    finish()
                }
                else -> {
                    extensionManager.setInstallationResult(downloadId, false)
                    finish()
                }
            }
        }
    }

    private companion object {
        const val INSTALL_REQUEST_CODE = 500
        const val EXTRA_SESSION_ID = "ExtensionInstaller.extra.SESSION_ID"
        const val PACKAGE_INSTALLED_ACTION =
            "eu.kanade.tachiyomi.SESSION_API_PACKAGE_INSTALLED"
    }
}
