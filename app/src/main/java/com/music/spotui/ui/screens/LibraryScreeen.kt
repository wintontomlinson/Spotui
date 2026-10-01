package com.music.spotui.ui.screens

import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.foundation.layout.heightIn
import com.music.spotui.ui.components.shimmer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import com.music.spotui.data.preferences.LibrarySortOption
import com.music.spotui.data.preferences.getLibrarySortOption
import com.music.spotui.data.preferences.isLibrarySortDescending
import com.music.spotui.data.preferences.setLibrarySortOption
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bumptech.glide.integration.compose.placeholder
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.R
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.preferences.LocalPlaylistPref
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.AppBackgroundBrush
import com.music.spotui.ui.viewmodel.LibraryFilterType
import com.music.spotui.ui.viewmodel.LibraryViewModel
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.AccentSoft
import com.music.spotui.ui.theme.AccentDeep
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.Surface1
import com.music.spotui.ui.theme.Elevated
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

fun isLibraryEntryDownloaded(context: android.content.Context, entry: LibraryEntry): Boolean {
    if (entry.isLocal || entry.spotifyId == Api.HomeCache.DOWNLOADS_ID) return true
    if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID) {
        return com.music.spotui.data.preferences.getDownloadedSongs(context).isNotEmpty()
    }
    val offlineCollections = com.music.spotui.data.preferences.OfflineCollectionsPref.getOfflineCollections(context)
    val cleanEntryId = Api.cleanId(entry.spotifyId)
    return offlineCollections.any { col ->
        val cleanColId = Api.cleanId(col.id)
        (cleanColId.isNotBlank() && cleanColId == cleanEntryId) ||
        (entry.isPlaylist == col.isPlaylist && entry.name.equals(col.name, ignoreCase = true))
    }
}

/**
 * Per-row overflow menu. A local playlist can be deleted outright; anything else is
 * removed from the library, which is recorded so derived rows do not come straight back.
 */
@Composable
private fun LibraryRowMenu(
    entry: com.music.spotui.data.entity.LibraryEntry,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accent = com.music.spotui.ui.theme.Accent
    val isLocalPlaylist = entry.spotifyId.startsWith("local_pl_")

    Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "More options for ${entry.name}",
        tint = TextTertiary,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { showSheet = true }
            .padding(13.dp),
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Surface2,
            shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
            dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
            scrimColor = com.music.spotui.ui.theme.Scrim,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = entry.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 12.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable {
                            showSheet = false
                            confirmDelete = true
                        }
                        .padding(20.dp, 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = com.music.spotui.ui.theme.Danger,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isLocalPlaylist) "Delete playlist" else "Remove from library",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Surface2,
            title = {
                Text(
                    text = if (isLocalPlaylist) "Delete playlist?" else "Remove from library?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = if (isLocalPlaylist) {
                        "\"${entry.name}\" and the songs you added to it will be deleted. " +
                            "This cannot be undone."
                    } else {
                        "\"${entry.name}\" will be taken out of your library. " +
                            "You can always open it again from search."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                com.music.spotui.ui.components.SoloDialogConfirm(
                    text = if (isLocalPlaylist) "Delete" else "Remove",
                    danger = true,
                    onClick = {
                            if (isLocalPlaylist) {
                                LocalPlaylistPref.deletePlaylist(context, entry.spotifyId)
                            }
                            // Also drop any saved copy, and record the removal so a row
                            // rebuilt from history does not reappear.
                            com.music.spotui.data.preferences.OfflineCollectionsPref
                                .removeCollection(context, entry.spotifyId)
                            com.music.spotui.data.preferences.LibraryHiddenPref
                                .hide(context, entry.spotifyId)
                            Api.HomeCache.library = null
                            confirmDelete = false
                            onChanged()
                        },
                )
            },
            dismissButton = {
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Cancel",
                    onClick = { confirmDelete = false },
                )
            },
        )
    }
}

/** Which library filters currently have anything behind them. */
data class LibraryChipAvailability(
    val hasPlaylists: Boolean = false,
    val hasAlbums: Boolean = false,
    val hasDownloads: Boolean = false,
)

