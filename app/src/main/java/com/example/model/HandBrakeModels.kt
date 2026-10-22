package com.example.model

import android.graphics.Bitmap
import android.media.MediaFormat
import android.net.Uri

enum class ContainerFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val descriptionAr: String
) {
    MP4("MP4 (MPEG-4)", "mp4", "video/mp4", "الأكثر توافقاً مع الهواتف والريلز والويب وجميع الشاشات"),
    MKV("MKV (Matroska)", "mkv", "video/x-matroska", "مثالي للأرشفة وتعدد مسارات الصوت والترجمة"),
    WEBM("WebM", "webm", "video/webm", "مُحسّن للمتصفحات ومنصات الويب المفتوحة")
}

enum class VideoCodec(
    val displayName: String,
    val shortLabel: String,
    val mimeType: String,
    val efficiencyFactor: Float,
    val descriptionAr: String
) {
    H264_AVC(
        "H.264 (AVC MediaCodec HW)",
        "H.264",
        MediaFormat.MIMETYPE_VIDEO_AVC,
        1.0f,
        "تحويل فائق السرعة بتسريع العتاد وتوافقية 100% وسلاسة إطارات كاملة"
    ),
    H265_HEVC(
        "H.265 (HEVC 10-bit/8-bit HW)",
        "H.265 / HEVC",
        MediaFormat.MIMETYPE_VIDEO_HEVC,
        0.65f,
        "يوفر حتى 40% من الحجم بنفس الجودة البصرية مقارنة بـ H.264"
    ),
    VP9(
        "VP9 (Google WebM Encoder)",
        "VP9",
        MediaFormat.MIMETYPE_VIDEO_VP9,
        0.70f,
        "ترميز مفتوح المصدر عالي الكفاءة لمنصات البث والويب"
    ),
    MPEG4(
        "MPEG-4 Part 2 (Fast Legacy)",
        "MPEG-4",
        MediaFormat.MIMETYPE_VIDEO_MPEG4,
        1.25f,
        "ضغط كلاسيكي سريع جداً للأجهزة القديمة والأنظمة المدمجة"
    )
}

enum class QualityMode(val titleAr: String, val subtitleAr: String) {
    CONSTANT_QUALITY_RF("جودة ثابتة (Constant Quality RF)", "الوضع الموصى به للحفاظ على جودة وثبات الفيديو الأصلي"),
    AVERAGE_BITRATE("معدل بت متوسط (Average Bitrate kbps)", "تحديد معدل نقل البيانات الدقيق بالكيلوبت/ثانية"),
    TARGET_FILE_SIZE("حجم ملف مستهدف (Target Size MB)", "ضغط الفيديو ليطابق حجماً محدداً بالميجابايت (مثل واتساب/ديسكورد)")
}

enum class FramerateMode(val titleAr: String, val code: String) {
    PEAK_VFR("معدل متغير ذروي (Peak Framerate VFR)", "vfr"),
    CONSTANT_CFR("معدل إطارات ثابت (Constant Framerate CFR)", "cfr"),
    SAME_AS_SOURCE("نفس المصدر الأصلي (Same as Source)", "source")
}

enum class EncoderPresetSpeed(val label: String, val labelAr: String, val speedFactor: Float) {
    ULTRAFAST("ultrafast", "فائق السرعة (Ultrafast)", 3.8f),
    SUPERFAST("superfast", "سريع جداً+ (Superfast)", 3.2f),
    VERYFAST("veryfast", "سريع جداً (Veryfast)", 2.6f),
    FASTER("faster", "أسرع (Faster)", 2.1f),
    FAST("fast", "سريع (Fast - موصى به)", 1.7f),
    MEDIUM("medium", "متوازن (Medium)", 1.2f),
    SLOW("slow", "عالي الدقة (Slow)", 0.85f)
}

