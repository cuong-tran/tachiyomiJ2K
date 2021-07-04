package eu.kanade.tachiyomi

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val context = preferences.context
        val oldVersion = preferences.lastVersionCode().getOrDefault()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            if (oldVersion == 0) {
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                ExtensionUpdateJob.setupTask(context)
                LibraryUpdateJob.setupTask(context)
                return BuildConfig.DEBUG
            }

            if (oldVersion < 14) {
                // Restore jobs after upgrading to evernote's job scheduler.
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                LibraryUpdateJob.setupTask(context)
            }
            if (oldVersion < 15) {
                // Delete internal chapter cache dir.
                File(context.cacheDir, "chapter_disk_cache").deleteRecursively()
            }
            if (oldVersion < 19) {
                // Move covers to external files dir.
                val oldDir = File(context.externalCacheDir, "cover_disk_cache")
                if (oldDir.exists()) {
                    val destDir = context.getExternalFilesDir("covers")
                    if (destDir != null) {
                        oldDir.listFiles()?.forEach {
                            it.renameTo(File(destDir, it.name))
                        }
                    }
                }
            }
            if (oldVersion < 26) {
                // Delete external chapter cache dir.
                val extCache = context.externalCacheDir
                if (extCache != null) {
                    val chapterCache = File(extCache, "chapter_disk_cache")
                    if (chapterCache.exists()) {
                        chapterCache.deleteRecursively()
                    }
                }
            }
            if (oldVersion < 54) {
                DownloadProvider(context).renameChapters()
            }
            if (oldVersion < 62) {
                LibraryPresenter.updateDB()
                // Restore jobs after migrating from Evernote's job scheduler to WorkManager.
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
                LibraryUpdateJob.setupTask(context)
                BackupCreatorJob.setupTask(context)
                ExtensionUpdateJob.setupTask(context)
            }
            if (oldVersion < 66) {
                LibraryPresenter.updateCustoms()
            }
            if (oldVersion < 68) {
                // Force MAL log out due to login flow change
                // v67: switched from scraping to WebView
                // v68: switched from WebView to OAuth
                val trackManager = Injekt.get<TrackManager>()
                if (trackManager.myAnimeList.isLogged) {
                    trackManager.myAnimeList.logout()
                    context.toast(R.string.myanimelist_relogin)
                }
            }
            if (oldVersion < 71) {
                // Migrate DNS over HTTPS setting
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val wasDohEnabled = prefs.getBoolean("enable_doh", false)
                if (wasDohEnabled) {
                    prefs.edit {
                        putInt(PreferenceKeys.dohProvider, PREF_DOH_CLOUDFLARE)
                        remove("enable_doh")
                    }
                }
            }
            if (oldVersion < 73) {
                // Reset rotation to Free after replacing Lock
                preferences.rotation().set(1)
            }
            if (oldVersion < 74) {
                // Turn on auto updates for all users
                if (BuildConfig.INCLUDE_UPDATER) {
                    UpdaterJob.setupTask(context)
                }
            }
            if (oldVersion < 75) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val wasShortcutsDisabled = !prefs.getBoolean("show_manga_app_shortcuts", true)
                if (wasShortcutsDisabled) {
                    prefs.edit {
                        putBoolean(PreferenceKeys.showSourcesInShortcuts, false)
                        putBoolean(PreferenceKeys.showSeriesInShortcuts, false)
                        remove("show_manga_app_shortcuts")
                    }
                }
                if (preferences.lang().get() in listOf("en-US", "en-GB")) {
                    preferences.lang().set("en")
                }
                // Handle removed every 1 or 2 hour library updates
                val updateInterval = preferences.libraryUpdateInterval().get()
                if (updateInterval == 1 || updateInterval == 2) {
                    preferences.libraryUpdateInterval().set(3)
                    LibraryUpdateJob.setupTask(context, 3)
                }
            }
            return true
        }
        return false
    }
}
