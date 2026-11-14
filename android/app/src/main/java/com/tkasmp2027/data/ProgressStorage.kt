package com.tkasmp2027.data

import android.content.Context
import com.tkasmp2027.model.ActivityEventType
import com.tkasmp2027.model.ExamActivityEvent
import com.tkasmp2027.model.LockModeStatus
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.UserAnswer
import org.json.JSONArray
import org.json.JSONObject

class ProgressStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAllProgress(): Map<String, PackageProgress> {
        val raw = prefs.getString(KEY_PROGRESS_JSON, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val pkgId = keys.next()
                    val obj = root.getJSONObject(pkgId)
                    put(pkgId, decodeProgress(pkgId, obj))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveProgress(progress: PackageProgress, synchronous: Boolean = false): Map<String, PackageProgress> {
        val current = loadAllProgress().toMutableMap()
        current[progress.packageId] = progress
        persistProgressMap(current, synchronous)
        return current
    }

    fun clearProgress(packageId: String): Map<String, PackageProgress> {
        val current = loadAllProgress().toMutableMap()
        current.remove(packageId)
        persistProgressMap(current, synchronous = true)
        return current
    }

    private fun persistProgressMap(map: Map<String, PackageProgress>, synchronous: Boolean) {
        val root = JSONObject()
        map.forEach { (pkgId, progress) ->
            root.put(pkgId, encodeProgress(progress))
        }
        val editor = prefs.edit().putString(KEY_PROGRESS_JSON, root.toString())
        if (synchronous) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    private fun encodeProgress(progress: PackageProgress): JSONObject {
        val answersJson = JSONObject()
        progress.answers.forEach { (qId, answer) ->
            val ansObj = JSONObject()
            when (answer) {
                is UserAnswer.Pg -> {
                    ansObj.put("type", "PG")
                    ansObj.put("selectedOptionId", answer.selectedOptionId ?: JSONObject.NULL)
                }
                is UserAnswer.PgKompleks -> {
                    ansObj.put("type", "PG_KOMPLEKS")
                    ansObj.put("selectedOptionIds", JSONArray(answer.selectedOptionIds.toList()))
                }
                is UserAnswer.BenarSalah -> {
                    ansObj.put("type", "BENAR_SALAH")
                    val mapObj = JSONObject()
                    answer.statementSelections.forEach { (sId, value) ->
                        mapObj.put(sId, value)
                    }
                    ansObj.put("statementSelections", mapObj)
                }
            }
            answersJson.put(qId, ansObj)
        }

        val logArray = JSONArray()
        progress.activityLog.forEach { event ->
            logArray.put(
                JSONObject().apply {
                    put("type", event.type.name)
                    put("timestampMillis", event.timestampMillis)
                    put("detail", event.detail)
                }
            )
        }

        return JSONObject().apply {
            put("packageId", progress.packageId)
            put("status", progress.status.name)
            put("currentQuestionIndex", progress.currentQuestionIndex)
            put("answers", answersJson)
            put("lastScorePercent", progress.lastScorePercent ?: JSONObject.NULL)
            put("lastCorrectCount", progress.lastCorrectCount)
            put("lastKeyedCount", progress.lastKeyedCount)
            put("lastAnsweredCount", progress.lastAnsweredCount)
            put("totalQuestions", progress.totalQuestions)
            put("attempts", progress.attempts)
            put("updatedAtMillis", progress.updatedAtMillis)
            put("examDurationSeconds", progress.examDurationSeconds)
            put("examStartedAtMillis", progress.examStartedAtMillis ?: JSONObject.NULL)
            put("examDeadlineMillis", progress.examDeadlineMillis ?: JSONObject.NULL)
            put("examCompletedAtMillis", progress.examCompletedAtMillis ?: JSONObject.NULL)
            put("lockModeStatus", progress.lockModeStatus.name)
            put("violationCount", progress.violationCount)
            put("activityLog", logArray)
        }
    }

    private fun decodeProgress(pkgId: String, obj: JSONObject): PackageProgress {
        val answersObj = obj.optJSONObject("answers") ?: JSONObject()
        val answers = buildMap {
            val qKeys = answersObj.keys()
            while (qKeys.hasNext()) {
                val qId = qKeys.next()
                val ansObj = answersObj.getJSONObject(qId)
                when (ansObj.optString("type")) {
                    "PG" -> {
                        val opt = ansObj.optString("selectedOptionId").takeIf {
                            it.isNotBlank() && !ansObj.isNull("selectedOptionId")
                        }
                        put(qId, UserAnswer.Pg(opt))
                    }
                    "PG_KOMPLEKS" -> {
                        val arr = ansObj.optJSONArray("selectedOptionIds") ?: JSONArray()
                        val set = buildSet {
                            for (i in 0 until arr.length()) {
                                add(arr.getString(i))
                            }
                        }
                        put(qId, UserAnswer.PgKompleks(set))
                    }
                    "BENAR_SALAH" -> {
                        val selObj = ansObj.optJSONObject("statementSelections") ?: JSONObject()
                        val map = buildMap {
                            val sKeys = selObj.keys()
                            while (sKeys.hasNext()) {
                                val sId = sKeys.next()
                                val num = selObj.optInt(sId, -1)
                                if (num > 0) {
                                    put(sId, num)
                                }
                            }
                        }
                        put(qId, UserAnswer.BenarSalah(map))
                    }
                }
            }
        }

        val logArray = obj.optJSONArray("activityLog") ?: JSONArray()
        val activityLog = buildList {
            for (i in 0 until logArray.length()) {
                val item = logArray.optJSONObject(i) ?: continue
                val type = try {
                    ActivityEventType.valueOf(item.optString("type"))
                } catch (_: Exception) {
                    continue
                }
                add(
                    ExamActivityEvent(
                        type = type,
                        timestampMillis = item.optLong("timestampMillis", 0L),
                        detail = item.optString("detail", type.label)
                    )
                )
            }
        }

        val status = try {
            ProgressStatus.valueOf(obj.optString("status", ProgressStatus.NOT_STARTED.name))
        } catch (_: Exception) {
            ProgressStatus.NOT_STARTED
        }

        val lockModeStatus = try {
            LockModeStatus.valueOf(
                obj.optString("lockModeStatus", LockModeStatus.MONITORING_MODE_ONLY.name)
            )
        } catch (_: Exception) {
            LockModeStatus.MONITORING_MODE_ONLY
        }

        val lastScore = if (obj.isNull("lastScorePercent")) null else obj.optInt("lastScorePercent")

        return PackageProgress(
            packageId = pkgId,
            status = status,
            currentQuestionIndex = obj.optInt("currentQuestionIndex", 0),
            answers = answers,
            lastScorePercent = lastScore,
            lastCorrectCount = obj.optInt("lastCorrectCount", 0),
            lastKeyedCount = obj.optInt("lastKeyedCount", 0),
            lastAnsweredCount = obj.optInt("lastAnsweredCount", 0),
            totalQuestions = obj.optInt("totalQuestions", 0),
            attempts = obj.optInt("attempts", 0),
            updatedAtMillis = obj.optLong("updatedAtMillis", 0L),
            examDurationSeconds = obj.optInt("examDurationSeconds", 0),
            examStartedAtMillis = if (obj.isNull("examStartedAtMillis") || !obj.has("examStartedAtMillis")) {
                null
            } else {
                obj.optLong("examStartedAtMillis")
            },
            examDeadlineMillis = if (obj.isNull("examDeadlineMillis") || !obj.has("examDeadlineMillis")) {
                null
            } else {
                obj.optLong("examDeadlineMillis")
            },
            examCompletedAtMillis = if (obj.isNull("examCompletedAtMillis") || !obj.has("examCompletedAtMillis")) {
                null
            } else {
                obj.optLong("examCompletedAtMillis")
            },
            lockModeStatus = lockModeStatus,
            violationCount = obj.optInt("violationCount", 0),
            activityLog = activityLog
        )
    }

    companion object {
        private const val PREFS_NAME = "tka_smp_2027_local_storage"
        private const val KEY_PROGRESS_JSON = "progress_map_v3"
    }
}
