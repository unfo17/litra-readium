@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
package io.github.unfo17.litra

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.readium.navigator.web.reflowable.*
import org.readium.navigator.web.fixedlayout.*
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferences
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.getOrElse

class ReaderViewModel(private val app: Application, private val repository: LibraryRepository, val bookId: String) : ViewModel() {
    val book = MutableStateFlow<Book?>(null)
    val publication = MutableStateFlow<Publication?>(null)
    val reflowable = MutableStateFlow<ReflowableWebRenditionState?>(null)
    val fixed = MutableStateFlow<FixedWebRenditionState?>(null)
    val error = MutableStateFlow<String?>(null)
    val current = MutableStateFlow<Locator?>(null)
    val progress = MutableStateFlow(0.0)
    val bookmarks = repository.dao.bookmarks(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val results = MutableStateFlow<List<Locator>>(emptyList())
    val searching = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    private var allPositions: List<Locator> = emptyList()
    init { load() }
    fun load() {
        if (publication.value != null) return
        error.value = null
        viewModelScope.launch {
            var opened: Publication? = null
            try {
                val value = repository.dao.book(bookId) ?: error("图书已移除")
                book.value = value
                opened = repository.readium.open(repository.file(value.fileName))
                ensureActive()
                val pub = opened
                allPositions = withContext(Dispatchers.IO) { pub.positions() }
                val initial = value.locator?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
                current.value = initial
                progress.value = value.progress
                if (pub.metadata.layout == Layout.FIXED) {
                    val factory = FixedWebRenditionFactory(app, pub, FixedWebConfiguration()) ?: error("无法打开固定版式 EPUB")
                    fixed.value = factory.createRenditionState(initialPreferences = FixedWebPreferences(), initialLocation = initial?.let { FixedWebGoLocation(it) }).getOrElse { error(it.message) }
                } else {
                    val factory = ReflowableWebRenditionFactory(app, pub) ?: error("无法打开此 EPUB")
                    reflowable.value = factory.createRenditionState(initialLocation = initial?.let { ReflowableWebGoLocation(it) }).getOrElse { error(it.message) }
                }
                publication.value = pub
                opened = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.localizedMessage ?: "阅读器打开失败" }
            finally { opened?.close() }
        }
    }
    fun location(locator: Locator, reachedEnd: Boolean = false) {
        current.value = locator
        progress.value = if (reachedEnd) 0.99 else BookRules.progress(locator.locations.totalProgression ?: 0.0)
        // Save every observed location, including the final one, outside the screen lifecycle.
        val json = locator.toJSON().toString()
        val value = progress.value
        saveJob = writes.launch { saveMutex.withLock { repository.progress(bookId, json, value) } }
    }
    suspend fun saveNow() { current.value?.let { locator -> val value = progress.value; saveMutex.withLock { repository.progress(bookId, locator.toJSON().toString(), value) } } }
    fun bookmark() {
        current.value?.let { locator -> viewModelScope.launch { repository.dao.addBookmark(Bookmark(bookId = bookId, locator = locator.toJSON().toString(), label = locator.title ?: "${BookRules.percent(locator.locations.totalProgression ?: 0.0)}%")) } }
    }
    fun deleteBookmark(bookmark: Bookmark) { viewModelScope.launch { repository.dao.deleteBookmark(bookmark) } }
    suspend fun go(locator: Locator) { reflowable.value?.controller?.goTo(ReflowableWebGoLocation(locator)); fixed.value?.controller?.goTo(FixedWebGoLocation(locator)) }
    suspend fun go(link: Link) { reflowable.value?.controller?.goTo(link.url()); fixed.value?.controller?.goTo(link.url()) }
    suspend fun seek(fraction: Float) {
        if (allPositions.isEmpty()) return
        val index = (fraction.coerceIn(0f, 1f) * (allPositions.size - 1)).toInt()
        go(allPositions[index])
    }
    suspend fun next() { reflowable.value?.controller?.moveForward(); fixed.value?.controller?.moveForward() }
    suspend fun previous() { reflowable.value?.controller?.moveBackward(); fixed.value?.controller?.moveBackward() }
    fun search(query: String) {
        searchJob?.cancel()
        results.value = emptyList(); searchError.value = null
        if (query.isBlank()) { searching.value = false; return }
        searchJob = viewModelScope.launch {
            searching.value = true
            try {
                val pub = publication.value ?: return@launch
                val iterator = pub.search(query.trim()) ?: error("这本 EPUB 不支持书内搜索")
                try {
                    while (true) {
                        ensureActive()
                        val page = withContext(Dispatchers.IO) { iterator.next().getOrElse { error(it.message) } } ?: break
                        results.value = (results.value + page.locators).take(500)
                        if (results.value.size >= 500) { searchError.value = "已显示前 500 个结果，请缩小搜索范围。"; break }
                    }
                } finally { iterator.close() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { searchError.value = e.localizedMessage }
            finally { searching.value = false }
        }
    }
    override fun onCleared() {
        val pub = publication.value
        writes.launch {
            saveNow()
            withContext(Dispatchers.Main) { yield(); pub?.close() }
            writes.cancel()
        }
    }
}
