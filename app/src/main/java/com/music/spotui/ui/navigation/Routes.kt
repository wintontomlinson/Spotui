package com.music.spotui.ui.navigation

sealed class Routes(
    val label : String,
    val route : String
) {
    object Home : Routes(label = "Home", route = "home")
    // Login-free YouTube search & play: works without any Spotify session.
    object YtSearch : Routes(label = "Explore", route = "ytsearch")
    object Library : Routes(label = "Library", route = "library")
    object Album : Routes("Album", "album")
    object Player : Routes("Player", "player")
    object Artist : Routes("Artist", "artist")
    object ArtistReleases : Routes("ArtistReleases", "artistreleases")
    object Playlist : Routes("Playlist", "playlist")
    object Show : Routes("Show", "show")
    object Queue : Routes("Queue", "queue")
    object Liked : Routes("Liked", "liked")
    object Downloads : Routes("Downloads", "downloads")
    object Settings : Routes("Settings", "settings")
    object History : Routes("History", "history")
    object LocalFiles : Routes("LocalFiles", "localfiles")
    object Equalizer : Routes("Equalizer", "equalizer")
}

/** Builds a playlist route carrying the Spotify playlist id (and a display name). */
fun playlistRoute(id: String, name: String = ""): String =
    "${Routes.Playlist.route}/${android.net.Uri.encode(id)}?name=${android.net.Uri.encode(name)}"

/** Builds a podcast-show route carrying the Spotify show id (and a display name). */
fun showRoute(id: String, name: String = ""): String =
    "${Routes.Show.route}/${android.net.Uri.encode(id)}?name=${android.net.Uri.encode(name)}"

/**
 * Builds an artist route. When the exact Spotify artist id is known it is
 * carried along so the artist page opens the exact artist instead of
 * re-resolving the name via a fuzzy search ("RAM" must not open "Rammstein").
 */
fun artistRoute(name: String, id: String = ""): String {
    val base = "${Routes.Artist.route}/${android.net.Uri.encode(name)}"
    return if (id.isBlank()) base
    else "$base?id=${android.net.Uri.encode(id)}"
}

/**
 * Builds an album route, optionally carrying the artist so same-named albums by
 * different artists resolve to the right one. The artist value is URL-encoded.
 */
fun albumRoute(name: String, artist: String = "", id: String = ""): String {
    // The name has to be encoded. Real album titles contain "?", "/", "#" and quotes,
    // for example 'Kesariya (From "Brahmastra")', and an unencoded "?" turned the rest
    // of the title into query arguments so the album screen opened with a truncated
    // name and found nothing.
    //
    // [id] is a YouTube Music album id ("MPREb_...") when the caller already knows
    // exactly which album this is, which lets the album screen skip resolving the name
    // by search. Names are ambiguous: many unrelated albums are called "Rockstar".
    val base = "${Routes.Album.route}/${android.net.Uri.encode(name)}"
    val args = buildList {
        if (artist.isNotBlank()) add("artist=${android.net.Uri.encode(artist)}")
        if (id.isNotBlank()) add("id=${android.net.Uri.encode(id)}")
    }
    return if (args.isEmpty()) base else "$base?${args.joinToString("&")}"
}
