package com.metrolist.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl() = thumbnail.thumbnails.lastOrNull()?.url

        /**
         * The smallest image that still covers [targetWidth] pixels, falling back to the
         * largest on offer.
         *
         * [getThumbnailUrl] always takes the last, which for a wide artist banner means a
         * 2456 pixel image downloaded to fill a phone-width header. Asking for the size
         * actually needed keeps the picture sharp without the waste.
         */
        fun getThumbnailUrl(targetWidth: Int): String? {
            val options = thumbnail.thumbnails
            if (options.isEmpty()) return null
            return options
                .filter { (it.width ?: 0) >= targetWidth }
                .minByOrNull { it.width ?: 0 }
                ?.url
                ?: options.maxByOrNull { it.width ?: 0 }?.url
        }
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
