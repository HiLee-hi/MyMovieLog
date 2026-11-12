package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.testing.FakeMovieRecordRepository
import com.mymovie.log.testing.FakePhotoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordPhotoCleanupTest {

    private val record = MovieRecord(
        id = "r1",
        tmdbId = 10,
        title = "영화",
        status = WatchStatus.WATCHED,
        photoUrls = listOf("u/10/a.jpg", "u/10/b.jpg")
    )

    @Test
    fun delete_removesRecordAndItsPhotos() = runTest {
        val records = FakeMovieRecordRepository(listOf(record))
        val photos = FakePhotoRepository()

        DeleteRecordUseCase(records, photos)("r1")

        assertTrue(records.records.value.isEmpty())
        assertEquals(listOf("u/10/a.jpg", "u/10/b.jpg"), photos.deletedPaths)
    }

    @Test
    fun delete_keepsRecordDeletedWhenPhotoCleanupFails() = runTest {
        val records = FakeMovieRecordRepository(listOf(record))

        DeleteRecordUseCase(records, FakePhotoRepository(failDelete = true))("r1")

        assertTrue(records.records.value.isEmpty())
    }

    @Test
    fun upsert_deletesOnlyPhotosRemovedInTheEdit() = runTest {
        val records = FakeMovieRecordRepository(listOf(record))
        val photos = FakePhotoRepository()

        UpsertRecordUseCase(records, photos)(record.copy(photoUrls = listOf("u/10/b.jpg", "u/10/c.jpg")))

        assertEquals(listOf("u/10/a.jpg"), photos.deletedPaths)
        assertEquals(listOf("u/10/b.jpg", "u/10/c.jpg"), records.records.value.single().photoUrls)
    }

    @Test
    fun upsert_deletesNothingForNewRecordOrUnchangedPhotos() = runTest {
        val records = FakeMovieRecordRepository()
        val photos = FakePhotoRepository()
        val upsert = UpsertRecordUseCase(records, photos)

        upsert(record)
        upsert(record.copy(rating = 4f))

        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun upsert_savesEvenWhenPhotoCleanupFails() = runTest {
        val records = FakeMovieRecordRepository(listOf(record))

        UpsertRecordUseCase(records, FakePhotoRepository(failDelete = true))(record.copy(photoUrls = emptyList()))

        assertTrue(records.records.value.single().photoUrls.isEmpty())
    }
}
