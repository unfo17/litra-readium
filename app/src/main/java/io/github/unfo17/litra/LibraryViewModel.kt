package io.github.unfo17.litra

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(val repository: LibraryRepository, val preferencesRepository: PreferencesRepository, val goalsRepository: GoalsRepository) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val books = repository.books.catch { error.value = "书库读取失败：${it.localizedMessage}"; emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val memberships = repository.memberships.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())
    val history = repository.dao.history().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val importing = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val goals = goalsRepository.goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val checkIns = goalsRepository.checkIns.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    init { action { goalsRepository.initialize() } }
    val filter = MutableStateFlow("图书")
    fun action(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { error.value = e.localizedMessage ?: "操作失败，请重试" } } }
    fun import(uris: List<Uri>) = action {
        if (importing.value) return@action
        importing.value = true
        var success = 0
        val failures = mutableListOf<String>()
        try {
            uris.forEach { uri ->
                try { repository.import(uri); success++ }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { failures += e.localizedMessage ?: "无法解析 EPUB" }
            }
            message.value = "已导入 $success 本" + if (failures.isEmpty()) "" else "；${failures.size} 本失败：${failures.first()}"
        } finally { importing.value = false }
    }
    fun finish(book: Book) = action { repository.finish(book.id) }
    fun want(book: Book) = action { repository.dao.want(book.id) }
    fun delete(book: Book) = action { repository.delete(book) }
    fun category(name: String) = action { repository.category(name) }
    fun deleteCategories(ids: List<String>) = action { repository.dao.deleteCategories(ids); if (filter.value in ids) filter.value = "图书" }
    fun addCategory(book: Book, category: Category) = action { repository.dao.addToCategory(BookCategory(book.id, category.id)) }
    fun textPreference(key: String, value: String) = action { preferencesRepository.text(key, value) }
    fun flagPreference(key: String, value: Boolean) = action { preferencesRepository.flag(key, value) }
    fun search(query: String) = action { if (preferences.value.rememberSearch && query.isNotBlank()) repository.dao.rememberSearch(query.trim().take(120)) }
}