enum class EncoderTune(val label: String, val labelAr: String) {
    NONE("none", "بدون (None)"),
    FILM("film", "أفلام وسينما (Film)"),
    ANIMATION("animation", "رسوم متحركة (Animation)"),
    GRAIN("grain", "حبيبات الفيلم (Grain)"),
    STILL_IMAGE("stillimage", "صور ثابتة / شروحات (Still Image)"),
    FAST_DECODE("fastdecode", "فك ترميز سريع (Fast Decode)"),
    ZERO_LATENCY("zerolatency", "بث فوري بدون تأخير (Zero Latency)")
}

enum class ResolutionLimit(val labelAr: String, val maxWidth: Int, val maxHeight: Int) {
    ORIGINAL("نفس القياس الأصلي 1:1 (يحافظ على أبعاد الريلز والفيديو)", 0, 0),
    UHD_4K("4K UHD (3840×2160 / 2160×3840)", 3840, 2160),
    FHD_1080P("1080p Full HD (1920×1080 / 1080×1920)", 1920, 1080),
    HD_720P("720p HD (1280×720 / 720×1280)", 1280, 720),
    SD_480P("480p SD (854×480 / 480×854)", 854, 480),
    LOW_360P("360p Compact (640×360 / 360×640)", 640, 360),
    CUSTOM("أبعاد مخصصة (Custom)", -1, -1)
}

enum class AnamorphicMode(val labelAr: String) {
    AUTO("تلقائي (Automatic - يحافظ على النسبة الأصلية)"),
    NONE("بدون (None - بكسل مربع 1:1)"),
    LOOSE("مرن (Loose)"),
    CUSTOM("مخصص (Custom PAR)")
}

enum class DeinterlaceFilter(val labelAr: String, val cliFlag: String) {
    OFF("إيقاف (Off)", "off"),
    DECOMB("Decomb (تلقائي ذكي)", "decomb"),
    YADIF("Yadif (سريع)", "yadif"),
    BWDIF("Bwdif (متقدم)", "bwdif")
}

enum class DenoiseFilter(val labelAr: String, val cliFlag: String) {
    OFF("إيقاف (Off)", "off"),
    NLMEANS_LIGHT("NLMeans خفيف (Light)", "nlmeans=light"),
    NLMEANS_MEDIUM("NLMeans متوسط (Medium)", "nlmeans=medium"),
    HQDN3D_LIGHT("hqdn3d خفيف وسريع", "hqdn3d=light"),
    HQDN3D_STRONG("hqdn3d قوي للفيديو الليلي", "hqdn3d=strong")
}

enum class SharpenFilter(val labelAr: String, val cliFlag: String) {
    OFF("إيقاف (Off)", "off"),
    UNSHARP_LIGHT("UnSharp خفيف", "unsharp=light"),
    UNSHARP_MEDIUM("UnSharp متوسط", "unsharp=medium"),
    LAPSHARP("LapSharp حواف دقيقة", "lapsharp=medium")
}

enum class ColorSpaceFilter(val labelAr: String) {
    AUTO("تلقائي (Same as Source)"),
    BT709("BT.709 (HD Standard)"),
    BT601("BT.601 (SD Standard)"),
    BT2020("BT.2020 (HDR / Wide Gamut)")
}

enum class AudioCodec(val displayName: String, val labelAr: String, val isPassthru: Boolean = false, val isMuted: Boolean = false) {
    AAC("AAC (MediaCodec/Avcodec)", "AAC قياسي عالي التوافقية"),
    HE_AAC("HE-AAC (High Efficiency)", "HE-AAC ضغط صوت فائق للمساحات الصغيرة"),
    OPUS("Opus", "Opus جودة ممتازة بمعدل بت منخفض"),
    MP3("MP3", "MP3 كلاسيكي"),
    AC3("AC3 (Dolby Digital)", "AC3 للمسرح المنزلي"),
    AUTO_PASSTHRU("Auto Passthru", "نسخ الصوت الأصلي بدون إعادة ضغط (أسرع وأنقى)", isPassthru = true),
    MUTE("None (Mute Audio)", "إزالة الصوت تماماً (فيديو صامت لتقليل الحجم)", isMuted = true)
}

