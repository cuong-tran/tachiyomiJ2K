package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.system.awaitSingle

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class LatestUpdatesPager(val source: CatalogueSource) : Pager() {

    override suspend fun requestNextPage() {
        val mangasPage = source.fetchLatestUpdates(currentPage).awaitSingle()
        onPageReceived(mangasPage)
    }
}