@Composable
fun LibraryFilterChips(
    selectedFilter: LibraryFilterType,
    isDownloadedOnly: Boolean,
    availability: LibraryChipAvailability = LibraryChipAvailability(true, true, true),
    onFilterSelected: (LibraryFilterType) -> Unit,
    onToggleDownloaded: () -> Unit,
    onClearFilters: () -> Unit
) {
    val isAnyFilterActive = selectedFilter != LibraryFilterType.ALL || isDownloadedOnly

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAnyFilterActive) {
            item {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(SoloShape.sm)
                        .background(Surface3)
                        .clickable { onClearFilters() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear filters",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (availability.hasPlaylists || selectedFilter == LibraryFilterType.PLAYLISTS) {
            item {
                LibraryChipItem(
                    label = "Playlists",
                    isSelected = selectedFilter == LibraryFilterType.PLAYLISTS,
                    onClick = { onFilterSelected(LibraryFilterType.PLAYLISTS) }
                )
            }
        }

        if (availability.hasAlbums || selectedFilter == LibraryFilterType.ALBUMS) {
            item {
                LibraryChipItem(
                    label = "Albums",
                    isSelected = selectedFilter == LibraryFilterType.ALBUMS,
                    onClick = { onFilterSelected(LibraryFilterType.ALBUMS) }
                )
            }
        }

        // The Artists filter is dropped: it hides every library entry and only shows
        // followed artists, which do not exist on this login free build, so it always
        // produced an empty screen.

        if (availability.hasDownloads || isDownloadedOnly) {
            item {
                LibraryChipItem(
                    label = "Downloaded",
                    isSelected = isDownloadedOnly,
                    onClick = { onToggleDownloaded() }
                )
            }
        }
    }
}

