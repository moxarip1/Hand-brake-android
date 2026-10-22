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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.VideoTranscodeEngine
import com.example.model.ColorSpaceFilter
import com.example.model.DeinterlaceFilter
import com.example.model.DenoiseFilter
import com.example.model.EncoderPresetSpeed
import com.example.model.EncoderTune
import com.example.model.FramerateMode
import com.example.model.HandBrakeConfig
import com.example.model.QualityMode
import com.example.model.SharpenFilter
import com.example.model.SourceVideoMetadata
import com.example.model.VideoCodec
import com.example.ui.theme.HandBrakeAmber
import com.example.ui.theme.HardwareCyan
import com.example.ui.theme.SuccessEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersTabContent(
    config: HandBrakeConfig,
    onUpdateConfig: ((HandBrakeConfig) -> HandBrakeConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Deinterlace, Denoise & Sharpen Filters
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
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "معالجة الصورة وإزالة الضوضاء (Video Filters)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "إزالة التشابك (Deinterlace)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeinterlaceFilter.entries.forEach { f ->
                        FilterChip(
                            selected = config.deinterlace == f,
                            onClick = { onUpdateConfig { it.copy(deinterlace = f) } },
                            label = { Text(f.labelAr) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "تنقية الضوضاء والتحبب (Denoise: NLMeans / hqdn3d)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DenoiseFilter.entries.forEach { d ->
                        FilterChip(
                            selected = config.denoise == d,
                            onClick = { onUpdateConfig { it.copy(denoise = d) } },
                            label = { Text(d.labelAr) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "تعزيز الحدة والتفاصيل (Sharpen: UnSharp / LapSharp)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SharpenFilter.entries.forEach { s ->
                        FilterChip(
                            selected = config.sharpen == s,
                            onClick = { onUpdateConfig { it.copy(sharpen = s) } },
                            label = { Text(s.labelAr) }
                        )
                    }
                }

                // Deblock Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إزالة التكسير الرقمي (Deblock Filter)", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = if (config.deblockStrength == 0) "إيقاف (Off)" else "قوة ${config.deblockStrength}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HandBrakeAmber
                    )
                }
                Slider(
                    value = config.deblockStrength.toFloat(),
                    onValueChange = { v -> onUpdateConfig { it.copy(deblockStrength = v.roundToInt()) } },
                    valueRange = 0f..15f
                )
            }
        }

        // 2. ColorSpace, Grayscale & Live Color Grading
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
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = HardwareCyan)
                    Text(
                        text = "الفضاء اللوني والتدرج (ColorSpace & Grading)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorSpaceFilter.entries.forEach { cs ->
                        FilterChip(
                            selected = config.colorSpace == cs,
                            onClick = { onUpdateConfig { it.copy(colorSpace = cs) } },
                            label = { Text(cs.labelAr) }
                        )
                    }
                }

                HandBrakeSwitchRow(
                    title = "تحويل إلى أبيض وأسود سينمائي (Grayscale Encoding)",
                    subtitle = "يزيل معلومات الألوان (Chroma) ويقلل حجم الملف أيضاً",
                    checked = config.grayscale,
                    tag = "switch_grayscale",
                    onCheckedChange = { g -> onUpdateConfig { it.copy(grayscale = g) } }
                )

                // Contrast & Saturation Sliders (actually applied by VideoTranscodeEngine!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التباين البصري (Contrast)", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${(config.contrastScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = HardwareCyan
                    )
                }
                Slider(
                    value = config.contrastScale,
                    onValueChange = { c -> onUpdateConfig { it.copy(contrastScale = c) } },
                    valueRange = 0.7f..1.5f
                )

                if (!config.grayscale) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تشبع الألوان (Saturation)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${(config.saturationScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = HardwareCyan
                        )
                    }
                    Slider(
                        value = config.saturationScale,
                        onValueChange = { s -> onUpdateConfig { it.copy(saturationScale = s) } },
                        valueRange = 0.2f..1.8f
                    )
                }

                OutlinedButton(
                    onClick = {
                        onUpdateConfig {
                            it.copy(
                                grayscale = false,
                                contrastScale = 1.0f,
                                saturationScale = 1.0f,
                                brightnessOffset = 0f,
                                deblockStrength = 0
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إعادة ضبط الألوان والفلاتر للافتراضي")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoTabContent(
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
        // 1. Video Codec / Hardware Encoder Selection
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
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "مُرمّز الفيديو (Video Encoder)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                VideoCodec.entries.forEach { codec ->
                    val selected = config.videoCodec == codec
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateConfig { it.copy(videoCodec = codec) } }
                            .testTag("codec_${codec.name}"),
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) HandBrakeAmber else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = codec.descriptionAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HardwareCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = codec.shortLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HardwareCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quality Control (Constant Quality RF / Average Bitrate / Target File Size MB)
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
                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = HardwareCyan)
                    Text(
                        text = "معيار الجودة وحجم الضغط (Quality & Bitrate)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                QualityMode.entries.forEach { mode ->
                    val selected = config.qualityMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) HardwareCyan.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .clickable { onUpdateConfig { it.copy(qualityMode = mode) } }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.titleAr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) HardwareCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.subtitleAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                when (config.qualityMode) {
                    QualityMode.CONSTANT_QUALITY_RF -> {
                        val rfInt = config.constantQualityRf.roundToInt()
                        val qualityBadge = when {
                            rfInt <= 16 -> "جودة شبه أصلية (Lossless-Like)"
                            rfInt in 17..21 -> "جودة عالية جداً (HQ Sweet Spot)"
                            rfInt in 22..25 -> "متوازن مثالي (Recommended RF)"
                            rfInt in 26..31 -> "ضغط عالٍ وتوفير كبير للحجم"
                            else -> "أقصى ضغط (حجم صغير جداً)"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معامل الجودة الثابتة: RF $rfInt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HandBrakeAmber
                            )
                            Text(
                                text = qualityBadge,
                                style = MaterialTheme.typography.labelLarge,
                                color = SuccessEmerald
                            )
                        }
                        Slider(
                            value = config.constantQualityRf,
                            onValueChange = { v -> onUpdateConfig { it.copy(constantQualityRf = v.roundToInt().toFloat()) } },
                            valueRange = 12f..38f,
                            modifier = Modifier.testTag("rf_quality_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RF 12 (أعلى جودة)", style = MaterialTheme.typography.labelSmall)
                            Text("RF 22 (قياسي)", style = MaterialTheme.typography.labelSmall, color = HandBrakeAmber)
                            Text("RF 38 (أصغر حجم)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    QualityMode.AVERAGE_BITRATE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("معدل البت المتوسط:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${config.averageBitrateKbps} kbps",
                                style = MaterialTheme.typography.titleSmall,
                                color = HandBrakeAmber
                            )
                        }
                        Slider(
                            value = config.averageBitrateKbps.toFloat(),
                            onValueChange = { v ->
                                onUpdateConfig { it.copy(averageBitrateKbps = ((v.roundToInt() / 100) * 100).coerceAtLeast(300)) }
                            },
                            valueRange = 300f..12000f
                        )
                        HandBrakeSwitchRow(
                            title = "الترميز ثنائي المراحل (2-Pass Encoding)",
                            subtitle = "يحلل الفيديو أولاً لتوزيع البتات بدقة أكبر على المشاهد السريعة",
                            checked = config.twoPassEncoding,
                            tag = "switch_two_pass",
                            onCheckedChange = { p -> onUpdateConfig { it.copy(twoPassEncoding = p) } }
                        )
                    }

                    QualityMode.TARGET_FILE_SIZE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الحجم النهائي المستهدف:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${config.targetFileSizeMb.roundToInt()} MB",
                                style = MaterialTheme.typography.titleSmall,
                                color = SuccessEmerald
                            )
                        }
                        Slider(
                            value = config.targetFileSizeMb,
                            onValueChange = { v -> onUpdateConfig { it.copy(targetFileSizeMb = v.roundToInt().toFloat().coerceAtLeast(1f)) } },
                            valueRange = 1f..100f
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2f, 5f, 8f, 10f, 16f, 25f, 50f).forEach { mb ->
                                FilterChip(
                                    selected = config.targetFileSizeMb.roundToInt() == mb.toInt(),
                                    onClick = { onUpdateConfig { it.copy(targetFileSizeMb = mb) } },
                                    label = { Text("${mb.toInt()} MB") }
                                )
                            }
                        }
                    }
                }

                if (source != null) {
                    val estSize = VideoTranscodeEngine.estimateOutputSizeBytes(source, config)
                    val targetKbps = VideoTranscodeEngine.computeTargetVideoBitrateBps(source, config) / 1000
                    Text(
                        text = "الحجم المتوقع بعد الضغط: ${VideoTranscodeEngine.formatSize(estSize)} (بمعدل بت فيديو ~$targetKbps kbps)",
                        style = MaterialTheme.typography.bodySmall,
                        color = HardwareCyan
                    )
                }
            }
        }

        // 3. Framerate (FPS) & Encoder Speed Preset
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
                    Icon(Icons.Default.Speed, contentDescription = null, tint = HandBrakeAmber)
                    Text(
                        text = "معدل الإطارات وسرعة المُرمّز (FPS & Encoder Speed)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("معدل الإطارات (Framerate FPS)", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "نفس المصدر", 60 to "60 fps", 50 to "50 fps", 30 to "30 fps", 25 to "25 fps", 24 to "24 fps", 15 to "15 fps")
                        .forEach { (fpsVal, label) ->
                            FilterChip(
                                selected = config.framerateTarget == fpsVal,
                                onClick = { onUpdateConfig { it.copy(framerateTarget = fpsVal) } },
                                label = { Text(label) }
                            )
                        }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FramerateMode.entries.forEach { mode ->
                        FilterChip(
                            selected = config.framerateMode == mode,
                            onClick = { onUpdateConfig { it.copy(framerateMode = mode) } },
                            label = { Text(mode.titleAr) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "سرعة الضغط مقابل الكفاءة (Encoder Preset: ${config.encoderPresetSpeed.labelAr})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EncoderPresetSpeed.entries.forEach { speed ->
                        FilterChip(
                            selected = config.encoderPresetSpeed == speed,
                            onClick = { onUpdateConfig { it.copy(encoderPresetSpeed = speed) } },
                            label = { Text(speed.labelAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HandBrakeAmber.copy(alpha = 0.22f),
                                selectedLabelColor = HandBrakeAmber
                            )
                        )
                    }
                }

                Text(
                    text = "معايرة نوع المحتوى (Encoder Tune)",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EncoderTune.entries.forEach { tune ->
                        FilterChip(
                            selected = config.encoderTune == tune,
                            onClick = { onUpdateConfig { it.copy(encoderTune = tune) } },
                            label = { Text(tune.labelAr) }
                        )
                    }
                }
            }
        }
    }
}
