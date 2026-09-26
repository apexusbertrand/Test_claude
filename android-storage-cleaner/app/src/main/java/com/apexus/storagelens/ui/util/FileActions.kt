package com.apexus.storagelens.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.apexus.storagelens.R
import java.io.File

object FileActions {
    fun open(context: Context, path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, R.string.error_file_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, R.string.error_cannot_open, Toast.LENGTH_SHORT).show()
            return
        }
        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(Intent.createChooser(intent, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    fun startSafely(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // On essaie l'intent suivant.
            }
        }
        Toast.makeText(context, R.string.error_settings_unavailable, Toast.LENGTH_SHORT).show()
    }
}
