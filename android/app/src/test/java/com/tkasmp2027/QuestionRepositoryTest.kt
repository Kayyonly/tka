package com.tkasmp2027

import com.tkasmp2027.data.PackageIds
import com.tkasmp2027.data.QuestionRepository
import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.QuestionMetadata
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.SubjectId
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuestionRepositoryTest {

    @Test
    fun loadsV2DatabaseWithoutChangingReferenceQuestions() {
        val oldDatabase = JSONObject(
            File("src/main/assets/TKA_SMP_2027_All_Packages.json").readText()
        )
        val v2Database = JSONObject(
            File("src/main/assets/TKA_SMP_2027_All_Packages_v2_Draft.json").readText()
        )
        val packages = QuestionRepository.parseMasterDatabaseJson(v2Database.toString())

        assertEquals(PackageIds.ORDERED, packages.map { it.packageId })
        assertEquals(8, packages.size)
        assertEquals(125, packages.sumOf { it.questions.size })

        val oldPackages = packages.filter { it.bankType == "reference" }
        val originalPackages = packages.filter { it.bankType == "original" }
        assertEquals(4, oldPackages.size)
        assertEquals(115, oldPackages.sumOf { it.questions.size })
        assertEquals(4, originalPackages.size)

        val oldById = oldDatabase.getJSONArray("packages").toPackageObjects().associateBy {
            it.getString("package_id")
        }
        oldPackages.forEach { pkg ->
            val v2Package = v2Database.getJSONArray("packages").toPackageObjects()
                .single { it.getString("package_id") == pkg.packageId }
            assertEquals(
                normalize(oldById.getValue(pkg.packageId).getJSONArray("soal")),
                normalize(v2Package.getJSONArray("soal"))
            )
            assertTrue(pkg.title.startsWith("Referensi · Paket "))
            assertTrue(pkg.description.contains("bukan bank orisinal utama"))
            assertTrue(pkg.questions.all { it.metadata == null })
        }

        val pilot = originalPackages.flatMap { it.questions }
        assertEquals(10, pilot.size)
        assertEquals(5, originalPackages.filter { it.subjectId == SubjectId.MATEMATIKA }
            .sumOf { it.questions.size })
        assertEquals(5, originalPackages.filter { it.subjectId == SubjectId.BAHASA_INDONESIA }
            .sumOf { it.questions.size })
        assertEquals(7, pilot.count { it.type == QuestionType.PG })
        assertEquals(3, pilot.count { it.type == QuestionType.PG_KOMPLEKS })
        assertTrue(pilot.all { it.answerKey != null && !it.explanationHtml.isNullOrBlank() })
        val pilotSourceQuestions = v2Database.getJSONArray("packages").toPackageObjects()
            .filter { it.optString("bank_type") == "original" }
            .flatMap { it.getJSONArray("soal").toPackageObjects() }
            .associateBy { it.getString("id_soal") }
        pilot.forEach { question ->
            val source = pilotSourceQuestions.getValue(question.id)
            assertEquals(source.getString("explanation_html"), question.explanationHtml)
            val sourceKey = source.getString("kunci_essai").trim().uppercase()
            when (val answerKey = question.answerKey) {
                is AnswerKey.Pg -> assertEquals(sourceKey, answerKey.correctOptionId)
                is AnswerKey.PgKompleks -> assertEquals(
                    sourceKey.split(";").map(String::trim),
                    answerKey.correctOptionIds
                )
                else -> throw AssertionError("Kunci pilot tidak terbaca untuk ${question.id}")
            }
            assertEquals(source.optStringOrNull("kompetensi"), question.competency)
            assertEquals(source.optStringOrNull("sub_kompetensi"), question.subCompetency)
            assertEquals(
                QuestionMetadata(
                    originality = source.getJSONObject("metadata").optStringOrNull("originality"),
                    subject = source.getJSONObject("metadata").optStringOrNull("subject"),
                    element = source.getJSONObject("metadata").optStringOrNull("element"),
                    topic = source.getJSONObject("metadata").optStringOrNull("topic"),
                    competency = source.getJSONObject("metadata").optStringOrNull("competency"),
                    subcompetency = source.getJSONObject("metadata").optStringOrNull("subcompetency"),
                    cognitiveLevel = source.getJSONObject("metadata").optStringOrNull("cognitive_level"),
                    difficulty = source.getJSONObject("metadata").optStringOrNull("difficulty"),
                    format = source.getJSONObject("metadata").optStringOrNull("format"),
                    stimulusType = source.getJSONObject("metadata").optStringOrNull("stimulus_type"),
                    stimulusAssetId = source.getJSONObject("metadata").optStringOrNull("stimulus_asset_id"),
                    explanationLanguage = source.getJSONObject("metadata").optStringOrNull("explanation_language")
                ),
                question.metadata
            )
        }
        assertTrue(pilot.all {
            !it.competency.isNullOrBlank() &&
                !it.subCompetency.isNullOrBlank() &&
                it.metadata?.originality == "original" &&
                it.metadata?.subject != null &&
                it.metadata?.element != null &&
                it.metadata?.topic != null &&
                it.metadata?.competency != null &&
                it.metadata?.subcompetency != null &&
                it.metadata?.cognitiveLevel != null &&
                it.metadata?.difficulty != null &&
                it.metadata?.format != null &&
                it.metadata?.explanationLanguage != null
        })

        listOf("M03", "M04", "BI05").forEach { id ->
            val question = pilot.single { it.id == id }
            val stimulus = question.stimulusHtml.orEmpty()
            assertTrue(stimulus.contains("<svg", ignoreCase = true))
            assertEquals("inline_svg", question.metadata?.stimulusType)
            assertNotNull(question.metadata?.stimulusAssetId)
            assertFalse(EXTERNAL_RESOURCE_REGEX.containsMatchIn(stimulus))
        }

        assertTrue(originalPackages.all { it.title.startsWith("Latihan Orisinal · Paket ") })
    }

    private fun JSONArray.toPackageObjects(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key)

    private fun normalize(value: Any?): Any? {
        if (value == null || value === JSONObject.NULL) return null
        return when (value) {
            is JSONObject -> {
                val normalized = sortedMapOf<String, Any?>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    normalized[key] = normalize(value.get(key))
                }
                normalized
            }
            is JSONArray -> (0 until value.length()).map { normalize(value.get(it)) }
            else -> value
        }
    }

    companion object {
        private val EXTERNAL_RESOURCE_REGEX = Regex(
            """(?i)(?:src|href)\s*=\s*["']\s*https?://|url\(\s*["']?\s*https?://"""
        )
    }
}