@Composable
private fun LibraryChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Accent else Surface3
    val textColor = if (isSelected) com.music.spotui.ui.theme.OnAccent else TextPrimary

    // Segmented look: sm-radius chips rather than round pills.
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(SoloShape.sm)
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier
                else Modifier.border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.sm)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController) {

    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val entries by libraryViewModel.entries.collectAsState()
    val account by libraryViewModel.account.collectAsState()
    val selectedFilter by libraryViewModel.selectedFilter.collectAsState()
    val isDownloadedOnly by libraryViewModel.isDownloadedOnly.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                libraryViewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Surface2,
            title = { Text("Create Local Playlist", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = {
                TextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Playlist name", color = TextTertiary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface3,
                        unfocusedContainerColor = Surface3,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.md)
                )
            },
            confirmButton = {
                com.music.spotui.ui.components.SoloDialogConfirm(
                    text = "Create",
                    onClick = {
                            val name = playlistNameInput.trim().ifBlank { "My Playlist" }
                            val created = LocalPlaylistPref.createPlaylist(context, name)
                            Api.HomeCache.library = null
                            libraryViewModel.load()
                            showCreateDialog = false
                            navController.navigate(playlistRoute(created.id, created.name))
                        },
                )
            },
            dismissButton = {
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Cancel",
                    onClick = { showCreateDialog = false },
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .statusBarsPadding()
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var isSearchVisible by remember { mutableStateOf(false) }
        var currentSort by remember { mutableStateOf(getLibrarySortOption(context)) }
        var isDescending by remember { mutableStateOf(isLibrarySortDescending(context)) }
        var showSortSheet by remember { mutableStateOf(false) }

        // Header: title + create playlist + search toggle + grid toggle + account avatar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 12.dp, 12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            // Add Playlist Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { showCreateDialog = true }
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Surface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }

            // Search & Filter Toggle Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { isSearchVisible = !isSearchVisible }
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (isSearchVisible || searchQuery.isNotEmpty()) Accent else Surface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Search),
                    contentDescription = "Search & Filter",
                    tint = if (isSearchVisible || searchQuery.isNotEmpty()) com.music.spotui.ui.theme.OnAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Grid/list toggle removed: the library is a single, consistent list view.

            // Profile / Settings Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate(Routes.Settings.route) }
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Surface4),
                contentAlignment = Alignment.Center
            ) {
                val avatar = (account as? Response.Success)?.data?.imageUrl.orEmpty()
                if (avatar.isNotBlank()) {
                    AccountAvatar(avatar, 40.dp)
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Account", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // A filter chip that can only ever lead to a blank screen is worse than no chip
        // at all, so each one is offered only when it actually has something behind it.
        // The chip currently in use always stays, otherwise selecting it would make it
        // vanish along with the only way to switch back.
        val availableFilters = remember(entries, isDownloadedOnly, context) {
            val rows = ((entries as? Response.Success)?.data).orEmpty().filterNot {
                it.spotifyId == Api.HomeCache.LIKED_SONGS_ID ||
                    it.spotifyId == Api.HomeCache.DOWNLOADS_ID
            }
            LibraryChipAvailability(
                hasPlaylists = rows.any { it.isPlaylist },
                hasAlbums = rows.any { !it.isPlaylist },
                hasDownloads = rows.any { isLibraryEntryDownloaded(context, it) },
            )
        }

        // Library Filter Chips bar
        LibraryFilterChips(
            selectedFilter = selectedFilter,
            isDownloadedOnly = isDownloadedOnly,
            availability = availableFilters,
            onFilterSelected = { libraryViewModel.setFilterType(it) },
            onToggleDownloaded = { libraryViewModel.toggleDownloadedOnly() },
            onClearFilters = { libraryViewModel.clearFilters() }
        )

        // Expandable Search & Sort Controls Bar
        AnimatedVisibility(
            visible = isSearchVisible || searchQuery.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 4.dp, 16.dp, 8.dp)
            ) {
                // Search Input Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(SoloShape.md)
                        .height(40.dp)
                        .background(Surface3)
                        .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.md)
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Search),
                        tint = TextTertiary,
                        contentDescription = "Search Library",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(Accent),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search in library",
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" }
                                .padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Pill Button
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(SoloShape.sm)
                        .background(Surface3)
                        .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.sm)
                        .clickable { showSortSheet = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSort.label,
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Sort Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp).padding(start = 2.dp)
                        )
                    }
                }
            }
        }

        val followedArtists by libraryViewModel.followedArtists.collectAsState()
        when (entries) {
            is Response.Loading -> LibrarySkeleton(PaddingValues(0.dp))
            is Response.Success -> {
                val rawEntries = (entries as Response.Success).data
                val filteredEntries = remember(rawEntries, selectedFilter, isDownloadedOnly, searchQuery, currentSort, isDescending, context) {
                    val filtered = rawEntries.filter { entry ->
                        // Liked Songs and Downloads live in the quick access grid at the
                        // top, so drop the pinned rows from the list below to avoid showing
                        // them twice. The list then holds only real playlists, albums and
                        // saved collections.
                        if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID ||
                            entry.spotifyId == Api.HomeCache.DOWNLOADS_ID
                        ) {
                            return@filter false
                        }
                        val matchesCategory = when (selectedFilter) {
                            LibraryFilterType.ALL -> true
                            LibraryFilterType.PLAYLISTS -> entry.isPlaylist
                            LibraryFilterType.ALBUMS -> !entry.isPlaylist
                            LibraryFilterType.ARTISTS -> false
                        }
                        val matchesDownload = if (isDownloadedOnly) isLibraryEntryDownloaded(context, entry) else true
                        val matchesSearch = if (searchQuery.isBlank()) true else (entry.name.contains(searchQuery, ignoreCase = true) || entry.artists.contains(searchQuery, ignoreCase = true) || entry.subtitle.contains(searchQuery, ignoreCase = true))
                        matchesCategory && matchesDownload && matchesSearch
                    }
                    when (currentSort) {
                        LibrarySortOption.RECENTS -> if (isDescending) filtered else filtered.reversed()
                        LibrarySortOption.TITLE -> if (isDescending) filtered.sortedByDescending { it.name.lowercase() } else filtered.sortedBy { it.name.lowercase() }
                        LibrarySortOption.CREATOR -> if (isDescending) filtered.sortedByDescending { it.artists.ifBlank { it.subtitle }.lowercase() } else filtered.sortedBy { it.artists.ifBlank { it.subtitle }.lowercase() }
                    }
                }
                val filteredArtists = remember(followedArtists, selectedFilter, isDownloadedOnly, searchQuery, currentSort, isDescending) {
                    if (selectedFilter == LibraryFilterType.PLAYLISTS || selectedFilter == LibraryFilterType.ALBUMS) {
                        emptyList()
                    } else {
                        val searchFiltered = if (searchQuery.isBlank()) followedArtists else followedArtists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        when (currentSort) {
                            LibrarySortOption.RECENTS -> if (isDescending) searchFiltered else searchFiltered.reversed()
                            LibrarySortOption.TITLE, LibrarySortOption.CREATOR -> if (isDescending) searchFiltered.sortedByDescending { it.name.lowercase() } else searchFiltered.sortedBy { it.name.lowercase() }
                        }
                    }
                }
                // Recently played already has a Quick access tile, so the separate history
                // tile in the list/grid is a duplicate. Turned off.
                val showHistoryTile = false

                // The library is list only now, so it always renders the list layout.
                SumUpLibraryScreen(
                    padding = PaddingValues(0.dp),
                    entries = filteredEntries,
                    followedArtists = filteredArtists,
                    navController = navController,
                    showHistoryTile = showHistoryTile,
                    onClearFilters = { libraryViewModel.clearFilters() },
                    onLibraryChanged = { libraryViewModel.load() },
                )
            }
            else -> com.music.spotui.ui.components.SoloEmptyState(
                icon = androidx.compose.material.icons.Icons.Rounded.CloudOff,
                title = "Couldn't load your library",
                message = "Check your connection and try again. Liked songs, downloads and local files still work offline.",
                actionLabel = "Retry",
                onAction = { libraryViewModel.load() },
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
            )
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = Surface2,
                shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
                dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
                scrimColor = com.music.spotui.ui.theme.Scrim,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Sort by",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 12.dp)
                    )
                    HorizontalDivider(color = com.music.spotui.ui.theme.Hairline)
                    Spacer(modifier = Modifier.height(4.dp))
                    LibrarySortOption.entries.forEach { option ->
                        val isSelected = option == currentSort
                        val icon = when (option) {
                            LibrarySortOption.RECENTS -> Icons.Default.DateRange
                            LibrarySortOption.TITLE -> Icons.AutoMirrored.Filled.List
                            LibrarySortOption.CREATOR -> Icons.Default.Person
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentSort == option) {
                                        isDescending = !isDescending
                                    } else {
                                        currentSort = option
                                        isDescending = (option == LibrarySortOption.RECENTS)
                                    }
                                    setLibrarySortOption(context, currentSort, isDescending)
                                    showSortSheet = false
                                }
                                .heightIn(min = 52.dp)
                                .padding(20.dp, 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Accent else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Text(
                                text = if (isSelected) option.getDescriptiveLabel(isDescending) else option.getDescriptiveLabel(option == LibrarySortOption.RECENTS),
                                color = if (isSelected) Accent else TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = if (isDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AccountAvatar(url: String, size: androidx.compose.ui.unit.Dp) {
    GlideImage(
        modifier = Modifier.size(size).clip(CircleShape),
        model = url,
        contentScale = ContentScale.Crop,
        contentDescription = ""
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SumUpLibraryScreen(
    padding: PaddingValues,
    entries: List<LibraryEntry>,
    followedArtists: List<com.music.spotui.data.entity.ArtistsModel>,
    navController: NavController,
    showHistoryTile: Boolean = true,
    onClearFilters: () -> Unit = {},
    onLibraryChanged: () -> Unit = {},
) {
    if (entries.isEmpty() && followedArtists.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nothing else here yet",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Songs you save and playlists you create will appear here.",
                color = TextTertiary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            com.music.spotui.ui.components.SoloPillButton(
                text = "Clear filters",
                onClick = { onClearFilters() },
            )
        }
        return
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Transparent)
        ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        // Premium quick access grid for the destinations that always work.
        item { LibraryQuickAccess(navController) }

        // The screen already has a "Your Library" title at the top, so the list starts
        // straight after the quick access grid without a second heading.
        items(entries) { entry ->
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable { openLibraryEntry(entry, navController) }
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                if (entry.spotifyId == Api.HomeCache.DOWNLOADS_ID) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SoloShape.sm)
                            .background(Surface3),
                    ) {
                        Icon(
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Download),
                            contentDescription = "Downloaded",
                            tint = Accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID && entry.coverUri.isBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SoloShape.sm)
                            .background(com.music.spotui.ui.theme.AccentBrush),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = com.music.spotui.ui.theme.OnAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    GlideImage(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(if (entry.isPlaylist) SoloShape.sm else SoloShape.xs)
                            .border(
                                1.dp,
                                com.music.spotui.ui.theme.Hairline,
                                if (entry.isPlaylist) SoloShape.sm else SoloShape.xs,
                            ),
                        model = entry.coverUri,
                        contentScale = ContentScale.Crop,
                        failure = placeholder(R.drawable.placeholder),
                        loading = placeholder(R.drawable.placeholder),
                        contentDescription = ""
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(text = entry.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.isLocal) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Local Storage",
                                tint = Accent,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 3.dp)
                            )
                        }
                        Text(text = entry.subtitle, color = TextTertiary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                // Deleting a playlist used to mean opening it and finding a small icon in
                // the header, so most people never found it. Every row now carries its own
                // menu, right where the playlist is.
                LibraryRowMenu(entry = entry, onChanged = onLibraryChanged)
            }
        }
        // When the only entries are the two pinned shortcuts, invite the user to start a
        // collection so the space below the list is not bare.
        val hasRealEntries = entries.any {
            it.spotifyId != Api.HomeCache.LIKED_SONGS_ID && it.spotifyId != Api.HomeCache.DOWNLOADS_ID
        }
        if (!hasRealEntries && followedArtists.isEmpty()) {
            item { LibraryEmptyState(navController) }
        }
        // ── Artists the user follows on Spotify ──
        if (followedArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Artists you follow",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp),
                )
            }
            items(followedArtists) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 6.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navController.navigate(artistRoute(artist.name, artist.id)) }
                ) {
                    GlideImage(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape),
                        model = artist.coverUri,
                        contentScale = ContentScale.Crop,
                        contentDescription = ""
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = artist.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Artist", color = TextTertiary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        // Extra room so the last library row clears the mini-player + nav bar
        // and is fully visible when scrolled to the bottom.
        item { Spacer(modifier = Modifier.height(230.dp)) }
    }
    com.music.spotui.ui.components.FastScrollbarForLazyList(
        state = listState,
        modifier = Modifier
            .padding(padding)
            .align(Alignment.CenterEnd)
    )
}
}

