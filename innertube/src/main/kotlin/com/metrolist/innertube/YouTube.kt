package com.metrolist.innertube

import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.metrolist.innertube.models.YouTubeLocale
import com.metrolist.innertube.models.getContinuation
import com.metrolist.innertube.models.getItems
import com.metrolist.innertube.models.response.BrowseResponse
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.innertube.models.response.SearchResponse
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.innertube.pages.BrowseParser
import com.metrolist.innertube.pages.BrowsePage
import com.metrolist.innertube.pages.SearchPage
import com.metrolist.innertube.pages.SearchResult
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.Proxy

/**
 * Parse useful data with [InnerTube] sending requests.
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic).
 *
 * Trimmed for spotui: only the pieces the app uses survive — track search
 * (to match a Spotify track to a YouTube video), the player endpoint (to
 * resolve the audio stream) and the NewPipe fallback deobfuscation.
 */
object YouTube {
    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }
    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }
    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }
    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) {
            innerTube.proxyAuth = value
        }
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        val shelves = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull { it.musicShelfRenderer }
            .orEmpty()
        SearchResult(
            items = shelves.flatMap { shelf ->
                shelf.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
            }.distinctBy { it.id },
            continuation = shelves.firstOrNull { it.continuations != null }
                ?.continuations?.getContinuation()
        )
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        val items = response.continuationContents?.musicShelfContinuation?.contents
            ?.mapNotNull {
                SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
            } ?: emptyList()
        SearchResult(
            items = items,
            continuation = if (items.isEmpty()) null else response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()
        )
    }

    suspend fun player(videoId: String, playlistId: String? = null, client: YouTubeClient, signatureTimestamp: Int? = null, poToken: String? = null): Result<PlayerResponse> = runCatching {
        innerTube.player(client, videoId, playlistId, signatureTimestamp, poToken).body<PlayerResponse>()
    }

    suspend fun visitorData(): Result<String> = runCatching {
        Json.parseToJsonElement(innerTube.getSwJsData().bodyAsText().substring(5))
            .jsonArray[0]
            .jsonArray[2]
            .jsonArray.first {
                (it as? JsonPrimitive)?.contentOrNull?.let { candidate ->
                    VISITOR_DATA_REGEX.containsMatchIn(candidate)
                } ?: false
            }
            .jsonPrimitive.content
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
        }
    }

    /**
     * An album's tracklist, in order.
     *
     * [browseId] is an album id such as "MPREb_xxxxxxxxx", which is what album search
     * results carry. Search alone cannot list an album's tracks, so this goes through
     * the browse endpoint.
     */
    suspend fun album(browseId: String): Result<BrowsePage> = runCatching {
        collection(browseId = browseId, id = browseId)
    }

    /**
     * A playlist's tracklist. Accepts either a bare playlist id ("PL...", "OLAK5uy_...")
     * or one that already carries the "VL" browse prefix.
     */
    suspend fun playlist(playlistId: String): Result<BrowsePage> = runCatching {
        collection(
            browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId",
            id = playlistId.removePrefix("VL"),
        )
    }

    /**
     * An artist's page: their picture, their releases, and their full song list.
     *
     * [channelId] is a YouTube channel id ("UC..."), which artist search results carry.
     * The page's own songs shelf shows only about five tracks, so the playlist it links
     * to is followed for the complete list.
     */
    suspend fun artist(channelId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = channelId).body<BrowseResponse>()
        val page = BrowseParser.parseArtist(channelId, response)
            ?: error("Artist $channelId could not be read")

        val playlistId = page.songsPlaylistId ?: return@runCatching page
        val full = runCatching { collection("VL$playlistId", playlistId) }.getOrNull()
        if (full != null && full.songs.size > page.songs.size) {
            page.copy(songs = full.songs)
        } else {
            page
        }
    }

    /** Upper bound on how many tracks one collection may load, to keep paging sane. */
    private const val MAX_COLLECTION_SONGS = 500

    /**
     * Fetches an album or playlist page and keeps following continuations until the
     * tracklist is complete. YouTube serves long playlists a hundred tracks at a time, so
     * without the follow-up requests a big playlist simply stopped at a hundred.
     */
    private suspend fun collection(browseId: String, id: String): BrowsePage {
        val first = innerTube.browse(WEB_REMIX, browseId = browseId).body<BrowseResponse>()
        val page = BrowseParser.parse(id, first) ?: error("$browseId has no tracklist")

        val songs = page.songs.toMutableList()
        var token = page.continuation
        var pages = 0
        while (token != null && songs.size < MAX_COLLECTION_SONGS && pages < 8) {
            pages++
            val next = innerTube.browse(WEB_REMIX, continuation = token).body<BrowseResponse>()
            val (more, nextToken) = BrowseParser.parseContinuation(next, page)
            if (more.isEmpty()) break
            songs += more
            token = nextToken
        }

        return page.copy(
            songs = songs.distinctBy { it.id }.take(MAX_COLLECTION_SONGS),
            continuation = null,
        )
    }

    private val VISITOR_DATA_REGEX = Regex("^Cg[t|s]")

    fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>> {
        return NewPipeExtractor.newPipePlayer(videoId)
    }

    suspend fun newPipePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        }

        val streamsList = getNewPipeStreamUrls(videoId)
        if (streamsList.isEmpty()) return null

        val decodedSigResponse = tempRes.copy(
            streamingData = tempRes.streamingData?.copy(
                formats = tempRes.streamingData.formats?.map { format ->
                    format.copy(
                        url = streamsList.find { it.first == format.itag }?.second ?: format.url,
                    )
                },
                adaptiveFormats = tempRes.streamingData.adaptiveFormats.map { adaptiveFormat ->
                    adaptiveFormat.copy(
                        url = streamsList.find { it.first == adaptiveFormat.itag }?.second ?: adaptiveFormat.url,
                    )
                },
            ),
        )

        val urlList = (
            decodedSigResponse.streamingData?.adaptiveFormats?.mapNotNull { it.url }?.toMutableList() ?: mutableListOf()
        ).apply {
            decodedSigResponse.streamingData?.formats?.mapNotNull { it.url }?.let { addAll(it) }
        }

        return if (urlList.isNotEmpty()) {
            decodedSigResponse
        } else {
            null
        }
    }
}
