package com.varsitycollege.st10303285.colligoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var plannerCard: LinearLayout
    private lateinit var lostFoundCard: LinearLayout
    private lateinit var carpoolCard: LinearLayout
    private lateinit var mapCard: LinearLayout

    private lateinit var lectureRecycler: RecyclerView
    private val lecturesAdapter = LectureAdapter()

    // Firestore
    private val firestore = FirebaseFirestore.getInstance()

    // nav views (nullable until found)
    private var navBar: LinearLayout? = null
    private var iconHome: ImageView? = null
    private var iconLocation: ImageView? = null
    private var iconCarpool: ImageView? = null
    private var iconCalendar: ImageView? = null
    private var iconLostFound: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        val nav = findViewById<LinearLayout>(R.id.bottomNav)
        nav?.bringToFront()
        nav?.invalidate()


        // Enable offline persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings

        // view binding for cards
        plannerCard = findViewById(R.id.plannerCard)
        lostFoundCard = findViewById(R.id.lostFoundCard)
        carpoolCard = findViewById(R.id.carpoolCard)
        mapCard = findViewById(R.id.mapCard)
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Recycler
        lectureRecycler = findViewById(R.id.lectureRecycler)
        lectureRecycler.layoutManager = LinearLayoutManager(this)
        lectureRecycler.adapter = lecturesAdapter

        // click listeners - adjust Target activities
        plannerCard.setOnClickListener {
            startActivity(Intent(this, PlannerActivity::class.java))
        }

        lostFoundCard.setOnClickListener {
            startActivity(Intent(this, LostFoundActivity::class.java))
        }

        carpoolCard.setOnClickListener {
            startActivity(Intent(this, RidesDashboardActivity::class.java))
        }

        mapCard.setOnClickListener {
            startActivity(Intent(this, CampusMapActivity::class.java))
        }



        loadSchedule()
        setupRealtimeScheduleListener()
        setupNavBarAndInsets()
        setupNavClicks()
        ensureUserDocExists()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.e("FCM-DEBUG", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            android.util.Log.d("FCM-DEBUG", "FORCED token: $token")

            // Register with backend immediately (uses ApiRepository)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = com.varsitycollege.st10303285.colligoapp.repository.ApiRepository()
                    val resp = repo.registerFcmToken(token)
                    android.util.Log.d("FCM-DEBUG", "registerFcmToken response: ${resp?.code() ?: resp}")
                } catch (e: Exception) {
                    android.util.Log.e("FCM-DEBUG", "registerFcmToken failed", e)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }


    }


    fun ensureUserDocExists() {
        val user = Firebase.auth.currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)

        // read once; if missing create minimal doc
        ref.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                // build a minimal profile from available info
                val doc = hashMapOf<String, Any?>(
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "email" to (user.email ?: ""),
                    "fullName" to (user.displayName ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: "")
                )
                // set with merge in case other fields exist
                ref.set(doc, SetOptions.merge())
                    .addOnSuccessListener { android.util.Log.d("USER-INIT", "User doc created for $uid") }
                    .addOnFailureListener { e -> android.util.Log.e("USER-INIT", "Failed to create user doc", e) }
            } else {
                android.util.Log.d("USER-INIT", "User doc exists for $uid")
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("USER-INIT", "Error reading user doc", e)
        }
    }
    fun goToHome(view: View) {

        val scroll = findViewById<View?>(R.id.scrollContent)
        scroll?.let {
            it.scrollTo(0, 0)
        }
    }

    private fun loadSchedule() {
        // load schedule from Firestore
        firestore.collection("schedule")
            .limit(10)
            .get()
            .addOnSuccessListener { snap ->
                val items = snap.documents.mapNotNull { doc ->
                    LectureItem(
                        title = doc.getString("title") ?: "Lecture",
                        time = doc.getString("time") ?: "09:00"
                    )
                }
                lecturesAdapter.setItems(items)
            }
            .addOnFailureListener {
                // fallback: show sample entries
                lecturesAdapter.setItems(
                    listOf(
                        LectureItem("Programming 3D", "08:00"),
                        LectureItem("Database Systems", "10:00")
                    )
                )
            }
    }

    private fun setupRealtimeScheduleListener() {
        // demonstrates real-time updates
        firestore.collection("schedule")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("HomeActivity", "schedule listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        LectureItem(doc.getString("title") ?: "Lecture", doc.getString("time") ?: "")
                    }
                    lecturesAdapter.setItems(items)
                }
            }
    }

    private fun setupNavBarAndInsets() {
        // get nav bar and icons
        navBar = findViewById(R.id.bottomNav)
        if (navBar == null) {
            Log.w("HomeActivity", "bottomNav view not found. Ensure include id '@+id/bottomNav' exists.")
            return
        }


        ViewCompat.setOnApplyWindowInsetsListener(navBar!!) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)

            insets
        }

        // find icons inside the nav bar
        iconHome = navBar!!.findViewById(R.id.iconHome)
        iconLocation = navBar!!.findViewById(R.id.iconLocation)
        iconCarpool = navBar!!.findViewById(R.id.iconCarpool)
        iconCalendar = navBar!!.findViewById(R.id.iconCalendar)
        iconLostFound = navBar!!.findViewById(R.id.iconLostFound)
    }

    private fun setupNavClicks() {
        // navBar might be null (guard)
        if (navBar == null) return


        iconHome?.setOnClickListener {
            // optional: scroll to top
            val scroll = findViewById<View?>(R.id.scrollContent)
            scroll?.scrollTo(0, 0)
            // small feedback so user knows we're already on home
            Toast.makeText(this, getString(R.string.home_icon_desc), Toast.LENGTH_SHORT).show()
        }

        iconLocation?.setOnClickListener {
            // launch CampusMapActivity (or Location feature)
            startActivity(Intent(this, CampusMapActivity::class.java))
        }


        iconCarpool?.setOnClickListener {
            // launch CarpoolActivity
            startActivity(Intent(this, RidesDashboardActivity::class.java))
        }

        iconCalendar?.setOnClickListener {
            // launch PlannerActivity
            startActivity(Intent(this, PlannerActivity::class.java))
        }

        iconLostFound?.setOnClickListener {
            // launch LostFoundActivity
            startActivity(Intent(this, LostFoundActivity::class.java))
        }
    }
}

//Lecture data + Adapter

data class LectureItem(val title: String, val time: String)

class LectureAdapter : RecyclerView.Adapter<LectureAdapter.LecVH>() {
    private val items = mutableListOf<LectureItem>()
    fun setItems(new: List<LectureItem>) {
        items.clear()
        items.addAll(new)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LecVH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return LecVH(v)
    }

    override fun onBindViewHolder(holder: LecVH, position: Int) {
        val it = items[position]
        holder.title.text = it.title
        holder.sub.text = it.time
    }

    override fun getItemCount(): Int = items.size

    class LecVH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(android.R.id.text1)
        val sub: android.widget.TextView = view.findViewById(android.R.id.text2)
    }
}