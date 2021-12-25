package ir.kazemcodes.infinity.domain.repository

import ir.kazemcodes.infinity.domain.models.BookEntity
import kotlinx.coroutines.flow.Flow

interface LocalBookRepository {

    fun getBooks(): Flow<List<BookEntity>>

    fun getBookById(bookId: Int): Flow<BookEntity?>

    fun getBookByName(bookName: String): Flow<BookEntity?>


    suspend fun insertBook(bookEntity: BookEntity)

    suspend fun insertBooks(bookEntities: List<BookEntity>)

    suspend fun deleteBook(bookName: String)

    suspend fun deleteAllBook()


}