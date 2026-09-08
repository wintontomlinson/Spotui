package com.music.spotui.data.entity

data class AlbumsModel(
    val id : Int,
    val artists : String,
    val coverUri : String,
    val name : String,
    val time : String,
    // Release kind from Spotify: "album", "single", "compilation"... Empty when unknown.
    val type : String = "",
    // Exact YouTube Music album id ("MPREb_..."), when it is known. Carried so opening
    // this album fetches this album, instead of resolving an ambiguous name by search.
    val browseId : String = ""
){
    constructor() : this( -1,"" ,"" ,"", "")
}
