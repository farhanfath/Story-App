package com.project.storyappproject.ui.home.stories

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.storyappproject.adapter.LoadingStateAdapter
import com.project.storyappproject.adapter.StoryAdapter
import com.project.storyappproject.data.model.response.ListStoryItem
import com.project.storyappproject.databinding.FragmentStoriesBinding
import com.project.storyappproject.ui.home.DetailStoryActivity
import com.project.storyappproject.ui.home.DetailStoryActivity.Companion.DETAIL_STORY
import com.project.storyappproject.ui.home.post.PostActivity
import com.project.storyappproject.utility.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StoriesFragment : Fragment() {

    private var _binding: FragmentStoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var factory: ViewModelFactory
    private val storiesViewModel: StoriesViewModel by viewModels { factory }

    private lateinit var storyAdapter: StoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStoriesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModelHandler()
        postButtonHandler()

        return root
    }

    private fun viewModelHandler() {
        factory = ViewModelFactory.getInstance(binding.root.context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { showListStories(it) }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            binding.progressBarStories.visibility = View.VISIBLE
            binding.rvStories.visibility = View.GONE

            delay(PostActivity.SPACE_TIME)

            context?.let { showListStories(it) }

            binding.progressBarStories.visibility = View.GONE
            binding.rvStories.visibility = View.VISIBLE
        }
    }

    private fun openDetailStory(story: ListStoryItem, sharedViews: Array<Pair<View, String>>) {
        val intent = Intent(binding.root.context, DetailStoryActivity::class.java)
        intent.putExtra(DETAIL_STORY, story)

        val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            *sharedViews
        )

        startActivity(intent, optionsCompat.toBundle())
    }

    private fun showListStories(context: Context) {
        storyAdapter = StoryAdapter()
        val storiesRv = binding.rvStories

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            storiesRv.layoutManager = GridLayoutManager(context, 2)
        } else {
            storiesRv.layoutManager = LinearLayoutManager(context)
        }

        storiesRv.adapter = storyAdapter.withLoadStateFooter(
            footer = LoadingStateAdapter {
                storyAdapter.retry()
            }
        )

        storiesViewModel.getListStory.observe(viewLifecycleOwner) {
            storyAdapter.submitData(lifecycle, it)
        }

        storyAdapter.setOnItemClickCallback(object : StoryAdapter.OnItemClickCallback {

            override fun onItemClicked(
                data: ListStoryItem,
                sharedViews: Array<Pair<View, String>>
            ) {
                openDetailStory(data, sharedViews)
            }
        })
    }

    private fun postButtonHandler() {
        binding.createStoryButton.setOnClickListener {
            val intent = Intent(binding.root.context, PostActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}