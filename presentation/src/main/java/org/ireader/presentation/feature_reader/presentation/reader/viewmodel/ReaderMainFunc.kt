package org.ireader.presentation.feature_reader.presentation.reader.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.ireader.core.utils.Constants
import org.ireader.core.utils.UiText
import org.ireader.core.utils.currentTimeToLong
import org.ireader.domain.R
import org.ireader.domain.models.entities.Book
import org.ireader.domain.models.entities.Chapter
import org.ireader.domain.models.entities.History
import org.ireader.domain.utils.withIOContext
import tachiyomi.source.Source
import javax.inject.Inject

interface ReaderMainFunctions {
    suspend fun ReaderScreenViewModel.getChapter(
        chapterId: Long,
        source: Source,
        onSuccess: suspend () -> Unit = {},
    )

    suspend fun ReaderScreenViewModel.getLocalBookById(
        bookId: Long,
        chapterId: Long,
        source: Source,
    )

    suspend fun ReaderScreenViewModel.insertChapter(chapter: Chapter)
    suspend fun ReaderScreenViewModel.insertBook(book: Book)

    fun ReaderScreenViewModel.getReadingContentRemotely(
        chapter: Chapter,
        source: Source,
        onSuccess: () -> Unit = {},
    )

    fun ReaderScreenViewModel.updateLastReadTime(chapter: Chapter)
    fun ReaderScreenViewModel.getLocalChaptersByPaging(bookId: Long)
}

class ReaderMainFunctionsImpl @Inject constructor() : ReaderMainFunctions {
    override suspend fun ReaderScreenViewModel.getChapter(
        chapterId: Long,
        source: Source,
        onSuccess: suspend () -> Unit,
    ) {
        toggleLoading(true)
        toggleLocalLoaded(false)
        viewModelScope.launch {
            onNextVoice()
            val resultChapter = getChapterUseCase.findChapterById(
                chapterId = chapterId,
                state.book?.id,
            )
            if (resultChapter != null) {

                clearError()
                toggleLoading(false)
                toggleLocalLoaded(true)
                setChapter(resultChapter.copy(
                    content = resultChapter.content,
                    read = true,
                    readAt = Clock.System.now().toEpochMilliseconds()))
                val chapter = state.stateChapter
                if (
                    chapter != null &&
                    chapter.content.joinToString()
                        .isBlank() &&
                    !state.isRemoteLoading &&
                    !state.isLoading
                ) {
                    getReadingContentRemotely(chapter = chapter, source = source) {

                    }
                }
                updateLastReadTime(resultChapter)
                updateChapterSliderIndex(getCurrentIndexOfChapter(resultChapter))
                if (!initialized) {
                    initialized = true
                }
                onSuccess()
            } else {
                toggleLoading(false)
                toggleLocalLoaded(false)
            }
        }
    }

    override suspend fun ReaderScreenViewModel.getLocalBookById(
        bookId: Long,
        chapterId: Long,
        source: Source,
    ) {
        viewModelScope.launch {
            val book = getBookUseCases.findBookById(id = bookId)
            if (book != null) {
                setStateChapter(book)
                toggleBookLoaded(true)
                getLocalChaptersByPaging(bookId)
                val last = historyUseCase.findHistoryByBookId(bookId)
                if (chapterId != Constants.LAST_CHAPTER && chapterId != Constants.NO_VALUE) {
                    getChapter(chapterId, source = source)
                } else if (last != null) {
                    getChapter(chapterId = last.chapterId, source = source)
                } else {
                    val chapters = getChapterUseCase.findChaptersByBookId(bookId)
                    if (chapters.isNotEmpty()) {
                        getChapter(chapters.first().id, source = source)
                    }
                }
                if (stateChapters.isNotEmpty()) {
                    state.currentChapterIndex =
                        stateChapters.indexOfFirst { state.stateChapter?.id == it.id }

//                    if (stateChapter == null && state.stateChapters.isNotEmpty()) {
//                        getChapter(state.stateChapters.first().id, source = source)
//                    }
                }


            }
        }
    }

    override suspend fun ReaderScreenViewModel.insertChapter(chapter: Chapter) {
        withIOContext {
            insertUseCases.insertChapter(chapter)
        }
    }

    override suspend fun ReaderScreenViewModel.insertBook(book: Book) {
        insertUseCases.insertBook(book)
    }

    override fun ReaderScreenViewModel.getReadingContentRemotely(
        chapter: Chapter,
        source: Source,
        onSuccess: () -> Unit,
    ) {
        clearError()
        toggleLocalLoaded(false)
        toggleRemoteLoading(true)
        toggleLoading(true)
        getContentJob?.cancel()
        getContentJob = viewModelScope.launch(Dispatchers.IO) {
            remoteUseCases.getRemoteReadingContent(
                chapter,
                source = source,
                onSuccess = { content ->
                    if (content != null) {
                        insertChapter(content.copy(
                            dateFetch = Clock.System.now()
                                .toEpochMilliseconds(),
                            read = true, readAt = Clock.System.now()
                                .toEpochMilliseconds()
                        ))
                        setChapter(content)
                        toggleLoading(false)
                        toggleRemoteLoading(false)
                        toggleLocalLoaded(true)
                        toggleRemoteLoaded(true)
                        clearError()
                        getChapter(chapter.id, source = source)
                        onSuccess()
                    } else {
                        showSnackBar(UiText.StringResource(R.string.something_is_wrong_with_this_chapter))
                    }
                },
                onError = { message ->
                    toggleRemoteLoading(false)
                    toggleLoading(false)
                    toggleLocalLoaded(false)
                    toggleRemoteLoading(false)
                    if (message != null) {
                        showSnackBar(message)
                    }
                }
            )
        }
    }

    override fun ReaderScreenViewModel.updateLastReadTime(chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            insertUseCases.insertChapter(
                chapter = chapter.copy(read = true,
                    readAt = Clock.System.now().toEpochMilliseconds())
            )
            historyUseCase.insertHistory(History(
                bookId = chapter.bookId,
                chapterId = chapter.id,
                readAt = currentTimeToLong()))
        }
    }

    override fun ReaderScreenViewModel.getLocalChaptersByPaging(bookId: Long) {
        getChapterJob?.cancel()
        getChapterJob = viewModelScope.launch {
            getChapterUseCase.subscribeChaptersByBookId(bookId = bookId,
                isAsc = prefState.isAsc, "")
                .collect {
                    stateChapters = it
                }
        }
    }

}