private fun openLibraryEntry(entry: LibraryEntry, navController: NavController) {
    if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID) navController.navigate(Routes.Liked.route)
    else if (entry.spotifyId == Api.HomeCache.DOWNLOADS_ID) navController.navigate(Routes.Downloads.route)
    else if (entry.isPlaylist) navController.navigate(playlistRoute(entry.spotifyId, entry.name))
    else navController.navigate(albumRoute(entry.name, entry.artists))
}


@Composable
private fun LibrarySkeleton(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.Transparent)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        repeat(8) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 6.dp)
            ) {
                Box(modifier = Modifier.size(56.dp).shimmer(SoloShape.sm))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Box(modifier = Modifier.height(14.dp).width(160.dp).shimmer(SoloShape.xs))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(11.dp).width(90.dp).shimmer(SoloShape.xs))
                }
            }
        }
    }
}



/**
 * Premium quick access grid at the top of the Library. These four destinations
 * always work regardless of any account state, so they are surfaced as large,
 * tappable cards instead of being buried in the list below.
 */
/**
 * Shown below the quick access grid when the user has no playlists, albums or offline
 * collections yet, so the Library never looks empty or broken. Invites creating a
 * playlist and points at Explore to start adding music.
 */
@Composable
private fun LibraryEmptyState(navController: NavController) {
    val accent = com.music.spotui.ui.theme.Accent
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = Surface2,
            title = { Text("Create playlist", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Playlist name", color = TextTertiary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface3,
                        unfocusedContainerColor = Surface3,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().clip(SoloShape.md),
                )
            },
            confirmButton = {
                com.music.spotui.ui.components.SoloDialogConfirm(
                    text = "Create",
                    onClick = {
                            val n = name.trim().ifBlank { "My Playlist" }
                            val created = LocalPlaylistPref.createPlaylist(context, n)
                            Api.HomeCache.library = null
                            showCreate = false
                            navController.navigate(playlistRoute(created.id, created.name))
                        },
                )
            },
            dismissButton = {
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Cancel",
                    onClick = { showCreate = false },
                )
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(SoloShape.md)
                .background(Surface3)
                .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.md),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Build your collection",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Create a playlist, or like and download songs from Explore. They will all show up here.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.music.spotui.ui.components.SoloPillButton(
                text = "Create playlist",
                onClick = { showCreate = true },
            )
            com.music.spotui.ui.components.SoloPillButton(
                text = "Explore",
                onClick = { navController.navigate(Routes.YtSearch.route) },
                primary = false,
            )
        }
    }
}

