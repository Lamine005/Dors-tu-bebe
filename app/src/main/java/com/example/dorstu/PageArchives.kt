package com.example.dorstu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dorstu.databinding.ActivityPageArchivesBinding
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.dorstu.databinding.ItemStoryBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.OptionalLong

class PageArchives : AppCompatActivity() {

    private lateinit var binding: ActivityPageArchivesBinding
    private lateinit var adapter: StoriesAdapter
    private val db = FirebaseFirestore.getInstance()

    private var userId: String? = null
    private var profileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageArchivesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid
        profileName = intent.getStringExtra("ProfileName")

        if (userId == null || profileName == null) {
            Log.d("PageArchives", "User ID or Profile Name is null")
            return
        }

        adapter = StoriesAdapter(emptyList()) { story ->
            val intent = Intent(this, PageLecture::class.java).apply {
                putExtra("generatedStory", story.content)
            }
            startActivity(intent)
        }

        setupRecyclerView()
        addDataToList(profileName!!)
        setupSearchView()
    }


    private fun setupRecyclerView() {
        binding.ArchivesRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@PageArchives)
            adapter = this@PageArchives.adapter
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.ArchivesRecyclerView)
    }

    private fun addDataToList(profileName: String) {
        Log.d("PageArchives", "addDataToList called for profile: $profileName")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("archives").document(profileName)
            .collection("stories")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("PageArchives", "Successfully fetched documents")
                val fetchedStories = documents.mapNotNull { doc ->
                    Story(
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        savedDate = doc.getLong("savedDate") ?: 0L,
                        documentId = doc.id
                    )
                }.sortedByDescending{it.savedDate}
                Log.d("PageArchives", "Fetched ${fetchedStories.size} stories")
                adapter.updateStories(fetchedStories)
            }
            .addOnFailureListener { e ->
                Log.w("PageArchives", "Error fetching stories", e)
            }
    }

    private fun setupSearchView() {
        val searchView =
            binding.BarRecherche

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterStories(it) }
                return true
            }
        })
    }

    private fun filterStories(query: String) {
        adapter.filter(query)
    }

    inner class StoriesAdapter(
        private var stories: List<Story>,

        private val onItemClickListener: (Story) -> Unit
    ) :
        RecyclerView.Adapter<StoriesAdapter.StoryViewHolder>() {

        private var fullStories: List<Story> = stories



        fun updateStories(newStories: List<Story>) {
            fullStories = newStories
            stories = newStories
            notifyDataSetChanged()
        }

        fun getStoryAtPosition(position: Int): Story {
            return stories[position]
        }

        fun removeStory(position: Int) {
            stories = stories.toMutableList().also {
                it.removeAt(position)
            }
            notifyItemRemoved(position)
        }

        fun filter(query: String) {
            stories = if (query.isEmpty()) {
                fullStories
            } else {
                fullStories.filter { story ->
                    story.title.contains(query, ignoreCase = true) ||
                            story.content.contains(query, ignoreCase = true)
                }
            }
            notifyDataSetChanged()
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemStoryBinding.inflate(inflater, parent, false)
            return StoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
            val story = stories[position]
            holder.bind(story)
            holder.itemView.setOnClickListener { onItemClickListener(story)}
        }

        override fun getItemCount() = stories.size

        inner class StoryViewHolder(private val binding: ItemStoryBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(story: Story) {
                binding.storyTitle.text = story.title
                binding.storyDate.text = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date(story.savedDate))
            }
        }

        fun restoreStory(story: Story, position: Int) {
            val mutableStories = stories.toMutableList()
            mutableStories.add(position, story)
            stories = mutableStories.toList()
            notifyItemInserted(position)
        }

    }

    val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            var pendingDeleteStory: Story? = null

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val storyToDelete = adapter.getStoryAtPosition(position)
                pendingDeleteStory = storyToDelete

                // Temporarily remove the story from the RecyclerView
                adapter.removeStory(position)

                // Show Snackbar with Undo option
                Snackbar.make(binding.ArchivesRecyclerView, "Deleted \"${storyToDelete.title}\"", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        // Restore the deleted story in RecyclerView (but it's still in Firestore)
                        pendingDeleteStory?.let { adapter.restoreStory(it, position) }
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_CONSECUTIVE) {
                                // Perform the Firestore deletion here, as the user didn't press Undo
                                pendingDeleteStory?.let { deleteStoryFromDatabase(storyToDelete) }
                            }
                        }
                    })
                    .show()
            }
        }

    data class Story(
        val title: String = "", // Default empty string
        val content: String = "", // Default empty string
        val savedDate: Long = 0L, // Default 0L for long
        val documentId: String = ""
    )
    private fun deleteStoryFromDatabase(story: Story) {
        userId?.let { uid ->
            db.collection("users").document(uid)
                .collection("archives").document(profileName!!)
                .collection("stories").document(story.documentId)
                .delete()
                .addOnSuccessListener {
                    Log.d("PageArchives", "Story successfully deleted")
                }
                .addOnFailureListener { e ->
                    Log.w("PageArchives", "Error deleting story", e)
                }
        }
    }
}