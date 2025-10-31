package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.repository.VideoRepository
import javax.inject.Inject

class GetDownloadHistoryUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(): List<DownloadItem> {
        return repository.getDownloadHistory()
    }
}