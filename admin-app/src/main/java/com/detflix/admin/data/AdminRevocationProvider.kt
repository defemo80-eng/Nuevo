package com.detflix.admin.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Exposes read-only IPC endpoints for DETFLIX APP on the same device:
 * - /revoked: list of REVOKED access codes and SUSPENDED client names
 * - /catalog: list of available custom catalog items published in DETFLIX Admin
 * - /episodes: list of custom episodes/chapters for SERIES and NOVELA items
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
        val ctx = context ?: return MatrixCursor(arrayOf("entry_type", "entry_value"))
        if (uri.lastPathSegment == "episodes") {
            val cursor = MatrixCursor(
                arrayOf(
                    "id",
                    "seriesId",
                    "seasonNumber",
                    "episodeNumber",
                    "title",
                    "description",
                    "thumbnailUrl",
                    "streamUrl",
                    "durationMinutes"
                )
            )
            runCatching {
                val db = AdminDatabase.getInstance(ctx).openHelper.readableDatabase
                db.query(
                    """
                    SELECT e.id, e.seriesId, e.seasonNumber, e.episodeNumber, e.title, e.description, e.thumbnailUrl, e.streamUrl, e.durationMinutes
                    FROM admin_episodes e
                    INNER JOIN admin_contents c ON c.id = e.seriesId
                    WHERE c.available = 1
                    ORDER BY e.seasonNumber ASC, e.episodeNumber ASC
                    """.trimIndent()
                ).use { c ->
                    while (c.moveToNext()) {
                        cursor.addRow(
                            arrayOf<Any?>(
                                c.getString(0),
                                c.getString(1),
                                c.getInt(2),
                                c.getInt(3),
                                c.getString(4),
                                c.getString(5),
                                c.getString(6),
                                c.getString(7),
                                c.getInt(8)
                            )
                        )
                    }
                }
            }
            return cursor
        }

        if (uri.lastPathSegment == "catalog") {
            val cursor = MatrixCursor(
                arrayOf(
                    "id",
                    "title",
                    "type",
                    "categories",
                    "year",
                    "posterUrl",
                    "streamUrl",
                    "description",
                    "featured",
                    "updatedAtMillis"
                )
            )
            runCatching {
                val db = AdminDatabase.getInstance(ctx).openHelper.readableDatabase
                db.query(
                    "SELECT id, title, type, categories, year, posterUrl, streamUrl, description, featured, updatedAtMillis FROM admin_contents WHERE available = 1"
                ).use { c ->
                    while (c.moveToNext()) {
                        cursor.addRow(
                            arrayOf<Any?>(
                                c.getString(0),
                                c.getString(1),
                                c.getString(2),
                                c.getString(3),
                                c.getInt(4),
                                c.getString(5),
                                c.getString(6),
                                c.getString(7),
                                c.getInt(8),
                                c.getLong(9)
                            )
                        )
                    }
                }
            }
            return cursor
        }

        val cursor = MatrixCursor(arrayOf("entry_type", "entry_value"))
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

    override fun getType(uri: Uri): String = when (uri.lastPathSegment) {
        "catalog" -> "vnd.android.cursor.dir/vnd.detflix.admin.catalog"
        "episodes" -> "vnd.android.cursor.dir/vnd.detflix.admin.episodes"
        else -> "vnd.android.cursor.dir/vnd.detflix.admin.revoked"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
