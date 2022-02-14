package com.eywa.projectlectito.features.addSnippet

import android.app.Application
import androidx.lifecycle.*
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsRepo
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddSnippetViewModel(application: Application) : AndroidViewModel(application) {
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

    val textId = MutableLiveData<Int?>(null)
    val text = textId.switchMap { textId ->
        if (textId == null) return@switchMap MutableLiveData<Text?>(null)
        textsRepo.getTextById(textId)
    }
    val textName = text.map { it?.name }

    val pageReference = MutableLiveData<Int?>(null)
    private val existingPagesOrdinals = object : MediatorLiveData<List<Int>>() {
        init {
            addSource(textId) { checkExists() }
            addSource(pageReference) { checkExists() }
        }

        fun checkExists() {
            val textId = textId.value
            val page = pageReference.value
            if (textId == null || page == null) {
                postValue(listOf())
                return
            }
            val newSource = snippetsRepo.getTextSnippetEntry(textId, page)
            addSource(newSource) { snippets ->
                postValue(snippets.map { it.ordinal })
                removeSource(newSource)
            }
        }
    }
    val pageExists = existingPagesOrdinals.map { !it.isNullOrEmpty() }

    fun insert(content: String, pageReference: Int, chapter: Int?) {
        require(content.isNotBlank()) { "Content cannot be blank" }
        val textId = textId.value ?: throw IllegalArgumentException("No text id")
        val nextOrdinal = existingPagesOrdinals.value?.maxOrNull()?.plus(1) ?: 1
        val newSnippet = TextSnippet(0, content, textId, pageReference, chapter, nextOrdinal)

        var insertedId: Long? = null
        viewModelScope.launch {
            insertedId = snippetsRepo.insert(newSnippet)
        }.invokeOnCompletion {
            // Set the text to incomplete if inserting a snippet onto the end
            val oldText = text.value
            if (it != null || oldText?.isComplete != true) return@invokeOnCompletion
            viewModelScope.launch {
                textsRepo.update(
                        Text(
                                oldText.id,
                                oldText.name,
                                insertedId!!.toInt(),
                                null,
                                false
                        )
                )
            }
        }
    }
}