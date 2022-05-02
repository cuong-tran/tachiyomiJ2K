package eu.kanade.tachiyomi.widget

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import kotlin.math.max

class GridLayoutManagerAccurateOffset(context: Context?, spanCount: Int) : GridLayoutManager(context, spanCount) {

    // map of child adapter position to its height.
    private val childSizesMap = HashMap<Int, Int>()
    private val childSpanMap = HashMap<Int, Int>()
    private val childTypeHeightMap = HashMap<Int, HashMap<Int, Int>>()
    private val childTypeMap = HashMap<Int, Int>()
    private val childTypeEstimateMap = HashMap<Int, Int>()
    var computedRange: Int? = null
    var rView: RecyclerView? = null

    private val toolbarHeight by lazy {
        val attrsArray = intArrayOf(R.attr.mainActionBarSize)
        val array = (context ?: rView?.context)?.obtainStyledAttributes(attrsArray)
        val height = array?.getDimensionPixelSize(0, 0) ?: 0
        array?.recycle()
        height
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        computedRange = null
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: return
            val position = getPosition(child)
            childSizesMap[position] = child.height
            childSpanMap[position] = spanSizeLookup.getSpanSize(getPosition(child))
            val type = getItemViewType(child)
            childTypeMap[position] = type
            if (childTypeHeightMap[type] != null) {
                childTypeHeightMap[type]!![position] = child.height
            } else {
                childTypeHeightMap[type] = hashMapOf(position to child.height)
            }
        }
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        rView = view
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        rView = null
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        if (childCount == 0) return 0
        computedRange?.let {
            return it
        }
        rView ?: return super.computeVerticalScrollRange(state)
        var scrolledY = 0
        var spanC = 0
        var maxHeight = 0
        val childAvgHeightMap = HashMap<Int, Int>()
        for (i in 0 until itemCount) {
            val height: Int = getItemHeight(i, childAvgHeightMap)
            val spanCurrentSize = childSpanMap[i] ?: spanSizeLookup.getSpanSize(i)
            if (spanCount <= spanCurrentSize) {
                scrolledY += height
                scrolledY += maxHeight
                maxHeight = 0
                spanC = 0
            } else if (spanCurrentSize == 1) {
                maxHeight = max(maxHeight, height)
                spanC++
                if (spanC <= spanCount) {
                    scrolledY += maxHeight
                    maxHeight = 0
                    spanC = 0
                }
            }
        }
        computedRange = scrolledY
        return scrolledY
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0) {
            return 0
        }
        rView ?: return super.computeVerticalScrollOffset(state)
        val firstChild = getChildAt(0) ?: return 0
        val firstChildPosition = (0 until childCount)
            .mapNotNull { getChildAt(it) }
            .mapNotNull { pos -> getPosition(pos).takeIf { it != RecyclerView.NO_POSITION } }
            .minOrNull() ?: 0
        var scrolledY: Int = -firstChild.y.toInt()
        var spanC = 0
        var maxHeight = 0
        val childAvgHeightMap = HashMap<Int, Int>()
        for (i in 0 until firstChildPosition) {
            val height: Int = getItemHeight(i, childAvgHeightMap)
            val spanCurrentSize = childSpanMap[i] ?: spanSizeLookup.getSpanSize(i)
            if (spanCount <= spanCurrentSize) {
                scrolledY += height
                scrolledY += maxHeight
                maxHeight = 0
                spanC = 0
            } else if (spanCurrentSize == 1) {
                maxHeight = max(maxHeight, height)
                spanC++
                if (spanC <= spanCount) {
                    scrolledY += maxHeight
                    maxHeight = 0
                    spanC = 0
                }
            }
        }
        scrolledY += maxHeight
        return scrolledY + paddingTop
    }

    private fun getItemHeight(pos: Int, childAvgHeightMap: HashMap<Int, Int>): Int {
        return EstimatedItemHeight.itemOrEstimatedHeight(
            pos,
            rView?.adapter?.getItemViewType(pos),
            childSizesMap,
            childTypeMap,
            childTypeHeightMap,
            childTypeEstimateMap,
            childAvgHeightMap,
        )
    }

    override fun findFirstVisibleItemPosition(): Int {
        return getFirstPos(rView, toolbarHeight)
    }

    override fun findFirstCompletelyVisibleItemPosition(): Int {
        return getFirstCompletePos(rView, toolbarHeight)
    }
}
