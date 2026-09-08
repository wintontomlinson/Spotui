package com.metrolist.innertube.models.response

import com.metrolist.innertube.models.SectionListRenderer
import com.metrolist.innertube.models.Tabs
import kotlinx.serialization.Serializable

/**
 * Response of the InnerTube "browse" endpoint for album and playlist pages.
 *
 * WEB_REMIX answers with a two column layout: the left column ([Contents.twoColumnBrowseResultsRenderer]
 * tabs) holds the header, and the right column ([TwoColumnBrowseResults.secondaryContents]) holds
 * the tracklist shelf. Older or narrower clients answer with a single column instead, so both
 * shapes are modelled and the parser takes whichever is present.
 */
@Serializable
data class BrowseResponse(
    val contents: Contents?,
    /** An artist page puts its name and picture here, outside [contents]. */
    val header: Header? = null,
    /**
     * Follow-up pages arrive here rather than under [contents]. A playlist page returns
     * only its first hundred tracks, and the rest come back as appended items.
     */
    val onResponseReceivedActions: List<ResponseAction>? = null,
) {
    @Serializable
    data class ResponseAction(
        val appendContinuationItemsAction: AppendContinuationItemsAction?,
    ) {
        @Serializable
        data class AppendContinuationItemsAction(
            val continuationItems: List<com.metrolist.innertube.models.MusicShelfRenderer.Content>?,
        )
    }

    @Serializable
    data class Contents(
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResults?,
        val singleColumnBrowseResultsRenderer: Tabs?,
    )

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: com.metrolist.innertube.models.Runs?,
            val thumbnail: com.metrolist.innertube.models.ThumbnailRenderer?,
        )
    }

    @Serializable
    data class TwoColumnBrowseResults(
        val tabs: List<Tabs.Tab>?,
        val secondaryContents: SecondaryContents?,
    ) {
        @Serializable
        data class SecondaryContents(
            val sectionListRenderer: SectionListRenderer?,
        )
    }
}