@Composable
private fun LibraryQuickAccess(navController: NavController) {
    data class Tile(
        val label: String,
        val caption: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val start: Color,
        val end: Color,
        val route: String,
    )

    // One cohesive palette. Liked songs leads in the Volt accent gradient; the rest are
    // Surface3 tiles with a Volt icon, so the grid reads as one premium set.
    // Each tile states how much is actually in it. A tile captioned "Saved offline" that
    // opens an empty screen is a dead end; a live count sets the expectation up front and
    // makes the tiles worth glancing at.
    val tileContext = LocalContext.current
    val counts = remember {
        listOf(
            runCatching { com.music.spotui.data.preferences.getLikedSongs(tileContext).size }.getOrDefault(0),
            runCatching { com.music.spotui.data.preferences.getListeningHistory(tileContext).size }.getOrDefault(0),
            runCatching { com.music.spotui.data.preferences.getDownloadedSongs(tileContext).size }.getOrDefault(0),
            runCatching { com.music.spotui.data.preferences.getLocalTracks(tileContext).size }.getOrDefault(0),
        )
    }

    /** "12 songs", or a nudge when there is nothing there yet. */
    fun caption(count: Int, empty: String, noun: String = "song"): String = when (count) {
        0 -> empty
        1 -> "1 $noun"
        else -> "$count ${noun}s"
    }

    val tiles = listOf(
        Tile(
            "Liked songs", caption(counts[0], "Tap the heart on any song"),
            Icons.Default.Favorite,
            AccentSoft, AccentDeep,
            Routes.Liked.route,
        ),
        Tile(
            "Recently played", caption(counts[1], "Nothing played yet", "play"),
            Icons.Default.DateRange,
            Surface3, Surface3,
            Routes.History.route,
        ),
        Tile(
            "Downloads", caption(counts[2], "Nothing saved offline"),
            androidx.compose.material.icons.Icons.Rounded.Download,
            Surface3, Surface3,
            Routes.Downloads.route,
        ),
        Tile(
            "Local files", caption(counts[3], "Music on this device", "track"),
            Icons.Default.PhoneAndroid,
            Surface3, Surface3,
            Routes.LocalFiles.route,
        ),
    )
    val accent = com.music.spotui.ui.theme.Accent

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { tile ->
                    // The Liked tile is the Volt one, so its content is dark for contrast;
                    // the Surface3 tiles use a Volt icon and light text.
                    val onTile = if (tile.route == Routes.Liked.route) com.music.spotui.ui.theme.OnAccent else TextPrimary
                    val iconTint = if (tile.route == Routes.Liked.route) com.music.spotui.ui.theme.OnAccent else accent
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = 8.dp,
                                shape = SoloShape.md,
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black,
                            )
                            .clip(SoloShape.md)
                            .then(
                                if (tile.route == Routes.Liked.route) Modifier.background(com.music.spotui.ui.theme.AccentBrush)
                                else Modifier.background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(tile.start, tile.end))
                                )
                            )
                            .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.md)
                            .clickable { navController.navigate(tile.route) }
                            .padding(16.dp),
                    ) {
                        // Icon sits in a soft rounded chip so each tile has a clear,
                        // premium focal point rather than a bare glyph.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(SoloShape.sm)
                                .background(
                                    if (tile.route == Routes.Liked.route)
                                        com.music.spotui.ui.theme.OnAccent.copy(alpha = 0.12f)
                                    else Surface4
                                ),
                        ) {
                            Icon(
                                tile.icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = tile.label,
                            color = onTile,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tile.caption,
                            color = onTile.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
