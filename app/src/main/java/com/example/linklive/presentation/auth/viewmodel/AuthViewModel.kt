package com.example.linklive.presentation.auth.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.linklive.R
import com.example.linklive.data.model.User
import com.example.linklive.data.preferences.saveUserLocally
import com.example.linklive.utils.UIState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel (application: Application) : AndroidViewModel(application) {

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> get() = _currentUser

    private val _authState = MutableStateFlow<UIState>(UIState.Idle)
    val authState: StateFlow<UIState> get() = _authState

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    fun provideGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.resources.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    init {
        _currentUser.value = auth.currentUser
    }

    fun register(name: String, email: String, password: String) {
        _authState.value = UIState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email
                    )

                    user?.let { firebaseUser ->
                        usersCollection.document(firebaseUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                _currentUser.value = firebaseUser
                                _authState.value = UIState.Success("Registration successful")
                            }
                            .addOnFailureListener { exception ->
                                _authState.value = UIState.Error(exception)
                            }
                    }
                } else {
                    _authState.value = UIState.Error(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun loginEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    _authState.value = UIState.Success("Login successful")
                } else {
                    _authState.value = UIState.Error(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val curUser = auth.currentUser
                    curUser?.let { firebaseUser ->
                        val userData = hashMapOf(
                            "name" to firebaseUser.displayName,
                            "email" to firebaseUser.email,
                            "profilePhotoUrl" to firebaseUser.photoUrl.toString()
                        )
                        Log.d("auth", "signInWithGoogle: signing in")
                        val user = User(
                            curUser.displayName.toString(),
                            curUser.photoUrl.toString(),
                            curUser.email.toString()
                        )
                        Log.d("auth", "signInWithGoogle: ${curUser.photoUrl.toString()}")

                        // Save user data to Firestore
                        usersCollection.document(firebaseUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                _currentUser.value = firebaseUser
                                _authState.value = UIState.Success("Google sign-in successful")

                                // Save user locally
                                val context = getApplication<Application>().applicationContext
                                val peerId = firebaseUser.uid
                                CoroutineScope(Dispatchers.IO).launch {
                                    saveUserLocally(
                                        context,
                                        user.name,
                                        user.profilePhotoUrl.toString(),
                                        peerId,
                                        user.email
                                    )
                                }
                            }
                            .addOnFailureListener { exception ->
                                _authState.value = UIState.Error(exception)
                            }
                    }
                } else {
                    _authState.value = UIState.Error(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}