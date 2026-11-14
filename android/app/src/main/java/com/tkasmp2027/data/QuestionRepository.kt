package com.tkasmp2027.data

import android.content.Context
import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.QuestionItem
import com.tkasmp2027.model.QuestionOption
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionStatement
import com.tkasmp2027.model.QuestionStatementChoice
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.SubjectId
import org.json.JSONArray
import org.json.JSONObject

object PackageIds {
    const val MATEMATIKA_PAKET_1 = "matematika_paket_1"
    const val MATEMATIKA_PAKET_2 = "matematika_paket_2"
    const val BAHASA_INDONESIA_PAKET_1 = "bahasa_indonesia_paket_1"
    const val BAHASA_INDONESIA_PAKET_2 = "bahasa_indonesia_paket_2"

    val ORDERED = listOf(
        MATEMATIKA_PAKET_1,
        MATEMATIKA_PAKET_2,
        BAHASA_INDONESIA_PAKET_1,
        BAHASA_INDONESIA_PAKET_2
    )
}

class QuestionRepository(private val context: Context) {

    fun loadAllPackages(): List<QuestionPackage> {
        val jsonString = context.assets.open(MASTER_ASSET_FILE)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseMasterDatabaseJson(jsonString)
    }

    companion object {
        private const val MASTER_ASSET_FILE = "TKA_SMP_2027_All_Packages.json"

        private val OPTION_FIELDS = listOf(
            "A" to "opsi_a",
            "B" to "opsi_b",
            "C" to "opsi_c",
            "D" to "opsi_d",
            "E" to "opsi_e",
            "F" to "opsi_f"
        )

        fun parseMasterDatabaseJson(jsonString: String): List<QuestionPackage> {
            val root = JSONObject(jsonString)
            val version = root.optString("database_version", "1.0")
            val packagesArr = root.getJSONArray("packages")
            val byId = mutableMapOf<String, QuestionPackage>()

            for (i in 0 until packagesArr.length()) {
                val pkgObj = packagesArr.getJSONObject(i)
                val parsed = parsePackageObject(pkgObj, version)
                byId[parsed.packageId] = parsed
            }

            return PackageIds.ORDERED.map { pkgId ->
                byId[pkgId] ?: throw IllegalStateException("Paket resmi tidak ditemukan: $pkgId")
            }
        }

        private fun parsePackageObject(pkgObj: JSONObject, schemaVersion: String): QuestionPackage {
            val packageId = pkgObj.getString("package_id")
            val subjectIdRaw = pkgObj.optString("subject_id")
            val subjectId = if (subjectIdRaw == SubjectId.BAHASA_INDONESIA.key || packageId.startsWith("bahasa")) {
                SubjectId.BAHASA_INDONESIA
            } else {
                SubjectId.MATEMATIKA
            }

            val soalArr = pkgObj.optJSONArray("soal") ?: JSONArray()
            val questions = buildList {
                for (i in 0 until soalArr.length()) {
                    add(parseQuestion(soalArr.getJSONObject(i), i + 1))
                }
            }.sortedBy { it.number }

            val packageNumber = pkgObj.optInt("package_number", 1)
            val packageName = pkgObj.optString("package_name", "${subjectId.displayName} Paket $packageNumber")

            return QuestionPackage(
                packageId = packageId,
                subjectId = subjectId,
                subjectName = pkgObj.optString("subject_name").ifBlank { subjectId.displayName },
                title = "Paket $packageNumber",
                description = "$packageName · ${questions.size} soal resmi TKA SMP 2027.",
                expectedQuestionCount = pkgObj.optInt("question_count", questions.size),
                schemaVersion = schemaVersion,
                questions = questions
            )
        }

        private fun hasMeaningfulHtml(rawHtml: String?): Boolean {
            if (rawHtml.isNullOrBlank()) return false
            if (Regex("<(img|table|svg|math)\\b", RegexOption.IGNORE_CASE).containsMatchIn(rawHtml)) {
                return true
            }
            val stripped = rawHtml
                .replace(Regex("<\\?xml[\\s\\S]*?\\?>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ", ignoreCase = true)
                .replace("\u00a0", " ")
                .trim()
            return stripped.isNotEmpty()
        }

        private fun parseQuestion(obj: JSONObject, fallbackNumber: Int): QuestionItem {
            val id = obj.optStringOrNull("id_soal") ?: obj.optStringOrNull("id") ?: fallbackNumber.toString()
            val number = obj.optInt("no_soal", obj.optInt("number", fallbackNumber))
            val type = QuestionType.fromRaw(obj.optStringOrNull("jenis_soal") ?: obj.getString("type"))

            val rawSoal = obj.optStringOrNull("soal")
            val rawPertanyaan = obj.optStringOrNull("pertanyaan")
            val hasStimulus = hasMeaningfulHtml(rawSoal)
            val hasPertanyaan = hasMeaningfulHtml(rawPertanyaan)

            val stimulusHtml: String?
            val questionHtml: String
            if (hasPertanyaan) {
                stimulusHtml = if (hasStimulus) rawSoal else null
                questionHtml = rawPertanyaan ?: ""
            } else {
                stimulusHtml = null
                questionHtml = rawSoal ?: ""
            }

            val options = if (type == QuestionType.PG || type == QuestionType.PG_KOMPLEKS) {
                extractOptions(obj)
            } else emptyList()

            val statementChoices = if (type == QuestionType.BENAR_SALAH) {
                extractStatementChoices(obj)
            } else emptyList()

            val statements = if (type == QuestionType.BENAR_SALAH) {
                extractStatements(obj)
            } else emptyList()

            val answerKey = parseAnswerKey(obj, type, statements)

            return QuestionItem(
                id = id,
                number = number,
                type = type,
                stimulusHtml = stimulusHtml,
                questionHtml = questionHtml,
                options = options,
                statementChoices = statementChoices,
                statements = statements,
                answerKey = answerKey
            )
        }

        private fun extractOptions(obj: JSONObject): List<QuestionOption> {
            val declaredCount = obj.optInt("jumlah_opsi", OPTION_FIELDS.size).coerceIn(1, OPTION_FIELDS.size)
            return buildList {
                for (i in 0 until declaredCount) {
                    val (letter, field) = OPTION_FIELDS[i]
                    val value = obj.optStringOrNull(field)
                    if (hasMeaningfulHtml(value)) {
                        add(QuestionOption(id = letter, label = letter, html = value!!))
                    }
                }
            }
        }

        private fun extractStatementChoices(obj: JSONObject): List<QuestionStatementChoice> {
            val arr = obj.optJSONArray("cbt_soal_answer_judul")
            if (arr == null || arr.length() == 0) {
                return listOf(
                    QuestionStatementChoice(id = 1, order = 1, label = "Benar"),
                    QuestionStatementChoice(id = 2, order = 2, label = "Salah")
                )
            }
            return buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        QuestionStatementChoice(
                            id = item.optInt("id", i + 1),
                            order = item.optInt("order_soal", i + 1),
                            label = item.optString("description", "Pilihan ${i + 1}")
                        )
                    )
                }
            }.sortedBy { it.order }
        }

        private fun extractStatements(obj: JSONObject): List<QuestionStatement> {
            val arr = obj.optJSONArray("cbt_soal_answer_text") ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val key = ('A' + i).toString()
                    val judul = item.optString("judul", "Pernyataan").ifBlank { "Pernyataan" }
                    add(
                        QuestionStatement(
                            id = item.optStringOrNull("id") ?: key,
                            key = key,
                            label = "$judul $key",
                            html = item.optString("text", "")
                        )
                    )
                }
            }
        }

        private fun parseAnswerKey(
            obj: JSONObject,
            type: QuestionType,
            statements: List<QuestionStatement>
        ): AnswerKey? {
            val kunciEssai = obj.optStringOrNull("kunci_essai")?.trim() ?: return null
            return when (type) {
                QuestionType.PG -> AnswerKey.Pg(correctOptionId = kunciEssai.uppercase())
                QuestionType.PG_KOMPLEKS -> {
                    val ids = kunciEssai.split(";").mapNotNull { it.trim().uppercase().takeIf(String::isNotEmpty) }
                    AnswerKey.PgKompleks(correctOptionIds = ids)
                }
                QuestionType.BENAR_SALAH -> {
                    val map = buildMap {
                        kunciEssai.split(";").forEach { entry ->
                            val parts = entry.split("|")
                            if (parts.size == 2) {
                                val letter = parts[0].trim().uppercase()
                                val order = parts[1].trim().toIntOrNull()
                                val stmt = statements.firstOrNull { it.key == letter }
                                    ?: statements.getOrNull(letter.firstOrNull()?.minus('A') ?: -1)
                                if (stmt != null && order != null) {
                                    put(stmt.id, order)
                                }
                            }
                        }
                    }
                    if (map.isEmpty()) null else AnswerKey.BenarSalah(statementAnswers = map)
                }
            }
        }

        private fun JSONObject.optStringOrNull(key: String): String? {
            if (!has(key) || isNull(key)) return null
            val v = optString(key)
            return v.takeIf { it.isNotBlank() && it != "null" }
        }
    }
}
