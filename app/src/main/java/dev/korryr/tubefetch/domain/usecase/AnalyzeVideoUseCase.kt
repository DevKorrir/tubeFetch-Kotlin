package dev.korryr.tubefetch.domain.usecase

import dev.korryr.tubefetch.domain.model.Result
import dev.korryr.tubefetch.domain.model.VideoInfo
import dev.korryr.tubefetch.domain.repository.VideoRepository
import javax.inject.Inject

class AnalyzeVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(url: String): Result<VideoInfo> {
        return repository.analyzeVideo(url)
    }
}