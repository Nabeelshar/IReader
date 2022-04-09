package org.ireader.domain.use_cases.local.delete_usecases.book

import org.ireader.domain.repository.LocalBookRepository
import javax.inject.Inject

/**
 * Delete All Book from database
 */
class DeleteAllBooks @Inject constructor(private val localBookRepository: LocalBookRepository) {
    suspend operator fun invoke() {
        return localBookRepository.deleteAllBooks()
    }
}
