package com.music.spotui.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.music.spotui.data.preferences.isLibraryGridView
import com.music.spotui.data.preferences.setLibraryGridView
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.viewmodel.LibraryFilterType
import com.music.spotui.ui.viewmodel.LibraryViewModel

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

@Composable
fun LibraryFilterChips(
    selectedFilter: LibraryFilterType,
    isDownloadedOnly: Boolean,
    onFilterSelected: (LibraryFilterType) -> Unit,
    onToggleDownloaded: () -> Unit,
    onClearFilters: () -> Unit
) {
    val isAnyFilterActive = selectedFilter != LibraryFilterType.ALL || isDownloadedOnly

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAnyFilterActive) {
            item {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262019))
                        .clickable { onClearFilters() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear filters",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            LibraryChipItem(
                label = "Playlists",
                isSelected = selectedFilter == LibraryFilterType.PLAYLISTS,
                onClick = { onFilterSelected(LibraryFilterType.PLAYLISTS) }
            )
        }

        item {
            LibraryChipItem(
                label = "Albums",
                isSelected = selectedFilter == LibraryFilterType.ALBUMS,
                onClick = { onFilterSelected(LibraryFilterType.ALBUMS) }
            )
        }

        // The Artists filter is dropped: it hides every library entry and only shows
        // followed artists, which do not exist on this login free build, so it always
        // produced an empty screen.

        item {
            LibraryChipItem(
                label = "Downloaded",
                isSelected = isDownloadedOnly,
                onClick = { onToggleDownloaded() }
            )
        }
    }
}

