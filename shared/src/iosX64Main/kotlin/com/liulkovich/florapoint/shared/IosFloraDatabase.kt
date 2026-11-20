
package com.liulkovich.florapoint.shared

import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import com.liulkovich.florapoint.shared.domain.SharedSpeciesConditions
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun floraDatabasePath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    val path = requireNotNull(directory?.path) {
        "Не удалось найти Application Support"
    }

    return "$path/flora.db"
}

fun countFloraSpecies(): Int {
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            "SELECT COUNT(*) FROM reference_table"
        ).use { statement ->
            return if (statement.step()) {
                statement.getInt(0)
            } else {
                0
            }
        }
    }
}

private fun SQLiteStatement.nullableText(index: Int): String? {
    return if (isNull(index)) null else getText(index)
}

fun loadFloraSpecies(): List<ReferenceModel> {
    val species = mutableListOf<ReferenceModel>()
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            SELECT
                id,
                category,
                name_ru,
                name_en,
                habitat_ru,
                habitat_en,
                look_alikes_ru,
                look_alikes_en,
                description_ru,
                description_en,
                start_month,
                end_month,
                image_name,
                is_notif_enabled,
                is_reference_only,
                differences_ru,
                differences_en
            FROM reference_table
            ORDER BY id
            """.trimIndent()
        ).use { statement ->

            while (statement.step()) {
                val item = ReferenceModel(
                    id = statement.getInt(0),
                    category = statement.getText(1),

                    nameRu = statement.getText(2),
                    nameEn = statement.nullableText(3),

                    habitatRu = statement.getText(4),
                    habitatEn = statement.nullableText(5),

                    lookAlikesRu = statement.getText(6),
                    lookAlikesEn = statement.nullableText(7),

                    descriptionRu = statement.getText(8),
                    descriptionEn = statement.nullableText(9),

                    startMonth = statement.getInt(10),
                    endMonth = statement.getInt(11),

                    imageName = statement.getText(12),

                    isNotifEnabled = statement.getInt(13),
                    isReferenceOnly = statement.getInt(14),

                    differencesRu = statement.nullableText(15),
                    differencesEn = statement.nullableText(16)
                )

                species.add(item)
            }
        }
    }

    return species
}

fun loadRandomFloraTip(language: String = "ru"): String? {
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            SELECT text_ru, text_en
            FROM tips
            ORDER BY RANDOM()
            LIMIT 1
            """.trimIndent()
        ).use { statement ->

            if (!statement.step()) {
                return null
            }

            val textRu = statement.getText(0)
            val textEn = if (statement.isNull(1)) {
                null
            } else {
                statement.getText(1)
            }

            return if (language == "en") {
                textEn?.takeIf { it.isNotBlank() } ?: textRu
            } else {
                textRu
            }
        }
    }
}


fun updateFloraNotification(
    speciesId: Int,
    enabled: Boolean
) {
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            UPDATE reference_table
            SET is_notif_enabled = ?
            WHERE id = ?
            """.trimIndent()
        ).use { statement ->
            statement.bindLong(
                1,
                if (enabled) 1L else 0L
            )
            statement.bindLong(2, speciesId.toLong())
            statement.step()
        }
    }
}


fun loadFloraConditions(): List<SharedSpeciesConditions> {
    val conditions = mutableListOf<SharedSpeciesConditions>()
    val driver = BundledSQLiteDriver()

    driver.open(floraDatabasePath()).use { connection ->
        connection.prepare(
            """
            SELECT
                species_id,
                temp_min,
                temp_max,
                humidity_min,
                humidity_max
            FROM species_conditions
            ORDER BY species_id
            """.trimIndent()
        ).use { statement ->

            while (statement.step()) {
                conditions.add(
                    SharedSpeciesConditions(
                        speciesId = statement.getInt(0),
                        tempMin = statement.getInt(1),
                        tempMax = statement.getInt(2),
                        humidityMin = statement.getInt(3),
                        humidityMax = statement.getInt(4)
                    )
                )
            }
        }
    }

    return conditions
}