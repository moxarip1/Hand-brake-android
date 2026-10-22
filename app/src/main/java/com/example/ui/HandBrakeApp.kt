package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.TranscodeJobEntity
import com.example.engine.VideoTranscodeEngine
import com.example.model.AiOptimizationGoal
import com.example.model.AiVideoAnalysisReport
import com.example.model.HandBrakeConfig
import com.example.model.HandBrakePreset
import com.example.model.HandBrakeTab
import com.example.model.SourceVideoMetadata
import com.example.ui.screens.ActivityLogAndCodecsScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.QueueAndResultsScreen
import com.example.ui.screens.SaveCustomPresetDialog
import com.example.ui.screens.VideoPlayerComparisonDialog
import com.example.ui.tabs.AudioTabContent
import com.example.ui.tabs.ChaptersTabContent
import com.example.ui.tabs.DimensionsTabContent
import com.example.ui.tabs.FiltersTabContent
import com.example.ui.tabs.SubtitlesTabContent
import com.example.ui.tabs.SummaryTabContent
import com.example.ui.tabs.VideoTabContent
import com.example.ui.theme.HandBrakeAmber
import com.example.ui.theme.HardwareCyan
import com.example.ui.theme.SuccessEmerald
import com.example.viewmodel.HandBrakeViewModel
import com.example.viewmodel.MainNavDestination
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandBrakeApp(
    viewModel: HandBrakeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val jobs by viewModel.allJobs.collectAsStateWithLifecycle()

    var pendingExportJob by remember { mutableStateOf<TranscodeJobEntity?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadVideoFromUri(uri)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/*")
    ) { targetUri ->
        val job = pendingExportJob
        if (targetUri != null && job != null) {
            viewModel.exportOutputVideoToUri(job, targetUri)
        }
        pendingExportJob = null
    }

    val exportApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { targetUri ->
        if (targetUri != null) {
            viewModel.exportCurrentAppApkToUri(targetUri)
        }
    }

    if (uiState.currentNav != MainNavDestination.STUDIO) {
        BackHandler {
            viewModel.selectNav(MainNavDestination.STUDIO)
        }
    }

    val queuedCount = jobs.count { it.status == "QUEUED" }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = HandBrakeAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "HandBrake Mobile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ضغط ذكي بالذكاء الاصطناعي وبنفس القياس الأصلي 1:1",
                                style = MaterialTheme.typography.labelSmall,
                                color = HardwareCyan
                            )
                        }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { exportApkLauncher.launch("app_name.apk") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("export_apk_button")
                    ) {
                        Text("APK", style = MaterialTheme.typography.labelLarge, color = HardwareCyan)
                    }
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("top_pick_video_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = HandBrakeAmber)
                        Spacer(Modifier.width(4.dp))
                        Text("فتح فيديو", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                if (uiState.currentNav == MainNavDestination.STUDIO && uiState.sourceVideo != null) {
                    BottomEncodeControlBar(
                        source = uiState.sourceVideo!!,
                        config = uiState.config,
                        isEncodingNow = uiState.isEncodingNow,
                        activeProgress = uiState.activeJobProgress,
                        activeStageAr = uiState.activeJobStageAr,
                        activeFps = uiState.activeJobFps,
                        onAddToQueue = { viewModel.enqueueCurrentJob(startImmediately = false) },
                        onStartEncodeNow = { viewModel.enqueueCurrentJob(startImmediately = true) }
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.currentNav == MainNavDestination.STUDIO,
                        onClick = { viewModel.selectNav(MainNavDestination.STUDIO) },
                        icon = { Icon(Icons.Default.MovieCreation, contentDescription = "الاستوديو") },
                        label = { Text("الاستوديو") },
                        modifier = Modifier.testTag("nav_studio")
                    )
                    NavigationBarItem(
                        selected = uiState.currentNav == MainNavDestination.PRESETS,
                        onClick = { viewModel.selectNav(MainNavDestination.PRESETS) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "الإعدادات المسبقة") },
                        label = { Text("الإعدادات المسبقة") },
                        modifier = Modifier.testTag("nav_presets")
                    )
                    NavigationBarItem(
                        selected = uiState.currentNav == MainNavDestination.QUEUE,
                        onClick = { viewModel.selectNav(MainNavDestination.QUEUE) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (queuedCount > 0 || uiState.isEncodingNow) {
                                        Badge(containerColor = HandBrakeAmber) {
                                            Text(if (uiState.isEncodingNow) "⚡" else "$queuedCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.QueuePlayNext, contentDescription = "طابور المهام")
                            }
                        },
                        label = { Text("طابور المهام") },
                        modifier = Modifier.testTag("nav_queue")
                    )
                    NavigationBarItem(
                        selected = uiState.currentNav == MainNavDestination.ACTIVITY_LOG,
                        onClick = { viewModel.selectNav(MainNavDestination.ACTIVITY_LOG) },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = "سجل الترميز") },
                        label = { Text("سجل الترميز") },
                        modifier = Modifier.testTag("nav_logs")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = uiState.bannerMessageAr != null) {
                uiState.bannerMessageAr?.let { msg ->
                    Surface(
                        color = HandBrakeAmber.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = HandBrakeAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق التنبيه", tint = HandBrakeAmber)
                            }
                        }
                    }
                }
            }

            when (uiState.currentNav) {
                MainNavDestination.STUDIO -> {
                    StudioWorkspaceScreen(
                        source = uiState.sourceVideo,
                        isLoadingSource = uiState.isLoadingSource,
                        config = uiState.config,
                        presets = presets,
                        selectedTab = uiState.selectedTab,
                        selectedThumbIndex = uiState.selectedPreviewThumbIndex,
                        selectedAiGoal = uiState.selectedAiGoal,
                        isAnalyzingWithAi = uiState.isAnalyzingWithAi,
                        aiAnalysisReport = uiState.aiAnalysisReport,
                        onSelectAiGoal = viewModel::selectAiGoalAndAnalyze,
                        onRunAiAnalysis = { viewModel.runAiVideoAnalysis(autoApply = true) },
                        onApplyAiReport = viewModel::applyAiRecommendation,
                        onSelectTab = viewModel::selectTab,
                        onSelectThumb = viewModel::selectPreviewThumb,
                        onPickVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onGenerateSampleVideo = { viewModel.loadOrGenerateSampleVideo(silentBanner = false) },
                        onApplyPreset = viewModel::applyPreset,
                        onOpenSavePreset = { viewModel.setShowSavePresetDialog(true) },
                        onUpdateConfig = viewModel::updateConfig,
                        onAddSubtitle = viewModel::addSubtitleTrack,
                        onRemoveSubtitle = viewModel::removeSubtitleTrack,
                        onToggleSubtitleBurnIn = viewModel::toggleSubtitleBurnIn,
                        onUpdateChapterTitle = viewModel::updateChapterTitle,
                        onAutoGenerateChapters = viewModel::autoGenerateChapters
                    )
                }

                MainNavDestination.PRESETS -> {
                    PresetsScreen(
                        presets = presets,
                        activePresetName = uiState.config.activePresetName,
                        onSelectPreset = viewModel::applyPreset,
                        onOpenSavePresetDialog = { viewModel.setShowSavePresetDialog(true) },
                        onDeleteCustomPreset = viewModel::deleteCustomPreset
                    )
                }

                MainNavDestination.QUEUE -> {
                    QueueAndResultsScreen(
                        jobs = jobs,
                        isEncodingNow = uiState.isEncodingNow,
                        activeProgress = uiState.activeJobProgress,
                        activeStageAr = uiState.activeJobStageAr,
                        activeFps = uiState.activeJobFps,
                        activeSpeed = uiState.activeJobSpeed,
                        activeLiveBytes = uiState.activeJobLiveBytes,
                        onStartQueueBatch = viewModel::startQueueBatch,
                        onCancelActive = viewModel::cancelActiveEncode,
                        onClearFinished = viewModel::clearFinishedJobs,
                        onDeleteJob = viewModel::deleteJob,
                        onInspectAndPlayJob = { job -> viewModel.setInspectingJobForPlayer(job) },
                        onShareJob = viewModel::shareOutputVideo,
                        onExportJobToDevice = { job ->
                            pendingExportJob = job
                            createDocumentLauncher.launch(job.outputFileName)
                        }
                    )
                }

                MainNavDestination.ACTIVITY_LOG -> {
                    ActivityLogAndCodecsScreen(
                        source = uiState.sourceVideo,
                        config = uiState.config,
                        logs = uiState.activityLogs,
                        hardwareCodecs = uiState.deviceHardwareCodecs,
                        onClearLogs = viewModel::clearLogs
                    )
                }
            }
        }
    }

    uiState.inspectingJobForPlayer?.let { job ->
        VideoPlayerComparisonDialog(
            job = job,
            onDismiss = { viewModel.setInspectingJobForPlayer(null) },
            onShare = { viewModel.shareOutputVideo(job) },
            onExport = {
                pendingExportJob = job
                createDocumentLauncher.launch(job.outputFileName)
            }
        )
    }

    if (uiState.showSavePresetDialog) {
        SaveCustomPresetDialog(
            currentConfig = uiState.config,
            onDismiss = { viewModel.setShowSavePresetDialog(false) },
            onSave = viewModel::saveCustomPreset
        )
    }
}

@Composable
private fun StudioWorkspaceScreen(
    source: SourceVideoMetadata?,
    isLoadingSource: Boolean,
    config: HandBrakeConfig,
    presets: List<HandBrakePreset>,
    selectedTab: HandBrakeTab,
    selectedThumbIndex: Int,
    selectedAiGoal: AiOptimizationGoal,
    isAnalyzingWithAi: Boolean,
    aiAnalysisReport: AiVideoAnalysisReport?,
    onSelectAiGoal: (AiOptimizationGoal) -> Unit,
    onRunAiAnalysis: () -> Unit,
    onApplyAiReport: (AiVideoAnalysisReport) -> Unit,
    onSelectTab: (HandBrakeTab) -> Unit,
    onSelectThumb: (Int) -> Unit,
    onPickVideo: () -> Unit,
    onGenerateSampleVideo: () -> Unit,
    onApplyPreset: (HandBrakePreset) -> Unit,
    onOpenSavePreset: () -> Unit,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit,
    onAddSubtitle: (String, String, Boolean) -> Unit,
    onRemoveSubtitle: (Int) -> Unit,
    onToggleSubtitleBurnIn: (Int) -> Unit,
    onUpdateChapterTitle: (Int, String) -> Unit,
    onAutoGenerateChapters: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SourceVideoHeaderCard(
                source = source,
                config = config,
                isLoading = isLoadingSource,
                isAnalyzingWithAi = isAnalyzingWithAi,
                onPickVideo = onPickVideo,
                onRunAiOptimize = {
                    onSelectTab(HandBrakeTab.SUMMARY)
                    onRunAiAnalysis()
                },
                onGenerateSampleVideo = onGenerateSampleVideo
            )
        }

        item {
            QuickPresetBar(
                presets = presets,
                activePresetName = config.activePresetName,
                onSelectPreset = onApplyPreset,
                onOpenSavePreset = onOpenSavePreset
            )
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = HandBrakeAmber,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                HandBrakeTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    Tab(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("hb_tab_${tab.name}"),
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tab.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tab.subtitleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }

        item {
            when (selectedTab) {
                HandBrakeTab.SUMMARY -> SummaryTabContent(
                    source = source,
                    config = config,
                    selectedThumbIndex = selectedThumbIndex,
                    selectedAiGoal = selectedAiGoal,
                    isAnalyzingWithAi = isAnalyzingWithAi,
                    aiAnalysisReport = aiAnalysisReport,
                    onSelectAiGoal = onSelectAiGoal,
                    onRunAiAnalysis = onRunAiAnalysis,
                    onApplyAiReport = onApplyAiReport,
                    onSelectThumb = onSelectThumb,
                    onUpdateConfig = onUpdateConfig
                )
                HandBrakeTab.DIMENSIONS -> DimensionsTabContent(
                    source = source,
                    config = config,
                    onUpdateConfig = onUpdateConfig
                )
                HandBrakeTab.FILTERS -> FiltersTabContent(
                    config = config,
                    onUpdateConfig = onUpdateConfig
                )
                HandBrakeTab.VIDEO -> VideoTabContent(
                    source = source,
                    config = config,
                    onUpdateConfig = onUpdateConfig
                )
                HandBrakeTab.AUDIO -> AudioTabContent(
                    source = source,
                    config = config,
                    onUpdateConfig = onUpdateConfig
                )
                HandBrakeTab.SUBTITLES -> SubtitlesTabContent(
                    config = config,
                    onAddSubtitle = onAddSubtitle,
                    onRemoveSubtitle = onRemoveSubtitle,
                    onToggleBurnIn = onToggleSubtitleBurnIn
                )
                HandBrakeTab.CHAPTERS -> ChaptersTabContent(
                    source = source,
                    config = config,
                    onUpdateConfig = onUpdateConfig,
                    onUpdateChapterTitle = onUpdateChapterTitle,
                    onAutoGenerateChapters = onAutoGenerateChapters
                )
            }
        }
    }
}

@Composable
private fun SourceVideoHeaderCard(
    source: SourceVideoMetadata?,
    config: HandBrakeConfig,
    isLoading: Boolean,
    isAnalyzingWithAi: Boolean,
    onPickVideo: () -> Unit,
    onRunAiOptimize: () -> Unit,
    onGenerateSampleVideo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_handbrake_hero),
                    contentDescription = "HandBrake Studio Graphic",
                    modifier = Modifier
                        .width(92.dp)
                        .height(62.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = HandBrakeAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = source?.fileName ?: "جاري تحضير الفيديو...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (source != null) {
                        val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, config)
                        Text(
                            text = "القياس المثبت: ${outW}×${outH} (${if (source.isPortraitReels) "ريلز 9:16" else "كامل 1:1"}) • ${source.frameRate.roundToInt()} fps سلس",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessEmerald
                        )
                        Text(
                            text = "الحجم الأصلي: ${VideoTranscodeEngine.formatSize(source.fileSizeBytes)} • المدة: ${
                                VideoTranscodeEngine.formatDuration(source.durationMs)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isLoading) {
                        Text(
                            text = "جاري فحص المسارات وتثبيت الأبعاد الأصلية...",
                            style = MaterialTheme.typography.bodySmall,
                            color = HandBrakeAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_device_video_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("اختيار فيديو", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRunAiOptimize,
                    enabled = !isAnalyzingWithAi && source != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_smart_optimize_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HardwareCyan)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("تحليل ذكي AI", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onGenerateSampleVideo,
                    modifier = Modifier.testTag("generate_sample_video_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("تجريبي")
                }
            }
        }
    }
}

@Composable
private fun QuickPresetBar(
    presets: List<HandBrakePreset>,
    activePresetName: String,
    onSelectPreset: (HandBrakePreset) -> Unit,
    onOpenSavePreset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الإعداد النشط: $activePresetName",
                style = MaterialTheme.typography.labelLarge,
                color = HandBrakeAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenSavePreset() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = HardwareCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "حفظ كإعداد مخصص",
                    style = MaterialTheme.typography.labelLarge,
                    color = HardwareCyan
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val selected = preset.name == activePresetName || preset.nameAr == activePresetName
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.nameAr) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HandBrakeAmber.copy(alpha = 0.22f),
                        selectedLabelColor = HandBrakeAmber
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomEncodeControlBar(
    source: SourceVideoMetadata,
    config: HandBrakeConfig,
    isEncodingNow: Boolean,
    activeProgress: Int,
    activeStageAr: String,
    activeFps: Float,
    onAddToQueue: () -> Unit,
    onStartEncodeNow: () -> Unit
) {
    val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, config)
    val estBytes = VideoTranscodeEngine.estimateOutputSizeBytes(source, config)
    val savingsPct = ((1.0 - (estBytes.toDouble() / source.fileSizeBytes.coerceAtLeast(1L))) * 100).roundToInt()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEncodingNow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeStageAr ($activeProgress%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = HandBrakeAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f fps", activeFps),
                        style = MaterialTheme.typography.labelMedium,
                        color = HardwareCyan
                    )
                }
                LinearProgressIndicator(
                    progress = { (activeProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = HandBrakeAmber
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الناتج: ${VideoTranscodeEngine.formatSize(estBytes)} • القياس: ${outW}×${outH}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (savingsPct > 0) {
                                "يوفر $savingsPct% من الحجم (${VideoTranscodeEngine.formatSize(source.fileSizeBytes)}) بدون تقطيع"
                            } else {
                                "استخراج سلس بنفس قياس المصدر (${outW}×${outH})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (savingsPct > 0) SuccessEmerald else HardwareCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onAddToQueue,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_to_queue_button")
                        ) {
                            Icon(Icons.Default.AddToQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("للطابور")
                        }

                        Button(
                            onClick = onStartEncodeNow,
                            colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("start_encode_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("بدء الضغط الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
