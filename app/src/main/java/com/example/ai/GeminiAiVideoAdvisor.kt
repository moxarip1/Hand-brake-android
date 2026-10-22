package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.engine.VideoTranscodeEngine
import com.example.model.AiOptimizationGoal
import com.example.model.AiVideoAnalysisReport
import com.example.model.AudioCodec
import com.example.model.ContainerFormat
import com.example.model.EncoderPresetSpeed
import com.example.model.FramerateMode
import com.example.model.ResolutionLimit
import com.example.model.SourceVideoMetadata
import com.example.model.VideoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GeminiRecommendedSettingsJson(
    val contentTypeAr: String = "فيديو عالي الدقة",
    val technicalDiagnosisAr: String = "",
    val whyTheseSettingsAr: String = "",
    val recommendedCodec: String = "H264_AVC",
    val recommendedContainer: String = "MP4",
    val recommendedRf: Float = 21f,
    val recommendedSpeed: String = "FAST",
    val recommendedAudioBitrateKbps: Int = 160,
    val useAudioPassthru: Boolean = true,
    val estimatedSavingsPercent: Int = 55
)

interface GeminiVideoAdvisorService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeVideoForCompression(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiAiVideoAdvisor {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val apiService: GeminiVideoAdvisorService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiVideoAdvisorService::class.java)
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 78, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analyzes the video's visual frames and technical telemetry using Gemini 3.5 Flash multimodal API
     * (with an intelligent on-device visual entropy & bitrate analyzer fallback if API key is unset/offline)
     * to recommend the exact optimal HandBrake compression settings while preserving 1:1 original dimensions
     * and smooth continuous framerate.
     */
    suspend fun analyzeVideo(
        source: SourceVideoMetadata,
        goal: AiOptimizationGoal
    ): AiVideoAnalysisReport = withContext(Dispatchers.IO) {
        val orientationDesc = if (source.isPortraitReels) {
            "فيديو عمودي بصيغة ريلز/شورتس (9:16 Portrait • ${source.width}×${source.height})"
        } else if (source.width == source.height) {
            "فيديو مربع (1:1 Square • ${source.width}×${source.height})"
        } else {
            "فيديو عرضي قياسي (16:9 Landscape • ${source.width}×${source.height})"
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() &&
            apiKey != "MY_GEMINI_API_KEY" &&
            !apiKey.startsWith("YOUR_", ignoreCase = true)

        if (hasValidApiKey) {
            try {
                val partsList = mutableListOf<Part>()
                // Attach up to 2 representative frames from the video for visual complexity & motion analysis
                val middleThumb = source.thumbnails.getOrNull(source.thumbnails.size / 2) ?: source.thumbnails.firstOrNull()
                if (middleThumb != null) {
                    partsList.add(
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = middleThumb.toBase64Jpeg()
                            )
                        )
                    )
                }

                val prompt = """
                    أنت خبير هندسة ضغط وتحويل الفيديو في تطبيق HandBrake Mobile.
                    قم بتحليل إطار الفيديو المرفق والبيانات التقنية التالية لاقتراح أفضل إعدادات ضغط واستخراج مثالية:
                    - اسم الملف: ${source.fileName}
                    - الأبعاد والاتجاه: ${source.width}x${source.height} ($orientationDesc)
                    - المدة: ${VideoTranscodeEngine.formatDuration(source.durationMs)}
                    - الحجم الأصلي: ${VideoTranscodeEngine.formatSize(source.fileSizeBytes)}
                    - معدل البت الأصلي: ${source.videoBitrateBps / 1000} kbps
                    - معدل الإطارات الأصلي: ${source.frameRate.roundToInt()} fps
                    - الترميز الأصلي: ${source.videoCodecName} | الصوت: ${source.audioCodecName} (${source.audioBitrateBps / 1000} kbps)
                    - هدف المستخدم المختار: ${goal.titleAr} (${goal.descriptionAr})

                    قواعد حاسمة:
                    1. يجب دائماً الحفاظ على القياس الأصلي للفيديو (1:1 Original Dimensions) خصوصاً لو كان ريلز عمودي ${source.width}x${source.height} حتى لا يتقلص حجمه.
                    2. يجب الحفاظ على سلاسة الإطارات ومنع أي تقطيع في الصورة أو الصوت.
                    أعد النتيجة بصيغة JSON مطابقة للمخطط المطلوب باللغة العربية الواضحة.
                """.trimIndent()

                partsList.add(Part(text = prompt))

                val schema = buildJsonObject {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contentTypeAr") {
                            put("type", "STRING")
                            put("description", "وصف محتوى الفيديو المكتشف من الإطار ونوع اللقطة")
                        }
                        putJsonObject("technicalDiagnosisAr") {
                            put("type", "STRING")
                            put("description", "تحليل تقني لمعدل البت والتعقيد البصري للفيديو")
                        }
                        putJsonObject("whyTheseSettingsAr") {
                            put("type", "STRING")
                            put("description", "لماذا هذه الإعدادات هي الأفضل لهذا الفيديو مع الحفاظ على قياسه وسلاسته")
                        }
                        putJsonObject("recommendedCodec") {
                            put("type", "STRING")
                            put("description", "H264_AVC or H265_HEVC")
                        }
                        putJsonObject("recommendedContainer") {
                            put("type", "STRING")
                            put("description", "MP4 or MKV")
                        }
                        putJsonObject("recommendedRf") {
                            put("type", "NUMBER")
                            put("description", "قيمة RF المثالية بين 18 و 25")
                        }
                        putJsonObject("recommendedSpeed") {
                            put("type", "STRING")
                            put("description", "FAST, VERYFAST, or MEDIUM")
                        }
                        putJsonObject("recommendedAudioBitrateKbps") {
                            put("type", "INTEGER")
                            put("description", "128, 160, or 192")
                        }
                        putJsonObject("useAudioPassthru") {
                            put("type", "BOOLEAN")
                            put("description", "true لنسخ الصوت الأصلي بدون تقطيع")
                        }
                        putJsonObject("estimatedSavingsPercent") {
                            put("type", "INTEGER")
                            put("description", "نسبة التوفير المتوقعة في الحجم بين 25 و 75")
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("contentTypeAr"))
                        add(kotlinx.serialization.json.JsonPrimitive("technicalDiagnosisAr"))
                        add(kotlinx.serialization.json.JsonPrimitive("whyTheseSettingsAr"))
                        add(kotlinx.serialization.json.JsonPrimitive("recommendedCodec"))
                        add(kotlinx.serialization.json.JsonPrimitive("recommendedContainer"))
                        add(kotlinx.serialization.json.JsonPrimitive("recommendedRf"))
                        add(kotlinx.serialization.json.JsonPrimitive("recommendedSpeed"))
                        add(kotlinx.serialization.json.JsonPrimitive("recommendedAudioBitrateKbps"))
                        add(kotlinx.serialization.json.JsonPrimitive("useAudioPassthru"))
                        add(kotlinx.serialization.json.JsonPrimitive("estimatedSavingsPercent"))
                    }
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = partsList)),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = schema,
                        temperature = 0.25f
                    )
                )

                val response = apiService.analyzeVideoForCompression(apiKey, request)
                val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawJson.isNullOrBlank()) {
                    val parsed = jsonParser.decodeFromString<GeminiRecommendedSettingsJson>(rawJson)
                    val vCodec = if (parsed.recommendedCodec.contains("265") || parsed.recommendedCodec.contains("HEVC")) {
                        VideoCodec.H265_HEVC
                    } else {
                        VideoCodec.H264_AVC
                    }
                    val container = if (parsed.recommendedContainer.equals("MKV", ignoreCase = true)) {
                        ContainerFormat.MKV
                    } else {
                        ContainerFormat.MP4
                    }
                    val speed = runCatching { EncoderPresetSpeed.valueOf(parsed.recommendedSpeed.uppercase()) }
                        .getOrDefault(EncoderPresetSpeed.FAST)

                    return@withContext AiVideoAnalysisReport(
                        goal = goal,
                        contentTypeAr = parsed.contentTypeAr,
                        aspectAndOrientationAr = orientationDesc,
                        technicalDiagnosisAr = parsed.technicalDiagnosisAr,
                        whyTheseSettingsAr = parsed.whyTheseSettingsAr,
                        recommendedCodec = vCodec,
                        recommendedContainer = container,
                        recommendedResolutionLimit = ResolutionLimit.ORIGINAL, // Always preserve exact original size!
                        recommendedRf = parsed.recommendedRf.coerceIn(17f, 26f),
                        recommendedFps = 0, // Same as source for smooth playback
                        recommendedFramerateMode = FramerateMode.SAME_AS_SOURCE,
                        recommendedSpeed = speed,
                        recommendedAudioCodec = if (parsed.useAudioPassthru) AudioCodec.AUTO_PASSTHRU else AudioCodec.AAC,
                        recommendedAudioBitrateKbps = parsed.recommendedAudioBitrateKbps.coerceIn(128, 256),
                        estimatedSavingsPercent = parsed.estimatedSavingsPercent.coerceIn(20, 80),
                        poweredByGeminiCloud = true
                    )
                }
            } catch (_: Exception) {
                // Fall through to intelligent on-device visual & bitrate analyzer
            }
        }

        // On-Device Visual Entropy & Bitrate Telemetry Analyzer
        analyzeOnDeviceVisualEntropy(source, goal, orientationDesc)
    }

    private fun analyzeOnDeviceVisualEntropy(
        source: SourceVideoMetadata,
        goal: AiOptimizationGoal,
        orientationDesc: String
    ): AiVideoAnalysisReport {
        // Compute spatial detail & contrast entropy from the extracted video frame
        val frame = source.thumbnails.getOrNull(source.thumbnails.size / 2) ?: source.thumbnails.firstOrNull()
        var edgeComplexity = 0.5f
        if (frame != null) {
            val w = frame.width
            val h = frame.height
            var diffSum = 0L
            var samples = 0
            for (y in 2 until h - 2 step 4) {
                for (x in 2 until w - 2 step 4) {
                    val c1 = frame.getPixel(x, y)
                    val c2 = frame.getPixel(x + 2, y + 2)
                    val l1 = ((c1 shr 16 and 0xFF) + (c1 shr 8 and 0xFF) + (c1 and 0xFF)) / 3
                    val l2 = ((c2 shr 16 and 0xFF) + (c2 shr 8 and 0xFF) + (c2 and 0xFF)) / 3
                    diffSum += abs(l1 - l2)
                    samples++
                }
            }
            if (samples > 0) {
                edgeComplexity = ((diffSum.toFloat() / samples) / 45f).coerceIn(0.2f, 1.0f)
            }
        }

        val pixelsPerFrame = max(1, source.width * source.height)
        val fps = source.frameRate.coerceAtLeast(24f)
        val bitsPerPixel = source.videoBitrateBps.toFloat() / (pixelsPerFrame * fps)

        val (codec, rf, speed, savings) = when (goal) {
            AiOptimizationGoal.BEST_QUALITY_ORIGINAL_SIZE -> {
                val optimalRf = if (edgeComplexity > 0.65f) 20f else 21f
                Quad(VideoCodec.H264_AVC, optimalRf, EncoderPresetSpeed.FAST, if (bitsPerPixel > 0.12f) 52 else 38)
            }
            AiOptimizationGoal.REELS_TIKTOK_SHORTS -> {
                Quad(VideoCodec.H264_AVC, 20f, EncoderPresetSpeed.FAST, 48)
            }
            AiOptimizationGoal.MAX_SIZE_REDUCTION -> {
                Quad(VideoCodec.H265_HEVC, 23f, EncoderPresetSpeed.FAST, 64)
            }
            AiOptimizationGoal.ULTRA_FAST_STREAM -> {
                Quad(VideoCodec.H264_AVC, 22f, EncoderPresetSpeed.VERYFAST, 45)
            }
        }

        val detailLevelAr = when {
            edgeComplexity > 0.7f -> "مشاهد غنية بالتفاصيل الدقيقة والحركة السريعة"
            edgeComplexity > 0.4f -> "توازن متوسط بين الحركة والتفاصيل البصرية"
            else -> "مشاهد ناعمة الإضاءة وقابلة للضغط العالي بدون فقدان"
        }

        return AiVideoAnalysisReport(
            goal = goal,
            contentTypeAr = "$orientationDesc • $detailLevelAr",
            aspectAndOrientationAr = orientationDesc,
            technicalDiagnosisAr = "الأبعاد الأصلية ${source.width}×${source.height} بمعدل ${source.frameRate.roundToInt()} إطار/ثانية ومعدل بت ${source.videoBitrateBps / 1000} kbps (كثافة البت: ${String.format(java.util.Locale.US, "%.3f", bitsPerPixel)} bpp).",
            whyTheseSettingsAr = "تم تثبيت الأبعاد على القياس الأصلي (1:1 ${source.width}×${source.height}) لمنع أي تقليص لحجم الريلز، مع تفعيل النقل المباشر للإطارات عبر السطح الرسومي (Hardware Surface Pipeline) ونسخ الصوت الأصلي (Audio Passthru) لضمان فيديو سلس وثابت 100% بسرعة فائقة.",
            recommendedCodec = codec,
            recommendedContainer = ContainerFormat.MP4,
            recommendedResolutionLimit = ResolutionLimit.ORIGINAL,
            recommendedRf = rf,
            recommendedFps = 0,
            recommendedFramerateMode = FramerateMode.SAME_AS_SOURCE,
            recommendedSpeed = speed,
            recommendedAudioCodec = AudioCodec.AUTO_PASSTHRU,
            recommendedAudioBitrateKbps = 160,
            estimatedSavingsPercent = savings,
            poweredByGeminiCloud = false
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
