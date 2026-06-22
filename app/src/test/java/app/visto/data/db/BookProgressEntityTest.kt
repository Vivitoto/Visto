package app.visto.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookProgressEntityTest {

    @Test
    fun defaultsMatchReaderDesign() {
        val entity = BookProgressEntity(
            accountId = 7,
            path = "/Books/demo.txt",
            name = "demo.txt",
            sizeBytes = 1024,
            etag = "etag-1",
            encoding = "UTF-8",
            chapterTitle = null,
            lastReadAt = 100,
            addedAt = 50,
        )

        assertEquals(0, entity.id)
        assertEquals(7, entity.accountId)
        assertEquals("/Books/demo.txt", entity.path)
        assertEquals("demo.txt", entity.name)
        assertEquals(1024L, entity.sizeBytes)
        assertEquals("etag-1", entity.etag)
        assertEquals("UTF-8", entity.encoding)
        assertEquals(0, entity.chapterIndex)
        assertEquals(null, entity.chapterTitle)
        assertEquals(0, entity.pageOffset)
        assertEquals(0, entity.totalChapters)
        assertEquals(18, entity.fontSizeSp)
        assertEquals(1.5f, entity.lineSpacing, 0.0001f)
        assertEquals("light", entity.theme)
        assertEquals("system", entity.fontChoice)
        assertEquals("default", entity.textColor)
        assertEquals("default", entity.backgroundStyle)
        assertEquals(100L, entity.lastReadAt)
        assertEquals(50L, entity.addedAt)
    }

    @Test
    fun roomEntityAnnotationMatchesSchema() {
        val entityAnnotation = BookProgressEntity::class.java.getAnnotation(Entity::class.java)
        assertNotNull(entityAnnotation)
        entityAnnotation!!

        assertEquals("book_progress", entityAnnotation.tableName)

        val uniquePathIndex = entityAnnotation.indices.single { index ->
            index.value.contentEquals(arrayOf("accountId", "path"))
        }
        assertTrue(uniquePathIndex.unique)

        val foreignKey = entityAnnotation.foreignKeys.single()
        assertEquals(DavAccountEntity::class, foreignKey.entity)
        assertTrue(foreignKey.parentColumns.contentEquals(arrayOf("id")))
        assertTrue(foreignKey.childColumns.contentEquals(arrayOf("accountId")))
        assertEquals(ForeignKey.CASCADE, foreignKey.onDelete)
    }
}
