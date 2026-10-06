package com.example

import com.seikotv.app.data.FirestoreContentDataSource
import com.seikotv.app.data.PaginatedContentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestorePaginationTest {

    @Test
    fun testDefaultPageSizeConstant() {
        assertEquals(20L, FirestoreContentDataSource.DEFAULT_PAGE_SIZE)
        assertEquals(20L, com.example.data.datasource.remote.FirestoreContentDataSource.DEFAULT_PAGE_SIZE)
    }

    @Test
    fun testPaginatedContentResult_properties() {
        val emptyResult = PaginatedContentResult(
            items = emptyList(),
            lastDocument = null,
            hasMore = false
        )
        assertTrue(emptyResult.items.isEmpty())
        assertNull(emptyResult.lastDocument)
        assertFalse(emptyResult.hasMore)
    }
}
