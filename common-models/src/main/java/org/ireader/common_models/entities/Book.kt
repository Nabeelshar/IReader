package org.ireader.common_models.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.ireader.core_api.source.model.MangaInfo

@Serializable
@Entity(tableName = BOOK_TABLE)
data class Book(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val tableId: Long = 0,
    val type:Long = 0,
    override val sourceId: Long,
    val key: String,
    override val title: String,
    override val author: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val status: Int = 0,
    override val cover: String = "",
    override val customCover: String = "",
    override val favorite: Boolean = false,
    val lastUpdated: Long = 0,
    val dataAdded: Long = 0,
    val viewer: Int = 0,
    val flags: Int = 0,
) : BaseBook {

    companion object {
        fun Book.toBookInfo(sourceId: Long): MangaInfo {
            return MangaInfo(
                cover = this.cover,
                key = this.key,
                title = this.title,
                status = this.status,
                genres = this.genres,
                description = this.description,
                author = this.author,
            )
        }

        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
    }

    fun getStatusByName(): String {
        return when (status) {
            0 -> "UNKNOWN"
            1 -> "ONGOING"
            2 -> "COMPLETED"
            3 -> "LICENSED"
            else -> "UNKNOWN"
        }
    }
}

fun String.takeIf(statement: () -> Boolean, defaultValue: String): String {
    return if (!statement()) {
        defaultValue
    } else {
        this
    }
}

fun MangaInfo.toBook(sourceId: Long, tableId: Long = 0, lastUpdated: Long = 0): Book {
    return Book(
        id = 0,
        sourceId = sourceId,
        customCover = this.cover,
        cover = this.cover,
        flags = 0,
        key = this.key,
        dataAdded = 0L,
        lastUpdated = lastUpdated,
        favorite = false,
        title = this.title,
        status = this.status,
        genres = this.genres,
        description = this.description,
        author = this.author,
        tableId = tableId
    )
}

fun MangaInfo.fromBookInfo(sourceId: Long): Book {
    return Book(
        id = 0,
        sourceId = sourceId,
        customCover = this.cover,
        cover = this.cover,
        flags = 0,
        key = this.key,
        dataAdded = 0L,
        lastUpdated = 0L,
        favorite = false,
        title = this.title,
        status = this.status,
        genres = this.genres,
        description = this.description,
        author = this.author,
    )
}

data class BookWithInfo(
    val id: Long = 0,
    val title: String,
    val lastRead: Long = 0,
    val lastUpdated: Long = 0,
    val unread: Boolean = false,
    val totalChapters: Int = 0,
    val dateUpload: Long = 0,
    val dateFetch: Long = 0,
    val dataAdded: Long = 0,
    val sourceId: Long,
    val totalDownload: Int,
    val isRead: Int,
    val link: String,
    val status: Int = 0,
    val cover: String = "",
    val customCover: String = "",
    val favorite: Boolean = false,
    val tableId: Long = 0,
    val author: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val viewer: Int = 0,
    val flags: Int = 0,
)

fun BookWithInfo.toBook(): Book {
    return Book(
        id = this.id,
        sourceId = sourceId,
        customCover = this.cover,
        cover = this.cover,
        flags = 0,
        key = this.link,
        dataAdded = 0L,
        lastUpdated = 0L,
        favorite = false,
        title = this.title,
        status = this.status,
        genres = this.genres,
        description = this.description,
        author = this.author,
    )
}

interface BaseBook {
    val id: Long
    val sourceId: Long
    val title: String
    val author: String
    val favorite: Boolean
    val cover: String
    val customCover: String
}

data class BookItem(
    override val id: Long = 0,
    override val sourceId: Long,
    override val title: String,
    override val author: String = "",
    override val favorite: Boolean = false,
    override val cover: String = "",
    override val customCover: String = "",
    val totalDownload: Int? = 0,
) : BaseBook
