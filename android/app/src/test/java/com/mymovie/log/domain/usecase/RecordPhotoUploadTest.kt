package com.mymovie.log.domain.usecase

import android.net.Uri
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.testing.FakeMovieRecordRepository
import com.mymovie.log.testing.FakePhotoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Uploading new photos inside UpsertRecordUseCase (Robolectric for android.net.Uri). */
@RunWith(RobolectricTestRunner::class)
class RecordPhotoUploadTest {

    private val kept = "u/10/kept.jpg"
    private val record = MovieRecord(
        id = "r1",
        userId = "u",
        tmdbId = 10,
        title = "영화",
        status = WatchStatus.WATCHED,
        photoUrls = listOf(kept)
    )
    private val picked = listOf(Uri.parse("content://media/1"), Uri.parse("content://media/2"))

    @Test
    fun upsert_appendsUploadedPhotosAfterTheKeptOnes() = runTest {
        val records = FakeMovieRecordRepository(listOf(record))
        val photos = FakePhotoRepository()

        UpsertRecordUseCase(records, photos)(record, newPhotoUris = picked)

        assertEquals(listOf(kept, "u/10/new0.jpg", "u/10/new1.jpg"), records.records.value.single().photoUrls)
        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun upsert_deletesJustUploadedPhotosWhenTheSaveFails() = runTest {
        val records = FakeMovieRecordRepository(listOf(record)).apply { failUpsert = true }
        val photos = FakePhotoRepository()

        try {
            UpsertRecordUseCase(records, photos)(record, newPhotoUris = picked)
            fail("the save error should reach the caller")
        } catch (e: IllegalStateException) {
            // expected
        }

        assertEquals(photos.uploadedPaths, photos.deletedPaths)
        assertEquals(listOf(kept), records.records.value.single().photoUrls)
    }
}
