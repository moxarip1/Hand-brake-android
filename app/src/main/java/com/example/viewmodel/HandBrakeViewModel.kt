package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HandBrakeDatabase
import com.example.data.HandBrakeRepository
import com.example.data.TranscodeJobEntity
import com.example.engine.VideoTranscodeEngine
import com.example.model.ChapterMarker
import com.example.model.HandBrakeConfig
import com.example.model.HandBrakePreset
import com.example.model.HandBrakeTab
import com.example.model.HardwareCodecInfo
import com.example.model.OfficialHandBrakePresets
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
        "[HandBrake Mobile 1.8.2-Android] تم تهيئة محرك الترميز المسرّع بالعتاد (MediaCodec HW Engine).",
        "[System] فحص مسرعات الفيديو والـ GPU المتوفرة على الجهاز..."
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

        // Load real device hardware codecs & auto-generate/load real sample HD video on startup
        viewModelScope.launch {
            val codecs = VideoTranscodeEngine.queryDeviceCodecs()
            val hwEncoders = codecs.count { it.isEncoder && it.isHardwareAccelerated }
            appendLog("تم اكتشاف ${codecs.size} مُرمّز وفك ترميز على الجهاز ($hwEncoders مسرّع بالعتاد الصلب HW).")
            _uiState.update { it.copy(deviceHardwareCodecs = codecs) }

            // Auto-prepare a real sample video so the user can immediately inspect & compress without waiting
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
            _uiState.update { it.copy(isLoadingSource = true, bannerMessageAr = null) }
            appendLog("جاري فحص مصدر الفيديو المختار: $uri")
            val metadata = VideoTranscodeEngine.inspectVideo(getApplication(), uri, null)
            _uiState.update { state ->
                state.copy(
                    sourceVideo = metadata,
                    isLoadingSource = false,
                    selectedPreviewThumbIndex = 0,
                    config = state.config.copy(
                        customWidth = metadata.width,
                        customHeight = metadata.height,
                        trimStartFraction = 0f,
                        trimEndFraction = 1f,
                        startChapter = 1,
                        endChapter = metadata.chapters.size.coerceAtLeast(1),
                        customChapters = metadata.chapters
                    ),
                    bannerMessageAr = "تم فتح الفيديو: ${metadata.fileName} (${metadata.width}×${metadata.height})"
                )
            }
            appendLog(
                "تم تحميل المصدر: ${metadata.fileName} | الدقة: ${metadata.width}x${metadata.height} | المدة: ${
                    VideoTranscodeEngine.formatDuration(metadata.durationMs)
                } | الحجم: ${VideoTranscodeEngine.formatSize(metadata.fileSizeBytes)}"
            )
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
                            customWidth = metadata.width,
                            customHeight = metadata.height,
                            trimStartFraction = 0f,
                            trimEndFraction = 1f,
                            startChapter = 1,
                            endChapter = metadata.chapters.size.coerceAtLeast(1),
                            customChapters = metadata.chapters
                        ),
                        bannerMessageAr = if (silentBanner) null else "تم توليد وتحميل الفيديو التجريبي الحقيقي (HD 720p MP4) بنجاح!"
                    )
                }
            } catch (e: Exception) {
                appendLog("خطأ أثناء توليد الفيديو التجريبي: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoadingSource = false) }
            }
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
            appendLog("تمت إضافة المهمة #$id إلى الطابور: $outFileName")

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
                activeJobStageAr = "بدء محرك الضغط والتحويل...",
                activeJobFps = 0f,
                activeJobSpeed = 1f,
                activeJobLiveBytes = 0L,
                latestCompletedJob = null
            )
        }

        val runningJob = job.copy(status = "RUNNING", progressPercent = 2, currentStageAr = "جاري التحويل...")
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
            val completedJob = runningJob.copy(
                status = "COMPLETED",
                progressPercent = 100,
                currentStageAr = "مكتمل بنجاح",
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
                    bannerMessageAr = "تم ضغط الفيديو بنجاح! ($savedRatio)"
                )
            }
            appendLog("اكتملت المهمة #${job.id} بنجاح! الملف الناتج: ${result.outputFile.absolutePath}")
        } else {
            val failedJob = runningJob.copy(
                status = "FAILED",
                currentStageAr = result.errorMessage ?: "فشل التحويل"
            )
            repository.updateJob(failedJob)
            _uiState.update {
                it.copy(
                    isEncodingNow = false,
                    bannerMessageAr = "تعذر إكمال المهمة: ${result.errorMessage}"
                )
            }
            appendLog("فشلت المهمة #${job.id}: ${result.errorMessage}")
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
