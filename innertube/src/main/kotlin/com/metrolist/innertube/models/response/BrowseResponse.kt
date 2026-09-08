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
) {
    @Serializable
    data class Contents(
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResults?,
        val singleColumnBrowseResultsRenderer: Tabs?,
    )

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
