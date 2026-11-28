package com.liulkovich.florapoint.shared

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liulkovich.florapoint.shared.domain.SharedUserPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

/** Native iOS share sheet and document picker, implemented in IosGpxTransfer.swift. */
interface IosGpxTransport {
    fun shareGpx(gpxXml: String)
    fun pickGpx(listener: IosGpxImportListener)
}

/** JSON is a transport-only representation of GPX waypoints, not a backup format. */
interface IosGpxImportListener {
    fun onGpxImportResult(json: String?, error: String?)
}

private fun gpxEscape(s: String) = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

/** GPX 1.1 compatible with ForestPoint Android's GpxExporter. */
fun exportIosPointsAsGpx(): String {
    val points = loadIosUserPoints()
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<gpx version=\"1.1\" creator=\"ForestPoint\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
        for (point in points) {
            val time = Instant.fromEpochSeconds(point.timestamp).toString()
            appendLine("  <wpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            appendLine("    <name>${gpxEscape(point.userName)}</name>")
            appendLine("    <desc>${gpxEscape(point.description)}</desc>")
            appendLine("    <time>$time</time>")
            appendLine("    <extensions>")
            appendLine("      <category>${gpxEscape(point.category ?: "custom")}</category>")
            appendLine("    </extensions>")
            appendLine("  </wpt>")
        }
        appendLine("</gpx>")
    }
}

data class IosGpxImportSummary(val inserted: Int, val duplicates: Int)

/** Import is atomic: malformed data causes no inserts; duplicates are left untouched. */
fun importIosPointsFromGpxJson(json: String): IosGpxImportSummary {
    val rows = Json.parseToJsonElement(json).jsonArray
    require(rows.isNotEmpty()) { "GPX does not contain any waypoints" }
    require(rows.size <= 5000) { "GPX has too many waypoints (max 5000)" }

    val items = rows.map { element ->
        val data = element.jsonObject
        val lat = requireNotNull(data["latitude"]?.jsonPrimitive?.doubleOrNull) { "Missing latitude" }
        val lon = requireNotNull(data["longitude"]?.jsonPrimitive?.doubleOrNull) { "Missing longitude" }
        require(lat.isFinite() && lat in -90.0..90.0) { "Invalid latitude" }
        require(lon.isFinite() && lon in -180.0..180.0) { "Invalid longitude" }
        val name = data["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            .ifBlank { "Point" }
        require(name.length <= 1000) { "Waypoint name is too long" }
        val desc = data["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
        require(desc.length <= 100_000) { "Waypoint description is too long" }
        val category = data["category"]?.jsonPrimitive?.contentOrNull
            ?.trim()?.takeIf { it.isNotBlank() } ?: "custom"
        require(category.length <= 100) { "Category is too long" }
        val timestamp = requireNotNull(data["timestamp"]?.jsonPrimitive?.longOrNull) {
            "Missing timestamp"
        }
        require(timestamp > 0) { "Invalid timestamp" }
        SharedUserPoint(
            latitude = lat,
            longitude = lon,
            userName = name,
            description = desc,
            timestamp = timestamp,
            category = category
        )
    }

    var inserted = 0
    var duplicates = 0
    BundledSQLiteDriver().open(floraDatabasePath()).use { db ->
        db.prepare("BEGIN IMMEDIATE TRANSACTION").use { it.step() }
        try {
            for (point in items) {
                val exists = db.prepare(
                    """SELECT 1 FROM user_points_table
                       WHERE latitude = ? AND longitude = ?
                         AND user_name = ? AND timestamp = ? LIMIT 1"""
                ).use { s ->
                    s.bindDouble(1, point.latitude)
                    s.bindDouble(2, point.longitude)
                    s.bindText(3, point.userName)
                    s.bindLong(4, point.timestamp)
                    s.step()
                }
                if (exists) {
                    duplicates++
                    continue
                }
                db.prepare(
                    """INSERT INTO user_points_table (
                         species_id, latitude, longitude, user_name, description,
                         timestamp, is_favorite, photo_path, accuracy, category,
                         is_public, owner_uid, cloud_id, sync_state
                       ) VALUES (NULL, ?, ?, ?, ?, ?, 0, '', 0, ?, 0, NULL, NULL, 'LOCAL')"""
                ).use { s ->
                    s.bindDouble(1, point.latitude)
                    s.bindDouble(2, point.longitude)
                    s.bindText(3, point.userName)
                    s.bindText(4, point.description)
                    s.bindLong(5, point.timestamp)
                    s.bindText(6, point.category ?: "custom")
                    s.step()
                }
                inserted++
            }
            db.prepare("COMMIT").use { it.step() }
        } catch (e: Throwable) {
            runCatching { db.prepare("ROLLBACK").use { it.step() } }
            throw e
        }
    }
    return IosGpxImportSummary(inserted = inserted, duplicates = duplicates)
}
