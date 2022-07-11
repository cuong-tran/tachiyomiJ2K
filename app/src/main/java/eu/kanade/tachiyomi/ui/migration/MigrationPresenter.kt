package eu.kanade.tachiyomi.ui.migration

import kotlinx.coroutines.launch

class MigrationPresenter : BaseMigrationPresenter<MigrationController>() {
    override fun onCreate() {
        super.onCreate()
        presenterScope.launch { firstTimeMigration() }
    }
}