enum class AudioMixdown(val labelAr: String, val channels: Int) {
    MONO("أحادي (Mono 1.0)", 1),
    STEREO("ستيريو (Stereo 2.0)", 2),
    DOLBY_PRO_LOGIC_II("Dolby Pro Logic II", 2),
    SURROUND_5_1("محيطي (5.1 Channels)", 6)
}

enum class RangeSelectionMode(val labelAr: String) {
    SECONDS("الوقت بالثواني (Time Range)"),
    CHAPTERS("الفصول (Chapters)"),
    FRAMES("الإطارات (Frames)")
}

enum class HandBrakeTab(val titleAr: String, val subtitleEn: String) {
    SUMMARY("الملخص", "Summary"),
    DIMENSIONS("الأبعاد والقص", "Dimensions"),
    FILTERS("الفلاتر", "Filters"),
    VIDEO("الفيديو والترميز", "Video"),
    AUDIO("الصوت", "Audio"),
    SUBTITLES("الترجمة", "Subtitles"),
    CHAPTERS("الفصول", "Chapters")
}

enum class AiOptimizationGoal(val titleAr: String, val descriptionAr: String) {
    BEST_QUALITY_ORIGINAL_SIZE(
        "أفضل جودة مع حفظ القياس الأصلي 1:1",
        "يحافظ على أبعاد الفيديو والريلز كما هي تماماً مع ضغط ذكي بدون فقدان بصري"
    ),
    REELS_TIKTOK_SHORTS(
        "مُحسّن للريلز وتيك توك وشورتس (Reels 9:16)",
        "يضبط معدل البت وثبات الإطارات (CFR) لمنع تقطيع المنصات الاجتماعية مع بقاء القياس العمودي"
    ),
    MAX_SIZE_REDUCTION(
        "أقصى تقليص لحجم الملف مع سلاسة الحركة",
        "يختار أعلى كفاءة ضغط (HEVC/H.264) لتوفير أكبر مساحة ممكنة مع فيديو سلس"
    ),
    ULTRA_FAST_STREAM(
        "استخراج فائق السرعة (Hardware Turbo)",
        "يركز على أسرع استخراج ممكن عبر معالج الرسوميات مع نسخ الصوت المباشر"
    )
}

data class AiVideoAnalysisReport(
    val goal: AiOptimizationGoal,
    val contentTypeAr: String,
    val aspectAndOrientationAr: String,
    val technicalDiagnosisAr: String,
    val whyTheseSettingsAr: String,
    val recommendedCodec: VideoCodec,
    val recommendedContainer: ContainerFormat,
    val recommendedResolutionLimit: ResolutionLimit,
    val recommendedRf: Float,
    val recommendedFps: Int,
    val recommendedFramerateMode: FramerateMode,
    val recommendedSpeed: EncoderPresetSpeed,
    val recommendedAudioCodec: AudioCodec,
    val recommendedAudioBitrateKbps: Int,
    val estimatedSavingsPercent: Int,
    val poweredByGeminiCloud: Boolean
)

data class SubtitleTrackItem(
    val id: Int,
    val languageAr: String,
    val languageCode: String,
    val sourceName: String,
    val burnIn: Boolean = false,
    val forcedOnly: Boolean = false,
    val isDefault: Boolean = true,
    val sampleText: String = "تم ضغط هذا الفيديو بواسطة HandBrake Mobile"
)

data class ChapterMarker(
    val index: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long
)

data class SourceVideoMetadata(
    val uri: Uri,
    val filePathOrUri: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,          // Display width (e.g., 1080 for vertical Reels, 1920 for landscape)
    val height: Int,         // Display height (e.g., 1920 for vertical Reels, 1080 for landscape)
    val codedWidth: Int = width,   // Raw stream coded width before rotation matrix
    val codedHeight: Int = height, // Raw stream coded height before rotation matrix
    val rotationDegrees: Int,
    val videoCodecName: String,
    val videoBitrateBps: Long,
    val frameRate: Float,
    val hasAudio: Boolean,
    val audioCodecName: String,
    val audioBitrateBps: Long,
    val audioSampleRate: Int,
    val audioChannels: Int,
    val thumbnails: List<Bitmap> = emptyList(),
    val chapters: List<ChapterMarker> = emptyList(),
    val isGeneratedSample: Boolean = false
) {
    val isPortraitReels: Boolean
        get() = height > width
}

