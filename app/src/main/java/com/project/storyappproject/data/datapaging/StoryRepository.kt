package com.project.storyappproject.data.datapaging

import androidx.lifecycle.LiveData
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.project.storyappproject.data.api.ApiService
import com.project.storyappproject.data.datastore.UserPreference
import com.project.storyappproject.data.model.response.ListStoryItem

class StoryRepository(private val pref: UserPreference, private val apiService: ApiService) {
    fun getListStories(): LiveData<PagingData<ListStoryItem>> {
        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(
                pageSize = 5
            ),
            remoteMediator = StoryRemoteMediator(pref, apiService),
            pagingSourceFactory = {
                StoryPagingSource(pref, apiService)
            }
        ).liveData
    }

    companion object {
        @Volatile
        private var instance: StoryRepository? = null
        fun getInstance(
            preferences: UserPreference,
            apiService: ApiService
        ): StoryRepository =
            instance ?: synchronized(this) {
                instance ?: StoryRepository(preferences, apiService)
            }.also { instance = it }
    }
}