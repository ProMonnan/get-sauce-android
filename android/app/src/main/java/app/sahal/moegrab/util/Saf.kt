package app.sahal.moegrab.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Copies a file from app-private staging into the user-chosen SAF tree.
 * Returns the destination content Uri as a string, or null on failure.
 *
 * Why not just download straight into the tree? Because the Go bridge writes
 * with plain `os.OpenFile` and doesn't know about ContentResolver. Staging in
 * cacheDir keeps that clean and lets us stream progress via a normal filesystem.
 */
object SafCopy {
    fun copyInto(
        ctx: Context,
        treeUri: Uri,
        source: File,
        displayName: String,
    ): String? {
        val cr = ctx.contentResolver
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return null
        // If a file with this name already exists, delete it first — the CLI
        // does the equivalent when -t is set.
        root.findFile(displayName)?.delete()

        val mime = mimeFor(source.extension)
        val doc = root.createFile(mime, displayName) ?: return null
        val outStream = cr.openOutputStream(doc.uri) ?: return null

        outStream.use { out ->
            source.inputStream().use { it.copyTo(out, DEFAULT_BUFFER_SIZE) }
        }
        return doc.uri.toString()
    }

    private fun mimeFor(ext: String): String {
        val m = MimeTypeMap.getSingleton()
        return m.getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }

    private const val DEFAULT_BUFFER_SIZE = 64 * 1024
}

/**
 * Persist read/write access to a tree URI across app restarts. Call this from
 * the ActivityResult callback of ACTION_OPEN_DOCUMENT_TREE before saving the
 * URI into DataStore.
 */
fun takePersistablePermissions(cr: ContentResolver, uri: Uri) {
    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    cr.takePersistableUriPermission(uri, flags)
}
