package com.tkasmp2027

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tkasmp2027.data.ProgressStorage
import com.tkasmp2027.data.QuestionRepository
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.engine.QuizScoringEngine
import com.tkasmp2027.model.ActivityEventType
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.UserAnswer
import com.tkasmp2027.security.ExamSecurityManager
import com.tkasmp2027.ui.screens.ActivityLogScreen
import com.tkasmp2027.ui.screens.ExamPrepScreen
import com.tkasmp2027.ui.screens.HomeScreen
import com.tkasmp2027.ui.screens.QuizScreen
import com.tkasmp2027.ui.screens.ResultScreen
import com.tkasmp2027.ui.screens.ReviewScreen
import com.tkasmp2027.ui.theme.TkaSmpTheme
import kotlinx.coroutines.delay

private sealed interface ScreenRoute {
    data object Home : ScreenRoute
    data class Prep(val packageId: String) : ScreenRoute
    data class Quiz(val packageId: String, val questionIndex: Int) : ScreenRoute
    data class Result(val packageId: String) : ScreenRoute
    data class Review(val packageId: String) : ScreenRoute
    data class ActivityLog(val packageId: String, val returnToResult: Boolean) : ScreenRoute
}

class MainActivity : ComponentActivity() {

    private var activeExamPackageIdForLifecycle: String? = null

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && activeExamPackageIdForLifecycle != null) {
            ExamSecurityManager.enableImmersiveMode(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = QuestionRepository(applicationContext)
        val storage = ProgressStorage(applicationContext)

        setContent {
            TkaSmpTheme {
                val packages = remember { repository.loadAllPackages() }
                fun packageById(packageId: String) = packages.first { it.packageId == packageId }

                // Restore state on launch, including auto-finalizing expired active sessions
                val initialLoad = remember {
                    val loaded = storage.loadAllProgress().toMutableMap()
                    val activeId = ExamSessionEngine.findActiveExamPackageId(loaded)
                    var startRoute: ScreenRoute = ScreenRoute.Home
                    if (activeId != null) {
                        val activeProgress = loaded[activeId]
                        val pkg = packages.firstOrNull { it.packageId == activeId }
                        if (activeProgress != null && pkg != null) {
                            val now = System.currentTimeMillis()
                            val rem = ExamSessionEngine.remainingSeconds(activeProgress.examDeadlineMillis, now)
                            if (rem <= 0) {
                                val finalized = ExamSessionEngine.finalizeExamProgress(
                                    pkg = pkg,
                                    current = activeProgress,
                                    completedAtMillis = now,
                                    submitDetail = "Waktu ujian habis saat aplikasi tidak aktif; jawaban tersimpan otomatis dikumpulkan."
                                )
                                loaded[activeId] = finalized
                                storage.saveProgress(finalized, synchronous = true)
                                startRoute = ScreenRoute.Result(activeId)
                            } else {
                                startRoute = ScreenRoute.Quiz(activeId, activeProgress.currentQuestionIndex)
                            }
                        }
                    }
                    loaded.toMap() to startRoute
                }

                var progressMap by remember { mutableStateOf(initialLoad.first) }
                var route by remember { mutableStateOf(initialLoad.second) }
                var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                var warningBannerMessage by remember { mutableStateOf<String?>(null) }
                var leftForegroundDuringExam by remember { mutableStateOf(false) }

                val deviceLockMode = remember(route) {
                    ExamSecurityManager.checkLockModeStatus(this@MainActivity)
                }

                val activeQuizPackageId = (route as? ScreenRoute.Quiz)?.packageId

                // Synchronize Activity window security flags, immersive mode, and DPC Lock Task Mode
                DisposableEffect(activeQuizPackageId) {
                    activeExamPackageIdForLifecycle = activeQuizPackageId
                    if (activeQuizPackageId != null) {
                        ExamSecurityManager.enterExamMode(this@MainActivity)
                    } else {
                        ExamSecurityManager.exitExamMode(this@MainActivity)
                    }
                    onDispose { }
                }

                // ProcessLifecycleOwner observer detects when app actually leaves foreground
                DisposableEffect(activeQuizPackageId) {
                    val lifecycle = ProcessLifecycleOwner.get().lifecycle
                    val observer = object : DefaultLifecycleObserver {
                        override fun onStop(owner: LifecycleOwner) {
                            val pkgId = activeQuizPackageId ?: return
                            val current = progressMap[pkgId] ?: return
                            if (current.status != ProgressStatus.IN_PROGRESS) return
                            leftForegroundDuringExam = true
                            val updated = ExamSessionEngine.recordActivityEvent(
                                current = current,
                                type = ActivityEventType.APP_LEFT_FOREGROUND,
                                timestampMillis = System.currentTimeMillis(),
                                detail = "Aplikasi meninggalkan foreground saat ujian berlangsung."
                            )
                            progressMap = storage.saveProgress(updated, synchronous = true)
                        }

                        override fun onStart(owner: LifecycleOwner) {
                            val pkgId = activeQuizPackageId ?: return
                            if (!leftForegroundDuringExam) return
                            leftForegroundDuringExam = false
                            val current = progressMap[pkgId] ?: return
                            if (current.status != ProgressStatus.IN_PROGRESS) return
                            val updated = ExamSessionEngine.recordActivityEvent(
                                current = current,
                                type = ActivityEventType.EXAM_RESUMED_AFTER_LEAVE,
                                timestampMillis = System.currentTimeMillis(),
                                detail = "Ujian dilanjutkan setelah aplikasi kembali ke foreground."
                            )
                            progressMap = storage.saveProgress(updated, synchronous = true)
                            warningBannerMessage =
                                "Aktivitas meninggalkan aplikasi telah dicatat (${updated.violationCount}x). Jawaban Anda tetap tersimpan."
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                // Real-time countdown ticker & automatic submit on timeout
                LaunchedEffect(activeQuizPackageId) {
                    if (activeQuizPackageId != null) {
                        while (true) {
                            val currentNow = System.currentTimeMillis()
                            nowMillis = currentNow
                            val activeProgress = progressMap[activeQuizPackageId]
                            if (activeProgress != null && activeProgress.status == ProgressStatus.IN_PROGRESS) {
                                val remaining = ExamSessionEngine.remainingSeconds(
                                    activeProgress.examDeadlineMillis,
                                    currentNow
                                )
                                if (remaining <= 0) {
                                    val pkg = packageById(activeQuizPackageId)
                                    val finalized = ExamSessionEngine.finalizeExamProgress(
                                        pkg = pkg,
                                        current = activeProgress,
                                        completedAtMillis = currentNow,
                                        submitDetail = "Waktu ujian habis; jawaban dikumpulkan otomatis."
                                    )
                                    progressMap = storage.saveProgress(finalized, synchronous = true)
                                    warningBannerMessage = null
                                    leftForegroundDuringExam = false
                                    route = ScreenRoute.Result(activeQuizPackageId)
                                    break
                                }
                            }
                            delay(1000L)
                        }
                    }
                }

                when (val current = route) {
                    is ScreenRoute.Home -> {
                        HomeScreen(
                            packages = packages,
                            progressMap = progressMap,
                            deviceLockMode = deviceLockMode,
                            onStartOrResumePackage = { pkgId ->
                                val pkg = packageById(pkgId)
                                if (pkg.questions.isNotEmpty()) {
                                    val existing = progressMap[pkgId]
                                    if (existing?.status == ProgressStatus.IN_PROGRESS) {
                                        warningBannerMessage = null
                                        route = ScreenRoute.Quiz(pkgId, existing.currentQuestionIndex)
                                    } else if (existing?.status != ProgressStatus.COMPLETED) {
                                        route = ScreenRoute.Prep(pkgId)
                                    }
                                }
                            },
                            onViewResultPackage = { pkgId ->
                                route = ScreenRoute.Result(pkgId)
                            },
                            onReviewPackage = { pkgId ->
                                route = ScreenRoute.Review(pkgId)
                            },
                            onViewActivityLog = { pkgId ->
                                route = ScreenRoute.ActivityLog(pkgId, returnToResult = false)
                            }
                        )
                    }

                    is ScreenRoute.Prep -> {
                        val pkg = packageById(current.packageId)
                        ExamPrepScreen(
                            pkg = pkg,
                            lockModeStatus = deviceLockMode,
                            onConfirmStartExam = {
                                val existing = progressMap[current.packageId]
                                if (existing?.status == ProgressStatus.COMPLETED) {
                                    route = ScreenRoute.Result(current.packageId)
                                } else {
                                    val started = ExamSessionEngine.startExamProgress(
                                        pkg = pkg,
                                        existing = existing,
                                        nowMillis = System.currentTimeMillis(),
                                        lockModeStatus = deviceLockMode
                                    )
                                    progressMap = storage.saveProgress(started, synchronous = true)
                                    warningBannerMessage = null
                                    leftForegroundDuringExam = false
                                    nowMillis = System.currentTimeMillis()
                                    route = ScreenRoute.Quiz(current.packageId, 0)
                                }
                            },
                            onCancelBack = { route = ScreenRoute.Home }
                        )
                    }

                    is ScreenRoute.Quiz -> {
                        val pkg = packageById(current.packageId)
                        val activeProgress = progressMap[current.packageId]
                        val answers = activeProgress?.answers.orEmpty()
                        val remaining = ExamSessionEngine.remainingSeconds(
                            activeProgress?.examDeadlineMillis,
                            nowMillis
                        )
                        QuizScreen(
                            pkg = pkg,
                            currentIndex = current.questionIndex,
                            answers = answers,
                            remainingSeconds = remaining,
                            lockModeStatus = activeProgress?.lockModeStatus ?: deviceLockMode,
                            violationCount = activeProgress?.violationCount ?: 0,
                            warningBannerMessage = warningBannerMessage,
                            onDismissWarning = { warningBannerMessage = null },
                            onIndexChange = { nextIdx ->
                                val base = progressMap[current.packageId]
                                if (base != null && base.status == ProgressStatus.IN_PROGRESS) {
                                    val updated = ExamSessionEngine.updateAnswersAndIndex(
                                        pkg = pkg,
                                        current = base,
                                        answers = base.answers,
                                        questionIndex = nextIdx,
                                        nowMillis = System.currentTimeMillis()
                                    )
                                    progressMap = storage.saveProgress(updated)
                                    route = ScreenRoute.Quiz(current.packageId, nextIdx)
                                }
                            },
                            onUpdateAnswer = { qId, ans ->
                                val base = progressMap[current.packageId]
                                if (base != null && base.status == ProgressStatus.IN_PROGRESS) {
                                    val nextAnswers = base.answers.toMutableMap().apply { put(qId, ans) }
                                    val updated = ExamSessionEngine.updateAnswersAndIndex(
                                        pkg = pkg,
                                        current = base,
                                        answers = nextAnswers,
                                        questionIndex = current.questionIndex,
                                        nowMillis = System.currentTimeMillis()
                                    )
                                    progressMap = storage.saveProgress(updated, synchronous = true)
                                }
                            },
                            onAttemptExitNavigation = {
                                val base = progressMap[current.packageId]
                                if (base != null && base.status == ProgressStatus.IN_PROGRESS) {
                                    val updated = ExamSessionEngine.recordActivityEvent(
                                        current = base,
                                        type = ActivityEventType.ATTEMPT_EXIT_NAVIGATION,
                                        timestampMillis = System.currentTimeMillis(),
                                        detail = "Percobaan keluar dari halaman ujian melalui tombol Back."
                                    )
                                    progressMap = storage.saveProgress(updated, synchronous = true)
                                    warningBannerMessage =
                                        "Navigasi keluar dinonaktifkan selama ujian. Percobaan keluar telah dicatat (${updated.violationCount}x)."
                                }
                            },
                            onSubmitQuiz = {
                                val base = progressMap[current.packageId]
                                if (base != null) {
                                    val finalized = ExamSessionEngine.finalizeExamProgress(
                                        pkg = pkg,
                                        current = base,
                                        completedAtMillis = System.currentTimeMillis(),
                                        submitDetail = "Peserta mengumpulkan ujian secara resmi."
                                    )
                                    progressMap = storage.saveProgress(finalized, synchronous = true)
                                    warningBannerMessage = null
                                    leftForegroundDuringExam = false
                                    route = ScreenRoute.Result(current.packageId)
                                }
                            }
                        )
                    }

                    is ScreenRoute.Result -> {
                        val pkg = packageById(current.packageId)
                        val progress = progressMap[current.packageId]
                        val answers = progress?.answers.orEmpty()
                        val evaluation = QuizScoringEngine.evaluateQuiz(pkg, answers)
                        ResultScreen(
                            pkg = pkg,
                            evaluation = evaluation,
                            progress = progress,
                            onReviewAnswers = { route = ScreenRoute.Review(current.packageId) },
                            onViewActivityLog = {
                                route = ScreenRoute.ActivityLog(current.packageId, returnToResult = true)
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
                            onViewActivityLog = {
                                route = ScreenRoute.ActivityLog(current.packageId, returnToResult = true)
                            }
                        )
                    }

                    is ScreenRoute.ActivityLog -> {
                        val pkg = packageById(current.packageId)
                        val progress = progressMap[current.packageId]
                        ActivityLogScreen(
                            pkg = pkg,
                            progress = progress,
                            onBack = {
                                route = if (current.returnToResult) {
                                    ScreenRoute.Result(current.packageId)
                                } else {
                                    ScreenRoute.Home
                                }
                            },
                            onProctorResetSession = {
                                progressMap = storage.clearProgress(current.packageId)
                                route = ScreenRoute.Home
                            }
                        )
                    }
                }
            }
        }
    }
}
