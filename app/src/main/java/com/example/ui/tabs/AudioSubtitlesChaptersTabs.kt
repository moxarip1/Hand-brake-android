package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.VideoTranscodeEngine
import com.example.model.AudioCodec
import com.example.model.AudioMixdown
import com.example.model.HandBrakeConfig
import com.example.model.SourceVideoMetadata
import com.example.ui.theme.HandBrakeAmber
import com.example.ui.theme.HardwareCyan
import com.example.ui.theme.SuccessEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioTabContent(
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
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "إعدادات ترميز الصوت (Audio Track & Codec)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (source != null) {
                    Text(
                        text = "المسار الأصلي: ${source.audioCodecName} • ${source.audioSampleRate} Hz • ${source.audioChannels} قنوات",
                        style = MaterialTheme.typography.bodySmall,
                        color = HardwareCyan
                    )
                }

                AudioCodec.entries.forEach { codec ->
                    val selected = config.audioCodec == codec
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateConfig { it.copy(audioCodec = codec) } }
                            .testTag("audio_codec_${codec.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                HandBrakeAmber.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) HandBrakeAmber else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = codec.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = codec.labelAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!config.audioCodec.isMuted && !config.audioCodec.isPassthru) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text(
                        text = "معدل البت الصوتي (Audio Bitrate kbps)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(64, 96, 128, 160, 192, 256, 320).forEach { kbps ->
                            FilterChip(
                                selected = config.audioBitrateKbps == kbps,
                                onClick = { onUpdateConfig { it.copy(audioBitrateKbps = kbps) } },
                                label = { Text("$kbps kbps") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HardwareCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = HardwareCyan
                                )
                            )
                        }
                    }

                    Text(
                        text = "توزيع القنوات الصوتية (Mixdown)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioMixdown.entries.forEach { mix ->
                            FilterChip(
                                selected = config.audioMixdown == mix,
                                onClick = { onUpdateConfig { it.copy(audioMixdown = mix) } },
                                label = { Text(mix.labelAr) }
                            )
                        }
                    }

                    Text(
                        text = "تردد العينة (Sample Rate)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(48000 to "48 kHz (DVD/Studio)", 44100 to "44.1 kHz (CD)", 32000 to "32 kHz")
                            .forEach { (hz, label) ->
                                FilterChip(
                                    selected = config.audioSampleRateHz == hz,
                                    onClick = { onUpdateConfig { it.copy(audioSampleRateHz = hz) } },
                                    label = { Text(label) }
                                )
                            }
                    }

                    // Audio Gain dB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعزيز مستوى الصوت (Gain dB)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${if (config.audioGainDb >= 0) "+" else ""}${config.audioGainDb.roundToInt()} dB",
                            style = MaterialTheme.typography.labelMedium,
                            color = HandBrakeAmber
                        )
                    }
                    Slider(
                        value = config.audioGainDb,
                        onValueChange = { g -> onUpdateConfig { it.copy(audioGainDb = g.roundToInt().toFloat()) } },
                        valueRange = -10f..15f
                    )
                }
            }
        }
    }
}

@Composable
fun SubtitlesTabContent(
    config: HandBrakeConfig,
    onAddSubtitle: (String, String, Boolean) -> Unit,
    onRemoveSubtitle: (Int) -> Unit,
    onToggleBurnIn: (Int) -> Unit
) {
    var newLang by remember { mutableStateOf("العربية (Arabic UTF-8)") }
    var newSampleCaption by remember { mutableStateOf("تم ضغط هذا الفيديو بسرعة فائقة عبر HandBrake Mobile") }
    var newBurnIn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "مسارات الترجمة والنصوص (Subtitles & Burn-In)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                config.subtitles.forEach { sub ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.languageAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sub.sourceName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onRemoveSubtitle(sub.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف مسار الترجمة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "النص المدمج: \"${sub.sampleText}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = HardwareCyan
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دمج الترجمة داخل صورة الفيديو (Burn-In / Hardsub)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = sub.burnIn,
                                    onCheckedChange = { onToggleBurnIn(sub.id) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "إضافة مسار ترجمة خارجي (SRT / النص المدمج)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = newLang,
                    onValueChange = { newLang = it },
                    label = { Text("اللغة والترميز (Language)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newSampleCaption,
                    onValueChange = { newSampleCaption = it },
                    label = { Text("نص الترجمة / العلامة النصية المدمجة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { onAddSubtitle(newLang, newSampleCaption, newBurnIn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_subtitle_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HandBrakeAmber)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = "إضافة مسار الترجمة",
                        modifier = Modifier.padding(start = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChaptersTabContent(
    source: SourceVideoMetadata?,
    config: HandBrakeConfig,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit,
    onUpdateChapterTitle: (Int, String) -> Unit,
    onAutoGenerateChapters: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "علامات الفصول الزمنية (Chapter Markers)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HandBrakeSwitchRow(
                    title = "تضمين علامات الفصول في الحاوية (Create Chapter Markers)",
                    subtitle = "يتيح التنقل السريع بين أقسام الفيديو في المشغلات الحديثة",
                    checked = config.includeChapterMarkers,
                    tag = "switch_include_chapters",
                    onCheckedChange = { c -> onUpdateConfig { it.copy(includeChapterMarkers = c) } }
                )

                Text(
                    text = "تقسيم تلقائي للفصول كل:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "ثانية واحدة", 2 to "ثانيتين", 15 to "15 ثانية", 30 to "30 ثانية", 60 to "دقيقة واحدة")
                        .forEach { (sec, label) ->
                            FilterChip(
                                selected = config.autoChapterIntervalSec == sec,
                                onClick = { onAutoGenerateChapters(sec) },
                                label = { Text(label) }
                            )
                        }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                val chapters = config.customChapters.ifEmpty { source?.chapters ?: emptyList() }
                chapters.forEach { chapter ->
                    OutlinedTextField(
                        value = chapter.title,
                        onValueChange = { onUpdateChapterTitle(chapter.index, it) },
                        label = {
                            Text(
                                "فصل #${chapter.index} (${VideoTranscodeEngine.formatDuration(chapter.startMs)} - ${
                                    VideoTranscodeEngine.formatDuration(chapter.endMs)
                                })"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}
