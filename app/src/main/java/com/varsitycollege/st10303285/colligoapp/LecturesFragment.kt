package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LecturesFragment : Fragment() {

    private val TAG = "LecturesFragment"

    // Views
    private val dayColumns = mutableMapOf<String, LinearLayout>()

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Search functionality
    private var currentSearchQuery = ""

    // Add this variable to track if fragment is attached
    private var isFragmentAttached = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView - inflating fragment_lectures")

        // Inflate the lectures layout (5-column Mon-Fri view)
        val view = inflater.inflate(R.layout.fragment_lectures, container, false)

        // Find all the day columns
        dayColumns["Monday"] = view.findViewById(R.id.mondayColumn)
        dayColumns["Tuesday"] = view.findViewById(R.id.tuesdayColumn)
        dayColumns["Wednesday"] = view.findViewById(R.id.wednesdayColumn)
        dayColumns["Thursday"] = view.findViewById(R.id.thursdayColumn)
        dayColumns["Friday"] = view.findViewById(R.id.fridayColumn)

        Log.d(TAG, "Day columns initialized: ${dayColumns.size} columns")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentAttached = true
        // Load lectures after view is created and fragment is attached
        loadLectures()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentAttached = false
    }

    // Load all lectures from Firebase for the current user
    private fun loadLectures() {
        Log.d(TAG, "loadLectures() - Starting to load from Firestore")

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear all existing cards first
        dayColumns.values.forEach { column ->
            column.removeAllViews()
            Log.d(TAG, "Cleared column views")
        }

        // Query Firestore for this user's lectures
        db.collection("lectures")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached
                if (!isFragmentAttached || !isAdded) {
                    Log.w(TAG, "Fragment not attached, skipping lecture loading")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "✓ Firestore query successful - found ${documents.size()} lectures")

                if (documents.isEmpty) {
                    Log.d(TAG, "No lectures found - showing empty state")
                    // You could show an empty state message here
                }

                for (document in documents) {
                    // Apply search filter if active
                    val moduleName = document.getString("moduleName") ?: ""
                    val room = document.getString("room") ?: ""
                    val notes = document.getString("notes") ?: ""
                    val searchable = "$moduleName $room $notes".lowercase()

                    if (currentSearchQuery.isNotEmpty() && !searchable.contains(currentSearchQuery)) {
                        Log.d(TAG, "Skipping lecture (doesn't match search): $moduleName")
                        continue
                    }

                    // Create Lecture object
                    val lecture = Lecture(
                        id = document.id,
                        moduleName = moduleName,
                        day = document.getString("day") ?: "",
                        startTime = document.getString("startTime") ?: "",
                        endTime = document.getString("endTime") ?: "",
                        room = room,
                        color = document.getString("color") ?: "#B5EAD7",
                        notes = notes
                    )

                    Log.d(TAG, "Adding lecture: ${lecture.moduleName} on ${lecture.day}")

                    // Add the lecture card to the correct day column
                    if (lecture.day.isNotEmpty()) {
                        addLectureCard(lecture)
                    }
                }

                Log.d(TAG, "✓ All lectures loaded and displayed")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Error loading lectures from Firestore", exception)
                // Check if fragment is still attached before showing Toast
                if (isFragmentAttached && isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading lectures: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Add a single lecture card to the correct day column
    private fun addLectureCard(lecture: Lecture) {
        // Check if fragment is still attached
        if (!isFragmentAttached || !isAdded) {
            Log.w(TAG, "Fragment not attached, skipping card creation")
            return
        }

        // Find the correct column for this day
        val column = dayColumns[lecture.day]
        if (column == null) {
            Log.e(TAG, "No column found for day: ${lecture.day}")
            return
        }

        try {
            // Use requireContext() to get the layout inflater safely
            val cardView = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_lecture_card,
                column,
                false
            ) as CardView

            // Set the text fields
            cardView.findViewById<TextView>(R.id.tvTime).text =
                "${lecture.startTime} - ${lecture.endTime}"

            cardView.findViewById<TextView>(R.id.tvModuleName).text =
                lecture.moduleName

            cardView.findViewById<TextView>(R.id.tvRoom).text =
                "Room: ${lecture.room.ifEmpty { "Not set" }}"

            // Handle notes (show/hide based on if there are any)
            val notesView = cardView.findViewById<TextView>(R.id.tvNotes)
            if (lecture.notes.isEmpty()) {
                notesView.visibility = View.GONE
            } else {
                notesView.visibility = View.VISIBLE
                notesView.text = lecture.notes
            }

            // Set the card background color
            try {
                cardView.setCardBackgroundColor(Color.parseColor(lecture.color))
            } catch (e: Exception) {
                Log.e(TAG, "Invalid color: ${lecture.color}", e)
                cardView.setCardBackgroundColor(Color.parseColor("#B5EAD7"))
            }

            // Make card clickable - opens edit screen
            cardView.setOnClickListener {
                Log.d(TAG, "Lecture card clicked: ${lecture.moduleName}")
                val intent = Intent(requireContext(), AddLectureActivity::class.java)
                intent.putExtra("LECTURE_ID", lecture.id)
                startActivity(intent)
            }

            // Add the card to the column
            column.addView(cardView)
            Log.d(TAG, "Card added to ${lecture.day} column")

        } catch (e: IllegalStateException) {
            Log.e(TAG, "Fragment not attached, cannot add lecture card", e)
        }
    }

    // Show search dialog (if you want to add search functionality back)
    private fun showSearchDialog() {
        if (!isFragmentAttached || !isAdded) return

        val input = EditText(requireContext()).apply {
            hint = "Search module, room or notes..."
            setText(currentSearchQuery)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Search Lectures")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                currentSearchQuery = input.text.toString().trim().lowercase()
                Log.d(TAG, "Search query: $currentSearchQuery")
                loadLectures()
            }
            .setNegativeButton("Clear") { _, _ ->
                currentSearchQuery = ""
                Log.d(TAG, "Search cleared")
                loadLectures()
            }
            .show()
    }
}

// Lecture data class remains the same
data class Lecture(
    val id: String = "",
    val moduleName: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val color: String = "#B5EAD7", // Default pastel green
    val notes: String = ""
)