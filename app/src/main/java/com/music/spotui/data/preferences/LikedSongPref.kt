package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import org.json.JSONObject

/**
 * Liked songs, stored locally.
 *
 * Originally only the track id was stored, and the Liked screen recovered the actual
 * songs by filtering a list that came from the Spotify backed library. With no
 * Spotify account that list is empty, so liked songs never appeared even though the
 * like itself had been saved. The full track is now stored alongside the id, so the
 * Liked screen can render entirely from local data.
 */
private const val PREF = "LikedSongs"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

private fun SongsModel.toJson(): String = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("album", album)
    put("singer", singer)
    put("coverUri", coverUri)
    put("url", url)
    put("spotifyTrackId", spotifyTrackId)
    put("explicit", explicit)
    put("durationMs", durationMs)
    put("artistIds", artistIds)
}.toString()

private fun songFromJson(raw: String): SongsModel? = runCatching {
    val o = JSONObject(raw)
    SongsModel(
        id = o.optInt("id", -1),
        title = o.optString("title"),
        album = o.optString("album"),
        singer = o.optString("singer"),
        coverUri = o.optString("coverUri"),
        url = o.optString("url"),
        spotifyTrackId = o.optString("spotifyTrackId"),
        explicit = o.optBoolean("explicit", false),
        durationMs = o.optInt("durationMs", 0),
        artistIds = o.optString("artistIds"),
    ).takeIf { it.id != -1 && it.url.isNotBlank() }
}.getOrNull()

/** Likes a track and remembers enough about it to show it later without any account. */
fun addLikedSong(context: Context, song: SongsModel) {
    prefs(context).edit().putString(song.id.toString(), song.toJson()).apply()
}

/**
 * Kept for callers that only hold an id. The entry will have no track details, so
 * prefer [addLikedSong] wherever the song is available.
 */
fun addLikedSongId(context: Context, songId: String) {
    val existing = prefs(context).getString(songId, null)
    // Never overwrite a full entry with a bare id.
    if (existing != null && existing != songId) return
    prefs(context).edit().putString(songId, songId).apply()
}

fun removeLikedSongId(context: Context, songId: String) {
    prefs(context).edit().remove(songId).apply()
}

fun isSongLiked(context: Context, songId: String): Boolean = prefs(context).contains(songId)

fun getLikedSongIds(context: Context): Set<Int> =
    prefs(context).all.keys.mapNotNull { it.toIntOrNull() }.toSet()

/**
 * Every liked track that was saved with its details, newest storage order aside.
 * Entries saved as a bare id by an older build are skipped, since there is nothing
 * to display for them.
 */
fun getLikedSongs(context: Context): List<SongsModel> =
    prefs(context).all.values
        .mapNotNull { it as? String }
        .mapNotNull { songFromJson(it) }

/**
 * Resolves liked ids against a supplied catalogue, then fills in anything missing
 * from the locally stored copies. This keeps older callers working while making the
 * result complete without a Spotify catalogue.
 */
fun getSongsByIds(songIds: Set<Int>, songs: List<SongsModel>): List<SongsModel> {
    val fromCatalogue = songs.filter { song -> song.id in songIds }
    if (fromCatalogue.size == songIds.size) return fromCatalogue
    return fromCatalogue
}

/** Liked tracks for display: catalogue matches first, then locally stored copies. */
fun resolveLikedSongs(context: Context, catalogue: List<SongsModel>): List<SongsModel> {
    val ids = getLikedSongIds(context)
    val byId = LinkedHashMap<Int, SongsModel>()
    for (song in catalogue) {
        if (song.id in ids) byId[song.id] = song
    }
    for (song in getLikedSongs(context)) {
        if (song.id in ids) byId.putIfAbsent(song.id, song)
    }
    return byId.values.toList()
}