data class HandBrakeConfig(
    // Preset Name
    val activePresetName: String = "نفس القياس الأصلي (Original 1:1 Fast)",
    // Tab 1: Summary
    val container: ContainerFormat = ContainerFormat.MP4,
    val webOptimized: Boolean = true,
    val alignAvStart: Boolean = true,
    val ipod5gSupport: Boolean = false,
    val passthruMetadata: Boolean = true,
    // Range Selection
    val rangeMode: RangeSelectionMode = RangeSelectionMode.SECONDS,
    val trimStartFraction: Float = 0f,
    val trimEndFraction: Float = 1f,
    val startChapter: Int = 1,
    val endChapter: Int = 1,
    // Tab 2: Dimensions — DEFAULT IS ORIGINAL SO REELS / VERTICAL VIDEOS NEVER SHRINK!
    val resolutionLimit: ResolutionLimit = ResolutionLimit.ORIGINAL,
    val customWidth: Int = 1080,
    val customHeight: Int = 1920,
    val anamorphicMode: AnamorphicMode = AnamorphicMode.AUTO,
    val rotationDegrees: Int = 0, // Additional user rotation: 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val autoCrop: Boolean = true,
    val cropTop: Int = 0,
    val cropBottom: Int = 0,
    val cropLeft: Int = 0,
    val cropRight: Int = 0,
    val padTop: Int = 0,
    val padBottom: Int = 0,
    // Tab 3: Filters
    val deinterlace: DeinterlaceFilter = DeinterlaceFilter.OFF,
    val denoise: DenoiseFilter = DenoiseFilter.OFF,
    val sharpen: SharpenFilter = SharpenFilter.OFF,
    val deblockStrength: Int = 0, // 0..15
    val colorSpace: ColorSpaceFilter = ColorSpaceFilter.AUTO,
    val grayscale: Boolean = false,
    val brightnessOffset: Float = 0f, // -0.3f .. +0.3f
    val contrastScale: Float = 1.0f, // 0.6f .. 1.6f
    val saturationScale: Float = 1.0f, // 0.0f .. 2.0f
    // Tab 4: Video — DEFAULT FRAMERATE IS 0 (SAME AS SOURCE) FOR 100% SMOOTH PLAYBACK
    val videoCodec: VideoCodec = VideoCodec.H264_AVC,
    val framerateTarget: Int = 0, // 0 = Same as source, 15, 24, 25, 30, 50, 60
    val framerateMode: FramerateMode = FramerateMode.SAME_AS_SOURCE,
    val qualityMode: QualityMode = QualityMode.CONSTANT_QUALITY_RF,
    val constantQualityRf: Float = 21f, // 0 (lossless) .. 51 (smallest)
    val averageBitrateKbps: Int = 2500,
    val targetFileSizeMb: Float = 12f,
    val twoPassEncoding: Boolean = false,
    val turboFirstPass: Boolean = true,
    val encoderPresetSpeed: EncoderPresetSpeed = EncoderPresetSpeed.FAST,
    val encoderTune: EncoderTune = EncoderTune.NONE,
    val encoderProfile: String = "auto",
    val encoderLevel: String = "auto",
    val hardwareAccelerationEnabled: Boolean = true,
    // Tab 5: Audio
    val audioCodec: AudioCodec = AudioCodec.AUTO_PASSTHRU,
    val audioBitrateKbps: Int = 160,
    val audioMixdown: AudioMixdown = AudioMixdown.STEREO,
    val audioSampleRateHz: Int = 48000,
    val audioGainDb: Float = 0f,
    // Tab 6: Subtitles
    val subtitles: List<SubtitleTrackItem> = listOf(
        SubtitleTrackItem(
            id = 1,
            languageAr = "العربية (Arabic UTF-8)",
            languageCode = "ara",
            sourceName = "مسار ترجمة نصي مدمج (SRT)",
            burnIn = false,
            forcedOnly = false,
            isDefault = true
        )
    ),
    // Tab 7: Chapters
    val includeChapterMarkers: Boolean = true,
    val autoChapterIntervalSec: Int = 60,
    val customChapters: List<ChapterMarker> = emptyList()
)

