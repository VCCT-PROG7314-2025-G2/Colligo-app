package com.varsitycollege.st10303285.colligoapp.network

import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response


 /*This class basically “catches” every network request before it is sent out, and it adds the Firebase user’s ID token into the Authorization header.
 * The backend requires a valid ID token for protected endpoints, so without this header, the API will return 401 (Unauthorized).
 */
class AuthTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        // Get the request that is about to be sent
        val originalRequest = chain.request()

        // FirebaseAuth can tell us if someone is logged in
        val currentUser = Firebase.auth.currentUser

        // Make a copy of the request so we can add headers to it
        val requestBuilder = originalRequest.newBuilder()

        // Only add the token if the user is actually logged in
        if (currentUser != null) {

            // runBlocking is used so we can call suspend functions here
            val idToken = runBlocking {
                try {
                    // Ask Firebase for the user's latest ID token
                    currentUser.getIdToken(false).await().token
                } catch (e: Exception) {
                    null
                }
            }

            // If we successfully got the token, attach it to the header
            if (!idToken.isNullOrEmpty()) {
                // The backend expects: Authorization: Bearer <token>
                requestBuilder.header("Authorization", "Bearer $idToken")
            }
        }

        // Build the new request (with token added)
        val finalRequest = requestBuilder.build()


        return chain.proceed(finalRequest)
    }
}
