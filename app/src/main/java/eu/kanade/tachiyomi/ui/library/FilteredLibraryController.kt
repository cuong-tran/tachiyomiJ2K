package eu.kanade.tachiyomi.ui.library

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.util.view.collapse
import uy.kohesive.injekt.api.get

class FilteredLibraryController(bundle: Bundle? = null) : LibraryController(bundle) {

    private var queryText: String? = null
    var filterDownloaded: Int = 0
        private set
    var filterUnread: Int = 0
        private set
    var filterStatus: Int? = null
        private set
    var filterTracked: Int = 0
        private set
    var filterMangaType: Int = 0
        private set

    private var customTitle: String? = null

    override fun getTitle(): String? {
        return customTitle ?: super.getTitle()
    }

    constructor(
        title: String,
        queryText: String? = null,
        filterDownloaded: Int = 0,
        filterUnread: Int = 0,
        filterStatus: Int? = null,
        filterTracked: Int = 0,
        filterTrackerName: String? = null,
        filterMangaType: Int = 0,
    ) : this() {
        customTitle = title
        this.filterDownloaded = filterDownloaded
        this.filterUnread = filterUnread
        this.filterStatus = filterStatus
        this.filterTracked = filterTracked
        if (filterTracked != 0 && filterTrackerName != null) {
            FilterBottomSheet.FILTER_TRACKER = filterTrackerName
        }
        this.filterMangaType = filterMangaType
        this.queryText = queryText
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.filterBottomSheet.root.sheetBehavior?.isHideable = false
        binding.filterBottomSheet.root.sheetBehavior?.collapse()
        binding.filterBottomSheet.filterScroll.isVisible = false
        binding.filterBottomSheet.secondLayout.isVisible = false
        binding.filterBottomSheet.viewOptions.isVisible = false
        binding.filterBottomSheet.pill.isVisible = false
        queryText?.let { search(it) }
    }

    override fun showFloatingBar() = false

    override fun handleBack(): Boolean {
        if (binding.recyclerCover.isClickable) {
            showCategories(false)
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) { }
    override fun toggleCategoryVisibility(position: Int) { }
    override fun hasActiveFiltersFromPref(): Boolean = false
}
