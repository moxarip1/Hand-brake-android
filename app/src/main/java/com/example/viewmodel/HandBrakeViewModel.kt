package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiVideoAdvisor
import com.example.data.HandBrakeDatabase
import com.example.data.HandBrakeRepository
import com.example.data.TranscodeJobEntity
import com.example.engine.VideoTranscodeEngine
import com.example.model.AiOptimizationGoal
import com.example.model.AiVideoAnalysisReport
import com.example.model.ChapterMarker
import com.example.model.HandBrakeConfig
import com.example.model.HandBrakePreset
import com.example.model.HandBrakeTab
import com.example.model.HardwareCodecInfo
import com.example.model.OfficialHandBrakePresets
import com.example.model.QualityMode
import com.example.model.ResolutionLimit
import com.example.model.SourceVideoMetadata
import com.example.model.SubtitleTrackItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainNavDestination(val titleAr: String) {
    STUDIO("الاستوديو"),
    PRESETS("الإعدادات المسبقة"),
    QUEUE("طابور المهام"),
    ACTIVITY_LOG("سجل الترميز")
}

data class StudioUiState(
    val currentNav: MainNavDestination = MainNavDestination.STUDIO,
    val selectedTab: HandBrakeTab = HandBrakeTab.SUMMARY,
    val sourceVideo: SourceVideoMetadata? = null,
    val isLoadingSource: Boolean = false,
    val config: HandBrakeConfig = HandBrakeConfig(),
    val selectedPreviewThumbIndex: Int = 0,
    val selectedAiGoal: AiOptimizationGoal = AiOptimizationGoal.BEST_QUALITY_ORIGINAL_SIZE,
    val isAnalyzingWithAi: Boolean = false,
    val aiAnalysisReport: AiVideoAnalysisReport? = null,
    val isEncodingNow: Boolean = false,
    val activeJobProgress: Int = 0,
    val activeJobStageAr: String = "",
    val activeJobFps: Float = 0f,
    val activeJobSpeed: Float = 0f,
    val activeJobLiveBytes: Long = 0L,
    val bannerMessageAr: String? = null,
    val latestCompletedJob: TranscodeJobEntity? = null,
    val inspectingJobForPlayer: TranscodeJobEntity? = null,
    val showSavePresetDialog: Boolean = false,
    val deviceHardwareCodecs: List<HardwareCodecInfo> = emptyList(),
    val activityLogs: List<String> = listOf(
        "[HandBrake Mobile 2.0-AI] تم تهيئة محرك النقل المباشر بالعتاد (Hardware Surface Pipeline) بدون تقطيع.",
        "[AI Engine] المحلل الذكي للفيديو (Gemini AI Advisor) جاهز لتحليل الأبعاد ومعدل البت."
    )
)

class HandBrakeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HandBrakeRepository
    private var activeTranscodeJob: Job? = null

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val allPresets: StateFlow<List<HandBrakePreset>>
    val allJobs: StateFlow<List<TranscodeJobEntity>>

    init {
        val dao = HandBrakeDatabase.getInstance(application).handBrakeDao()
        repository = HandBrakeRepository(dao)

        allPresets = repository.customPresets
            .map { customList ->
                customList.map { it.toHandBrakePreset() } + OfficialHandBrakePresets.allPresets
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = OfficialHandBrakePresets.allPresets
            )

        allJobs = repository.allJobs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        viewModelScope.launch {
            val codecs = VideoTranscodeEngine.queryDeviceCodecs()
            val hwEncoders = codecs.count { it.isEncoder && it.isHardwareAccelerated }
            appendLog("تم اكتشاف ${codecs.size} مُرمّز وفك ترميز على الجهاز ($hwEncoders مسرّع بالعتاد الصلب HW).")
            _uiState.update { it.copy(deviceHardwareCodecs = codecs) }

            loadOrGenerateSampleVideo(silentBanner = true)
        }
    }

    fun appendLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "[$time] $message"
        _uiState.update { state ->
            state.copy(activityLogs = (state.activityLogs + line).takeLast(120))
        }
    }

    fun clearLogs() {
        _uiState.update {
            it.copy(activityLogs = listOf("[HandBrake Mobile] تم مسح سجل العمليات."))
        }
    }

    fun selectNav(dest: MainNavDestination) {
        _uiState.update { it.copy(currentNav = dest) }
    }

    fun selectTab(tab: HandBrakeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectPreviewThumb(index: Int) {
        _uiState.update { it.copy(selectedPreviewThumbIndex = index) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessageAr = null) }
    }

    fun setShowSavePresetDialog(show: Boolean) {
        _uiState.update { it.copy(showSavePresetDialog = show) }
    }

    fun setInspectingJobForPlayer(job: TranscodeJobEntity?) {
        _uiState.update { it.copy(inspectingJobForPlayer = job) }
    }

    fun loadVideoFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSource = true, bannerMessageAr = null, aiAnalysisReport = null) }
            appendLog("جاري فحص مصدر الفيديو المختار مع الحفاظ التام على القياس الأصلي 1:1...")
            val metadata = VideoTranscodeEngine.inspectVideo(getApplication(), uri, null)
            val defaultGoal = if (metadata.isPortraitReels) {
                AiOptimizationGoal.REELS_TIKTOK_SHORTS
            } else {
                AiOptimizationGoal.BEST_QUALITY_ORIGINAL_SIZE
            }
            _uiState.update { state ->
                state.copy(
                    sourceVideo = metadata,
                    isLoadingSource = false,
                    selectedPreviewThumbIndex = 0,
                    selectedAiGoal = defaultGoal,
                    // Always lock to ORIGINAL 1:1 dimensions and source framerate when loading a video so Reels never shrink!
                    config = state.config.copy(
                        activePresetName = if (metadata.isPortraitReels) {
                            "ريلز عمودي 1:1 (${metadata.width}×${metadata.height})"
                        } else {
                            "نفس القياس الأصلي 1:1 (${metadata.width}×${metadata.height})"
                        },
                        resolutionLimit = ResolutionLimit.ORIGINAL,
                        customWidth = metadata.width,
                        customHeight = metadata.height,
                        framerateTarget = 0,
                        rotationDegrees = 0,
                        trimStartFraction = 0f,
                        trimEndFraction = 1f,
                        startChapter = 1,
                        endChapter = metadata.chapters.size.coerceAtLeast(1),
                        customChapters = metadata.chapters
                    ),
                    bannerMessageAr = "تم فتح الفيديو بنفس قياسه الأصلي تماماً: ${metadata.width}×${metadata.height} (${if (metadata.isPortraitReels) "قياس ريلز 9:16" else "قياس كامل"})"
                )
            }
            appendLog(
                "تم تحميل المصدر: ${metadata.fileName} | القياس الأصلي المثبت: ${metadata.width}x${metadata.height} | المدة: ${
                    VideoTranscodeEngine.formatDuration(metadata.durationMs)
                } | الحجم: ${VideoTranscodeEngine.formatSize(metadata.fileSizeBytes)}"
            )
            // Automatically run AI Video Analysis on the newly selected video
            runAiVideoAnalysis(defaultGoal, autoApply = true)
        }
    }

    fun loadOrGenerateSampleVideo(silentBanner: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSource = true) }
            try {
                val sampleFile = VideoTranscodeEngine.generateSampleTestVideo(getApplication()) { logMsg ->
                    appendLog(logMsg)
                }
                val uri = Uri.fromFile(sampleFile)
                val metadata = VideoTranscodeEngine.inspectVideo(getApplication(), uri, sampleFile)
                _uiState.update { state ->
                    state.copy(
                        sourceVideo = metadata,
                        isLoadingSource = false,
                        selectedPreviewThumbIndex = 0,
                        config = state.config.copy(
                            resolutionLimit = ResolutionLimit.ORIGINAL,
                            customWidth = metadata.width,
                            customHeight = metadata.height,
                            framerateTarget = 0,
                            trimStartFraction = 0f,
                            trimEndFraction = 1f,
                            startChapter = 1,
                            endChapter = metadata.chapters.size.coerceAtLeast(1),
                            customChapters = metadata.chapters
                        ),
                        bannerMessageAr = if (silentBanner) null else "تم توليد وتحميل الفيديو التجريبي السلس (HD 30fps) بنجاح!"
                    )
                }
                runAiVideoAnalysis(AiOptimizationGoal.BEST_QUALITY_ORIGINAL_SIZE, autoApply = false)
            } catch (e: Exception) {
                appendLog("خطأ أثناء توليد الفيديو التجريبي: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoadingSource = false) }
            }
        }
    }

    fun selectAiGoalAndAnalyze(goal: AiOptimizationGoal) {
        _uiState.update { it.copy(selectedAiGoal = goal) }
        runAiVideoAnalysis(goal, autoApply = true)
    }

    fun runAiVideoAnalysis(goal: AiOptimizationGoal = _uiState.value.selectedAiGoal, autoApply: Boolean = true) {
        val source = _uiState.value.sourceVideo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingWithAi = true, selectedAiGoal = goal) }
            appendLog("بدء تحليل الفيديو بالذكاء الاصطناعي لهدف: ${goal.titleAr}...")
            val report = GeminiAiVideoAdvisor.analyzeVideo(source, goal)
            _uiState.update { state ->
                state.copy(
                    isAnalyzingWithAi = false,
                    aiAnalysisReport = report
                )
            }
            appendLog("اكتمل تحليل الذكاء الاصطناعي: ${report.contentTypeAr} | موصى بـ ${report.recommendedCodec.shortLabel} RF ${report.recommendedRf.toInt()} مع حفظ الأبعاد ${source.width}×${source.height}.")
            if (autoApply) {
                applyAiRecommendation(report)
            }
        }
    }

    fun applyAiRecommendation(report: AiVideoAnalysisReport) {
        val source = _uiState.value.sourceVideo
        _uiState.update { state ->
            state.copy(
                config = state.config.copy(
                    activePresetName = "ضبط الذكاء الاصطناعي (${report.goal.titleAr})",
                    container = report.recommendedContainer,
                    resolutionLimit = ResolutionLimit.ORIGINAL, // Always preserve exact original video/Reels size
                    customWidth = source?.width ?: state.config.customWidth,
                    customHeight = source?.height ?: state.config.customHeight,
                    videoCodec = report.recommendedCodec,
                    qualityMode = QualityMode.CONSTANT_QUALITY_RF,
                    constantQualityRf = report.recommendedRf,
                    framerateTarget = report.recommendedFps,
                    framerateMode = report.recommendedFramerateMode,
                    encoderPresetSpeed = report.recommendedSpeed,
                    audioCodec = report.recommendedAudioCodec,
                    audioBitrateKbps = report.recommendedAudioBitrateKbps,
                    webOptimized = true
                ),
                bannerMessageAr = "تم تطبيق إعدادات الذكاء الاصطناعي المثالية مع الحفاظ على القياس الأصلي (${source?.width ?: 1080}×${source?.height ?: 1920}) وتوفير ~${report.estimatedSavingsPercent}%!"
            )
        }
    }

    fun updateConfig(transform: (HandBrakeConfig) -> HandBrakeConfig) {
        _uiState.update { state ->
            state.copy(config = transform(state.config))
        }
    }

    fun applyPreset(preset: HandBrakePreset) {
        _uiState.update { state ->
            val currentSource = state.sourceVideo
            val updatedConfig = preset.config.copy(
                customWidth = currentSource?.width ?: preset.config.customWidth,
                customHeight = currentSource?.height ?: preset.config.customHeight,
                customChapters = currentSource?.chapters ?: emptyList(),
                endChapter = currentSource?.chapters?.size ?: 1
            )
            state.copy(
                config = updatedConfig,
                currentNav = MainNavDestination.STUDIO,
                bannerMessageAr = "تم تطبيق الإعداد المسبق: ${preset.nameAr}"
            )
        }
        appendLog("تم تطبيق الإعداد المسبق (Preset): ${preset.name} (${preset.badgeText})")
    }

    fun saveCustomPreset(nameAr: String, descriptionAr: String) {
        if (nameAr.isBlank()) return
        viewModelScope.launch {
            val currentConfig = _uiState.value.config.copy(activePresetName = nameAr.trim())
            repository.saveCustomPreset(
                nameAr = nameAr.trim(),
                descriptionAr = descriptionAr.ifBlank { "إعداد مسبق مخصص محفوظ بواسطة المستخدم" },
                config = currentConfig
            )
            _uiState.update {
                it.copy(
                    config = currentConfig,
                    showSavePresetDialog = false,
                    bannerMessageAr = "تم حفظ الإعداد المسبق '${nameAr.trim()}' في قاعدة البيانات"
                )
            }
            appendLog("تم حفظ إعداد مسبق مخصص جديد: ${nameAr.trim()}")
        }
    }

    fun deleteCustomPreset(preset: HandBrakePreset) {
        val numericId = preset.id.removePrefix("custom_").toIntOrNull() ?: return
        viewModelScope.launch {
            repository.deleteCustomPreset(numericId)
            appendLog("تم حذف الإعداد المسبق المخصص: ${preset.nameAr}")
        }
    }

    fun addSubtitleTrack(languageAr: String, sampleText: String, burnIn: Boolean) {
        updateConfig { cfg ->
            val nextId = (cfg.subtitles.maxOfOrNull { it.id } ?: 0) + 1
            val newTrack = SubtitleTrackItem(
                id = nextId,
                languageAr = languageAr.ifBlank { "العربية (Arabic SRT)" },
                languageCode = "ara",
                sourceName = "ملف ترجمة مضاف #$nextId (.srt)",
                burnIn = burnIn,
                sampleText = sampleText.ifBlank { "ترجمة مدمجة عبر HandBrake Mobile" }
            )
            cfg.copy(subtitles = cfg.subtitles + newTrack)
        }
        appendLog("تمت إضافة مسار ترجمة جديد (${languageAr.ifBlank { "العربية" }})")
    }

    fun removeSubtitleTrack(id: Int) {
        updateConfig { cfg ->
            cfg.copy(subtitles = cfg.subtitles.filterNot { it.id == id })
        }
    }

    fun toggleSubtitleBurnIn(id: Int) {
        updateConfig { cfg ->
            cfg.copy(
                subtitles = cfg.subtitles.map {
                    if (it.id == id) it.copy(burnIn = !it.burnIn) else it
                }
            )
        }
    }

    fun updateChapterTitle(index: Int, newTitle: String) {
        updateConfig { cfg ->
            cfg.copy(
                customChapters = cfg.customChapters.map {
                    if (it.index == index) it.copy(title = newTitle) else it
                }
            )
        }
    }

    fun autoGenerateChapters(intervalSeconds: Int) {
        val source = _uiState.value.sourceVideo ?: return
        val stepMs = (intervalSeconds.coerceAtLeast(1) * 1000L)
        val count = (source.durationMs / stepMs).toInt().coerceIn(1, 20)
        val generated = (0 until count).map { idx ->
            ChapterMarker(
                index = idx + 1,
                title = "الفصل ${idx + 1}",
                startMs = idx * stepMs,
                endMs = if (idx == count - 1) source.durationMs else (idx + 1) * stepMs
            )
        }
        updateConfig {
            it.copy(
                autoChapterIntervalSec = intervalSeconds,
                customChapters = generated,
                startChapter = 1,
                endChapter = generated.size
            )
        }
        appendLog("تم توليد ${generated.size} فصول تلقائياً للفيديو.")
    }

    fun enqueueCurrentJob(startImmediately: Boolean) {
        val state = _uiState.value
        val source = state.sourceVideo ?: return
        val cfg = state.config
        val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, cfg)
        val targetKbps = VideoTranscodeEngine.computeTargetVideoBitrateBps(source, cfg) / 1000
        val (hbCli, _) = VideoTranscodeEngine.generateCliCommands(source, cfg)
        val baseName = source.fileName.substringBeforeLast(".")
        val outFileName = "${baseName}_HB_${outW}x${outH}.${cfg.container.extension}"

        viewModelScope.launch {
            val jobEntity = TranscodeJobEntity(
                sourceUriString = source.filePathOrUri,
                sourceFileName = source.fileName,
                sourceSizeBytes = source.fileSizeBytes,
                sourceDurationMs = source.durationMs,
                sourceResolution = "${source.width}×${source.height}",
                outputFilePath = "",
                outputFileName = outFileName,
                presetName = cfg.activePresetName,
                containerExtension = cfg.container.extension.uppercase(),
                videoCodecLabel = cfg.videoCodec.shortLabel,
                targetResolution = "${outW}×${outH}",
                qualitySummary = "RF ${cfg.constantQualityRf.toInt()} (~$targetKbps kbps)",
                audioSummary = if (cfg.audioCodec.isMuted) "بدون صوت (Muted)" else "${cfg.audioCodec.name} ${cfg.audioBitrateKbps}k",
                status = "QUEUED",
                cliCommand = hbCli
            )
            val id = repository.addJob(jobEntity).toInt()
            appendLog("تمت إضافة المهمة #$id إلى الطابور: $outFileName (${outW}×${outH})")

            if (startImmediately) {
                startEncodingJob(jobEntity.copy(id = id), source, cfg)
            } else {
                _uiState.update {
                    it.copy(bannerMessageAr = "تمت إضافة '${outFileName}' إلى طابور المهام (${allJobs.value.count { j -> j.status == "QUEUED" } + 1} في الطابور)")
                }
            }
        }
    }

    fun startQueueBatch() {
        val state = _uiState.value
        if (state.isEncodingNow) return
        val source = state.sourceVideo ?: return
        val cfg = state.config

        viewModelScope.launch {
            val queued = repository.getQueuedJobs()
            if (queued.isEmpty()) {
                enqueueCurrentJob(startImmediately = true)
            } else {
                for (job in queued) {
                    startEncodingJobSuspend(job, source, cfg)
                }
            }
        }
    }

    private fun startEncodingJob(
        job: TranscodeJobEntity,
        source: SourceVideoMetadata,
        config: HandBrakeConfig
    ) {
        if (_uiState.value.isEncodingNow) return
        activeTranscodeJob = viewModelScope.launch {
            startEncodingJobSuspend(job, source, config)
        }
    }

    private suspend fun startEncodingJobSuspend(
        job: TranscodeJobEntity,
        source: SourceVideoMetadata,
        config: HandBrakeConfig
    ) {
        _uiState.update {
            it.copy(
                isEncodingNow = true,
                activeJobProgress = 1,
                activeJobStageAr = "بدء النقل المباشر للإطارات عبر العتاد...",
                activeJobFps = 0f,
                activeJobSpeed = 1f,
                activeJobLiveBytes = 0L,
                latestCompletedJob = null
            )
        }

        val runningJob = job.copy(status = "RUNNING", progressPercent = 2, currentStageAr = "جاري التحويل السلس...")
        repository.updateJob(runningJob)
        appendLog("بدء تنفيذ المهمة #${job.id}: ${job.cliCommand}")

        val result = VideoTranscodeEngine.transcodeVideo(
            context = getApplication(),
            source = source,
            config = config
        ) { progress ->
            _uiState.update {
                it.copy(
                    activeJobProgress = progress.percent,
                    activeJobStageAr = progress.stageAr,
                    activeJobFps = progress.currentFps,
                    activeJobSpeed = progress.speedMultiplier,
                    activeJobLiveBytes = progress.liveOutputBytes
                )
            }
            if (progress.logMessage != null) {
                appendLog(progress.logMessage)
            }
        }

        if (result.success && result.outputFile != null) {
            val finalRes = result.actualResolution.ifBlank { job.targetResolution }
            val completedJob = runningJob.copy(
                status = "COMPLETED",
                progressPercent = 100,
                targetResolution = finalRes,
                currentStageAr = "مكتمل بنجاح (فيديو سلس $finalRes)",
                outputFilePath = result.outputFile.absolutePath,
                outputFileName = result.outputFile.name,
                outputSizeBytes = result.outputSizeBytes,
                elapsedTimeMs = result.elapsedMs,
                encodingFps = result.averageFps,
                speedMultiplier = result.speedMultiplier
            )
            repository.updateJob(completedJob)
            val savedRatio = if (source.fileSizeBytes > 0 && result.outputSizeBytes < source.fileSizeBytes) {
                val pct = ((1.0 - (result.outputSizeBytes.toDouble() / source.fileSizeBytes.toDouble())) * 100).toInt()
                "وفّرت $pct% من الحجم"
            } else {
                "الحجم النهائي ${VideoTranscodeEngine.formatSize(result.outputSizeBytes)}"
            }
            _uiState.update {
                it.copy(
                    isEncodingNow = false,
                    activeJobProgress = 100,
                    latestCompletedJob = completedJob,
                    inspectingJobForPlayer = completedJob,
                    bannerMessageAr = "تم ضغط الفيديو بسلاسة كاملة وبنفس نسبة القياس ($finalRes)! ($savedRatio)"
                )
            }
            appendLog("اكتملت المهمة #${job.id} بنجاح ($finalRes)! الملف الناتج: ${result.outputFile.absolutePath}")
        } else {
            val detailedErr = result.errorMessage?.takeIf { it.isNotBlank() } ?: "فشل التحويل بواسطة مشفر العتاد"
            val failedJob = runningJob.copy(
                status = "FAILED",
                currentStageAr = detailedErr
            )
            repository.updateJob(failedJob)
            _uiState.update {
                it.copy(
                    isEncodingNow = false,
                    bannerMessageAr = "تعذر إكمال المهمة: $detailedErr"
                )
            }
            appendLog("فشلت المهمة #${job.id}: $detailedErr")
        }
    }

    fun cancelActiveEncode() {
        activeTranscodeJob?.cancel()
        activeTranscodeJob = null
        _uiState.update {
            it.copy(
                isEncodingNow = false,
                activeJobStageAr = "تم إلغاء عملية الضغط",
                bannerMessageAr = "تم إيقاف عملية الضغط الحالية"
            )
        }
        appendLog("تم إلغاء عملية الضغط بناءً على طلب المستخدم.")
    }

    fun deleteJob(jobId: Int) {
        viewModelScope.launch {
            repository.deleteJob(jobId)
        }
    }

    fun clearFinishedJobs() {
        viewModelScope.launch {
            repository.clearFinishedJobs()
            appendLog("تم مسح المهام المكتملة من السجل.")
        }
    }

    fun shareOutputVideo(job: TranscodeJobEntity) {
        val file = File(job.outputFilePath)
        if (!file.exists()) {
            _uiState.update { it.copy(bannerMessageAr = "ملف الفيديو غير موجود على المسار المحدد") }
            return
        }
        try {
            val context = getApplication<Application>()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, job.outputFileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة الفيديو المضغوط").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            appendLog("تم فتح نافذة مشاركة الفيديو: ${job.outputFileName}")
        } catch (e: Exception) {
            appendLog("خطأ أثناء مشاركة الفيديو: ${e.localizedMessage}")
        }
    }

    fun exportOutputVideoToUri(job: TranscodeJobEntity, targetUri: Uri) {
        viewModelScope.launch {
            try {
                val srcFile = File(job.outputFilePath)
                if (!srcFile.exists()) return@launch
                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    srcFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                _uiState.update {
                    it.copy(bannerMessageAr = "تم حفظ الفيديو المضغوط في ملفات الجهاز بنجاح!")
                }
                appendLog("تم تصدير وحفظ الفيديو إلى وحدة التخزين: $targetUri")
            } catch (e: Exception) {
                appendLog("خطأ أثناء حفظ الملف: ${e.localizedMessage}")
            }
        }
    }

    fun exportCurrentAppApkToUri(targetUri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val sourceApk = File(context.applicationInfo.sourceDir)
                if (!sourceApk.exists()) return@launch
                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    sourceApk.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                _uiState.update {
                    it.copy(bannerMessageAr = "تم حفظ نسخة التطبيق المحدثة (app_name.apk) بنجاح!")
                }
                appendLog("تم تصدير حزمة التطبيق الحالية بصيغة app_name.apk (${VideoTranscodeEngine.formatSize(sourceApk.length())})")
            } catch (e: Exception) {
                appendLog("خطأ أثناء تصدير ملف APK: ${e.localizedMessage}")
            }
        }
    }
}
