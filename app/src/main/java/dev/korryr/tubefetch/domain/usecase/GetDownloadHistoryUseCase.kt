package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadHistoryUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<List<DownloadItem>> {
        return repository.getDownloadHistory()
    }
}