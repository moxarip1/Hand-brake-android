package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.TranscodeJobEntity
import com.example.engine.VideoTranscodeEngine
import com.example.model.HandBrakeConfig
import com.example.model.HandBrakePreset
import com.example.model.HardwareCodecInfo
import com.example.model.PresetCategory
import com.example.model.SourceVideoMetadata
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.HandBrakeAmber
import com.example.ui.theme.HardwareCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetsScreen(
    presets: List<HandBrakePreset>,
    activePresetName: String,
    onSelectPreset: (HandBrakePreset) -> Unit,
    onOpenSavePresetDialog: () -> Unit,
    onDeleteCustomPreset: (HandBrakePreset) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    val filteredPresets = remember(presets, selectedCategory) {
        if (selectedCategory == null) presets
        else presets.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HandBrakeAmber.copy(alpha = 0.4f))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مكتبة الإعدادات المسبقة (Official & Custom Presets)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اختر إعداداً جاهزاً من HandBrake أو احفظ إعداداتك الخاصة بضغطة واحدة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenSavePresetDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber),
                            modifier = Modifier.testTag("open_save_preset_dialog_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ إعداد جديد", fontWeight = FontWeight.Bold)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل (${presets.size})") }
                        )
                        PresetCategory.entries.forEach { cat ->
                            val count = presets.count { it.category == cat }
                            if (count > 0 || cat == PresetCategory.CUSTOM) {
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${cat.titleAr} ($count)") }
                                )
                            }
                        }
                    }
                }
            }
        }

        items(filteredPresets, key = { it.id }) { preset ->
            val isSelected = preset.name == activePresetName || preset.nameAr == activePresetName
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectPreset(preset) }
                    .testTag("preset_item_${preset.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        HandBrakeAmber.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) HandBrakeAmber else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isSelected) HandBrakeAmber else HardwareCyan
                            )
                            Column {
                                Text(
                                    text = preset.nameAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.category.titleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HardwareCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset.badgeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HardwareCyan
                                )
                            }
                            if (preset.isCustom) {
                                IconButton(onClick = { onDeleteCustomPreset(preset) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف الإعداد المخصص",
                                        tint = ErrorCoral
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = preset.descriptionAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QueueAndResultsScreen(
    jobs: List<TranscodeJobEntity>,
    isEncodingNow: Boolean,
    activeProgress: Int,
    activeStageAr: String,
    activeFps: Float,
    activeSpeed: Float,
    activeLiveBytes: Long,
    onStartQueueBatch: () -> Unit,
    onCancelActive: () -> Unit,
    onClearFinished: () -> Unit,
    onDeleteJob: (Int) -> Unit,
    onInspectAndPlayJob: (TranscodeJobEntity) -> Unit,
    onShareJob: (TranscodeJobEntity) -> Unit,
    onExportJobToDevice: (TranscodeJobEntity) -> Unit
) {
    val queuedCount = jobs.count { it.status == "QUEUED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Encoding Monitor Card
        if (isEncodingNow) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, HandBrakeAmber)
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
                            Text(
                                text = "جاري الضغط والتحويل الآن ($activeProgress%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HandBrakeAmber
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f fps (%.1fx)", activeFps, activeSpeed),
                                style = MaterialTheme.typography.labelMedium,
                                color = HardwareCyan
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (activeProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = HandBrakeAmber
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeStageAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = VideoTranscodeEngine.formatSize(activeLiveBytes),
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessEmerald
                            )
                        }

                        OutlinedButton(
                            onClick = onCancelActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cancel_active_encode_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral)
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("إيقاف عملية الضغط الحالية")
                        }
                    }
                }
            }
        }

        // Batch Queue Header Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "طابور المهام وسجل الفيديوهات المضغوطة (${jobs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (queuedCount > 0) "يوجد $queuedCount مهام في الانتظار جاهزة للتشغيل الدفعي"
                            else "جميع المهام مكتملة — يمكنك تشغيل أو مشاركة أو حفظ أي فيديو مضغوط",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (jobs.any { it.status == "COMPLETED" || it.status == "FAILED" }) {
                            IconButton(
                                onClick = onClearFinished,
                                modifier = Modifier.testTag("clear_finished_jobs_button")
                            ) {
                                Icon(Icons.Default.ClearAll, contentDescription = "مسح المكتمل")
                            }
                        }
                        Button(
                            onClick = onStartQueueBatch,
                            enabled = !isEncodingNow,
                            colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber),
                            modifier = Modifier.testTag("start_queue_batch_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (queuedCount > 0) "بدء الطابور ($queuedCount)" else "ضغط جديد")
                        }
                    }
                }
            }
        }

        if (jobs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.QueuePlayNext,
                            contentDescription = null,
                            tint = HandBrakeAmber,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "طابور المهام فارغ حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط على 'بدء الضغط الآن' أو 'إضافة للطابور' من شاشة الاستوديو لضغط الفيديوهات بسرعة فائقة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(jobs, key = { it.id }) { job ->
            val isCompleted = job.status == "COMPLETED"
            val savingsPct = if (job.sourceSizeBytes > 0 && job.outputSizeBytes in 1 until job.sourceSizeBytes) {
                ((1.0 - (job.outputSizeBytes.toDouble() / job.sourceSizeBytes.toDouble())) * 100).roundToInt()
            } else 0

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = when (job.status) {
                        "COMPLETED" -> SuccessEmerald.copy(alpha = 0.5f)
                        "RUNNING" -> HandBrakeAmber
                        "FAILED" -> ErrorCoral.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.outputFileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الإعداد: ${job.presetName} • ${job.videoCodecLabel} • ${job.targetResolution}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onDeleteJob(job.id) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "حذف المهمة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Before / After Size & Speed Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCompleted) {
                                "الحجم: ${VideoTranscodeEngine.formatSize(job.sourceSizeBytes)} ← ${
                                    VideoTranscodeEngine.formatSize(job.outputSizeBytes)
                                }"
                            } else {
                                "الحجم الأصلي: ${VideoTranscodeEngine.formatSize(job.sourceSizeBytes)} • الحالة: ${job.currentStageAr}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCompleted) SuccessEmerald else HandBrakeAmber
                        )

                        if (isCompleted && savingsPct > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuccessEmerald.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "وفّر $savingsPct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SuccessEmerald
                                )
                            }
                        }
                    }

                    if (isCompleted) {
                        Text(
                            text = String.format(
                                java.util.Locale.US,
                                "السرعة: %.1f fps (%.1fx) • زمن التنفيذ: %.2f ثانية",
                                job.encodingFps,
                                job.speedMultiplier,
                                job.elapsedTimeMs / 1000f
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = HardwareCyan
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onInspectAndPlayJob(job) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_job_${job.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = HardwareCyan)
                            ) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("تشغيل ومعاينة", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onShareJob(job) },
                                modifier = Modifier.testTag("share_job_${job.id}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة")
                            }

                            OutlinedButton(
                                onClick = { onExportJobToDevice(job) },
                                modifier = Modifier.testTag("export_job_${job.id}")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "حفظ في الجهاز")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogAndCodecsScreen(
    source: SourceVideoMetadata?,
    config: HandBrakeConfig,
    logs: List<String>,
    hardwareCodecs: List<HardwareCodecInfo>,
    onClearLogs: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val (handBrakeCli, ffmpegCli) = remember(source, config) {
        VideoTranscodeEngine.generateCliCommands(source, config)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HandBrakeCLI & FFmpeg Equivalent Command Generator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HandBrakeAmber.copy(alpha = 0.5f))
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
                            Icon(Icons.Default.Code, contentDescription = null, tint = HandBrakeAmber)
                            Text(
                                text = "أوامر سطر الأوامر (HandBrakeCLI & FFmpeg)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(handBrakeCli)) },
                            modifier = Modifier.testTag("copy_cli_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ أمر HandBrakeCLI", tint = HandBrakeAmber)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070A12))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = handBrakeCli,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = HandBrakeAmber
                        )
                    }

                    Text(
                        text = "المكافئ في FFmpeg:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070A12))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = ffmpegCli,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = HardwareCyan
                        )
                    }
                }
            }
        }

        // 2. Real-Time Activity Log Console
        item {
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = HardwareCyan)
                            Text(
                                text = "سجل النشاط المباشر (Activity Log)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(logs.joinToString("\n"))) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ السجل")
                            }
                            IconButton(onClick = onClearLogs) {
                                Icon(Icons.Default.ClearAll, contentDescription = "مسح السجل")
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070A12))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            logs.takeLast(25).forEach { line ->
                                Text(
                                    text = line,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Real Device Hardware MediaCodec Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = SuccessEmerald)
                        Text(
                            text = "مسرّعات الفيديو والعتاد المكتشفة (${hardwareCodecs.size} MediaCodecs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    hardwareCodecs.take(18).forEach { codec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = codec.codecName,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (codec.isEncoder) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${codec.mimeTypes.joinToString(", ")} • Max ${codec.maxSupportedWidth}×${codec.maxSupportedHeight}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (codec.isHardwareAccelerated) SuccessEmerald.copy(alpha = 0.2f)
                                        else HardwareCyan.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (codec.isEncoder) {
                                        if (codec.isHardwareAccelerated) "HW Encoder" else "SW Encoder"
                                    } else {
                                        if (codec.isHardwareAccelerated) "HW Decoder" else "SW Decoder"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (codec.isHardwareAccelerated) SuccessEmerald else HardwareCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerComparisonDialog(
    job: TranscodeJobEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    val fileExists = remember(job.outputFilePath) { File(job.outputFilePath).exists() }
    val savingsPct = if (job.sourceSizeBytes > 0 && job.outputSizeBytes in 1 until job.sourceSizeBytes) {
        ((1.0 - (job.outputSizeBytes.toDouble() / job.sourceSizeBytes.toDouble())) * 100).roundToInt()
    } else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "مشغل ومعاينة الفيديو المضغوط",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = job.outputFileName,
                    style = MaterialTheme.typography.labelMedium,
                    color = HardwareCyan
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fileExists) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(Uri.fromFile(File(job.outputFilePath)))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "قبل الضغط: ${VideoTranscodeEngine.formatSize(job.sourceSizeBytes)} (${job.sourceResolution})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "بعد الضغط: ${VideoTranscodeEngine.formatSize(job.outputSizeBytes)} (${job.targetResolution} • ${job.videoCodecLabel})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessEmerald
                        )
                        if (savingsPct > 0) {
                            Text(
                                text = "نسبة التوفير الفعلية: $savingsPct% (السرعة: ${
                                    String.format(java.util.Locale.US, "%.1f fps", job.encodingFps)
                                })",
                                style = MaterialTheme.typography.labelMedium,
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
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("حفظ بالجهاز")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun SaveCustomPresetDialog(
    currentConfig: HandBrakeConfig,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var descAr by remember {
        mutableStateOf(
            "حاوية ${currentConfig.container.extension.uppercase()} بترميز ${currentConfig.videoCodec.shortLabel} وجودة RF ${currentConfig.constantQualityRf.toInt()}"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حفظ إعداد مسبق خاص (Add Custom Preset)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("اسم الإعداد المسبق (مثال: ضغط يوتيوب السريع)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_preset_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text("الوصف المختصر") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameAr, descAr) },
                enabled = nameAr.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("حفظ في المكتبة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
