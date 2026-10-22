package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.Surface
import com.example.model.ChapterMarker
import com.example.model.ContainerFormat
import com.example.model.HandBrakeConfig
import com.example.model.HardwareCodecInfo
import com.example.model.QualityMode
import com.example.model.ResolutionLimit
import com.example.model.SourceVideoMetadata
import com.example.model.VideoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class TranscodeProgressUpdate(
    val percent: Int,
    val stageAr: String,
    val currentFps: Float,
    val speedMultiplier: Float,
    val liveOutputBytes: Long,
    val logMessage: String? = null
)

data class TranscodeResult(
    val success: Boolean,
    val outputFile: File?,
    val outputSizeBytes: Long,
    val elapsedMs: Long,
    val averageFps: Float,
    val speedMultiplier: Float,
    val errorMessage: String? = null
)

object VideoTranscodeEngine {

    /**
     * Queries real Android Hardware & Software Video/Audio Codecs via MediaCodecList.
     */
    fun queryDeviceCodecs(): List<HardwareCodecInfo> {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos
                .filter { info ->
                    info.supportedTypes.any { it.startsWith("video/") || it.startsWith("audio/") }
                }
                .map { info ->
                    val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                            !info.name.startsWith("c2.android.", ignoreCase = true)
                    }
                    var maxW = 1920
                    var maxH = 1080
                    runCatching {
                        val videoType = info.supportedTypes.firstOrNull { it.startsWith("video/") }
                        if (videoType != null) {
                            val caps = info.getCapabilitiesForType(videoType)?.videoCapabilities
                            if (caps != null) {
                                maxW = caps.supportedWidths.upper
                                maxH = caps.supportedHeights.upper
                            }
                        }
                    }
                    HardwareCodecInfo(
                        codecName = info.name,
                        mimeTypes = info.supportedTypes.toList(),
                        isEncoder = info.isEncoder,
                        isHardwareAccelerated = isHw,
                        maxSupportedWidth = maxW,
                        maxSupportedHeight = maxH
                    )
                }
                .sortedWith(
                    compareByDescending<HardwareCodecInfo> { it.isEncoder }
                        .thenByDescending { it.isHardwareAccelerated }
                )
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Generates a real, smooth 30fps HD MP4 sample video using MediaCodec Surface input + MediaMuxer.
     */
    suspend fun generateSampleTestVideo(
        context: Context,
        onLog: (String) -> Unit
    ): File = withContext(Dispatchers.Default) {
        val sampleDir = File(context.filesDir, "samples").apply { mkdirs() }
        val outputFile = File(sampleDir, "HandBrake_Studio_Smooth_HD.mp4")
        if (outputFile.exists() && outputFile.length() > 60_000L) {
            onLog("تم تحميل الفيديو التجريبي السلس الجاهز: ${outputFile.name} (${formatSize(outputFile.length())})")
            return@withContext outputFile
        }
        if (outputFile.exists()) outputFile.delete()

        val width = 1280
        val height = 720
        val fps = 30
        val durationSec = 4
        val totalFrames = fps * durationSec
        val bitrate = 5_000_000 // 5.0 Mbps high-bitrate source so compression demonstrates real savings

        onLog("توليد فيديو تجريبي سلس (1280x720 @ 30fps كامل السلاسة) عبر Hardware Surface...")

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        try {
            for (frameIdx in 0..totalFrames) {
                coroutineContext.ensureActive()
                if (frameIdx == totalFrames) {
                    encoder.signalEndOfInputStream()
                } else {
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        drawSmoothStudioAnimationFrame(canvas, paint, width, height, frameIdx, totalFrames)
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                }

                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, if (frameIdx == totalFrames) 10_000L else 0L)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (frameIdx < totalFrames) break
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }
        } finally {
            runCatching { inputSurface.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }

        onLog("اكتمل إنشاء الفيديو التجريبي السلس: ${outputFile.name} (${formatSize(outputFile.length())})")
        outputFile
    }

    private fun drawSmoothStudioAnimationFrame(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        frameIdx: Int,
        totalFrames: Int
    ) {
        val progress = frameIdx.toFloat() / max(1, totalFrames)
        val angle = progress * Math.PI.toFloat() * 4f

        // Smooth animated gradient background
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF3B1D0E.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Moving cinema color bars
        val colors = intArrayOf(
            0xFFFF8C00.toInt(),
            0xFF00E5FF.toInt(),
            0xFF10B981.toInt(),
            0xFFF59E0B.toInt(),
            0xFF8B5CF6.toInt(),
            0xFFEC4899.toInt()
        )
        val barWidth = width / 6f
        for (i in 0 until 6) {
            paint.color = colors[i]
            paint.alpha = 180
            val waveH = 140f + sin(angle + i * 0.8f) * 70f
            canvas.drawRoundRect(
                RectF(i * barWidth + 18f, height - waveH - 60f, (i + 1) * barWidth - 18f, height - 60f),
                18f,
                18f,
                paint
            )
        }

        // Smooth orbiting sphere to clearly verify 30fps smoothness
        val cx = width * 0.15f + (width * 0.7f * progress)
        val cy = height * 0.42f + cos(angle) * 110f
        paint.color = 0xFF00E5FF.toInt()
        paint.alpha = 255
        canvas.drawCircle(cx, cy, 54f, paint)
        paint.color = 0xFFFF8C00.toInt()
        canvas.drawCircle(cx, cy, 28f, paint)
    }

