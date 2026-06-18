package com.example.l33t_h4ck3r_android

import android.content.Context
import android.net.Uri

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("l33t_h4ck3r_prefs", Context.MODE_PRIVATE)

    fun getLastFolderUri(): Uri? = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse)

    fun setLastFolderUri(uri: Uri?) {
        prefs.edit().putOrRemove(KEY_FOLDER_URI, uri?.toString()).apply()
    }

    fun getLastFileUri(): Uri? = prefs.getString(KEY_FILE_URI, null)?.let(Uri::parse)

    fun setLastFileUri(uri: Uri?) {
        prefs.edit().putOrRemove(KEY_FILE_URI, uri?.toString()).apply()
    }

    fun clearProject() {
        prefs.edit()
            .remove(KEY_FOLDER_URI)
            .remove(KEY_FILE_URI)
            .apply()
    }

    private fun android.content.SharedPreferences.Editor.putOrRemove(key: String, value: String?) =
        if (value == null) remove(key) else putString(key, value)

    private companion object {
        const val KEY_FOLDER_URI = "last_folder_uri"
        const val KEY_FILE_URI = "last_file_uri"
    }
}
