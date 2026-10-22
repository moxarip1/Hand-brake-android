package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.VideoTranscodeEngine
import com.example.model.AiOptimizationGoal
import com.example.model.AiVideoAnalysisReport
import com.example.model.AnamorphicMode
import com.example.model.ContainerFormat
import com.example.model.HandBrakeConfig
import com.example.model.ResolutionLimit
import com.example.model.SourceVideoMetadata
import com.example.ui.theme.HandBrakeAmber
import com.example.ui.theme.HardwareCyan
import com.example.ui.theme.SuccessEmerald
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryTabContent(
    source: SourceVideoMetadata?,
    config: HandBrakeConfig,
    selectedThumbIndex: Int,
    selectedAiGoal: AiOptimizationGoal,
    isAnalyzingWithAi: Boolean,
    aiAnalysisReport: AiVideoAnalysisReport?,
    onSelectAiGoal: (AiOptimizationGoal) -> Unit,
    onRunAiAnalysis: () -> Unit,
    onApplyAiReport: (AiVideoAnalysisReport) -> Unit,
    onSelectThumb: (Int) -> Unit,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. AI Smart Video Analyzer & Optimal Settings Advisor Card
        if (source != null) {
            AiVideoAdvisorCard(
                source = source,
                selectedGoal = selectedAiGoal,
                isAnalyzing = isAnalyzingWithAi,
                report = aiAnalysisReport,
                onSelectGoal = onSelectAiGoal,
                onRunAnalysis = onRunAiAnalysis,
                onApplyReport = onApplyAiReport
            )
        }

        // 2. Live Interactive Frame Preview + Exact Aspect Ratio (Reels 9:16 & Landscape 16:9)
        if (source != null) {
            LivePreviewCard(
                source = source,
                config = config,
                selectedThumbIndex = selectedThumbIndex,
                onSelectThumb = onSelectThumb,
                onUpdateConfig = onUpdateConfig
            )
        }

        // 3. Container Format Selection (Format: MP4, MKV, WebM)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "صيغة الحاوية (Container Format)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContainerFormat.entries.forEach { format ->
                        val selected = config.container == format
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdateConfig { it.copy(container = format) } }
                                .testTag("container_${format.extension}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    HandBrakeAmber.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) HandBrakeAmber else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = format.extension.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = format.displayName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = config.container.descriptionAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                HandBrakeSwitchRow(
                    title = "تحسين للبث السريع عبر الويب (Web Optimized / FastStart)",
                    subtitle = "ينقل ذرة moov إلى بداية ملف MP4 ليعمل فوراً أثناء التحميل",
                    checked = config.webOptimized,
                    tag = "switch_web_optimized",
                    onCheckedChange = { checked -> onUpdateConfig { it.copy(webOptimized = checked) } }
                )

                HandBrakeSwitchRow(
                    title = "مزامنة بداية الصوت والفيديو (Align A/V Start)",
                    subtitle = "يمنع تأخر الصوت عن الصورة ويحافظ على تزامن الإطارات",
                    checked = config.alignAvStart,
                    tag = "switch_align_av",
                    onCheckedChange = { checked -> onUpdateConfig { it.copy(alignAvStart = checked) } }
                )

                HandBrakeSwitchRow(
                    title = "الاحتفاظ بالبيانات الوصفية (Passthru Common Metadata)",
                    subtitle = "ينسخ تاريخ التصوير واتجاه الفيديو الأصلي إلى الملف المضغوط",
                    checked = config.passthruMetadata,
                    tag = "switch_metadata_passthru",
                    onCheckedChange = { checked -> onUpdateConfig { it.copy(passthruMetadata = checked) } }
                )
            }
        }

        // 4. Summary Technical Track Inspector
        if (source != null) {
            val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, config)
            val targetBitrateKbps = VideoTranscodeEngine.computeTargetVideoBitrateBps(source, config) / 1000
            val estBytes = VideoTranscodeEngine.estimateOutputSizeBytes(source, config)
            val savingsPct = ((1.0 - (estBytes.toDouble() / source.fileSizeBytes.coerceAtLeast(1L))) * 100).roundToInt()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HardwareCyan.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null, tint = HardwareCyan)
                            Text(
                                text = "معاينة ناتج الضغط (Output Track Summary)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (savingsPct > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuccessEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "توفير $savingsPct% من الحجم",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SuccessEmerald
                                )
                            }
                        }
                    }

                    SummaryDetailRow(
                        label = "الحجم الأصلي ← المتوقع",
                        value = "${VideoTranscodeEngine.formatSize(source.fileSizeBytes)}  ←  ${VideoTranscodeEngine.formatSize(estBytes)}",
                        highlightColor = SuccessEmerald
                    )
                    SummaryDetailRow(
                        label = "الأبعاد والقياس (Dimensions)",
                        value = "${source.width}×${source.height}  ←  ${outW}×${outH} (${if (outW == source.width && outH == source.height) "نفس القياس الأصلي 1:1" else config.resolutionLimit.name})",
                        highlightColor = HardwareCyan
                    )
                    SummaryDetailRow(
                        label = "مُرمّز الفيديو (Video Encoder)",
                        value = "${config.videoCodec.displayName} • ~$targetBitrateKbps kbps"
                    )
                    SummaryDetailRow(
                        label = "سلاسة الإطارات والسرعة",
                        value = "${if (config.framerateTarget == 0) "${source.frameRate.roundToInt()} fps (نفس المصدر)" else "${config.framerateTarget} fps"} • Hardware Surface"
                    )
                    SummaryDetailRow(
                        label = "مسار الصوت (Audio Track)",
                        value = if (config.audioCodec.isMuted) {
                            "بدون صوت (Muted - صامت)"
                        } else if (config.audioCodec.isPassthru) {
                            "نسخ مباشر نقي (Auto Passthru ${source.audioCodecName})"
                        } else {
                            "${config.audioCodec.name} • ${config.audioBitrateKbps} kbps • ${config.audioMixdown.labelAr}"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiVideoAdvisorCard(
    source: SourceVideoMetadata,
    selectedGoal: AiOptimizationGoal,
    isAnalyzing: Boolean,
    report: AiVideoAnalysisReport?,
    onSelectGoal: (AiOptimizationGoal) -> Unit,
    onRunAnalysis: () -> Unit,
    onApplyReport: (AiVideoAnalysisReport) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, HardwareCyan.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = HardwareCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "محلل الفيديو الذكي بالذكاء الاصطناعي (AI Optimizer)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HardwareCyan
                        )
                        Text(
                            text = "يحلل أبعاد الفيديو ومحتوى الإطارات لاختيار أفضل إعدادات ضغط بدون تقليص أو تقطيع",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = HardwareCyan,
                        strokeWidth = 2.5.dp
                    )
                }
            }

            Text(
                text = "اختر هدف التحليل والضبط الذكي:",
                style = MaterialTheme.typography.labelLarge,
                color = HandBrakeAmber
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiOptimizationGoal.entries.forEach { goal ->
                    val selected = selectedGoal == goal
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectGoal(goal) },
                        label = { Text(goal.titleAr) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HardwareCyan.copy(alpha = 0.22f),
                            selectedLabelColor = HardwareCyan
                        ),
                        modifier = Modifier.testTag("ai_goal_${goal.name}")
                    )
                }
            }

            if (report != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نتيجة فحص الذكاء الاصطناعي (${if (report.poweredByGeminiCloud) "Gemini 3.5 Flash" else "Smart AI Analyzer"}):",
                                style = MaterialTheme.typography.labelLarge,
                                color = HandBrakeAmber
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SuccessEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "توفير ~${report.estimatedSavingsPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessEmerald
                                )
                            }
                        }

                        Text(
                            text = "• نوع الفيديو المكتشف: ${report.contentTypeAr}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• التشخيص التقني: ${report.technicalDiagnosisAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• الإعدادات المثالية المطبقة: ${report.recommendedCodec.shortLabel} • RF ${report.recommendedRf.toInt()} • أبعاد 1:1 (${source.width}×${source.height}) • ${report.recommendedSpeed.labelAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HardwareCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = report.whyTheseSettingsAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (report != null) onApplyReport(report) else onRunAnalysis()
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_ai_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HardwareCyan)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (report != null) "تطبيق الإعدادات المثالية تلقائياً" else "تحليل الفيديو بالذكاء الاصطناعي",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Prototype API Key Security Caution Notice per gemini-api skill
            Text(
                text = "ملاحظة أمان (Prototype): يتم استدعاء Gemini API عبر مفتاح BuildConfig.GEMINI_API_KEY المضاف في لوحة Secrets. لا تشارك ملف الـ APK علناً إذا كان يتضمن مفتاحاً خاصاً.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun LivePreviewCard(
    source: SourceVideoMetadata,
    config: HandBrakeConfig,
    selectedThumbIndex: Int,
    onSelectThumb: (Int) -> Unit,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit
) {
    val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, config)
    val isPortrait = outH > outW
    val rawBitmap = source.thumbnails.getOrNull(selectedThumbIndex) ?: source.thumbnails.firstOrNull()
    val processedPreview = remember(
        rawBitmap,
        outW,
        outH,
        config.cropTop,
        config.cropBottom,
        config.cropLeft,
        config.cropRight,
        config.rotationDegrees,
        config.flipHorizontal,
        config.grayscale,
        config.brightnessOffset,
        config.contrastScale,
        config.saturationScale,
        config.padTop,
        config.padBottom
    ) {
        rawBitmap?.let {
            val maxDim = 340
            val scale = maxDim.toFloat() / max(outW, outH).coerceAtLeast(1)
            val prevW = (outW * scale).roundToInt().coerceAtLeast(120)
            val prevH = (outH * scale).roundToInt().coerceAtLeast(120)
            VideoTranscodeEngine.applyHandBrakeFiltersAndCrop(
                it,
                prevW,
                prevH,
                config
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المعاينة الحية وتحديد النطاق (${if (isPortrait) "قياس ريلز عمودي 9:16" else "قياس كامل"})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HardwareCyan.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${outW}×${outH} (1:1)",
                        style = MaterialTheme.typography.labelMedium,
                        color = HardwareCyan
                    )
                }
            }

            if (processedPreview != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isPortrait) 250.dp else 185.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = processedPreview.asImageBitmap(),
                        contentDescription = "معاينة الإطار بالقياس الفعلي",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    val burnInSub = config.subtitles.firstOrNull { it.burnIn }
                    if (burnInSub != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = burnInSub.sampleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (source.thumbnails.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    source.thumbnails.forEachIndexed { idx, thumb ->
                        val isSelected = idx == selectedThumbIndex
                        Box(
                            modifier = Modifier
                                .width(if (isPortrait) 52.dp else 76.dp)
                                .height(if (isPortrait) 76.dp else 46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) HandBrakeAmber else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectThumb(idx) }
                        ) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "إطار معاينة ${idx + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            val startMs = (source.durationMs * config.trimStartFraction).toLong()
            val endMs = (source.durationMs * config.trimEndFraction).toLong()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نطاق القص: ${VideoTranscodeEngine.formatDuration(startMs)} إلى ${VideoTranscodeEngine.formatDuration(endMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HandBrakeAmber
                )
                Text(
                    text = "المدة الصافية: ${VideoTranscodeEngine.formatDuration((endMs - startMs).coerceAtLeast(500L))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RangeSlider(
                value = config.trimStartFraction..config.trimEndFraction,
                onValueChange = { range ->
                    val start = range.start.coerceIn(0f, 0.9f)
                    val end = range.endInclusive.coerceIn(start + 0.1f, 1f)
                    onUpdateConfig { it.copy(trimStartFraction = start, trimEndFraction = end) }
                },
                valueRange = 0f..1f,
                modifier = Modifier.testTag("trim_range_slider")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DimensionsTabContent(
    source: SourceVideoMetadata?,
    config: HandBrakeConfig,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Resolution Limit & Anamorphic
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "الدقة وتغيير الحجم (Resolution & Scaling)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (source != null) {
                    val (outW, outH) = VideoTranscodeEngine.computeOutputDimensions(source, config)
                    Text(
                        text = "القياس الأصلي: ${source.width}×${source.height}  ←  قياس الاستخراج الفعلي: ${outW}×${outH}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessEmerald
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResolutionLimit.entries.forEach { limit ->
                        FilterChip(
                            selected = config.resolutionLimit == limit,
                            onClick = { onUpdateConfig { it.copy(resolutionLimit = limit) } },
                            label = { Text(limit.labelAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HandBrakeAmber.copy(alpha = 0.22f),
                                selectedLabelColor = HandBrakeAmber
                            ),
                            modifier = Modifier.testTag("res_${limit.name}")
                        )
                    }
                }

                if (config.resolutionLimit == ResolutionLimit.CUSTOM) {
                    val isPortrait = (source?.height ?: 1920) > (source?.width ?: 1080)
                    Text(
                        text = "العرض المخصص: ${config.customWidth}px × الارتفاع: ${config.customHeight}px",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HardwareCyan
                    )
                    Slider(
                        value = config.customWidth.toFloat(),
                        onValueChange = { w ->
                            val alignedW = ((w.roundToInt() / 2) * 2).coerceIn(320, 3840)
                            val alignedH = if (isPortrait) {
                                (((alignedW * 16) / 9) / 2) * 2
                            } else {
                                (((alignedW * 9) / 16) / 2) * 2
                            }
                            onUpdateConfig { it.copy(customWidth = alignedW, customHeight = alignedH) }
                        },
                        valueRange = 320f..3840f
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "النظام الأنومورفي (Anamorphic Pixel Aspect)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnamorphicMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.anamorphicMode == mode,
                            onClick = { onUpdateConfig { it.copy(anamorphicMode = mode) } },
                            label = { Text(mode.labelAr) }
                        )
                    }
                }
            }
        }

        // 2. Rotation & Flip
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, tint = HardwareCyan)
                    Text(
                        text = "التدوير والانعكاس (Orientation & Flip)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 90, 180, 270).forEach { deg ->
                        FilterChip(
                            selected = config.rotationDegrees == deg,
                            onClick = { onUpdateConfig { it.copy(rotationDegrees = deg) } },
                            label = { Text("${deg}°") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HandBrakeSwitchRow(
                    title = "عكس الفيديو أفقياً (Horizontal Flip / Mirror)",
                    subtitle = "يعكس اتجاه الصورة يميناً ويساراً أثناء الضغط",
                    checked = config.flipHorizontal,
                    tag = "switch_flip_horizontal",
                    onCheckedChange = { f -> onUpdateConfig { it.copy(flipHorizontal = f) } }
                )
            }
        }

        // 3. Cropping & Padding Borders
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "القص والحواف (Cropping & Padding)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HandBrakeSwitchRow(
                    title = "بدون قص / الحفاظ على الإطار الكامل (Full Frame 0px Crop)",
                    subtitle = "يحافظ على كامل إطار الفيديو الأصلي بدون اقتطاع أي بكسل",
                    checked = config.autoCrop,
                    tag = "switch_auto_crop",
                    onCheckedChange = { auto ->
                        onUpdateConfig {
                            if (auto) it.copy(autoCrop = true, cropTop = 0, cropBottom = 0, cropLeft = 0, cropRight = 0)
                            else it.copy(autoCrop = false)
                        }
                    }
                )

                if (!config.autoCrop) {
                    CropSliderRow("قص من الأعلى (Top)", config.cropTop) { v ->
                        onUpdateConfig { it.copy(cropTop = v) }
                    }
                    CropSliderRow("قص من الأسفل (Bottom)", config.cropBottom) { v ->
                        onUpdateConfig { it.copy(cropBottom = v) }
                    }
                    CropSliderRow("قص من اليمين (Right)", config.cropRight) { v ->
                        onUpdateConfig { it.copy(cropRight = v) }
                    }
                    CropSliderRow("قص من اليسار (Left)", config.cropLeft) { v ->
                        onUpdateConfig { it.copy(cropLeft = v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropSliderRow(label: String, valuePx: Int, onValueChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = "${valuePx}px", style = MaterialTheme.typography.labelMedium, color = HandBrakeAmber)
        }
        Slider(
            value = valuePx.toFloat(),
            onValueChange = { onValueChange((it.roundToInt() / 2) * 2) },
            valueRange = 0f..160f
        )
    }
}

@Composable
fun HandBrakeSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun SummaryDetailRow(
    label: String,
    value: String,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = highlightColor
        )
    }
}
