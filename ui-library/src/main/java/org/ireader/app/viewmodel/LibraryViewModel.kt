package org.ireader.app.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.ireader.common_models.DisplayMode
import org.ireader.common_models.entities.BookCategory
import org.ireader.common_models.entities.BookItem
import org.ireader.common_models.entities.Category
import org.ireader.common_models.library.LibraryFilter
import org.ireader.common_models.library.LibrarySort
import org.ireader.core_ui.preferences.LibraryPreferences
import org.ireader.core_ui.viewmodel.BaseViewModel
import org.ireader.domain.use_cases.category.CategoriesUseCases
import org.ireader.domain.use_cases.local.DeleteUseCase
import org.ireader.domain.use_cases.local.LocalGetBookUseCases
import org.ireader.domain.use_cases.local.LocalGetChapterUseCase
import org.ireader.domain.use_cases.local.LocalInsertUseCases
import org.ireader.domain.use_cases.local.book_usecases.GetLibraryCategory
import org.ireader.domain.use_cases.preferences.reader_preferences.screens.LibraryScreenPrefUseCases
import org.ireader.domain.use_cases.services.ServiceUseCases
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localGetBookUseCases: LocalGetBookUseCases,
    private val insertUseCases: LocalInsertUseCases,
    private val deleteUseCase: DeleteUseCase,
    private val localGetChapterUseCase: LocalGetChapterUseCase,
    private val libraryScreenPrefUseCases: LibraryScreenPrefUseCases,
    private val state: LibraryStateImpl,
    private val serviceUseCases: ServiceUseCases,
    private val getLibraryCategory: GetLibraryCategory,
    private val libraryPreferences: LibraryPreferences,
    val getCategory: CategoriesUseCases
) : BaseViewModel(), LibraryState by state {

    var lastUsedCategory = libraryPreferences.lastUsedCategory().asState()
    var filters = libraryPreferences.filters(true).asState()

    var sorting = libraryPreferences.sorting().asState()
    val showCategoryTabs = libraryPreferences.showCategoryTabs().asState()
    val showAllCategoryTab = libraryPreferences.showAllCategory().asState()
    val showCountInCategory = libraryPreferences.showCountInCategory().asState()

    val readBadge = libraryPreferences.downloadBadges().asState()
    val unreadBadge = libraryPreferences.unreadBadges().asState()
    val goToLastChapterBadge = libraryPreferences.goToLastChapterBadges().asState()

    val bookCategories = getCategory.subscribeBookCategories().asState(emptyList())
    val deleteQueues: SnapshotStateList<BookCategory> = mutableStateListOf()
    val addQueues: SnapshotStateList<BookCategory> = mutableStateListOf()
    var showDialog: Boolean by mutableStateOf(false)

    val perCategorySettings = libraryPreferences.perCategorySettings().asState()
    val layouts = libraryPreferences.categoryFlags().asState()
    var columnInPortrait = libraryPreferences.columnsInPortrait().asState()
    val columnInLandscape by libraryPreferences.columnsInLandscape().asState()
    val layout by derivedStateOf { DisplayMode.getFlag(layouts.value) ?: DisplayMode.CompactGrid }

    init {
        readLayoutTypeAndFilterTypeAndSortType()
        libraryPreferences.showAllCategory().stateIn(scope)
            .flatMapLatest { showAll ->
                getCategory.subscribe(showAll).onEach { categories ->
                    val lastCategoryId = lastUsedCategory.value

                    val index =
                        categories.indexOfFirst { it.id == lastCategoryId }.takeIf { it >= 0 } ?: 0

                    state.categories = categories
                    state.selectedCategoryIndex = index
                }
            }.launchIn(scope)
    }

    private val loadedManga = mutableMapOf<Long, List<BookItem>>()

    fun onLayoutTypeChange(layoutType: DisplayMode) {
        categories.firstOrNull { it.id == lastUsedCategory.value }?.let { category ->
            viewModelScope.launch {
                libraryScreenPrefUseCases.libraryLayoutTypeUseCase.await(
                    category = category.category,
                    displayMode = layoutType
                )
            }

        }
    }

    fun downloadChapters() {
        serviceUseCases.startDownloadServicesUseCase(bookIds = selectedBooks.toLongArray())
        selectedBooks.clear()
    }

    fun markAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedBooks.forEach { bookId ->
                val chapters = localGetChapterUseCase.findChaptersByBookId(bookId)
                insertUseCases.insertChapters(chapters.map { it.copy(read = true) })
            }
            selectedBooks.clear()
        }
    }

    fun markAsNotRead() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedBooks.forEach { bookId ->
                val chapters = localGetChapterUseCase.findChaptersByBookId(bookId)
                insertUseCases.insertChapters(chapters.map { it.copy(read = false) })
            }
            selectedBooks.clear()
        }
    }

    fun deleteBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                deleteUseCase.unFavoriteBook(selectedBooks)
            }
