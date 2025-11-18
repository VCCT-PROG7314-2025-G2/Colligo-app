package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.varsitycollege.st10303285.colligoapp.adapter.RideListAdapter
import com.varsitycollege.st10303285.colligoapp.repository.ApiRepository
import kotlinx.coroutines.launch

class MyRidesActivity : AppCompatActivity() {

    private val repo = ApiRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offered_rides)

        val rv = findViewById<RecyclerView?>(R.id.rvRides) ?: run {
            Toast.makeText(this, "Add a RecyclerView with id rvRides to activity_offered_rides.xml", Toast.LENGTH_LONG).show()
            return
        }

        rv.layoutManager = LinearLayoutManager(this)
        loadMyRides(rv)
    }

    private fun loadMyRides(rv: RecyclerView) {
        lifecycleScope.launch {
            val resp = repo.getMyRides()
            if (resp.isSuccessful) {
                val list = resp.body() ?: emptyList()
                rv.adapter = RideListAdapter(list) { ride ->
                    val rideId = ride["id"]?.toString() ?: return@RideListAdapter
                    val i = Intent(this@MyRidesActivity, DriverRequestsActivity::class.java)
                    i.putExtra("rideId", rideId)
                    startActivity(i)
                }
            } else {
                Log.e("MyRides", "getMyRides failed code=${resp.code()} body=${resp.errorBody()?.string()}")
                Toast.makeText(this@MyRidesActivity, "Failed to load your rides", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
