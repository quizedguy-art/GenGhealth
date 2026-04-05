package com.quizedguy.genghealth.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        checkUserRole()
    }

    private fun checkUserRole() {
        val user = auth.currentUser
        profileListener?.remove()
        
        if (user != null) {
            profileListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    
                    if (snapshot != null && snapshot.exists()) {
                        // Resilient check for isAdmin (works even if stored as String "true")
                        val adminVal = snapshot.get("isAdmin")
                        _isAdmin.value = adminVal == true || adminVal.toString().lowercase() == "true"
                        _userName.value = snapshot.getString("name")
                    } else if (snapshot != null && !snapshot.exists()) {
                        // Create profile if missing
                        val userData = hashMapOf(
                            "email" to (user.email ?: ""),
                            "points" to 0,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(userData)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    checkUserRole()
                    onSuccess()
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    fun signUp(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = hashMapOf(
                            "email" to email,
                            "name" to name,
                            "points" to 0,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(userData)
                            .addOnCompleteListener { firestoreTask ->
                                _isLoading.value = false
                                if (firestoreTask.isSuccessful) {
                                    _currentUser.value = user
                                    _userName.value = name
                                    onSuccess()
                                } else {
                                    _error.value = "Auth successful but profile setup failed"
                                }
                            }
                    }
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }

    fun signOut() {
        profileListener?.remove()
        profileListener = null
        auth.signOut()
        _currentUser.value = null
        _isAdmin.value = false
        _userName.value = null
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