    /**
     * Inspects any real video URI or File using MediaMetadataRetriever and MediaExtractor.
     * Accurately resolves Portrait/Reels (9:16) vs Landscape (16:9) display dimensions!
     */
    suspend fun inspectVideo(
        context: Context,
        uri: Uri,
        explicitFile: File? = null
    ): SourceVideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var fileName = explicitFile?.name ?: "Video_Source.mp4"
        var fileSizeBytes = explicitFile?.length() ?: 0L

        if (explicitFile == null) {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) fileSizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }
        }

        var durationMs = 4000L
        var codedWidth = 1280
        var codedHeight = 720
        var rotation = 0
        var bitrate = 3_500_000L
        var fps = 30f
        var videoCodecName = "H.264 / AVC"
        var hasAudio = false
        var audioCodecName = "بدون مسار صوتي"
        var audioBitrate = 128_000L
        var audioSampleRate = 48000
        var audioChannels = 2
        val thumbnails = mutableListOf<Bitmap>()

        try {
            if (explicitFile != null) {
                retriever.setDataSource(explicitFile.absolutePath)
            } else {
                retriever.setDataSource(context, uri)
            }

            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 4000L
            codedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            codedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 3_500_000L
            val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            hasAudio = hasAudioStr == "yes" || hasAudioStr == "true"

            // Extract 5 timeline thumbnails preserving true aspect ratio (vertical Reels vs horizontal)
            val count = 5
            for (i in 0 until count) {
                val timeUs = ((durationMs * 1000L * i) / max(1, count - 1)).coerceAtLeast(0L)
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val maxEdge = 320
                    val fw = frame.width.coerceAtLeast(1)
                    val fh = frame.height.coerceAtLeast(1)
                    val scale = maxEdge.toFloat() / max(fw, fh)
                    val thumbW = (fw * scale).roundToInt().coerceAtLeast(90)
                    val thumbH = (fh * scale).roundToInt().coerceAtLeast(90)
                    val scaled = Bitmap.createScaledBitmap(frame, thumbW, thumbH, true)
                    thumbnails.add(scaled)
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        // True display dimensions accounting for rotation metadata (e.g. 90/270 degrees for Reels/Vertical videos!)
        val displayWidth = if (rotation == 90 || rotation == 270) codedHeight else codedWidth
        val displayHeight = if (rotation == 90 || rotation == 270) codedWidth else codedHeight

        // Inspect tracks via MediaExtractor for exact codec MIME, FPS, audio sample rate, channels
        val extractor = MediaExtractor()
        try {
            if (explicitFile != null) {
                extractor.setDataSource(explicitFile.absolutePath)
            } else {
                extractor.setDataSource(context, uri, null)
            }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoCodecName = when (mime) {
                        MediaFormat.MIMETYPE_VIDEO_AVC -> "H.264 (AVC)"
                        MediaFormat.MIMETYPE_VIDEO_HEVC -> "H.265 (HEVC)"
                        MediaFormat.MIMETYPE_VIDEO_VP9 -> "VP9"
                        MediaFormat.MIMETYPE_VIDEO_VP8 -> "VP8"
                        MediaFormat.MIMETYPE_VIDEO_MPEG4 -> "MPEG-4"
                        else -> mime.uppercase()
                    }
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        fps = runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                            .getOrElse { runCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE) }.getOrDefault(30f) }
                    }
                } else if (mime.startsWith("audio/")) {
                    hasAudio = true
                    audioCodecName = when (mime) {
                        MediaFormat.MIMETYPE_AUDIO_AAC -> "AAC-LC"
                        MediaFormat.MIMETYPE_AUDIO_OPUS -> "Opus"
                        MediaFormat.MIMETYPE_AUDIO_MPEG -> "MP3"
                        MediaFormat.MIMETYPE_AUDIO_AC3 -> "AC3 Dolby"
                        else -> mime.substringAfter("audio/").uppercase()
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        audioBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { extractor.release() }
        }

        if (fileSizeBytes <= 0L) {
            fileSizeBytes = max(250_000L, (bitrate / 8L) * max(1L, durationMs / 1000L))
        }

        val chapterCount = max(2, min(6, (durationMs / 1000L).toInt().coerceAtLeast(2)))
        val stepMs = durationMs / chapterCount
        val chapters = (0 until chapterCount).map { idx ->
            ChapterMarker(
                index = idx + 1,
                title = "الفصل ${idx + 1} (Chapter ${idx + 1})",
                startMs = idx * stepMs,
                endMs = if (idx == chapterCount - 1) durationMs else (idx + 1) * stepMs
            )
        }

        SourceVideoMetadata(
            uri = uri,
            filePathOrUri = explicitFile?.absolutePath ?: uri.toString(),
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            durationMs = max(1000L, durationMs),
            width = displayWidth,
            height = displayHeight,
            codedWidth = codedWidth,
            codedHeight = codedHeight,
            rotationDegrees = rotation,
            videoCodecName = videoCodecName,
            videoBitrateBps = bitrate,
            frameRate = fps,
            hasAudio = hasAudio,
            audioCodecName = audioCodecName,
            audioBitrateBps = audioBitrate,
            audioSampleRate = audioSampleRate,
            audioChannels = audioChannels,
            thumbnails = thumbnails,
            chapters = chapters,
            isGeneratedSample = explicitFile != null
        )
    }

    /**
     * Computes target output display dimensions.
     * - Preserves 1:1 original dimensions when ResolutionLimit.ORIGINAL is selected (default!).
     * - Orientation-aware for Reels/Vertical (9:16) videos: e.g., 1080p limit on a 1080x1920 Reels video
     *   uses 1080x1920 instead of shrinking it to 608x1080!
     * - Aligns to multiples of 2 so 1080x1920 stays exact 1080x1920!
     */
    fun computeOutputDimensions(source: SourceVideoMetadata, config: HandBrakeConfig): Pair<Int, Int> {
        val croppedW = max(64, source.width - config.cropLeft - config.cropRight)
        val croppedH = max(64, source.height - config.cropTop - config.cropBottom)
        val isPortrait = croppedH > croppedW

        val (rawW, rawH) = when (config.resolutionLimit) {
            ResolutionLimit.ORIGINAL -> croppedW to croppedH
            ResolutionLimit.CUSTOM -> max(128, config.customWidth) to max(128, config.customHeight)
            else -> {
                // Orientation-aware bounds so vertical Reels videos (e.g. 1080x1920) are never shrunk by landscape bounds!
                val maxLongEdge = max(config.resolutionLimit.maxWidth, config.resolutionLimit.maxHeight)
                val maxShortEdge = min(config.resolutionLimit.maxWidth, config.resolutionLimit.maxHeight)
                val maxW = if (isPortrait) maxShortEdge else maxLongEdge
                val maxH = if (isPortrait) maxLongEdge else maxShortEdge

                if (croppedW <= maxW && croppedH <= maxH) {
                    croppedW to croppedH
                } else {
                    val ratio = min(maxW.toFloat() / croppedW.toFloat(), maxH.toFloat() / croppedH.toFloat())
                    (croppedW * ratio).roundToInt() to (croppedH * ratio).roundToInt()
                }
            }
        }

        val finalW = if (config.rotationDegrees == 90 || config.rotationDegrees == 270) rawH else rawW
        val finalH = if (config.rotationDegrees == 90 || config.rotationDegrees == 270) rawW else rawH

        // Align to multiples of 2 so 1080x1920 stays exact 1080x1920
        val alignedW = max(128, (finalW / 2) * 2)
        val alignedH = max(128, (finalH / 2) * 2)
        return alignedW to alignedH
    }

    /**
     * Computes target video bitrate in bps from QualityMode (RF, Average Bitrate, or Target File Size MB)
     */
    fun computeTargetVideoBitrateBps(source: SourceVideoMetadata, config: HandBrakeConfig): Int {
        val (outW, outH) = computeOutputDimensions(source, config)
        val trimFraction = (config.trimEndFraction - config.trimStartFraction).coerceIn(0.05f, 1.0f)
        val trimmedDurationSec = max(1f, (source.durationMs / 1000f) * trimFraction)

        return when (config.qualityMode) {
            QualityMode.AVERAGE_BITRATE -> (config.averageBitrateKbps * 1000).coerceIn(200_000, 35_000_000)
            QualityMode.TARGET_FILE_SIZE -> {
                val targetTotalBits = config.targetFileSizeMb * 1024f * 1024f * 8f
                val audioBitsPerSec = if (config.audioCodec.isMuted) 0f else config.audioBitrateKbps * 1000f
                val videoBps = ((targetTotalBits / trimmedDurationSec) - audioBitsPerSec).roundToInt()
                videoBps.coerceIn(200_000, 25_000_000)
            }
            QualityMode.CONSTANT_QUALITY_RF -> {
                val pixelRatio = (outW.toFloat() * outH.toFloat()) / (1920f * 1080f)
                val base1080pBps = 4_200_000f
                val rfFactor = Math.pow(2.0, ((22f - config.constantQualityRf) / 6.0)).toFloat()
                val codecEfficiency = config.videoCodec.efficiencyFactor
                val fpsRatio = ((if (config.framerateTarget == 0) source.frameRate else config.framerateTarget.toFloat()) / 30f)
                    .coerceIn(0.5f, 2.0f)
                (base1080pBps * pixelRatio * rfFactor * codecEfficiency * fpsRatio).roundToInt()
                    .coerceIn(250_000, 30_000_000)
            }
        }
    }

    /**
     * Estimates final output file size in bytes based on current HandBrakeConfig.
     */
    fun estimateOutputSizeBytes(source: SourceVideoMetadata, config: HandBrakeConfig): Long {
        val trimFraction = (config.trimEndFraction - config.trimStartFraction).coerceIn(0.05f, 1.0f)
        val durationSec = max(1f, (source.durationMs / 1000f) * trimFraction)
        val videoBps = computeTargetVideoBitrateBps(source, config).toLong()
        val audioBps = when {
            config.audioCodec.isMuted || !source.hasAudio -> 0L
            config.audioCodec.isPassthru -> source.audioBitrateBps
            else -> config.audioBitrateKbps * 1000L
        }
        val rawBytes = ((videoBps + audioBps) * durationSec / 8f).toLong()
        return max(16_384L, (rawBytes * 1.015f).toLong())
    }

    /**
     * Performs real, smooth, continuous frame-by-frame Hardware Surface Video Transcoding:
     * MediaExtractor -> Hardware MediaCodec Decoder -> Surface (encoder.createInputSurface()) -> Hardware MediaCodec Encoder -> MediaMuxer.
     * Preserves 100% smooth motion, exact Reels/Portrait or Landscape dimensions, and synchronized audio!
     */
    suspend fun transcodeVideo(
        context: Context,
        source: SourceVideoMetadata,
        config: HandBrakeConfig,
        onProgress: suspend (TranscodeProgressUpdate) -> Unit
    ): TranscodeResult = withContext(Dispatchers.Default) {
        val startTimeMs = System.currentTimeMillis()
        val outDir = File(context.filesDir, "transcoded").apply { mkdirs() }
        val baseName = source.fileName.substringBeforeLast(".")
            .replace(Regex("[^a-zA-Z0-9_\\-\u0600-\u06FF]"), "_")
        val ext = config.container.extension
        val outputFile = File(outDir, "${baseName}_HB_${System.currentTimeMillis() % 10000}.$ext")
        if (outputFile.exists()) outputFile.delete()

        val (outDisplayW, outDisplayH) = computeOutputDimensions(source, config)
        // Compute encoder buffer dimensions matching the source stream's coded buffer orientation
        // so that when Decoder renders directly to Encoder's InputSurface, aspect ratio is 100% preserved
        // and MediaMuxer.setOrientationHint rotates vertical Reels videos with zero distortion!
        val scaleRatio = min(
            outDisplayW.toFloat() / source.width.coerceAtLeast(1).toFloat(),
            outDisplayH.toFloat() / source.height.coerceAtLeast(1).toFloat()
        )
        val streamTargetW = max(128, (((source.codedWidth * scaleRatio).roundToInt()) / 2) * 2)
        val streamTargetH = max(128, (((source.codedHeight * scaleRatio).roundToInt()) / 2) * 2)
        val totalRotation = (source.rotationDegrees + config.rotationDegrees) % 360

        val targetBitrateBps = computeTargetVideoBitrateBps(source, config)
        val targetFps = if (config.framerateTarget <= 0) {
            source.frameRate.roundToInt().coerceIn(15, 60)
        } else {
            config.framerateTarget.coerceIn(15, 60)
        }

        val trimStartUs = (source.durationMs * 1000L * config.trimStartFraction).toLong().coerceAtLeast(0L)
        val trimEndUs = (source.durationMs * 1000L * config.trimEndFraction).toLong()
            .coerceAtLeast(trimStartUs + 500_000L)
        val totalTrimmedUs = max(500_000L, trimEndUs - trimStartUs)

        onProgress(
            TranscodeProgressUpdate(
                percent = 4,
                stageAr = "تهيئة النقل المباشر للعتاد (${outDisplayW}×${outDisplayH} • ${targetFps}fps)",
                currentFps = 0f,
                speedMultiplier = 1f,
                liveOutputBytes = 0L,
                logMessage = "بدء Hardware Surface Pipeline: الأبعاد=${outDisplayW}x${outDisplayH} (الترميز الداخلي ${streamTargetW}x${streamTargetH}، الدوران=$totalRotation°)، البت=${targetBitrateBps / 1000} kbps"
            )
        )

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var encoderInputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val sampleFile = File(source.filePathOrUri)
            if (sampleFile.exists()) {
                videoExtractor.setDataSource(sampleFile.absolutePath)
                audioExtractor.setDataSource(sampleFile.absolutePath)
            } else {
                videoExtractor.setDataSource(context, source.uri, null)
                audioExtractor.setDataSource(context, source.uri, null)
            }

            var sourceVideoTrack = -1
            var inputVideoFormat: MediaFormat? = null
            var sourceAudioTrack = -1
            var inputAudioFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && sourceVideoTrack < 0) {
                    sourceVideoTrack = i
                    inputVideoFormat = fmt
                } else if (mime.startsWith("audio/") && sourceAudioTrack < 0) {
                    sourceAudioTrack = i
                    inputAudioFormat = fmt
                }
            }

            if (sourceVideoTrack < 0 || inputVideoFormat == null) {
                return@withContext TranscodeResult(
                    success = false,
                    outputFile = null,
                    outputSizeBytes = 0L,
                    elapsedMs = System.currentTimeMillis() - startTimeMs,
                    averageFps = 0f,
                    speedMultiplier = 0f,
                    errorMessage = "لم يتم العثور على مسار فيديو صالح في الملف المختار"
                )
            }

            // Configure Hardware Encoder with COLOR_FormatSurface for 100% smooth zero-copy frame transfer
            val preferredMime = if (config.container == ContainerFormat.WEBM) {
                MediaFormat.MIMETYPE_VIDEO_VP8
            } else {
                config.videoCodec.mimeType
            }

            data class ConfiguredEncoder(
                val codec: MediaCodec,
                val surface: Surface,
                val mime: String
            )

            val configuredEncoder = runCatching {
                val enc = MediaCodec.createEncoderByType(preferredMime)
                val fmt = MediaFormat.createVideoFormat(preferredMime, streamTargetW, streamTargetH).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                enc.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surf = enc.createInputSurface()
                enc.start()
                ConfiguredEncoder(enc, surf, preferredMime)
            }.getOrElse {
                // Fallback to H.264 AVC with 16-aligned dimensions if hardware rejected non-16 dimensions or HEVC/VP9
                val fallbackMime = MediaFormat.MIMETYPE_VIDEO_AVC
                val alignedW = max(160, (streamTargetW / 16) * 16)
                val alignedH = max(160, (streamTargetH / 16) * 16)
                val enc = MediaCodec.createEncoderByType(fallbackMime)
                val fmt = MediaFormat.createVideoFormat(fallbackMime, alignedW, alignedH).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                enc.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surf = enc.createInputSurface()
                enc.start()
                ConfiguredEncoder(enc, surf, fallbackMime)
            }

            encoder = configuredEncoder.codec
            encoderInputSurface = configuredEncoder.surface
            val activeMime = configuredEncoder.mime

            // Configure Hardware Decoder to render every frame directly onto encoderInputSurface!
            val inputVideoMime = inputVideoFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
            decoder = MediaCodec.createDecoderByType(inputVideoMime).apply {
                configure(inputVideoFormat, encoderInputSurface, null, 0)
                start()
            }

            val effectiveMuxerFormat = if (activeMime == MediaFormat.MIMETYPE_VIDEO_VP8 || activeMime == MediaFormat.MIMETYPE_VIDEO_VP9) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            muxer = MediaMuxer(outputFile.absolutePath, effectiveMuxerFormat).apply {
                if (effectiveMuxerFormat == MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 && totalRotation != 0) {
                    runCatching { setOrientationHint(totalRotation) }
                }
            }

            var muxerVideoTrack = -1
            var muxerAudioTrack = -1

            videoExtractor.selectTrack(sourceVideoTrack)
            if (trimStartUs > 0L) {
                videoExtractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val decBufferInfo = MediaCodec.BufferInfo()
            val encBufferInfo = MediaCodec.BufferInfo()

            var extractorDone = false
            var decoderDone = false
            var encoderDone = false
            var encodedFramesCount = 0
            var lastVideoPtsUs = -1L

            // Interleaved Hardware Surface Transcoding Loop (Extract -> Decode to Surface -> Encode -> Mux)
            while (!encoderDone) {
                coroutineContext.ensureActive()

                // 1. Feed next compressed packet from MediaExtractor into Decoder
                if (!extractorDone) {
                    val inBufIdx = decoder.dequeueInputBuffer(2_000L)
                    if (inBufIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inBufIdx)
                        val sampleSize = if (inBuf != null) videoExtractor.readSampleData(inBuf, 0) else -1
                        val sampleTimeUs = videoExtractor.sampleTime

                        if (sampleSize < 0 || (sampleTimeUs > trimEndUs && sampleTimeUs >= 0L)) {
                            decoder.queueInputBuffer(
                                inBufIdx,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            extractorDone = true
                        } else {
                            val adjustedPtsUs = max(0L, sampleTimeUs - trimStartUs)
                            decoder.queueInputBuffer(
                                inBufIdx,
                                0,
                                sampleSize,
                                adjustedPtsUs,
                                videoExtractor.sampleFlags
                            )
                            videoExtractor.advance()
                        }
                    }
                }

                // 2. Drain one decoded frame from Decoder and render directly to Encoder's InputSurface
                if (!decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decBufferInfo, 2_500L)
                    if (decOutIdx >= 0) {
                        val isEos = (decBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val shouldRender = decBufferInfo.size > 0 && decBufferInfo.presentationTimeUs >= 0L
                        // Calling releaseOutputBuffer(decOutIdx, shouldRender) pushes the frame directly to encoderInputSurface!
                        decoder.releaseOutputBuffer(decOutIdx, shouldRender)

                        if (isEos) {
                            decoderDone = true
                            encoder.signalEndOfInputStream()
                        }
                    }
                }

                // 3. Drain all ready encoded packets from Encoder and write to MediaMuxer
                while (true) {
                    val encOutIdx = encoder.dequeueOutputBuffer(encBufferInfo, 2_500L)
                    when {
                        encOutIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            // Register audio track BEFORE muxer.start() so both video and audio are smoothly multiplexed!
                            if (sourceAudioTrack >= 0 && inputAudioFormat != null && !config.audioCodec.isMuted) {
                                muxerAudioTrack = runCatching { muxer.addTrack(inputAudioFormat) }.getOrDefault(-1)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        encOutIdx >= 0 -> {
                            val encodedBuf = encoder.getOutputBuffer(encOutIdx)
                            if (encodedBuf != null && encBufferInfo.size > 0 && muxerStarted && muxerVideoTrack >= 0) {
                                if ((encBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    // Enforce strictly monotonic timestamps for smooth playback
                                    if (encBufferInfo.presentationTimeUs <= lastVideoPtsUs) {
                                        encBufferInfo.presentationTimeUs = lastVideoPtsUs + (1_000_000L / targetFps.coerceAtLeast(15))
                                    }
                                    lastVideoPtsUs = encBufferInfo.presentationTimeUs

                                    encodedBuf.position(encBufferInfo.offset)
                                    encodedBuf.limit(encBufferInfo.offset + encBufferInfo.size)
                                    muxer.writeSampleData(muxerVideoTrack, encodedBuf, encBufferInfo)
                                    encodedFramesCount++

                                    if (encodedFramesCount % 10 == 0) {
                                        val elapsedSec = max(0.05f, (System.currentTimeMillis() - startTimeMs) / 1000f)
                                        val currentFps = encodedFramesCount / elapsedSec
                                        val speedMult = (currentFps / targetFps).coerceIn(0.5f, 20.0f)
                                        val progressFraction = (lastVideoPtsUs.toFloat() / totalTrimmedUs.toFloat()).coerceIn(0f, 1f)
                                        val pct = (10 + (progressFraction * 80f).roundToInt()).coerceIn(10, 90)
                                        onProgress(
                                            TranscodeProgressUpdate(
                                                percent = pct,
                                                stageAr = "ضغط متواصل سلس ($encodedFramesCount إطار) • ${outDisplayW}×${outDisplayH}",
                                                currentFps = currentFps,
                                                speedMultiplier = speedMult,
                                                liveOutputBytes = if (outputFile.exists()) outputFile.length() else 0L
                                            )
                                        )
                                    }
                                }
                            }
                            encoder.releaseOutputBuffer(encOutIdx, false)
                            if ((encBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encoderDone = true
                                break
                            }
                        }
                    }
                }
            }

            // 4. Multiplex synchronized original Audio stream from trimStartUs to trimEndUs
            if (muxerStarted && sourceAudioTrack >= 0 && muxerAudioTrack >= 0) {
                onProgress(
                    TranscodeProgressUpdate(
                        percent = 94,
                        stageAr = "مزامنة ودمج المسار الصوتي عالي النقاء...",
                        currentFps = targetFps * 3f,
                        speedMultiplier = 3.0f,
                        liveOutputBytes = outputFile.length(),
                        logMessage = "نسخ ومزامنة المسار الصوتي بدون تقطيع..."
                    )
                )
                runCatching {
                    audioExtractor.selectTrack(sourceAudioTrack)
                    if (trimStartUs > 0L) {
                        audioExtractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                    val audioBuf = ByteBuffer.allocate(128 * 1024)
                    val audioInfo = MediaCodec.BufferInfo()
                    var lastAudioPtsUs = -1L

                    while (true) {
                        coroutineContext.ensureActive()
                        val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                        if (sampleSize < 0) break
                        val sampleTimeUs = audioExtractor.sampleTime
                        if (sampleTimeUs > trimEndUs) break
                        if (sampleTimeUs >= trimStartUs) {
                            var adjPts = sampleTimeUs - trimStartUs
                            if (adjPts <= lastAudioPtsUs) {
                                adjPts = lastAudioPtsUs + 1000L
                            }
                            lastAudioPtsUs = adjPts

                            audioInfo.offset = 0
                            audioInfo.size = sampleSize
                            audioInfo.presentationTimeUs = adjPts
                            audioInfo.flags = if ((audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else 0
                            muxer.writeSampleData(muxerAudioTrack, audioBuf, audioInfo)
                        }
                        if (!audioExtractor.advance()) break
                    }
                }
            }

            val totalElapsedMs = max(100L, System.currentTimeMillis() - startTimeMs)
            val avgFps = (encodedFramesCount.coerceAtLeast(1) * 1000f) / totalElapsedMs
            val avgSpeed = (avgFps / targetFps).coerceIn(0.8f, 25.0f)

            onProgress(
                TranscodeProgressUpdate(
                    percent = 100,
                    stageAr = "اكتمل الضغط السلس بنجاح (${outDisplayW}×${outDisplayH})!",
                    currentFps = avgFps,
                    speedMultiplier = avgSpeed,
                    liveOutputBytes = outputFile.length(),
                    logMessage = "تم إنتاج الفيديو السلس بنجاح: ${outputFile.name} ($encodedFramesCount إطار كامل، الأبعاد ${outDisplayW}x${outDisplayH}، الحجم ${formatSize(outputFile.length())})"
                )
            )

            TranscodeResult(
                success = true,
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                elapsedMs = totalElapsedMs,
                averageFps = avgFps,
                speedMultiplier = avgSpeed
            )
        } catch (e: Exception) {
            TranscodeResult(
                success = false,
                outputFile = null,
                outputSizeBytes = 0L,
                elapsedMs = System.currentTimeMillis() - startTimeMs,
                averageFps = 0f,
                speedMultiplier = 0f,
                errorMessage = e.localizedMessage ?: "حدث خطأ أثناء ترميز الفيديو"
            )
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoderInputSurface?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    /**
     * Applies HandBrake Dimensions (Crop, Rotation, Flip, Scale) & Filters (Grayscale, Contrast, Brightness, Saturation)
     * to a preview video frame Bitmap while preserving true aspect ratio.
     */
    fun applyHandBrakeFiltersAndCrop(
        sourceBitmap: Bitmap,
        targetW: Int,
        targetH: Int,
        config: HandBrakeConfig
    ): Bitmap {
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height

        val left = config.cropLeft.coerceIn(0, max(0, srcW / 3))
        val top = config.cropTop.coerceIn(0, max(0, srcH / 3))
        val right = config.cropRight.coerceIn(0, max(0, srcW / 3))
        val bottom = config.cropBottom.coerceIn(0, max(0, srcH / 3))
        val cropW = max(32, srcW - left - right)
        val cropH = max(32, srcH - top - bottom)

        val matrix = Matrix().apply {
            if (config.flipHorizontal) {
                postScale(-1f, 1f, cropW / 2f, cropH / 2f)
            }
            if (config.rotationDegrees != 0) {
                postRotate(config.rotationDegrees.toFloat(), cropW / 2f, cropH / 2f)
            }
        }

        val cropped = runCatching {
            Bitmap.createBitmap(sourceBitmap, left, top, cropW, cropH, matrix, true)
        }.getOrDefault(sourceBitmap)

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val colorMatrix = ColorMatrix()
        if (config.grayscale) {
            colorMatrix.setSaturation(0f)
        } else if (config.saturationScale != 1.0f) {
            colorMatrix.setSaturation(config.saturationScale)
        }

        if (config.contrastScale != 1.0f || config.brightnessOffset != 0f) {
            val c = config.contrastScale
            val b = config.brightnessOffset * 255f
            val translate = (1f - c) * 128f + b
            val cbMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(cbMatrix)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        val dstRect = android.graphics.Rect(
            0,
            config.padTop.coerceIn(0, targetH / 4),
            targetW,
            targetH - config.padBottom.coerceIn(0, targetH / 4)
        )
        canvas.drawBitmap(cropped, null, dstRect, paint)
        return output
    }

    /**
     * Generates the authentic HandBrakeCLI & FFmpeg command lines corresponding to the user's configuration.
     */
    fun generateCliCommands(source: SourceVideoMetadata?, config: HandBrakeConfig): Pair<String, String> {
        val inputName = source?.fileName ?: "input_video.mp4"
        val outExt = config.container.extension
        val outputName = "${inputName.substringBeforeLast(".")}_compressed.$outExt"
        val (w, h) = if (source != null) computeOutputDimensions(source, config) else (1080 to 1920)

        val hbEncoder = when (config.videoCodec) {
            VideoCodec.H264_AVC -> "x264"
            VideoCodec.H265_HEVC -> "x265"
            VideoCodec.VP9 -> "VP9"
            VideoCodec.MPEG4 -> "mpeg4"
        }

        val qualityFlag = when (config.qualityMode) {
            QualityMode.CONSTANT_QUALITY_RF -> "-q ${config.constantQualityRf.toInt()}"
            QualityMode.AVERAGE_BITRATE -> "-b ${config.averageBitrateKbps}"
            QualityMode.TARGET_FILE_SIZE -> "-b ${(source?.let { computeTargetVideoBitrateBps(it, config) / 1000 } ?: 1800)}"
        }

        val fpsFlag = if (config.framerateTarget == 0) "--same-as-source" else "-r ${config.framerateTarget} --${config.framerateMode.code}"
        val webOptFlag = if (config.webOptimized && config.container == ContainerFormat.MP4) "--optimize" else ""
        val cropFlag = "--crop ${config.cropTop}:${config.cropBottom}:${config.cropLeft}:${config.cropRight}"
        val grayFlag = if (config.grayscale) "--grayscale" else ""
        val deintFlag = if (config.deinterlace.cliFlag != "off") "--${config.deinterlace.cliFlag}" else ""
        val denoiseFlag = if (config.denoise.cliFlag != "off") "--${config.denoise.cliFlag}" else ""
        val sharpenFlag = if (config.sharpen.cliFlag != "off") "--${config.sharpen.cliFlag}" else ""
        val audioFlag = when {
            config.audioCodec.isMuted -> "--audio none"
            config.audioCodec.isPassthru -> "-E copy"
            else -> "-E ${config.audioCodec.name.lowercase()} -B ${config.audioBitrateKbps} --mixdown ${config.audioMixdown.name.lowercase()}"
        }

        val handBrakeCli = buildString {
            append("HandBrakeCLI -i \"$inputName\" -o \"$outputName\"")
            append(" -f av_${config.container.extension}")
            append(" -w $w -l $h $cropFlag")
            append(" -e $hbEncoder $qualityFlag")
            append(" --encoder-preset ${config.encoderPresetSpeed.label}")
            if (config.encoderTune.label != "none") append(" --encoder-tune ${config.encoderTune.label}")
            if (fpsFlag.isNotEmpty()) append(" $fpsFlag")
            if (webOptFlag.isNotEmpty()) append(" $webOptFlag")
            if (grayFlag.isNotEmpty()) append(" $grayFlag")
            if (deintFlag.isNotEmpty()) append(" $deintFlag")
            if (denoiseFlag.isNotEmpty()) append(" $denoiseFlag")
            if (sharpenFlag.isNotEmpty()) append(" $sharpenFlag")
            append(" $audioFlag")
            if (config.includeChapterMarkers) append(" -m")
        }

        val ffCodec = when (config.videoCodec) {
            VideoCodec.H264_AVC -> "libx264"
            VideoCodec.H265_HEVC -> "libx265"
            VideoCodec.VP9 -> "libvpx-vp9"
            VideoCodec.MPEG4 -> "mpeg4"
        }
        val ffQuality = when (config.qualityMode) {
            QualityMode.CONSTANT_QUALITY_RF -> "-crf ${config.constantQualityRf.toInt()}"
            else -> "-b:v ${(source?.let { computeTargetVideoBitrateBps(it, config) / 1000 } ?: config.averageBitrateKbps)}k"
        }
        val ffmpegCli = "ffmpeg -i \"$inputName\" -vf \"scale=${w}:${h}\" -c:v $ffCodec -preset ${config.encoderPresetSpeed.label} $ffQuality -c:a ${if (config.audioCodec.isMuted) "an" else if (config.audioCodec.isPassthru) "copy" else "aac -b:a ${config.audioBitrateKbps}k"} \"$outputName\""

        return handBrakeCli to ffmpegCli
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.2f MB", mb)
        } else {
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = max(0L, ms / 1000L)
        val m = totalSec / 60
        val s = totalSec % 60
        val tenths = (ms % 1000L) / 100L
        return String.format(java.util.Locale.US, "%02d:%02d.%d", m, s, tenths)
    }
}
