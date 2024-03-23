package com.project.storyappproject.utility.di

import android.content.Context
import com.project.storyappproject.data.api.ApiConfig
import com.project.storyappproject.data.datapaging.StoryRepository
import com.project.storyappproject.data.datastore.UserPreference

object Injection {
    fun provideRepository(context: Context): StoryRepository {
        val preferences = UserPreference(context)
        val apiService = ApiConfig().getApiService()
        return StoryRepository.getInstance(preferences, apiService)
    }
}