package dev.korryr.tubefetch.domain.model

enum class VideoQuality(val displayName: String, val resolution: String) {
    AUTO("Auto", "Best Available"),
    SD360("360p", "640×360"),
    SD480("480p", "854×480"),
    HD720("720p", "1280×720"),
    HD1080("1080p", "1920×1080"),
    QHD1440("1440p", "2560×1440"),
    UHD2160("2160p", "3840×2160"),
    UHD4K("4K", "4096×2160")
}