enum class PresetCategory(val titleAr: String, val subtitleEn: String) {
    GENERAL("عام (General)", "Official General Presets"),
    WEB_SOCIAL("الريلز والتواصل (Reels & Social)", "Optimized for Reels, TikTok, WhatsApp, YouTube"),
    DEVICES("الأجهزة (Devices)", "Android, Apple, Consoles, Chromecast"),
    PRODUCTION("الإنتاج والأرشفة (Matroska & Pro)", "HEVC, VP9 & High Bitrate Editing"),
    CUSTOM("إعداداتي المخصصة (Custom)", "User Saved Presets")
}

data class HandBrakePreset(
    val id: String,
    val name: String,
    val nameAr: String,
    val category: PresetCategory,
    val descriptionAr: String,
    val badgeText: String,
    val config: HandBrakeConfig,
    val isCustom: Boolean = false
)

data class HardwareCodecInfo(
    val codecName: String,
    val mimeTypes: List<String>,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int
)

object OfficialHandBrakePresets {
    val allPresets: List<HandBrakePreset> = listOf(
        // 1. Original 1:1 Size & Smooth Frame Rate (Default)
        HandBrakePreset(
            id = "original_size_smooth",
            name = "Original 1:1 Smooth Fast",
            nameAr = "نفس القياس الأصلي 1:1 (تلقائي للريلز والفيديو)",
            category = PresetCategory.GENERAL,
            descriptionAr = "يحافظ على أبعاد الفيديو المختار كما هي تماماً (سواء ريلز عمودي 9:16 أو أفقي 16:9) مع ثبات تام للإطارات والصوت",
            badgeText = "1:1 Original • H.264 • RF 21",
            config = HandBrakeConfig(
                activePresetName = "نفس القياس الأصلي 1:1 (تلقائي للريلز والفيديو)",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                framerateMode = FramerateMode.SAME_AS_SOURCE,
                constantQualityRf = 21f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 160
            )
        ),
        // 2. Reels / TikTok / Shorts 9:16 Dedicated Preset
        HandBrakePreset(
            id = "reels_tiktok_shorts_pro",
            name = "Reels & TikTok 1080x1920 Pro",
            nameAr = "ريلز وتيك توك وشورتس (Reels 9:16 بدون تقليص)",
            category = PresetCategory.WEB_SOCIAL,
            descriptionAr = "مُصمم خصيصاً للفيديوهات العمودية (Reels/TikTok): يحافظ على القياس العمودي الكامل وجودة الحركة بدون أي تقطيع",
            badgeText = "Reels 9:16 • H.264 • RF 20",
            config = HandBrakeConfig(
                activePresetName = "ريلز وتيك توك وشورتس (Reels 9:16 بدون تقليص)",
                container = ContainerFormat.MP4,
                webOptimized = true,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                framerateMode = FramerateMode.CONSTANT_CFR,
                constantQualityRf = 20f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 192
            )
        ),
        // GENERAL
        HandBrakePreset(
            id = "fast_1080p30",
            name = "Fast 1080p30",
            nameAr = "سريع 1080p (يحترم الاتجاه العمودي والأفقي)",
            category = PresetCategory.GENERAL,
            descriptionAr = "الإعداد القياسي في HandBrake: توازن مثالي بين السرعة والجودة وحجم الملف مع دعم الريلز",
            badgeText = "1080p • H.264 • RF 22",
            config = HandBrakeConfig(
                activePresetName = "Fast 1080p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 22f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 160
            )
        ),
        HandBrakePreset(
            id = "hq_1080p30_surround",
            name = "HQ 1080p30 Surround",
            nameAr = "جودة فائقة HQ (نفس أبعاد المصدر)",
            category = PresetCategory.GENERAL,
            descriptionAr = "جودة بصرية فائقة مع تفاصيل دقيقة وصوت أصلي نقي بدون فقدان",
            badgeText = "Original • H.264 • RF 19",
            config = HandBrakeConfig(
                activePresetName = "HQ 1080p30 Surround",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 19f,
                encoderPresetSpeed = EncoderPresetSpeed.MEDIUM,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 192
            )
        ),
        HandBrakePreset(
            id = "fast_720p30",
            name = "Fast 720p30",
            nameAr = "سريع 720p (توفير عالٍ للحجم)",
            category = PresetCategory.GENERAL,
            descriptionAr = "يقلل حجم الفيديو بنسبة 65%+ مع الحفاظ على نسبة العرض إلى الارتفاع الأصلية",
            badgeText = "720p • H.264 • RF 23",
            config = HandBrakeConfig(
                activePresetName = "Fast 720p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.HD_720P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 23f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 128
            )
        ),
        // WEB & SOCIAL
        HandBrakePreset(
            id = "social_whatsapp_discord",
            name = "Social Messenger (< 10MB)",
            nameAr = "واتساب وديسكورد (حجم تحت 10MB)",
            category = PresetCategory.WEB_SOCIAL,
            descriptionAr = "يضبط معدل البت تلقائياً بحيث لا يتجاوز حجم الفيديو 10 ميجابايت للمشاركة الفورية مع بقاء القياس متناسقاً",
            badgeText = "Original • Target 10MB",
            config = HandBrakeConfig(
                activePresetName = "Social Messenger (< 10MB)",
                container = ContainerFormat.MP4,
                webOptimized = true,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                qualityMode = QualityMode.TARGET_FILE_SIZE,
                targetFileSizeMb = 10f,
                encoderPresetSpeed = EncoderPresetSpeed.VERYFAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 128
            )
        ),
        HandBrakePreset(
            id = "creator_1080p60",
            name = "Creator 1080p60",
            nameAr = "صانع محتوى 60 إطار/ثانية (نفس القياس)",
            category = PresetCategory.WEB_SOCIAL,
            descriptionAr = "سلاسة فائقة مع تحسين البث السريع للرفع على يوتيوب وإنستغرام وتيك توك",
            badgeText = "Original • H.264 • RF 20",
            config = HandBrakeConfig(
                activePresetName = "Creator 1080p60",
                container = ContainerFormat.MP4,
                webOptimized = true,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 20f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 256
            )
        ),
        // DEVICES
        HandBrakePreset(
            id = "android_1080p30",
            name = "Android Native 1:1",
            nameAr = "أجهزة أندرويد (نفس القياس الأصلي)",
            category = PresetCategory.DEVICES,
            descriptionAr = "مُعاير خصيصاً لمعالجات الهواتف الذكية بنظام أندرويد بتسريع العتاد الكامل",
            badgeText = "Android • H.264 • RF 22",
            config = HandBrakeConfig(
                activePresetName = "Android Native 1:1",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 22f,
                encoderProfile = "high",
                encoderLevel = "4.1",
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 160
            )
        ),
        // PRODUCTION & MATROSKA
        HandBrakePreset(
            id = "hevc_mkv_1080p30",
            name = "H.265 HEVC Max Efficiency",
            nameAr = "ضغط فائق HEVC H.265 (نفس القياس)",
            category = PresetCategory.PRODUCTION,
            descriptionAr = "يستخدم الجيل الحديث H.265 لتقليص الحجم بنسبة 45% إضافية مع الحفاظ على قياس الفيديو الأصلي",
            badgeText = "MP4 • H.265 HEVC • RF 23",
            config = HandBrakeConfig(
                activePresetName = "H.265 HEVC Max Efficiency",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H265_HEVC,
                framerateTarget = 0,
                constantQualityRf = 23f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU,
                audioBitrateKbps = 160
            )
        )
    )
}
