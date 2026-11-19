package com.detflix.admin.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Exposes read-only list of revoked access codes and suspended client names
 * so DETFLIX APP on the same device can immediately close sessions even if
 * either app was closed or offline when the code was revoked.
 */
class AdminRevocationProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("entry_type", "entry_value"))
        val ctx = context ?: return cursor
        runCatching {
            val db = AdminDatabase.getInstance(ctx).openHelper.readableDatabase
            db.query("SELECT code FROM admin_codes WHERE status = 'REVOKED'").use { c ->
                while (c.moveToNext()) {
                    cursor.addRow(arrayOf("CODE", c.getString(0)))
                }
            }
            db.query("SELECT name FROM admin_clients WHERE status = 'SUSPENDED'").use { c ->
                while (c.moveToNext()) {
                    cursor.addRow(arrayOf("CLIENT", c.getString(0)))
                }
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.detflix.admin.revoked"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
