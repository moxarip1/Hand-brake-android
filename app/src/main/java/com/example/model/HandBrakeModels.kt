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
    MP4("MP4 (MPEG-4)", "mp4", "video/mp4", "الأكثر توافقاً مع الهواتف والويب وجميع الشاشات"),
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
        "تحويل فائق السرعة بتسريع العتاد وتوافقية 100% مع كل الأجهزة"
    ),
    H265_HEVC(
        "H.265 (HEVC 10-bit/8-bit HW)",
        "H.265 / HEVC",
        MediaFormat.MIMETYPE_VIDEO_HEVC,
        0.65f,
        "يوفر حتى 40% من الحجم بنفس الجودة البصرية compared to H.264"
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
    CONSTANT_QUALITY_RF("جودة ثابتة (Constant Quality RF)", "الوضع الموصى به في HandBrake للحفاظ على الجودة المثلى"),
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
    ORIGINAL("الأصلي بدون تصغير (Original)", 0, 0),
    UHD_4K("4K UHD (3840×2160)", 3840, 2160),
    FHD_1080P("1080p Full HD (1920×1080)", 1920, 1080),
    HD_720P("720p HD (1280×720)", 1280, 720),
    SD_480P("480p SD (854×480)", 854, 480),
    LOW_360P("360p Compact (640×360)", 640, 360),
    CUSTOM("أبعاد مخصصة (Custom)", -1, -1)
}

