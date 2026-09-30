package com.music.spotui.data.preferences

import android.content.Context

fun addLikedAlbumId(context: Context, albumId: String) {
    val sharedPreferences = context.getSharedPreferences("LikedAlbums", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString(albumId, albumId)
    editor.apply()
}

fun removeLikedAlbumId(context: Context, albumId: String) {
    val sharedPreferences = context.getSharedPreferences("LikedAlbums", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(albumId)
    editor.apply()
}

fun isAlbumLiked(context: Context, albumId: String): Boolean {
    val sharedPreferences = context.getSharedPreferences("LikedAlbums", Context.MODE_PRIVATE)
    return sharedPreferences.contains(albumId)
}


