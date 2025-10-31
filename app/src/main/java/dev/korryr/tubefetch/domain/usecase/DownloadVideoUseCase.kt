package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.model.DownloadRequest
import dev.korryr.tubefetch.domain.model.Result
import dev.korryr.tubefetch.domain.repository.VideoRepository
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(request: DownloadRequest): Result<Unit> {
        return repository.downloadVideo(request)
    }
}