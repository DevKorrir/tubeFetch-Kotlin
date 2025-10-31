package dev.korryr.tubefetch.domain.model

//sealed class ApiResult<out T> {
//    data class Success<T>(val data: T) : ApiResult<T>()
//    data class Error(val message: String, val exception: Exception? = null) : ApiResult<Nothing>()
//    object Loading : ApiResult<Nothing>()
//}
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}