package com.tkasmp2027

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tkasmp2027.data.ProgressStorage
import com.tkasmp2027.data.QuestionRepository
import com.tkasmp2027.engine.QuizScoringEngine
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.UserAnswer
import com.tkasmp2027.ui.screens.HomeScreen
import com.tkasmp2027.ui.screens.QuizScreen
import com.tkasmp2027.ui.screens.ResultScreen
import com.tkasmp2027.ui.screens.ReviewScreen
import com.tkasmp2027.ui.theme.TkaSmpTheme

private sealed interface ScreenRoute {
    data object Home : ScreenRoute
    data class Quiz(val packageId: String, val questionIndex: Int) : ScreenRoute
    data class Result(val packageId: String) : ScreenRoute
    data class Review(val packageId: String) : ScreenRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = QuestionRepository(applicationContext)
        val storage = ProgressStorage(applicationContext)

        setContent {
            TkaSmpTheme {
                val packages = remember { repository.loadAllPackages() }
                var progressMap by remember { mutableStateOf(storage.loadAllProgress()) }
                var route by remember { mutableStateOf<ScreenRoute>(ScreenRoute.Home) }

                fun packageById(packageId: String) =
                    packages.first { it.packageId == packageId }

                fun persistInProgress(
                    packageId: String,
                    answers: Map<String, UserAnswer>,
                    questionIndex: Int
                ) {
                    val pkg = packageById(packageId)
                    val existing = progressMap[packageId]
                    val answeredCount = pkg.questions.count {
                        QuizScoringEngine.isQuestionAnswered(it, answers[it.id])
                    }
                    val nextProgress = PackageProgress(
                        packageId = packageId,
                        status = ProgressStatus.IN_PROGRESS,
                        currentQuestionIndex = questionIndex,
                        answers = answers,
                        lastScorePercent = existing?.lastScorePercent,
                        lastCorrectCount = existing?.lastCorrectCount ?: 0,
                        lastKeyedCount = existing?.lastKeyedCount ?: 0,
                        lastAnsweredCount = answeredCount,
                        totalQuestions = pkg.questions.size,
                        attempts = existing?.attempts ?: 0,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    progressMap = storage.saveProgress(nextProgress)
                }

                when (val current = route) {
                    is ScreenRoute.Home -> {
                        HomeScreen(
                            packages = packages,
                            progressMap = progressMap,
                            onStartPackage = { pkgId ->
                                val pkg = packageById(pkgId)
                                if (pkg.questions.isNotEmpty()) {
                                    val existing = progressMap[pkgId]
                                    if (existing?.status == ProgressStatus.COMPLETED) {
                                        persistInProgress(pkgId, emptyMap(), 0)
                                        route = ScreenRoute.Quiz(pkgId, 0)
                                    } else {
                                        val startIndex = existing?.currentQuestionIndex ?: 0
                                        route = ScreenRoute.Quiz(pkgId, startIndex)
                                    }
                                }
                            },
                            onReviewPackage = { pkgId ->
                                route = ScreenRoute.Review(pkgId)
                            },
                            onResetProgress = { pkgId ->
                                progressMap = storage.clearProgress(pkgId)
                            }
                        )
                    }

                    is ScreenRoute.Quiz -> {
                        val pkg = packageById(current.packageId)
                        val answers = progressMap[current.packageId]?.answers.orEmpty()
                        QuizScreen(
                            pkg = pkg,
                            currentIndex = current.questionIndex,
                            answers = answers,
                            onIndexChange = { nextIdx ->
                                route = ScreenRoute.Quiz(current.packageId, nextIdx)
                                if (progressMap[current.packageId] != null) {
                                    persistInProgress(current.packageId, answers, nextIdx)
                                }
                            },
                            onUpdateAnswer = { qId, ans ->
                                val updated = answers.toMutableMap().apply { put(qId, ans) }
                                persistInProgress(current.packageId, updated, current.questionIndex)
                            },
                            onExitHome = { route = ScreenRoute.Home },
                            onSubmitQuiz = {
                                val evaluation = QuizScoringEngine.evaluateQuiz(pkg, answers)
                                val existing = progressMap[current.packageId]
                                progressMap = storage.saveProgress(
                                    PackageProgress(
                                        packageId = current.packageId,
                                        status = ProgressStatus.COMPLETED,
                                        currentQuestionIndex = 0,
                                        answers = answers,
                                        lastScorePercent = evaluation.scorePercent,
                                        lastCorrectCount = evaluation.correctCount,
                                        lastKeyedCount = evaluation.keyedCount,
                                        lastAnsweredCount = evaluation.answeredCount,
                                        totalQuestions = evaluation.totalQuestions,
                                        attempts = (existing?.attempts ?: 0) + 1,
                                        updatedAtMillis = System.currentTimeMillis()
                                    )
                                )
                                route = ScreenRoute.Result(current.packageId)
                            }
                        )
                    }

                    is ScreenRoute.Result -> {
                        val pkg = packageById(current.packageId)
                        val answers = progressMap[current.packageId]?.answers.orEmpty()
                        val evaluation = QuizScoringEngine.evaluateQuiz(pkg, answers)
                        ResultScreen(
                            pkg = pkg,
                            evaluation = evaluation,
                            onReviewAnswers = { route = ScreenRoute.Review(current.packageId) },
                            onRetryQuiz = {
                                persistInProgress(current.packageId, emptyMap(), 0)
                                route = ScreenRoute.Quiz(current.packageId, 0)
                            },
                            onBackHome = { route = ScreenRoute.Home }
                        )
                    }

                    is ScreenRoute.Review -> {
                        val pkg = packageById(current.packageId)
                        val answers = progressMap[current.packageId]?.answers.orEmpty()
                        val evaluation = QuizScoringEngine.evaluateQuiz(pkg, answers)
                        ReviewScreen(
                            pkg = pkg,
                            answers = answers,
                            evaluation = evaluation,
                            onBackToResult = { route = ScreenRoute.Result(current.packageId) },
                            onRetryQuiz = {
                                persistInProgress(current.packageId, emptyMap(), 0)
                                route = ScreenRoute.Quiz(current.packageId, 0)
                            }
                        )
                    }
                }
            }
        }
    }
}
