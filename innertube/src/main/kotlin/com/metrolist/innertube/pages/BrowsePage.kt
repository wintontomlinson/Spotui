package com.metrolist.innertube.pages

import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.MusicResponsiveHeaderRenderer
import com.metrolist.innertube.models.MusicResponsiveListItemRenderer
import com.metrolist.innertube.models.SectionListRenderer
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.getContinuation
import com.metrolist.innertube.models.oddElements
import com.metrolist.innertube.models.response.BrowseResponse
import com.metrolist.innertube.models.splitBySeparator
import com.metrolist.innertube.utils.parseTime

/**
 * An album or playlist page with its real, ordered tracklist.
 *
 * A search can tell you that an album exists but not what is on it, which is why the
 * app previously showed empty album screens. This is the parsed result of the browse
 * endpoint, which does carry the tracks.
 */
data class BrowsePage(
    val id: String,
    val title: String,
    val subtitle: String,
    val artist: String,
    val artistId: String?,
    val year: Int?,
    val thumbnail: String?,
    val songs: List<SongItem>,
    /** Token for the next page of tracks, or null when the tracklist is complete. */
    val continuation: String? = null,
)

/**
 * An artist page: who they are, their songs, and their releases.
 *
 * [songsPlaylistId] is the playlist the page links behind its "Top songs" shelf. The
 * shelf itself only shows a handful, and that handful is all the app used to be able to
 * offer, which is why an artist looked like they had five songs. Following the link gives
 * the full list.
 */
data class ArtistPage(
    val id: String,
    val name: String,
    /**
     * The artist's wide header picture. It is a banner, roughly two and a half to one, so
     * it must not be squared off into an avatar: doing that pads it into a square and
     * leaves bars down the sides.
     */
    val banner: String?,
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val songsPlaylistId: String?,
)

object BrowseParser {

    /**
     * Parses an album or playlist browse response.
     *
     * WEB_REMIX replies with a two column layout, where the header sits in the tab and
     * the tracklist sits in the secondary column. Older shapes put both in a single
     * column, so every section list in the response is scanned for each piece.
     */
    fun parse(id: String, response: BrowseResponse): BrowsePage? {
        val sections = collectSections(response)
        if (sections.isEmpty()) return null

        val header = sections.firstNotNullOfOrNull { it.musicResponsiveHeaderRenderer }

        val title = header?.title?.runs?.joinToString("") { it.text }.orEmpty()
        // Sized for a large cover on a phone, rather than whichever variant happens to be
        // listed last.
        val thumbnail = header?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(720)
            ?: header?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl(720)

        // An album puts its artist on the strapline; a playlist puts its author in the
        // subtitle, after the "Playlist" type label.
        val straplineRun = header?.straplineTextOne?.runs?.firstOrNull { it.text.isNotBlank() }
        val subtitleParts = header?.subtitle?.runs?.splitBySeparator().orEmpty()
        val subtitleAuthor = subtitleParts
            .drop(1)
            .firstOrNull { part -> part.any { it.navigationEndpoint?.browseEndpoint != null } }
            ?.firstOrNull()

        val artistRun = straplineRun ?: subtitleAuthor
        val artist = artistRun?.text.orEmpty()
        val artistId = artistRun?.navigationEndpoint?.browseEndpoint?.browseId

        // Only accept a plausible release year. Playlist subtitles end in all sorts of
        // things, and a stray number should not be shown as a year.
        val year = subtitleParts.lastOrNull()
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?.takeIf { it.length == 4 }
            ?.toIntOrNull()
            ?.takeIf { it in 1900..2100 }
        val subtitle = header?.subtitle?.runs?.joinToString("") { it.text }.orEmpty()

        // Albums come back in a plain music shelf, playlists in a playlist shelf.
        val trackContents = sections.flatMap { section ->
            section.musicPlaylistShelfRenderer?.contents.orEmpty() +
                section.musicShelfRenderer?.contents.orEmpty()
        }

        val songs = trackContents
            .mapNotNull { it.musicResponsiveListItemRenderer }
            .mapNotNull { renderer ->
                toSongItem(
                    renderer = renderer,
                    fallbackThumbnail = thumbnail,
                    fallbackArtist = artist,
                    collectionTitle = title,
                    collectionId = id,
                )
            }
            .distinctBy { it.id }

        if (title.isBlank() && songs.isEmpty()) return null

        return BrowsePage(
            id = id,
            title = title,
            subtitle = subtitle,
            artist = artist,
            artistId = artistId,
            year = year,
            thumbnail = thumbnail,
            songs = songs,
            continuation = trackContents.getContinuation(),
        )
    }

    /**
     * Parses a follow-up page of tracks. Long playlists are served a hundred at a time,
     * and without this the tracklist simply stopped at the first hundred.
     */
    fun parseContinuation(response: BrowseResponse, page: BrowsePage): Pair<List<SongItem>, String?> {
        val items = response.onResponseReceivedActions
            ?.mapNotNull { it.appendContinuationItemsAction?.continuationItems }
            ?.flatten()
            .orEmpty()

        val songs = items
            .mapNotNull { it.musicResponsiveListItemRenderer }
            .mapNotNull { renderer ->
                toSongItem(
                    renderer = renderer,
                    fallbackThumbnail = page.thumbnail,
                    fallbackArtist = page.artist,
                    collectionTitle = page.title,
                    collectionId = page.id,
                )
            }

        return songs to items.getContinuation()
    }

