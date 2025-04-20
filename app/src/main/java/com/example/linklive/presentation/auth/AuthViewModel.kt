package com.example.linklive.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.linklive.utils.UIState
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val usersCollection = firestore.collection("users")

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> get() = _currentUser

    private val _authState = MutableStateFlow<UIState>(UIState.Idle)
    val authState: StateFlow<UIState> get() = _authState

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
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        val userData = hashMapOf(
                            "name" to firebaseUser.displayName,
                            "email" to firebaseUser.email
                        )

                        usersCollection.document(firebaseUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                _currentUser.value = firebaseUser
                                _authState.value = UIState.Success("Google sign-in successful")
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
