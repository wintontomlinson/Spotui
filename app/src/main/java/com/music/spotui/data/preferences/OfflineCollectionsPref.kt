package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import org.json.JSONArray
import org.json.JSONObject

data class OfflineCollection(
    val id: String,
    val name: String,
    val coverUri: String,
    val artists: String,
    val isPlaylist: Boolean,
    val songs: List<SongsModel>
)

object OfflineCollectionsPref {
    private const val PREF = "OfflineCollections"

    private fun SongsModel.toJsonObject(): JSONObject = JSONObject().apply {
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
    }

    private fun parseSongFromObj(o: JSONObject): SongsModel? = runCatching {
        SongsModel(
            id = o.getInt("id"),
            title = o.getString("title"),
            album = o.optString("album"),
            singer = o.getString("singer"),
            coverUri = o.optString("coverUri"),
            url = o.getString("url"),
            spotifyTrackId = o.optString("spotifyTrackId"),
            explicit = o.optBoolean("explicit", false),
            durationMs = o.optInt("durationMs", 0),
            artistIds = o.optString("artistIds", ""),
        )
    }.getOrNull()

    private fun OfflineCollection.toJson(): String = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("coverUri", coverUri)
        put("artists", artists)
        put("isPlaylist", isPlaylist)
        val songArray = JSONArray()
        songs.forEach { songArray.put(it.toJsonObject()) }
        put("songs", songArray)
    }.toString()

    private fun parseCollection(json: String): OfflineCollection? = runCatching {
        val o = JSONObject(json)
        val songArray = o.getJSONArray("songs")
        val songList = mutableListOf<SongsModel>()
        for (i in 0 until songArray.length()) {
            parseSongFromObj(songArray.getJSONObject(i))?.let { songList.add(it) }
        }
        OfflineCollection(
            id = o.getString("id"),
            name = o.getString("name"),
            coverUri = o.getString("coverUri"),
            artists = o.getString("artists"),
            isPlaylist = o.getBoolean("isPlaylist"),
            songs = songList
        )
    }.getOrNull()

    fun saveCollection(
        context: Context,
        id: String,
        name: String,
        coverUri: String,
        artists: String,
        isPlaylist: Boolean,
        songs: List<SongsModel>
    ) {
        val col = OfflineCollection(id, name, coverUri, artists, isPlaylist, songs)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(id, col.toJson())
            .apply()
    }

    fun getCollection(context: Context, id: String): OfflineCollection? {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(id, null) ?: return null
        return parseCollection(json)
    }

    fun getOfflineCollections(context: Context): List<OfflineCollection> {
        // Only collections with something actually downloaded, for the offline views.
        return getAllCollections(context).filter { col ->
            col.songs.any { song ->
                com.music.spotui.data.preferences.isDownloaded(context, song.id.toString())
            }
        }
    }

    /**
     * Every saved collection, downloaded or not.
     *
     * The library needs this rather than the downloaded-only list: an album or playlist you
     * opened is saved here, and filtering on downloads meant it never appeared in your
     * library unless you had also downloaded a track from it.
     */
    fun getAllCollections(context: Context): List<OfflineCollection> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.all.values.mapNotNull { (it as? String)?.let { json -> parseCollection(json) } }
    }

    fun isDownloadedCollection(context: Context, col: OfflineCollection): Boolean =
        col.songs.any { com.music.spotui.data.preferences.isDownloaded(context, it.id.toString()) }

    fun removeCollection(context: Context, id: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(id).apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
