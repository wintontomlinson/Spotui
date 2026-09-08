package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

/**
 * Header of an album or playlist page.
 *
 * Shape for an album:
 *   title            -> "Arijit Singh (All Time Hits)"
 *   straplineTextOne -> the album artist, carrying a browseEndpoint to the artist
 *   subtitle         -> "Album • 2023"
 *   secondSubtitle   -> "12 songs • 53 minutes"
 *   thumbnail        -> the cover art
 *
 * A playlist page uses the same renderer, with the author in [subtitle] instead of
 * [straplineTextOne].
 */
@Serializable
data class MusicResponsiveHeaderRenderer(
    val thumbnail: ThumbnailRenderer?,
    val title: Runs?,
    val subtitle: Runs?,
    val secondSubtitle: Runs?,
    val straplineTextOne: Runs?,
    val straplineThumbnail: ThumbnailRenderer?,
)
