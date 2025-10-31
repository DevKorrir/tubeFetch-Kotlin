package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.repository.VideoRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteDownload(id)
    }
}