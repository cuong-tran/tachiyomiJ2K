package eu.kanade.tachiyomi.ui.library.filter

data class LibraryFilter(
    val headerName: String,
    val filters: List<String>,
    val tagGroup: FilterTagGroup,
    var activeFilter: Int,
)
