package com.mymovie.log.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.mymovie.log.domain.repository.PhotoRepository
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.Storage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class PhotoRepositoryImpl @Inject constructor(
    private val storage: Storage,
    @ApplicationContext private val context: Context
) : PhotoRepository {

    companion object {
        private const val BUCKET = "movie-photos"
        private const val SIGNED_URL_EXPIRES_SECONDS = 3600L
        private const val MAX_IMAGE_DIMENSION = 1920
        private const val JPEG_QUALITY = 85
    }

    override suspend fun uploadPhotos(userId: String, tmdbId: Int, uris: List<Uri>): List<String> {
        return uris.map { uri ->
            val path = "$userId/$tmdbId/${UUID.randomUUID()}.jpg"
            val bytes = compressImage(uri)
            storage.from(BUCKET).upload(path, bytes) { upsert = true }
            AppLogger.d("REPO_PHOTO", "Uploaded: $path (${bytes.size / 1024}KB)")
            path
        }
    }

    override suspend fun getSignedUrls(paths: List<String>): List<String> {
        if (paths.isEmpty()) return emptyList()
        return paths.map { path ->
            storage.from(BUCKET).createSignedUrl(path, SIGNED_URL_EXPIRES_SECONDS.seconds)
        }
    }

    override suspend fun deletePhotos(paths: List<String>) {
        if (paths.isEmpty()) return
        storage.from(BUCKET).delete(paths)
        AppLogger.d("REPO_PHOTO", "Deleted ${paths.size} photos")
    }

    private fun compressImage(uri: Uri): ByteArray {
        val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val original = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IOException("Cannot decode image: $uri")

        val scale = minOf(1f, MAX_IMAGE_DIMENSION.toFloat() / maxOf(original.width, original.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            ).also { original.recycle() }
        } else original

        val output = if (rotationDegrees != 0f) {
            Bitmap.createBitmap(
                scaled, 0, 0, scaled.width, scaled.height,
                Matrix().apply { postRotate(rotationDegrees) },
                true
            ).also { scaled.recycle() }
        } else scaled

        return try {
            ByteArrayOutputStream().also { out ->
                output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }.toByteArray()
        } finally {
            output.recycle()
        }
    }
}
