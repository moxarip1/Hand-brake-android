package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.model.AudioCodec
import com.example.model.AudioMixdown
import com.example.model.ContainerFormat
import com.example.model.DeinterlaceFilter
import com.example.model.DenoiseFilter
import com.example.model.EncoderPresetSpeed
import com.example.model.EncoderTune
import com.example.model.FramerateMode
import com.example.model.HandBrakeConfig
import com.example.model.HandBrakePreset
import com.example.model.PresetCategory
import com.example.model.QualityMode
import com.example.model.ResolutionLimit
import com.example.model.SharpenFilter
import com.example.model.VideoCodec
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_presets")
data class CustomPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val descriptionAr: String,
    val containerName: String,
    val resolutionLimitName: String,
    val customWidth: Int,
    val customHeight: Int,
    val videoCodecName: String,
    val framerateTarget: Int,
    val framerateModeName: String,
    val qualityModeName: String,
    val constantQualityRf: Float,
    val averageBitrateKbps: Int,
    val targetFileSizeMb: Float,
    val encoderPresetSpeedName: String,
    val encoderTuneName: String,
    val audioCodecName: String,
    val audioBitrateKbps: Int,
    val audioMixdownName: String,
    val webOptimized: Boolean,
    val grayscale: Boolean,
    val deinterlaceName: String,
    val denoiseName: String,
    val sharpenName: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toHandBrakePreset(): HandBrakePreset {
        val container = runCatching { ContainerFormat.valueOf(containerName) }.getOrDefault(ContainerFormat.MP4)
        val resLimit = runCatching { ResolutionLimit.valueOf(resolutionLimitName) }.getOrDefault(ResolutionLimit.FHD_1080P)
        val vCodec = runCatching { VideoCodec.valueOf(videoCodecName) }.getOrDefault(VideoCodec.H264_AVC)
        val qMode = runCatching { QualityMode.valueOf(qualityModeName) }.getOrDefault(QualityMode.CONSTANT_QUALITY_RF)
        val fpsMode = runCatching { FramerateMode.valueOf(framerateModeName) }.getOrDefault(FramerateMode.PEAK_VFR)
        val speed = runCatching { EncoderPresetSpeed.valueOf(encoderPresetSpeedName) }.getOrDefault(EncoderPresetSpeed.FAST)
        val tune = runCatching { EncoderTune.valueOf(encoderTuneName) }.getOrDefault(EncoderTune.NONE)
        val aCodec = runCatching { AudioCodec.valueOf(audioCodecName) }.getOrDefault(AudioCodec.AAC)
        val mixdown = runCatching { AudioMixdown.valueOf(audioMixdownName) }.getOrDefault(AudioMixdown.STEREO)
        val deint = runCatching { DeinterlaceFilter.valueOf(deinterlaceName) }.getOrDefault(DeinterlaceFilter.DECOMB)
        val denoiseFilter = runCatching { DenoiseFilter.valueOf(denoiseName) }.getOrDefault(DenoiseFilter.OFF)
        val sharpenFilter = runCatching { SharpenFilter.valueOf(sharpenName) }.getOrDefault(SharpenFilter.OFF)

        val cfg = HandBrakeConfig(
            activePresetName = nameAr,
            container = container,
            webOptimized = webOptimized,
            resolutionLimit = resLimit,
            customWidth = customWidth,
            customHeight = customHeight,
            videoCodec = vCodec,
            framerateTarget = framerateTarget,
            framerateMode = fpsMode,
            qualityMode = qMode,
            constantQualityRf = constantQualityRf,
            averageBitrateKbps = averageBitrateKbps,
            targetFileSizeMb = targetFileSizeMb,
            encoderPresetSpeed = speed,
            encoderTune = tune,
            audioCodec = aCodec,
            audioBitrateKbps = audioBitrateKbps,
            audioMixdown = mixdown,
            grayscale = grayscale,
            deinterlace = deint,
            denoise = denoiseFilter,
            sharpen = sharpenFilter
        )
        return HandBrakePreset(
            id = "custom_$id",
            name = nameAr,
            nameAr = nameAr,
            category = PresetCategory.CUSTOM,
            descriptionAr = descriptionAr,
            badgeText = "${container.extension.uppercase()} • ${vCodec.shortLabel} • RF ${constantQualityRf.toInt()}",
            config = cfg,
            isCustom = true
        )
    }
}