    /** Parses an artist channel page. */
    fun parseArtist(channelId: String, response: BrowseResponse): ArtistPage? {
        val sections = collectSections(response)
        val header = response.header?.musicImmersiveHeaderRenderer

        val name = header?.title?.runs?.joinToString("") { it.text }.orEmpty()
        // Sized for a phone-width header rather than taking the largest on offer, which
        // for an artist banner is around 2456 pixels wide.
        val banner = header?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(1280)
            ?: header?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl(1280)
        val thumbnail = banner

        // The songs shelf, plus the playlist it links to for the complete list.
        val songShelf = sections.firstNotNullOfOrNull { it.musicShelfRenderer }
        val songsPlaylistId = songShelf?.bottomEndpoint?.browseEndpoint?.browseId
            ?.takeIf { it.startsWith("VL") }
            ?.removePrefix("VL")

        val songs = songShelf?.contents
            ?.mapNotNull { it.musicResponsiveListItemRenderer }
            ?.mapNotNull { renderer ->
                toSongItem(
                    renderer = renderer,
                    fallbackThumbnail = thumbnail,
                    fallbackArtist = name,
                    collectionTitle = "",
                    collectionId = "",
                )
            }
            .orEmpty()
            .distinctBy { it.id }

        // Albums and singles come back as carousels of two row cards.
        val albums = sections
            .mapNotNull { it.musicCarouselShelfRenderer }
            .flatMap { carousel ->
                val label = carousel.header
                    ?.musicCarouselShelfBasicHeaderRenderer
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                carousel.contents.mapNotNull { content ->
                    val item = content.musicTwoRowItemRenderer ?: return@mapNotNull null
                    if (!item.isAlbum) return@mapNotNull null
                    val browseId = item.navigationEndpoint.browseEndpoint?.browseId
                        ?: return@mapNotNull null
                    AlbumItem(
                        browseId = browseId,
                        // Only the album page knows the playlist id, and nothing here
                        // needs it, so it stays empty rather than being invented.
                        playlistId = "",
                        title = item.title.runs?.joinToString("") { it.text }.orEmpty(),
                        artists = null,
                        year = item.subtitle?.runs
                            ?.lastOrNull()
                            ?.text
                            ?.trim()
                            ?.takeIf { it.length == 4 }
                            ?.toIntOrNull()
                            ?.takeIf { it in 1900..2100 },
                        type = label,
                        thumbnail = item.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl(544)
                            ?: item.thumbnailRenderer.croppedSquareThumbnailRenderer?.getThumbnailUrl(544)
                            ?: return@mapNotNull null,
                    )
                }
            }
            .distinctBy { it.browseId }

        if (name.isBlank() && songs.isEmpty() && albums.isEmpty()) return null

        return ArtistPage(
            id = channelId,
            name = name,
            banner = banner,
            songs = songs,
            albums = albums,
            songsPlaylistId = songsPlaylistId,
        )
    }

    private fun collectSections(response: BrowseResponse): List<SectionListRenderer.Content> {
        val contents = response.contents ?: return emptyList()
        val out = mutableListOf<SectionListRenderer.Content>()

        contents.twoColumnBrowseResultsRenderer?.let { two ->
            two.tabs
                ?.mapNotNull { it.tabRenderer.content?.sectionListRenderer?.contents }
                ?.forEach { out += it }
            two.secondaryContents?.sectionListRenderer?.contents?.let { out += it }
        }
        contents.singleColumnBrowseResultsRenderer?.tabs
            ?.mapNotNull { it.tabRenderer.content?.sectionListRenderer?.contents }
            ?.forEach { out += it }

        return out
    }

    /**
     * Maps one tracklist row.
     *
     * This cannot reuse the search mapper: album rows carry no per track thumbnail (the
     * cover is shown once in the header) and the search mapper reads the duration from
     * the last flex column, which on an album page holds the play count instead. Here
     * the duration comes from the fixed column, which is where the page really puts it.
     */
    private fun toSongItem(
        renderer: MusicResponsiveListItemRenderer,
        fallbackThumbnail: String?,
        fallbackArtist: String,
        collectionTitle: String,
        collectionId: String,
    ): SongItem? {
        val watch = renderer.navigationEndpoint?.anyWatchEndpoint
            ?: renderer.overlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.anyWatchEndpoint
        val videoId = renderer.playlistItemData?.videoId ?: watch?.videoId ?: return null

        val title = renderer.flexColumns
            .firstOrNull()
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.firstOrNull()
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val artistRuns = renderer.flexColumns
            .getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.splitBySeparator()
            ?.firstOrNull()
            ?.oddElements()
            .orEmpty()
        val artists = artistRuns
            .filter { it.text.isNotBlank() }
            .map { Artist(name = it.text.trim(), id = it.navigationEndpoint?.browseEndpoint?.browseId) }
            .ifEmpty {
                if (fallbackArtist.isBlank()) emptyList() else listOf(Artist(fallbackArtist, null))
            }

        val duration = renderer.fixedColumns
            ?.firstNotNullOfOrNull { column ->
                column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                    ?.firstOrNull()?.text?.parseTime()
            }

        val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
            ?: fallbackThumbnail
            ?: return null

        return SongItem(
            id = videoId,
            title = title,
            artists = artists,
            album = if (collectionTitle.isBlank()) null else Album(collectionTitle, collectionId),
            duration = duration,
            musicVideoType = renderer.musicVideoType,
            thumbnail = thumbnail,
            explicit = renderer.badges?.any {
                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
            } == true,
            endpoint = watch?.let { WatchEndpoint(videoId = videoId, playlistId = it.playlistId) },
            setVideoId = renderer.playlistItemData?.playlistSetVideoId,
        )
    }
}