enum class AnamorphicMode(val labelAr: String) {
    AUTO("تلقائي (Automatic - يحافظ على النسبة)"),
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
    AUTO_PASSTHRU("Auto Passthru", "نسخ الصوت الأصلي بدون إعادة ضغط (أسرع)", isPassthru = true),
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

data class SubtitleTrackItem(
    val id: Int,
    val languageAr: String,
    val languageCode: String,
    val sourceName: String,
    val burnIn: Boolean = true,
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
    val width: Int,
    val height: Int,
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
)

data class HandBrakeConfig(
    // Preset Name
    val activePresetName: String = "Fast 1080p30",
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
    // Tab 2: Dimensions
    val resolutionLimit: ResolutionLimit = ResolutionLimit.FHD_1080P,
    val customWidth: Int = 1280,
    val customHeight: Int = 720,
    val anamorphicMode: AnamorphicMode = AnamorphicMode.AUTO,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val autoCrop: Boolean = true,
    val cropTop: Int = 0,
    val cropBottom: Int = 0,
    val cropLeft: Int = 0,
    val cropRight: Int = 0,
    val padTop: Int = 0,
    val padBottom: Int = 0,
    // Tab 3: Filters
    val deinterlace: DeinterlaceFilter = DeinterlaceFilter.DECOMB,
    val denoise: DenoiseFilter = DenoiseFilter.OFF,
    val sharpen: SharpenFilter = SharpenFilter.OFF,
    val deblockStrength: Int = 0, // 0..15
    val colorSpace: ColorSpaceFilter = ColorSpaceFilter.AUTO,
    val grayscale: Boolean = false,
    val brightnessOffset: Float = 0f, // -0.3f .. +0.3f
    val contrastScale: Float = 1.0f, // 0.6f .. 1.6f
    val saturationScale: Float = 1.0f, // 0.0f .. 2.0f
    // Tab 4: Video
    val videoCodec: VideoCodec = VideoCodec.H264_AVC,
    val framerateTarget: Int = 30, // 0 = Same as source, 15, 24, 25, 30, 50, 60
    val framerateMode: FramerateMode = FramerateMode.PEAK_VFR,
    val qualityMode: QualityMode = QualityMode.CONSTANT_QUALITY_RF,
    val constantQualityRf: Float = 22f, // 0 (lossless) .. 51 (smallest)
    val averageBitrateKbps: Int = 2500,
    val targetFileSizeMb: Float = 12f,
    val twoPassEncoding: Boolean = false,
    val turboFirstPass: Boolean = true,
    val encoderPresetSpeed: EncoderPresetSpeed = EncoderPresetSpeed.FAST,
    val encoderTune: EncoderTune = EncoderTune.NONE,
    val encoderProfile: String = "auto", // auto, baseline, main, high
    val encoderLevel: String = "auto", // auto, 4.0, 4.1, 4.2, 5.1
    val hardwareAccelerationEnabled: Boolean = true,
    // Tab 5: Audio
    val audioCodec: AudioCodec = AudioCodec.AAC,
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
    WEB_SOCIAL("الويب والتواصل (Web & Social)", "Optimized for WhatsApp, Discord, YouTube"),
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
        // GENERAL
        HandBrakePreset(
            id = "very_fast_1080p30",
            name = "Very Fast 1080p30",
            nameAr = "سريع جداً 1080p30 (Very Fast)",
            category = PresetCategory.GENERAL,
            descriptionAr = "ضغط سريع للغاية بدقة Full HD H.264 مثالي للهواتف والمشاركة اليومية",
            badgeText = "1080p • H.264 • RF 24",
            config = HandBrakeConfig(
                activePresetName = "Very Fast 1080p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 24f,
                encoderPresetSpeed = EncoderPresetSpeed.VERYFAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 128
            )
        ),
        HandBrakePreset(
            id = "fast_1080p30",
            name = "Fast 1080p30",
            nameAr = "سريع 1080p30 (الإعداد الافتراضي)",
            category = PresetCategory.GENERAL,
            descriptionAr = "الإعداد الافتراضي الرسمي في HandBrake: توازن مثالي بين السرعة والجودة وحجم الملف",
            badgeText = "1080p • H.264 • RF 22",
            config = HandBrakeConfig(
                activePresetName = "Fast 1080p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 22f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 160
            )
        ),
        HandBrakePreset(
            id = "hq_1080p30_surround",
            name = "HQ 1080p30 Surround",
            nameAr = "جودة عالية HQ 1080p30",
            category = PresetCategory.GENERAL,
            descriptionAr = "جودة بصرية فائقة مع تفاصيل دقيقة وصوت عالي النقاء 192 kbps",
            badgeText = "1080p • H.264 • RF 20",
            config = HandBrakeConfig(
                activePresetName = "HQ 1080p30 Surround",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 20f,
                encoderPresetSpeed = EncoderPresetSpeed.MEDIUM,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 192
            )
        ),
        HandBrakePreset(
            id = "fast_720p30",
            name = "Fast 720p30",
            nameAr = "سريع 720p30 (توفير عالٍ للحجم)",
            category = PresetCategory.GENERAL,
            descriptionAr = "يقلل حجم الفيديو بنسبة 65%+ مع الحفاظ على وضوح ممتاز على شاشة الهاتف",
            badgeText = "720p • H.264 • RF 23",
            config = HandBrakeConfig(
                activePresetName = "Fast 720p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.HD_720P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 23f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 128
            )
        ),
        HandBrakePreset(
            id = "very_fast_480p30",
            name = "Very Fast 480p30",
            nameAr = "سريع جداً 480p30 (أقصى ضغط)",
            category = PresetCategory.GENERAL,
            descriptionAr = "أصغر حجم ممكن مع سرعة تحويل خاطفة للاتصالات البطيئة",
            badgeText = "480p • H.264 • RF 26",
            config = HandBrakeConfig(
                activePresetName = "Very Fast 480p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.SD_480P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 26f,
                encoderPresetSpeed = EncoderPresetSpeed.ULTRAFAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 96
            )
        ),
        // WEB & SOCIAL
        HandBrakePreset(
            id = "social_whatsapp_discord",
            name = "Social Messenger (< 10MB)",
            nameAr = "واتساب وديسكورد (حجم تحت 10MB)",
            category = PresetCategory.WEB_SOCIAL,
            descriptionAr = "يضبط معدل البت تلقائياً بحيث لا يتجاوز حجم الفيديو 10 ميجابايت للمشاركة الفورية",
            badgeText = "720p • Target 10MB • FastStart",
            config = HandBrakeConfig(
                activePresetName = "Social Messenger (< 10MB)",
                container = ContainerFormat.MP4,
                webOptimized = true,
                resolutionLimit = ResolutionLimit.HD_720P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                qualityMode = QualityMode.TARGET_FILE_SIZE,
                targetFileSizeMb = 10f,
                encoderPresetSpeed = EncoderPresetSpeed.VERYFAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 128
            )
        ),
        HandBrakePreset(
            id = "creator_1080p60",
            name = "Creator 1080p60",
            nameAr = "صانع محتوى يوتيوب 1080p60",
            category = PresetCategory.WEB_SOCIAL,
            descriptionAr = "سلاسة 60 إطار/ثانية مع تحسين البث السريع للرفع على يوتيوب وتيك توك",
            badgeText = "1080p60 • H.264 • RF 20",
            config = HandBrakeConfig(
                activePresetName = "Creator 1080p60",
                container = ContainerFormat.MP4,
                webOptimized = true,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 60,
                constantQualityRf = 20f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 256
            )
        ),
        // DEVICES
        HandBrakePreset(
            id = "android_1080p30",
            name = "Android 1080p30",
            nameAr = "أجهزة أندرويد 1080p30",
            category = PresetCategory.DEVICES,
            descriptionAr = "مُعاير خصيصاً لمعالجات الهواتف الذكية والأجهزة اللوحية بنظام أندرويد",
            badgeText = "Android • H.264 • RF 22",
            config = HandBrakeConfig(
                activePresetName = "Android 1080p30",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 30,
                constantQualityRf = 22f,
                encoderProfile = "high",
                encoderLevel = "4.1",
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 160
            )
        ),
        HandBrakePreset(
            id = "apple_1080p60",
            name = "Apple 1080p60 Surround",
            nameAr = "أجهزة Apple / iPhone 1080p60",
            category = PresetCategory.DEVICES,
            descriptionAr = "متوافق تماماً مع iOS و macOS و Apple TV",
            badgeText = "Apple • H.264 • RF 21",
            config = HandBrakeConfig(
                activePresetName = "Apple 1080p60 Surround",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 60,
                constantQualityRf = 21f,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 160
            )
        ),
        // PRODUCTION & MATROSKA
        HandBrakePreset(
            id = "hevc_mkv_1080p30",
            name = "H.265 MKV 1080p30",
            nameAr = "ضغط فائق HEVC H.265 1080p",
            category = PresetCategory.PRODUCTION,
            descriptionAr = "يستخدم الجيل الحديث H.265 لتقليص الحجم بنسبة 45% إضافية مع الحفاظ على دقة 1080p",
            badgeText = "MKV • H.265 HEVC • RF 23",
            config = HandBrakeConfig(
                activePresetName = "H.265 MKV 1080p30",
                container = ContainerFormat.MKV,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.H265_HEVC,
                framerateTarget = 30,
                constantQualityRf = 23f,
                encoderPresetSpeed = EncoderPresetSpeed.FAST,
                audioCodec = AudioCodec.AAC,
                audioBitrateKbps = 160
            )
        ),
        HandBrakePreset(
            id = "vp9_webm_1080p30",
            name = "VP9 WebM 1080p30",
            nameAr = "حاوية WebM بترميز VP9",
            category = PresetCategory.PRODUCTION,
            descriptionAr = "تحويل إلى صيغة WebM القياسية بترميز VP9 وصوت Opus",
            badgeText = "WebM • VP9 • Opus",
            config = HandBrakeConfig(
                activePresetName = "VP9 WebM 1080p30",
                container = ContainerFormat.WEBM,
                resolutionLimit = ResolutionLimit.FHD_1080P,
                videoCodec = VideoCodec.VP9,
                framerateTarget = 30,
                constantQualityRf = 24f,
                audioCodec = AudioCodec.OPUS,
                audioBitrateKbps = 128
            )
        ),
        HandBrakePreset(
            id = "remux_ultrafast_passthru",
            name = "Ultra-Fast Stream Passthru",
            nameAr = "قص وتحويل حاوية فوري (Audio Passthru)",
            category = PresetCategory.PRODUCTION,
            descriptionAr = "سرعة قصوى مع نسخ مسار الصوت الأصلي كما هو دون إعادة ضغط الصوت",
            badgeText = "MP4 • Passthru • Ultrafast",
            config = HandBrakeConfig(
                activePresetName = "Ultra-Fast Stream Passthru",
                container = ContainerFormat.MP4,
                resolutionLimit = ResolutionLimit.ORIGINAL,
                videoCodec = VideoCodec.H264_AVC,
                framerateTarget = 0,
                constantQualityRf = 22f,
                encoderPresetSpeed = EncoderPresetSpeed.ULTRAFAST,
                audioCodec = AudioCodec.AUTO_PASSTHRU
            )
        )
    )
}
