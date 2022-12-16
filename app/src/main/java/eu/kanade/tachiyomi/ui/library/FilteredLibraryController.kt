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
    var filterStatus = emptyArray<Int>()
        private set
    var filterTracked: Int = 0
        private set
    var filterMangaType = emptyArray<Int>()
        private set
    var filterSources = emptyArray<Long>()
        private set
    var filterLanguages = emptyArray<String>()
        private set

    private var customTitle: String? = null

    override fun getTitle(): String? {
        return customTitle ?: super.getTitle()
    }

    constructor(
        title: String,
        queryText: String? = null,
        filterStatus: Array<Int> = emptyArray(),
        filterSources: Array<Long> = emptyArray(),
        filterMangaType: Array<Int> = emptyArray(),
        filterLanguages: Array<String> = emptyArray(),
        filterTracked: Int = 0,
        filterTrackerName: String? = null,
    ) : this() {
        customTitle = title
        this.filterStatus = filterStatus
        this.filterLanguages = filterLanguages
        this.filterSources = filterSources
        this.filterTracked = filterTracked
        this.filterMangaType = filterMangaType
        if (filterTracked != 0 && filterTrackerName != null) {
            FilterBottomSheet.FILTER_TRACKER = filterTrackerName
        }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {}
    override fun toggleCategoryVisibility(position: Int) {}
    override fun hasActiveFiltersFromPref(): Boolean = false
    override fun manageCategory(position: Int) {}
}
