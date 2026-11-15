package com.tkasmp2027

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tkasmp2027.data.ProgressStorage
import com.tkasmp2027.data.QuestionRepository
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.engine.QuizScoringEngine
import com.tkasmp2027.model.ActivityEventType
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.UserAnswer
import com.tkasmp2027.security.ExamSecurityManager
import com.tkasmp2027.ui.screens.ActivityLogScreen
import com.tkasmp2027.ui.screens.ExamPrepScreen
import com.tkasmp2027.ui.screens.HomeScreen
import com.tkasmp2027.ui.screens.QuizScreen
import com.tkasmp2027.ui.screens.ResultScreen
import com.tkasmp2027.ui.screens.ReviewScreen
import com.tkasmp2027.ui.theme.TkaSmpTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface ScreenRoute {
    data object Home : ScreenRoute
    data class Prep(val packageId: String) : ScreenRoute
    data class Quiz(val packageId: String, val questionIndex: Int) : ScreenRoute
    data class Result(val packageId: String) : ScreenRoute
    data class Review(val packageId: String) : ScreenRoute
    data class ActivityLog(val packageId: String, val returnToResult: Boolean) : ScreenRoute
}

private sealed interface StartupState {
    data object Loading : StartupState
    data class Failed(val message: String) : StartupState
    data class Ready(
        val attempt: Int,
        val packages: List<QuestionPackage>,
        val storage: ProgressStorage,
        val progressMap: Map<String, PackageProgress>,
        val startRoute: ScreenRoute,
        val warningMessage: String?
    ) : StartupState
}

class MainActivity : ComponentActivity() {

    private var activeExamPackageIdForLifecycle: String? = null
    private var startupStartedAtElapsedRealtime = 0L

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && activeExamPackageIdForLifecycle != null) {
            ExamSecurityManager.enableImmersiveMode(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startupStartedAtElapsedRealtime = SystemClock.elapsedRealtime()
        val appContext = applicationContext

        setContent {
            TkaSmpTheme {
                var retryLoad by remember { mutableIntStateOf(0) }
                var startupState by remember { mutableStateOf<StartupState>(StartupState.Loading) }

                LaunchedEffect(retryLoad) {
                    startupState = StartupState.Loading
                    startupState = try {
                        withContext(Dispatchers.IO) {
                            loadStartupState(appContext, retryLoad)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.e(TAG, "Failed to load local app data", error)
                        StartupState.Failed(
                            "Database soal atau progres tersimpan tidak dapat dimuat. Data progres tidak dihapus."
                        )
                    }
                }

                when (val startup = startupState) {
                    StartupState.Loading -> StartupLoadingScreen()
                    is StartupState.Failed -> StartupErrorScreen(
                        message = startup.message,
                        onRetry = { retryLoad += 1 }
                    )
                    is StartupState.Ready -> {
                val packages = startup.packages
                val storage = startup.storage
                fun packageById(packageId: String) = packages.first { it.packageId == packageId }

                var progressMap by remember(startup.attempt) { mutableStateOf(startup.progressMap) }
                var route by remember(startup.attempt) { mutableStateOf(startup.startRoute) }
                var nowMillis by remember(startup.attempt) { mutableLongStateOf(System.currentTimeMillis()) }
                var warningBannerMessage by remember(startup.attempt) {
                    mutableStateOf(startup.warningMessage)
                }
                var leftForegroundDuringExam by remember(startup.attempt) { mutableStateOf(false) }

                var deviceLockMode by remember(startup.attempt) {
                    mutableStateOf(ExamSecurityManager.checkLockModeStatus(this@MainActivity))
                }

                LaunchedEffect(startup.attempt) {
                    withFrameNanos { }
                    val elapsed = SystemClock.elapsedRealtime() - startupStartedAtElapsedRealtime
                    val questionCount = packages.sumOf { it.questions.size }
                    Log.i(TAG, "Initial screen ready in " + elapsed + "ms (" + packages.size + " packages, " + questionCount + " questions)")
                }

                val activeQuizPackageId = (route as? ScreenRoute.Quiz)?.packageId

                // Synchronize Activity window security flags, immersive mode, and DPC Lock Task Mode
                DisposableEffect(activeQuizPackageId) {
                    activeExamPackageIdForLifecycle = activeQuizPackageId
                    if (activeQuizPackageId != null) {
                        deviceLockMode = ExamSecurityManager.enterExamMode(this@MainActivity)
                    } else {
                        ExamSecurityManager.exitExamMode(this@MainActivity)
                        deviceLockMode = ExamSecurityManager.checkLockModeStatus(this@MainActivity)
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
                                        lockModeStatus = ExamSecurityManager.enterExamMode(this@MainActivity)
                                    )
                                    deviceLockMode = started.lockModeStatus
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
                                    progressMap = storage.saveProgress(updated, synchronous = true)
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
                                    progressMap = storage.saveProgress(updated)
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
    }
}

private fun loadStartupState(context: android.content.Context, attempt: Int): StartupState.Ready {
    val repository = QuestionRepository(context)
    val storage = ProgressStorage(context)
    val packages = repository.loadAllPackages()
    val loaded = storage.loadAllProgress().toMutableMap()
    var startRoute: ScreenRoute = ScreenRoute.Home
    var warningMessage: String? = null

    val activeId = ExamSessionEngine.findActiveExamPackageId(loaded)
    if (activeId != null) {
        var activeProgress = loaded[activeId]
        val pkg = packages.firstOrNull { it.packageId == activeId }
        if (activeProgress != null && pkg != null) {
            var progressChanged = false
            if (activeProgress.activityLog.lastOrNull()?.type == ActivityEventType.APP_LEFT_FOREGROUND) {
                activeProgress = ExamSessionEngine.recordActivityEvent(
                    current = activeProgress,
                    type = ActivityEventType.EXAM_RESUMED_AFTER_LEAVE,
                    timestampMillis = System.currentTimeMillis(),
                    detail = "Ujian dipulihkan setelah aplikasi dibuka kembali."
                )
                loaded[activeId] = activeProgress
                progressChanged = true
                warningMessage =
                    "Aktivitas meninggalkan aplikasi telah dicatat (" +
                        activeProgress.violationCount +
                        "x). Jawaban Anda tetap tersimpan."
            }

            val now = System.currentTimeMillis()
            val remaining = ExamSessionEngine.remainingSeconds(activeProgress.examDeadlineMillis, now)
            if (remaining <= 0) {
                val finalized = ExamSessionEngine.finalizeExamProgress(
                    pkg = pkg,
                    current = activeProgress,
                    completedAtMillis = now,
                    submitDetail = "Waktu ujian habis saat aplikasi tidak aktif; jawaban tersimpan otomatis dikumpulkan."
                )
                loaded[activeId] = finalized
                startRoute = ScreenRoute.Result(activeId)
                progressChanged = true
            } else {
                startRoute = ScreenRoute.Quiz(activeId, activeProgress.currentQuestionIndex)
            }

            if (progressChanged) {
                storage.saveProgress(loaded.getValue(activeId), synchronous = true)
            }
        }
    }

    return StartupState.Ready(
        attempt = attempt,
        packages = packages,
        storage = storage,
        progressMap = loaded.toMap(),
        startRoute = startRoute,
        warningMessage = warningMessage
    )
}

@Composable
private fun StartupLoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Memuat soal dan progres tersimpan…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StartupErrorScreen(message: String, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Aplikasi belum dapat dimuat",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(onClick = onRetry) {
                    Text("Coba Lagi")
                }
            }
        }
    }
}

private const val TAG = "TkaSmpStartup"
