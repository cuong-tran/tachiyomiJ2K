package eu.kanade.tachiyomi.ui.source.browse.repos

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [RepoController]. Used to manage the repos for the extensions.
 */
class RepoPresenter(
    private val controller: RepoController,
    private val preferences: PreferencesHelper = Injekt.get(),
) : BaseCoroutinePresenter<RepoController>() {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    /**
     * List containing repos.
     */
    private val repos: Set<String>
        get() = preferences.extensionRepos().get()

    /**
     * Called when the presenter is created.
     */
    fun getRepos() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                controller.updateRepos()
            }
        }
    }

    fun getReposWithCreate(): List<RepoItem> {
        return (listOf(CREATE_REPO_ITEM) + repos).map(::RepoItem)
    }

    /**
     * Creates and adds a new repo to the database.
     *
     * @param name The name of the repo to create.
     */
    fun createRepo(name: String): Boolean {
        if (isInvalidRepo(name)) return false

        preferences.extensionRepos() += name.removeSuffix("/index.min.json")
        controller.updateRepos()
        return true
    }

    /**
     * Deletes the repo from the database.
     *
     * @param repo The repo to delete.
     */
    fun deleteRepo(repo: String?) {
        val safeRepo = repo ?: return
        preferences.extensionRepos() -= safeRepo
        controller.updateRepos()
    }

    /**
     * Renames a repo.
     *
     * @param repo The repo to rename.
     * @param name The new name of the repo.
     */
    fun renameRepo(repo: String, name: String): Boolean {
        val truncName = name.removeSuffix("/index.min.json")
        if (!repo.equals(truncName, true)) {
            if (isInvalidRepo(name)) return false
            preferences.extensionRepos() -= repo
            preferences.extensionRepos() += truncName
            controller.updateRepos()
        }
        return true
    }

    private fun isInvalidRepo(name: String): Boolean {
        // Do not allow invalid formats
        if (!name.matches(repoRegex)) {
            controller.onRepoInvalidNameError()
            return true
        }
        return false
    }

    companion object {
        private val repoRegex = """^https://.*/index\.min\.json$""".toRegex()
        const val CREATE_REPO_ITEM = "create_repo"
    }
}
