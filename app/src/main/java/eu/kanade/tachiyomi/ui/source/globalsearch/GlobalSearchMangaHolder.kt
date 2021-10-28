package eu.kanade.tachiyomi.ui.source.globalsearch

import android.view.View
import androidx.core.view.isVisible
import coil.Coil
import coil.clear
import coil.request.CachePolicy
import coil.request.ImageRequest
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.CoverViewTarget
import eu.kanade.tachiyomi.databinding.SourceGlobalSearchControllerCardItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.makeShapeCorners
import eu.kanade.tachiyomi.util.view.setCards

class GlobalSearchMangaHolder(view: View, adapter: GlobalSearchCardAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    private val binding = SourceGlobalSearchControllerCardItemBinding.bind(view)
    init {
        // Call onMangaClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(flexibleAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
        binding.favoriteButton.shapeAppearanceModel =
            binding.card.makeShapeCorners(binding.card.radius, binding.card.radius)
        itemView.setOnLongClickListener {
            adapter.mangaClickListener.onMangaLongClick(flexibleAdapterPosition, adapter)
            true
        }
        setCards(adapter.showOutlines, binding.card, binding.favoriteButton)
    }

    fun bind(manga: Manga) {
        binding.title.text = manga.title
        binding.favoriteButton.isVisible = manga.favorite
        setImage(manga)
    }

    fun setImage(manga: Manga) {
        binding.itemImage.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val request = ImageRequest.Builder(itemView.context).data(manga)
                .placeholder(android.R.color.transparent)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .target(CoverViewTarget(binding.itemImage, binding.progress)).build()
            Coil.imageLoader(itemView.context).enqueue(request)
        }
    }
}
