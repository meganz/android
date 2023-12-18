package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import javax.inject.Inject

/**
 * Extract the gps coordinates from the file and assign `latitude` and `longitude`
 * to the [CameraUploadsRecord]
 * This operation is only needed if the records does not exists yet in the cloud
 */
class ExtractGpsCoordinatesUseCase @Inject constructor(
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
) {
    /**
     * Extract the gps coordinates from the file corresponding to the [CameraUploadsRecord]
     * and set the record property `latitude` and `longitude` if retrieved
     *
     * @param recordList
     * @return a list of [CameraUploadsRecord] with the gps coordinates populated
     */
    suspend operator fun invoke(
        recordList: List<CameraUploadsRecord>,
    ): List<CameraUploadsRecord> = coroutineScope {
        return@coroutineScope recordList.map { record ->
            async {
                yield()
                if (record.existingNodeId == null) {
                    runCatching {
                        getGPSCoordinatesUseCase(
                            record.filePath,
                            record.type == CameraUploadsRecordType.TYPE_VIDEO,
                        )?.let { (latitude, longitude) ->
                            record.copy(latitude = latitude, longitude = longitude)
                        } ?: record
                    }.getOrDefault(record)
                } else record
            }
        }.awaitAll()
    }
}
