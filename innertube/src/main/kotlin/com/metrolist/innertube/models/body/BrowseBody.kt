package com.metrolist.innertube.models.body

import com.metrolist.innertube.models.Context
import kotlinx.serialization.Serializable

/**
 * Body for the InnerTube "browse" endpoint.
 *
 * The app uses it for album and playlist pages: an album's browseId looks like
 * "MPREb_xxxxxxxx" and a playlist's is its id with a "VL" prefix. Both return the
 * real, ordered tracklist, which a plain search cannot give.
 */
@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String? = null,
)
