package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult

class GithubAppUpdateChecker : AppUpdateChecker() {

    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): AppUpdateResult {
        val release = service.getLatestVersion()
        val newVersion = release.version.replace("[^\\d.]".toRegex(), "")

        // Check if latest version is different from current version
        return if (newVersion != BuildConfig.VERSION_NAME) {
            GithubAppUpdateResult.NewUpdate(release)
        } else {
            GithubAppUpdateResult.NoNewUpdate()
        }
    }
}
