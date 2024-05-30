package eu.kanade.tachiyomi.util.manga

import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.image.coil.getBestColor
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private var coverRatioMap = ConcurrentHashMap<Long, Float>()

    /**
     * [coverColorMap] stores favorite manga's cover & text's color as a joined string in Prefs.
     * They will be loaded each time [MangaCoverMetadata] is initialized with [MangaCoverMetadata.load]
     *
     * They will be saved back when [MangaCoverFetcher.setRatioAndColorsInScope] is called.
     */
    private var coverColorMap = ConcurrentHashMap<Long, Pair<Int, Int>>()
    private val preferences by injectLazy<PreferencesHelper>()
    private val coverCache by injectLazy<CoverCache>()

    fun load() {
        val ratios = preferences.coverRatios().get()
        coverRatioMap = ConcurrentHashMap(
            ratios.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val ratio = splits.lastOrNull()?.toFloatOrNull()
                if (id != null && ratio != null) {
                    id to ratio
                } else {
                    null
                }
            }.toMap(),
        )
        val colors = preferences.coverColors().get()
        coverColorMap = ConcurrentHashMap(
            colors.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val color = splits.getOrNull(1)?.toIntOrNull()
                val textColor = splits.getOrNull(2)?.toIntOrNull()
                if (id != null && color != null) {
                    id to (color to (textColor ?: 0))
                } else {
                    null
                }
            }.toMap(),
        )
    }

    /**
     * [setRatioAndColors] won't run if manga is not in library but already has [Manga.vibrantCoverColor].
     *
     * It removes saved colors from saved Prefs of [MangaCoverMetadata.coverColorMap] if manga is not favorite.
     *
     * If manga already has color (wrote by previous run or when opened detail page
     * with [MangaDetailsController.setPaletteColor] and not in library, it won't do anything.
     * It only run with favorite manga or non-favorite manga without color.
     *
     * If manga already restored color (except that favorite manga doesn't load color yet), then it
     * will skip actually reading [CoverCache].
     * For example when a manga updates its cover and next time it goes back to browsing page, this
     * function will skip loading bitmap but trying set dominant color with new cover. By doing so,
     * new cover's color will be saved into Prefs.
     *
     * Set [Manga.dominantCoverColors] for favorite manga only.
     * Set [Manga.vibrantCoverColor] for all mangas.
     *
     * This function is called when updating old library, to initially store color for all favorite mangas.
     *
     * It should also be called everytime while browsing to get manga's color from [CoverCache].
     *
     */
    fun setRatioAndColors(manga: Manga, ogFile: File? = null, force: Boolean = false) {
        if (!manga.favorite) {
            remove(manga)
        }
        // Won't do anything if manga is browsing & color loaded
        if (manga.vibrantCoverColor != null && !manga.favorite) return
        val file = ogFile ?: coverCache.getCustomCoverFile(manga).takeIf { it.exists() } ?: coverCache.getCoverFile(manga)
        // if the file exists and the there was still an error then the file is corrupted
        if (file.exists()) {
            val options = BitmapFactory.Options()
            val hasVibrantColor = if (manga.favorite) manga.vibrantCoverColor != null else true
            // If dominantCoverColors is not null, it means that color is restored from Prefs
            // and also has vibrantCoverColor (e.g. new color caused by updated cover)
            if (manga.dominantCoverColors != null && hasVibrantColor && !force) {
                // Trying update color without needs for actually reading file
                options.inJustDecodeBounds = true
            } else {
                options.inSampleSize = 4
            }
            val bitmap = BitmapFactory.decodeFile(file.path, options)
            if (bitmap != null) {
                Palette.from(bitmap).generate {
                    if (it == null) return@generate
                    if (manga.favorite) {
                        it.dominantSwatch?.let { swatch ->
                            manga.dominantCoverColors = swatch.rgb to swatch.titleTextColor
                        }
                    }
                    val color = it.getBestColor() ?: return@generate
                    manga.vibrantCoverColor = color
                }
            }
            if (manga.favorite && !(options.outWidth == -1 || options.outHeight == -1)) {
                addCoverRatio(manga, options.outWidth / options.outHeight.toFloat())
            }
        }
    }

    fun remove(manga: Manga) {
        val id = manga.id ?: return
        coverRatioMap.remove(id)
        coverColorMap.remove(id)
    }

    fun addCoverRatio(manga: Manga, ratio: Float) {
        val id = manga.id ?: return
        coverRatioMap[id] = ratio
    }

    fun addCoverColor(manga: Manga, @ColorInt color: Int, @ColorInt textColor: Int) {
        val id = manga.id ?: return
        coverColorMap[id] = color to textColor
    }

    fun getColors(manga: Manga): Pair<Int, Int>? {
        return coverColorMap[manga.id]
    }

    fun getRatio(manga: Manga): Float? {
        return coverRatioMap[manga.id]
    }

    fun savePrefs() {
        val mapCopy = coverRatioMap.toMap()
        preferences.coverRatios().set(mapCopy.map { "${it.key}|${it.value}" }.toSet())
        val mapColorCopy = coverColorMap.toMap()
        preferences.coverColors().set(mapColorCopy.map { "${it.key}|${it.value.first}|${it.value.second}" }.toSet())
    }
}