//            books.filter { it.id in selectedBooks }.let {
//
//            }
            //insertUseCases.updateBook.update(it, false)
        selectedBooks.clear()
        }
    }

    fun readLayoutTypeAndFilterTypeAndSortType() {
        viewModelScope.launch {
            val sortType = libraryScreenPrefUseCases.sortersUseCase.read()
            val sortBy = libraryScreenPrefUseCases.sortersDescUseCase.read()
            this@LibraryViewModel.sortType = sortType
            this@LibraryViewModel.desc = sortBy
        }
    }

    fun toggleFilter(type: LibraryFilter.Type) {
        val newFilters = filters.value
            .map { filterState ->
                if (type == filterState.type) {
                    LibraryFilter(
                        type, when (filterState.value) {
                            LibraryFilter.Value.Included -> LibraryFilter.Value.Excluded
                            LibraryFilter.Value.Excluded -> LibraryFilter.Value.Missing
                            LibraryFilter.Value.Missing -> LibraryFilter.Value.Included
                        }
                    )
                } else {
                    filterState
                }
            }

        this.filters.value = newFilters
    }

    fun toggleSort(type: LibrarySort.Type) {
        val currentSort = sorting
        sorting.value = if (type == currentSort.value.type) {
            currentSort.value.copy(isAscending = !currentSort.value.isAscending)
        } else {
            currentSort.value.copy(type = type)
        }
    }

    fun refreshUpdate() {
        serviceUseCases.startLibraryUpdateServicesUseCase()
    }

    fun setSelectedPage(index: Int) {
        if (index == selectedCategoryIndex) return
        val categories = categories
        val category = categories.getOrNull(index) ?: return
        state.selectedCategoryIndex = index
        state.selectedCategory
        lastUsedCategory.value = category.id
    }

    fun unselectAll() {
        state.selectedBooks.clear()
    }

    fun selectAllInCurrentCategory() {
        val mangaInCurrentCategory = loadedManga[selectedCategory?.id] ?: return
        val currentSelected = selectedBooks.toList()
        val mangaIds = mangaInCurrentCategory.map { it.id }.filter { it !in currentSelected }
        state.selectedBooks.addAll(mangaIds)
    }

    fun flipAllInCurrentCategory() {
        val mangaInCurrentCategory = loadedManga[selectedCategory?.id] ?: return
        val currentSelected = selectedBooks.toList()
        val (toRemove, toAdd) = mangaInCurrentCategory.map { it.id }
            .partition { it in currentSelected }
        state.selectedBooks.removeAll(toRemove)
        state.selectedBooks.addAll(toAdd)
    }

    fun getDefaultValue(categories: Category): ToggleableState {
        val defaultValue: Boolean = selectedBooks.any { id ->
            id in bookCategories.value.filter { it.categoryId == categories.id }.map { it.bookId }
        }

        //categories.id in bookCategories.map { it.categoryId } &&

        return if (defaultValue) ToggleableState.On else ToggleableState.Off
    }

    @Composable
    fun getLibraryForCategoryIndex(categoryIndex: Int): State<List<BookItem>> {
        val scope = rememberCoroutineScope()
        val categoryId = categories.getOrNull(categoryIndex)?.id ?: return remember {
            mutableStateOf(emptyList())
        }

        val unfiltered = remember(sorting.value, filters.value, categoryId) {
            getLibraryCategory.subscribe(categoryId, sorting.value, filters.value)
                .map { list ->
                    books = list
                    list.map {
                        it.toBookItem()
                    }
                }
                .shareIn(scope, SharingStarted.WhileSubscribed(1000), 1)
        }

        return remember(sorting.value, filters.value, searchQuery) {
            val query = searchQuery
            if (query.isNullOrBlank()) {
                unfiltered
            } else {
                unfiltered.map { mangas ->
                    mangas.filter { it.title.contains(query, true) }
                }
            }
                .onEach { loadedManga[categoryId] = it }
                .onCompletion { loadedManga.remove(categoryId) }
        }.collectAsState(emptyList())
    }

    fun getColumnsForOrientation(isLandscape: Boolean, scope: CoroutineScope): StateFlow<Int> {
        return if (isLandscape) {
            libraryPreferences.columnsInLandscape()
        } else {
            libraryPreferences.columnsInPortrait()
        }.stateIn(scope)
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
