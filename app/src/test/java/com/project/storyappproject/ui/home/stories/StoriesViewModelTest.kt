package com.project.storyappproject.ui.home.stories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.ListUpdateCallback
import com.project.storyappproject.MainDispatcherRule
import com.project.storyappproject.adapter.StoryAdapter
import com.project.storyappproject.data.datapaging.StoryRepository
import com.project.storyappproject.data.model.response.ListStoryItem
import com.project.storyappproject.data.model.response.StoriesResponse
import com.project.storyappproject.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StoriesViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val mainDispatcherRules = MainDispatcherRule()

    @Mock
    private lateinit var storyRepository: StoryRepository
    private var dummyStory = generateDummyStories()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when Get Story Should Not Null and Return Success`() = mainDispatcherRules.runBlockingTest {
        val data: PagingData<ListStoryItem> = StoriesPagingSource.snapshot(dummyStory.listStory)
        val expectedResponse = MutableLiveData<PagingData<ListStoryItem>>()

        expectedResponse.value = data
        Mockito.`when`(storyRepository.getListStories()).thenReturn(expectedResponse)

        val storiesViewModel = StoriesViewModel(storyRepository)
        val actualStory: PagingData<ListStoryItem> = storiesViewModel.getListStory.getOrAwaitValue()

        val differ = AsyncPagingDataDiffer(
            diffCallback = StoryAdapter.DIFF_CALLBACK,
            updateCallback = noopListUpdateCallback,
            workerDispatcher = Dispatchers.Main,
        )
        differ.submitData(actualStory)

        Assert.assertNotNull(differ.snapshot())
        Assert.assertEquals(dummyStory.listStory, differ.snapshot())
        Assert.assertEquals(dummyStory.listStory.size, differ.snapshot().size)
        Assert.assertEquals(dummyStory.listStory[0].name, differ.snapshot()[0]?.name)
    }
}

class StoriesPagingSource : PagingSource<Int, LiveData<List<ListStoryItem>>>() {
    companion object {
        fun snapshot(items: List<ListStoryItem>): PagingData<ListStoryItem> {
            return PagingData.from(items)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, LiveData<List<ListStoryItem>>>): Int {
        return 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LiveData<List<ListStoryItem>>> {
        return LoadResult.Page(emptyList(), 0, 1)
    }
}

val noopListUpdateCallback = object : ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {}
    override fun onRemoved(position: Int, count: Int) {}
    override fun onMoved(fromPosition: Int, toPosition: Int) {}
    override fun onChanged(position: Int, count: Int, payload: Any?) {}
}

fun generateDummyStories(): StoriesResponse {
    return StoriesResponse(
        error = false,
        message = "success",
        listStory = arrayListOf(
            ListStoryItem(
                id = "id",
                name = "name",
                description = "description",
                photoUrl = "photoUrl",
                createdAt = "createdAt",
                lat = 0.01,
                lon = 0.01
            )
        )
    )
}
