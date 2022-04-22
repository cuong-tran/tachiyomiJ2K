package eu.kanade.tachiyomi.ui.setting.database

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.Payload
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ClearDatabaseControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.fullAppBarHeight
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.snack
import kotlin.math.max
import kotlin.math.roundToInt

class ClearDatabaseController :
    NucleusController<ClearDatabaseControllerBinding, ClearDatabasePresenter>(),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnUpdateListener {

    private var adapter: FlexibleAdapter<ClearDatabaseSourceItem>? = null
    private var menu: Menu? = null

    override fun createBinding(inflater: LayoutInflater): ClearDatabaseControllerBinding {
        return ClearDatabaseControllerBinding.inflate(inflater)
    }

    override fun createPresenter(): ClearDatabasePresenter {
        return ClearDatabasePresenter()
    }

    override fun getTitle(): String? {
        return activity?.getString(R.string.clear_database)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = FlexibleAdapter<ClearDatabaseSourceItem>(null, this, true)
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)
        adapter?.fastScroller = binding.fastScroller
        val fabBaseMarginBottom = binding.fab.marginBottom

        scrollViewWith(
            binding.recycler,
            true,
            afterInsets = { insets ->
                if (binding.fastScroller.marginBottom != insets.getInsets(systemBars()).bottom + fabBaseMarginBottom) {
                    binding.fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = insets.getInsets(systemBars()).bottom + fabBaseMarginBottom
                    }
                }
                binding.recycler.updatePadding(
                    bottom = if (adapter?.selectedItemCount ?: 0 > 0) {
                        binding.fab.height + binding.fab.marginBottom
                    } else {
                        insets.getInsets(systemBars()).bottom
                    }
                )
                binding.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.getInsets(systemBars()).bottom
                }
                binding.emptyView.updatePadding(
                    top = (fullAppBarHeight ?: 0) + (activityBinding?.appBar?.paddingTop ?: 0),
                    bottom = insets.getInsets(systemBars()).bottom
                )
            }
        )
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            fun updateFastScrollMargins() {
                if (binding.fastScroller.isFastScrolling) return
                val activityBinding = activityBinding ?: return
                val bigToolbarHeight = fullAppBarHeight ?: return
                val value = max(
                    0,
                    bigToolbarHeight + activityBinding.appBar.y.roundToInt()
                ) + activityBinding.appBar.paddingTop
                if (value != binding.fastScroller.marginTop) {
                    binding.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = value
                    }
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateFastScrollMargins()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                updateFastScrollMargins()
            }
        })
        binding.fab.isInvisible = true
        binding.fab.setOnClickListener {
            if (adapter!!.selectedItemCount > 0) {
                val ctrl = ClearDatabaseSourcesDialog()
                ctrl.targetController = this
                ctrl.showDialog(router)
            }
        }
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.generic_selection, menu)
        this.menu = menu
        menu.forEach { menuItem -> menuItem.isVisible = (adapter?.itemCount ?: 0) > 0 }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val adapter = adapter ?: return false
        when (item.itemId) {
            R.id.action_select_all -> adapter.selectAll()
            R.id.action_select_inverse -> {
                val currentSelection = adapter.selectedPositionsAsSet
                val invertedSelection = (0..adapter.itemCount)
                    .filterNot { currentSelection.contains(it) }
                currentSelection.clear()
                currentSelection.addAll(invertedSelection)
            }
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, Payload.SELECTION)
        updateFab()
        return super.onOptionsItemSelected(item)
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(
                R.drawable.ic_book_24dp,
                R.string.database_clean,
            )
        }
        menu?.forEach { menuItem -> menuItem.isVisible = size > 0 }
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        adapter.toggleSelection(position)
        adapter.notifyItemChanged(position, Payload.SELECTION)
        updateFab()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val size = adapter?.itemCount ?: 0
        menu.forEach { menuItem -> menuItem.isVisible = size > 0 }
    }

    fun setItems(items: List<ClearDatabaseSourceItem>) {
        adapter?.updateDataSet(items)
    }

    private fun updateFab() {
        val adapter = adapter ?: return
        if (adapter.selectedItemCount > 0) {
            binding.fab.show()
        } else {
            binding.fab.hide()
        }
        val bottomPadding = if (adapter.selectedItemCount > 0) {
            binding.fab.height + binding.fab.marginBottom
        } else {
            binding.root.rootWindowInsetsCompat?.getInsets(systemBars())?.bottom ?: 0
        }
        if (bottomPadding != binding.recycler.paddingBottom) {
            binding.recycler.updatePadding(bottom = bottomPadding)
        }
    }

    class ClearDatabaseSourcesDialog : DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(activity!!)
                .setMessage(R.string.clear_database_confirmation)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    (targetController as? ClearDatabaseController)?.clearDatabaseForSelectedSources()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearDatabaseForSelectedSources() {
        val adapter = adapter ?: return
        val selectedSourceIds = adapter.selectedPositions.mapNotNull { position ->
            adapter.getItem(position)?.source?.id
        }
        presenter.clearDatabaseForSourceIds(selectedSourceIds)
        binding.fab.isVisible = false
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        view?.snack(R.string.clear_database_completed)
    }
}
