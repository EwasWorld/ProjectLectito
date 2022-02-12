package com.eywa.projectlectito.editSnippet

import android.app.Application
import androidx.lifecycle.*
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import kotlinx.coroutines.launch
import javax.inject.Inject

class EditSnippetViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    /*
     * Repos
     */
    private val textsRepo = TextsRepo(db.textsDao())
    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())

    /*
     * Edit section info
     */
    val snippetId = MutableLiveData<Int?>(null)
    val startEnd = MutableLiveData<Pair<Int, Int?>?>(null)
    private val snippet = snippetId.distinctUntilChanged().switchMap {
        if (it == null) return@switchMap MutableLiveData<TextSnippet?>(null)
        @Suppress("unchecked_cast") // Explicit cast required else compiler error
        snippetsRepo.getTextSnippetById(it) as LiveData<TextSnippet?>
    }

    /*
     * Items for display
     */
    val textName = snippet.switchMap { snippet ->
        if (snippet == null) return@switchMap MutableLiveData<String?>(null)
        textsRepo.getTextById(snippet.textId).map { it?.name }
    }
    val pageInfo = snippet.map { snippet ->
        if (snippet == null) return@map null
        snippet.getChapterPageString()
    }
    val editSection: LiveData<String?> = object : MediatorLiveData<String?>() {
        init {
            addSource(snippet.distinctUntilChanged()) { getEditSection() }
            addSource(startEnd.distinctUntilChanged()) { getEditSection() }
        }

        fun getEditSection() {
            val content = snippet.value?.content
            val start = startEnd.value?.first
            val end = startEnd.value?.second

            if (content.isNullOrBlank() || start == null) {
                postValue(null)
                return
            }

            if (start < 0 || end != null && (start >= end || end > content.length)) {
                postValue(null)
                return
            }

            postValue(content.substring(start, end ?: content.length))
        }
    }

    fun update(newContent: String) {
        val current = snippet.value ?: throw IllegalStateException("No snippet")
        val startEnd = startEnd.value ?: throw IllegalStateException("No start or end boundaries")

        val updatedSnippet = TextSnippet(
                current.id,
                current.content.replaceRange(startEnd.first, startEnd.second ?: current.content.length, newContent),
                current.textId,
                current.pageReference,
                current.chapterId,
                current.ordinal
        )
        viewModelScope.launch {
            snippetsRepo.update(updatedSnippet)
        }
    }
}