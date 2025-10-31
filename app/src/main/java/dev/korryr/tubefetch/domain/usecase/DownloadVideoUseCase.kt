package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.DownloadRequest
import dev.korryr.tubefetch.domain.repository.VideoRepository
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(request: DownloadRequest): ApiResult<Unit> {
        return repository.downloadVideo(request)
    }
}