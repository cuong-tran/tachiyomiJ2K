package eu.kanade.tachiyomi.ui.extension

import android.content.pm.PackageInstaller
import android.widget.Toast
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionsChangedListener
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.migration.MangaItem
import eu.kanade.tachiyomi.ui.migration.SelectionHeader
import eu.kanade.tachiyomi.ui.migration.SourceItem
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

typealias ExtensionTuple =
    Triple<List<Extension.Installed>, List<Extension.Untrusted>, List<Extension.Available>>
typealias ExtensionIntallInfo = Pair<InstallStep, PackageInstaller.SessionInfo?>

/**
 * Presenter of [ExtensionBottomSheet].
 */
class ExtensionBottomPresenter(
    private val bottomSheet: ExtensionBottomSheet,
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get()
) : BaseCoroutinePresenter(), ExtensionsChangedListener {

    private var extensions = emptyList<ExtensionItem>()

    var sourceItems = emptyList<SourceItem>()
        private set

    var mangaItems = hashMapOf<Long, List<MangaItem>>()
        private set

    private var currentDownloads = hashMapOf<String, ExtensionIntallInfo>()

    private val sourceManager: SourceManager = Injekt.get()

    private var selectedSource: Long? = null
    private val db: DatabaseHelper = Injekt.get()

    override fun onCreate() {
        super.onCreate()
        presenterScope.launch {
            val extensionJob = async {
                extensionManager.findAvailableExtensionsAsync()
                extensions = toItems(
                    Triple(
                        extensionManager.installedExtensions,
                        extensionManager.untrustedExtensions,
                        extensionManager.availableExtensions
                    )
                )
                withContext(Dispatchers.Main) { bottomSheet.setExtensions(extensions) }
                extensionManager.setListener(this@ExtensionBottomPresenter)
            }
            val migrationJob = async {
                val favs = db.getFavoriteMangas().executeOnIO()
                sourceItems = findSourcesWithManga(favs)
                mangaItems = HashMap(
                    sourceItems.associate {
                        it.source.id to this@ExtensionBottomPresenter.libraryToMigrationItem(favs, it.source.id)
                    }
                )
                withContext(Dispatchers.Main) {
                    if (selectedSource != null) {
                        bottomSheet.setMigrationManga(mangaItems[selectedSource])
                    } else {
                        bottomSheet.setMigrationSources(sourceItems)
                    }
                }
            }
            listOf(migrationJob, extensionJob).awaitAll()
        }
    }

    private fun findSourcesWithManga(library: List<Manga>): List<SourceItem> {
        val header = SelectionHeader()
        return library.map { it.source }.toSet()
            .mapNotNull { if (it != LocalSource.ID) sourceManager.getOrStub(it) else null }
            .sortedBy { it.name }
            .map { SourceItem(it, header) }
    }

    private fun libraryToMigrationItem(library: List<Manga>, sourceId: Long): List<MangaItem> {
        return library.filter { it.source == sourceId }.map(::MangaItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        extensionManager.removeListener(this)
    }

    fun refreshExtensions() {
        presenterScope.launch {
            extensions = toItems(
                Triple(
                    extensionManager.installedExtensions,
                    extensionManager.untrustedExtensions,
                    extensionManager.availableExtensions
                )
            )
            withContext(Dispatchers.Main) { bottomSheet.setExtensions(extensions) }
        }
    }

    fun refreshMigrations() {
        presenterScope.launch {
            val favs = db.getFavoriteMangas().executeOnIO()
            sourceItems = findSourcesWithManga(favs)
            mangaItems = HashMap(
                sourceItems.associate {
                    it.source.id to this@ExtensionBottomPresenter.libraryToMigrationItem(favs, it.source.id)
                }
            )
            withContext(Dispatchers.Main) {
                if (selectedSource != null) {
                    bottomSheet.setMigrationManga(mangaItems[selectedSource])
                } else {
                    bottomSheet.setMigrationSources(sourceItems)
                }
            }
        }
    }

    override fun extensionsUpdated() {
        refreshExtensions()
    }

    @Synchronized
    private fun toItems(tuple: ExtensionTuple): List<ExtensionItem> {
        val context = bottomSheet.context
        val activeLangs = preferences.enabledLanguages().get()
        val showNsfwExtensions = preferences.showNsfwExtension().get()

        val (installed, untrusted, available) = tuple

        val items = mutableListOf<ExtensionItem>()

        val updatesSorted = installed.filter { it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }.sortedBy { it.pkgName }
        val installedSorted = installed
            .filter { !it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }
            .sortedWith(compareBy({ !it.isObsolete }, { it.pkgName }))
        val untrustedSorted = untrusted.sortedBy { it.pkgName }
        val availableSorted = available
            // Filter out already installed extensions and disabled languages
            .filter { avail ->
                installed.none { it.pkgName == avail.pkgName } &&
                    untrusted.none { it.pkgName == avail.pkgName } &&
                    (avail.lang in activeLangs || avail.lang == "all") &&
                    (showNsfwExtensions || !avail.isNsfw)
            }
            .sortedBy { it.pkgName }

        if (updatesSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(
                context.resources.getQuantityString(
                    R.plurals._updates_pending,
                    updatesSorted.size,
                    updatesSorted.size
                ),
                updatesSorted.size
            )
            items += updatesSorted.map { extension ->
                ExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }
        }
        if (installedSorted.isNotEmpty() || untrustedSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(context.getString(R.string.installed), installedSorted.size + untrustedSorted.size)
            items += installedSorted.map { extension ->
                ExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }
            items += untrustedSorted.map { extension ->
                ExtensionItem(extension, header)
            }
        }
        if (availableSorted.isNotEmpty()) {
            val availableGroupedByLang = availableSorted
                .groupBy { LocaleHelper.getSourceDisplayName(it.lang, context) }
                .toSortedMap()

            availableGroupedByLang
                .forEach {
                    val header = ExtensionGroupItem(it.key, it.value.size)
                    items += it.value.map { extension ->
                        ExtensionItem(extension, header, currentDownloads[extension.pkgName])
                    }
                }
        }

        this.extensions = items
        return items
    }

    fun getExtensionUpdateCount(): Int = preferences.extensionUpdatesCount().getOrDefault()
    fun getAutoCheckPref() = preferences.automaticExtUpdates()

    @Synchronized
    private fun updateInstallStep(
        extension: Extension,
        state: InstallStep,
        session: PackageInstaller.SessionInfo?
    ): ExtensionItem? {
        val extensions = extensions.toMutableList()
        val position = extensions.indexOfFirst { it.extension.pkgName == extension.pkgName }

        return if (position != -1) {
            val item = extensions[position].copy(
                installStep = state,
                session = session
            )
            extensions[position] = item

            this.extensions = extensions
            item
        } else {
            null
        }
    }

    fun cancelExtensionInstall(extItem: ExtensionItem) {
        val sessionId = extItem.session?.sessionId ?: return
        extensionManager.cancelInstallation(sessionId)
    }

    fun installExtension(extension: Extension.Available) {
        if (isNotMIUIOptimized()) {
            extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        if (isNotMIUIOptimized()) {
            extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
        }
    }

    fun isNotMIUIOptimized(): Boolean {
        if (MiuiUtil.isMiui() && !MiuiUtil.isMiuiOptimizationDisabled()) {
            preferences.context.toast(R.string.extensions_miui_warning, Toast.LENGTH_LONG)
            return false
        }
        return true
    }

    private fun Observable<ExtensionIntallInfo>.subscribeToInstallUpdate(extension: Extension) {
        this.doOnNext { currentDownloads[extension.pkgName] = it }
            .doOnUnsubscribe { currentDownloads.remove(extension.pkgName) }
            .map { state -> updateInstallStep(extension, state.first, state.second) }
            .subscribe { item ->
                if (item != null) {
                    bottomSheet.downloadUpdate(item)
                }
            }
    }

    fun uninstallExtension(pkgName: String) {
        extensionManager.uninstallExtension(pkgName)
    }

    fun findAvailableExtensions() {
        extensionManager.findAvailableExtensions()
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }

    fun setSelectedSource(source: Source) {
        selectedSource = source.id
        presenterScope.launch {
            withContext(Dispatchers.Main) { bottomSheet.setMigrationManga(mangaItems[source.id]) }
        }
    }

    fun deselectSource() {
        selectedSource = null
        presenterScope.launch {
            withContext(Dispatchers.Main) { bottomSheet.setMigrationSources(sourceItems) }
        }
    }
}