@Entity(tableName = "transcode_jobs")
data class TranscodeJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceUriString: String,
    val sourceFileName: String,
    val sourceSizeBytes: Long,
    val sourceDurationMs: Long,
    val sourceResolution: String,
    val outputFilePath: String,
    val outputFileName: String,
    val presetName: String,
    val containerExtension: String,
    val videoCodecLabel: String,
    val targetResolution: String,
    val qualitySummary: String,
    val audioSummary: String,
    val status: String, // QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    val progressPercent: Int = 0,
    val currentStageAr: String = "في الانتظار في الطابور",
    val encodingFps: Float = 0f,
    val speedMultiplier: Float = 0f,
    val outputSizeBytes: Long = 0L,
    val elapsedTimeMs: Long = 0L,
    val cliCommand: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface HandBrakeDao {
    @Query("SELECT * FROM custom_presets ORDER BY createdAt DESC")
    fun getAllCustomPresets(): Flow<List<CustomPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPreset(preset: CustomPresetEntity)

    @Query("DELETE FROM custom_presets WHERE id = :id")
    suspend fun deleteCustomPreset(id: Int)

    @Query("SELECT * FROM transcode_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<TranscodeJobEntity>>

    @Query("SELECT * FROM transcode_jobs WHERE status = 'QUEUED' ORDER BY createdAt ASC")
    suspend fun getQueuedJobs(): List<TranscodeJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: TranscodeJobEntity): Long

    @Update
    suspend fun updateJob(job: TranscodeJobEntity)

    @Query("DELETE FROM transcode_jobs WHERE id = :id")
    suspend fun deleteJob(id: Int)

    @Query("DELETE FROM transcode_jobs WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearFinishedJobs()
}

@Database(
    entities = [CustomPresetEntity::class, TranscodeJobEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HandBrakeDatabase : RoomDatabase() {
    abstract fun handBrakeDao(): HandBrakeDao

    companion object {
        @Volatile
        private var INSTANCE: HandBrakeDatabase? = null

        fun getInstance(context: Context): HandBrakeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HandBrakeDatabase::class.java,
                    "handbrake_mobile_studio.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class HandBrakeRepository(private val dao: HandBrakeDao) {
    val customPresets: Flow<List<CustomPresetEntity>> = dao.getAllCustomPresets()
    val allJobs: Flow<List<TranscodeJobEntity>> = dao.getAllJobs()

    suspend fun saveCustomPreset(nameAr: String, descriptionAr: String, config: HandBrakeConfig) {
        val entity = CustomPresetEntity(
            nameAr = nameAr,
            descriptionAr = descriptionAr,
            containerName = config.container.name,
            resolutionLimitName = config.resolutionLimit.name,
            customWidth = config.customWidth,
            customHeight = config.customHeight,
            videoCodecName = config.videoCodec.name,
            framerateTarget = config.framerateTarget,
            framerateModeName = config.framerateMode.name,
            qualityModeName = config.qualityMode.name,
            constantQualityRf = config.constantQualityRf,
            averageBitrateKbps = config.averageBitrateKbps,
            targetFileSizeMb = config.targetFileSizeMb,
            encoderPresetSpeedName = config.encoderPresetSpeed.name,
            encoderTuneName = config.encoderTune.name,
            audioCodecName = config.audioCodec.name,
            audioBitrateKbps = config.audioBitrateKbps,
            audioMixdownName = config.audioMixdown.name,
            webOptimized = config.webOptimized,
            grayscale = config.grayscale,
            deinterlaceName = config.deinterlace.name,
            denoiseName = config.denoise.name,
            sharpenName = config.sharpen.name
        )
        dao.insertCustomPreset(entity)
    }

    suspend fun deleteCustomPreset(id: Int) = dao.deleteCustomPreset(id)

    suspend fun addJob(job: TranscodeJobEntity): Long = dao.insertJob(job)

    suspend fun updateJob(job: TranscodeJobEntity) = dao.updateJob(job)

    suspend fun getQueuedJobs(): List<TranscodeJobEntity> = dao.getQueuedJobs()

    suspend fun deleteJob(id: Int) = dao.deleteJob(id)

    suspend fun clearFinishedJobs() = dao.clearFinishedJobs()
}