@Composable
private fun LibraryChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFF5A524) else Color(0xFF1C1C21)
    val textColor = if (isSelected) Color(0xFF1A1206) else Color.White

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(17.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

    var gridView by remember { mutableStateOf(isLibraryGridView(context)) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF282828),
            title = { Text("Create Local Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Playlist name", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF383838),
                        unfocusedContainerColor = Color(0xFF383838),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFF5A524),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
            },
            confirmButton = {
                Text(
                    "Create",
                    color = Color(0xFFF5A524),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable {
                            val name = playlistNameInput.trim().ifBlank { "My Playlist" }
                            val created = LocalPlaylistPref.createPlaylist(context, name)
                            Api.HomeCache.library = null
                            libraryViewModel.load()
                            showCreateDialog = false
                            navController.navigate(playlistRoute(created.id, created.name))
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    "Cancel",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { showCreateDialog = false }
                        .padding(8.dp)
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(AppBackground.toArgb()))
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
                .padding(16.dp, 12.dp, 16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f)
            )
            // Add Playlist Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262019))
                    .clickable { showCreateDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Search & Filter Toggle Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSearchVisible || searchQuery.isNotEmpty()) Color(0xFFF5A524) else Color(0xFF262019))
                    .clickable { isSearchVisible = !isSearchVisible },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_big),
                    contentDescription = "Search & Filter",
                    tint = if (isSearchVisible || searchQuery.isNotEmpty()) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Grid/List View Toggle Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262019))
                    .clickable {
                        gridView = !gridView
                        setLibraryGridView(context, gridView)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (gridView) R.drawable.ic_view_list else R.drawable.ic_view_grid),
                    contentDescription = if (gridView) "Show as list" else "Show as grid",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Profile / Settings Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF352E24))
                    .clickable { navController.navigate(Routes.Settings.route) },
                contentAlignment = Alignment.Center
            ) {
                val avatar = (account as? Response.Success)?.data?.imageUrl.orEmpty()
                if (avatar.isNotBlank()) {
                    AccountAvatar(avatar, 34.dp)
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Account", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Library Filter Chips bar
        LibraryFilterChips(
            selectedFilter = selectedFilter,
            isDownloadedOnly = isDownloadedOnly,
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
                        .clip(RoundedCornerShape(8.dp))
                        .height(36.dp)
                        .background(Color(0xFF262019))
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search_big),
                        tint = Color.Gray,
                        contentDescription = "Search Library",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle.Default.copy(
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFF5A524)),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search in library",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
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
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Pill Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF262019))
                        .clickable { showSortSheet = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSort.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Sort Options",
                            tint = Color.White,
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

                if (gridView) {
                    LibraryGridScreen(
                        padding = PaddingValues(0.dp),
                        entries = filteredEntries,
                        followedArtists = filteredArtists,
                        navController = navController,
                        showHistoryTile = showHistoryTile,
                        onClearFilters = { libraryViewModel.clearFilters() }
                    )
                } else {
                    SumUpLibraryScreen(
                        padding = PaddingValues(0.dp),
                        entries = filteredEntries,
                        followedArtists = filteredArtists,
                        navController = navController,
                        showHistoryTile = showHistoryTile,
                        onClearFilters = { libraryViewModel.clearFilters() }
                    )
                }
            }
            else -> Box(modifier = Modifier.padding(20.dp, 100.dp)) { Snackbar(showMessage = "Couldn't load your library") }
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = Color(0xFF1A1A1A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Sort by",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp)
                    )
                    HorizontalDivider(color = Color(0xFF262019))
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
                                .padding(16.dp, 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFF5A524) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Text(
                                text = if (isSelected) option.getDescriptiveLabel(isDescending) else option.getDescriptiveLabel(option == LibrarySortOption.RECENTS),
                                color = if (isSelected) Color(0xFFF5A524) else Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = if (isDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color(0xFFF5A524),
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
    onClearFilters: () -> Unit = {}
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
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Songs you save and playlists you create will appear here.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5A524))
                    .clickable { onClearFilters() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Clear filters",
                    color = Color(0xFF1A1206),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
                .background(Color(0xFF0E0E13))
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
                    .padding(20.dp, 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { openLibraryEntry(entry, navController) }
            ) {
                if (entry.spotifyId == Api.HomeCache.DOWNLOADS_ID) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(55.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF006450), Color(0xFF00382B))
                                )
                            ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Downloaded",
                            tint = Color(0xFFF5A524),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID && entry.coverUri.isBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(55.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF450AF5), Color(0xFF8E8E93))
                                )
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    GlideImage(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(RoundedCornerShape(if (entry.isPlaylist) 6.dp else 4.dp)),
                        model = entry.coverUri,
                        contentScale = ContentScale.Crop,
                        failure = placeholder(R.drawable.placeholder),
                        loading = placeholder(R.drawable.placeholder),
                        contentDescription = ""
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = entry.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.isLocal) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Local Storage",
                                tint = Color(0xFFF5A524),
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 3.dp)
                            )
                        }
                        Text(text = entry.subtitle, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
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
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 4.dp),
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
                        Text(text = artist.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Artist", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(160.dp)) }
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

/**
 * Grid layout of the same library content: 3 columns of square covers with the
 * title/subtitle underneath, like Spotify's "Grid" library view. Followed
 * artists render as circles under their own full-width header.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LibraryGridScreen(
    padding: PaddingValues,
    entries: List<LibraryEntry>,
    followedArtists: List<com.music.spotui.data.entity.ArtistsModel>,
    navController: NavController,
    showHistoryTile: Boolean = true,
    onClearFilters: () -> Unit = {}
) {
    if (entries.isEmpty() && followedArtists.isEmpty()) {
        // Keep quick access reachable even with an empty library, otherwise the grid
        // view becomes a dead end.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0E0E13)),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            LibraryQuickAccess(navController)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Nothing else here yet",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Songs you save and playlists you create will appear here.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5A524))
                        .clickable { onClearFilters() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Clear filters",
                        color = Color(0xFF1A1206),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFF0E0E13)),
        contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 130.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showHistoryTile) {
            // Listening history & stats entry, as a tile.
            item {
                Column(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { navController.navigate(Routes.History.route) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF27856A)),
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Listening history", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Your plays and stats", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        items(entries) { entry ->
            Column(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { openLibraryEntry(entry, navController) }
            ) {
                if (entry.spotifyId == Api.HomeCache.DOWNLOADS_ID) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF006450), Color(0xFF00382B))
                                )
                            ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Downloaded",
                            tint = Color(0xFFF5A524),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (entry.spotifyId == Api.HomeCache.LIKED_SONGS_ID && entry.coverUri.isBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF450AF5), Color(0xFF8E8E93))
                                )
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    GlideImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(if (entry.isPlaylist) 6.dp else 4.dp)),
                        model = entry.coverUri,
                        contentScale = ContentScale.Crop,
                        failure = placeholder(R.drawable.placeholder),
                        loading = placeholder(R.drawable.placeholder),
                        contentDescription = ""
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = entry.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.isLocal) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Local Storage",
                            tint = Color(0xFFF5A524),
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 3.dp)
                        )
                    }
                    Text(text = entry.subtitle, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (followedArtists.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Artists you follow",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            items(followedArtists) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { navController.navigate(artistRoute(artist.name, artist.id)) }
                ) {
                    GlideImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(CircleShape),
                        model = artist.coverUri,
                        contentScale = ContentScale.Crop,
                        contentDescription = ""
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = artist.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text(text = "Artist", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun LibrarySkeleton(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFF0E0E13))
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        repeat(8) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 6.dp)
            ) {
                Box(modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1E1E1E)))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Box(modifier = Modifier.height(14.dp).width(160.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1E1E1E)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(11.dp).width(90.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1E1E1E)))
                }
            }
        }
    }
}

@Composable
private fun AccountRow(label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Text(
        text = label,
        color = tint,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp, 16.dp)
    )
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
            containerColor = Color(0xFF262019),
            title = { Text("Create playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Playlist name", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF352E24),
                        unfocusedContainerColor = Color(0xFF352E24),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                )
            },
            confirmButton = {
                Text(
                    "Create", color = accent, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val n = name.trim().ifBlank { "My Playlist" }
                            val created = LocalPlaylistPref.createPlaylist(context, n)
                            Api.HomeCache.library = null
                            showCreate = false
                            navController.navigate(playlistRoute(created.id, created.name))
                        }
                        .padding(8.dp),
                )
            },
            dismissButton = {
                Text(
                    "Cancel", color = Color.Gray,
                    modifier = Modifier.clickable { showCreate = false }.padding(8.dp),
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
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Build your collection",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Create a playlist, or like and download songs from Explore. They will all show up here.",
            color = Color(0xFFB3B3B3),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Create playlist",
                color = Color(0xFF1A1206),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .clickable { showCreate = true }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
            )
            Text(
                "Explore",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF262019))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                    .clickable { navController.navigate(Routes.YtSearch.route) }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
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

    // One cohesive palette instead of the old blue/teal/purple mix. Liked songs leads in
    // the amber accent; the rest are graded dark surfaces with an amber tinted icon, so
    // the grid reads as one premium set rather than four unrelated colours.
    val tiles = listOf(
        Tile(
            "Liked songs", "Your favourites",
            Icons.Default.Favorite,
            Color(0xFFF5A524), Color(0xFFC87F0A),
            Routes.Liked.route,
        ),
        Tile(
            "Recently played", "Plays and stats",
            Icons.Default.DateRange,
            Color(0xFF2C2C34), Color(0xFF1A1A1F),
            Routes.History.route,
        ),
        Tile(
            "Downloads", "Saved offline",
            Icons.Default.Add,
            Color(0xFF2C2C34), Color(0xFF1A1A1F),
            Routes.Downloads.route,
        ),
        Tile(
            "Local files", "Music on this device",
            Icons.Default.PhoneAndroid,
            Color(0xFF2C2C34), Color(0xFF1A1A1F),
            Routes.LocalFiles.route,
        ),
    )
    val accent = com.music.spotui.ui.theme.Accent

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { tile ->
                    // The Liked tile is the amber one, so its content is dark for contrast;
                    // the dark tiles use an amber icon and white text.
                    val onTile = if (tile.route == Routes.Liked.route) Color(0xFF1A1206) else Color.White
                    val iconTint = if (tile.route == Routes.Liked.route) Color(0xFF1A1206) else accent
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black,
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(tile.start, tile.end)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { navController.navigate(tile.route) }
                            .padding(16.dp),
                    ) {
                        Icon(
                            tile.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = tile.label,
                            color = onTile,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tile.caption,
                            color = onTile.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
