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

    private val _userReferralCode = MutableStateFlow<String?>(null)
    val userReferralCode = _userReferralCode.asStateFlow()

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
                        
                        val refCode = snapshot.getString("referralCode")
                        if (refCode == null) {
                            // Assign a referral code to legacy users
                            val newCode = generateReferralCode()
                            db.collection("users").document(user.uid).update("referralCode", newCode)
                            _userReferralCode.value = newCode
                        } else {
                            _userReferralCode.value = refCode
                        }
                    } else if (snapshot != null && !snapshot.exists()) {
                        // Create profile if missing
                        val newCode = generateReferralCode()
                        val userData = hashMapOf(
                            "email" to (user.email ?: ""),
                            "points" to 0,
                            "referralCode" to newCode,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(userData)
                        _userReferralCode.value = newCode
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

    fun signUp(email: String, pass: String, name: String, referralCode: String? = null, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Profile setup first
                        val newCode = generateReferralCode()
                        val userData = hashMapOf<String, Any>(
                            "email" to email,
                            "name" to name,
                            "points" to 0, // Points will be added below if referral is valid
                            "referralCode" to newCode,
                            "createdAt" to System.currentTimeMillis()
                        )
                        
                        db.collection("users").document(user.uid).set(userData)
                            .addOnSuccessListener {
                                _currentUser.value = user
                                _userName.value = name
                                _userReferralCode.value = newCode
                                
                                // Now process referral if provided
                                if (!referralCode.isNullOrEmpty()) {
                                    applyReferral(user.uid, name, email, referralCode, onSuccess)
                                } else {
                                    _isLoading.value = false
                                    onSuccess()
                                }
                            }
                            .addOnFailureListener {
                                _isLoading.value = false
                                _error.value = "Auth successful but profile setup failed"
                            }
                    }
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }

    private fun applyReferral(newUserId: String, newUserName: String, newUserEmail: String, referralCode: String, onSuccess: () -> Unit) {
        db.collection("users")
            .whereEqualTo("referralCode", referralCode.uppercase().trim())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Invalid code, but user is already created, so we just finish
                    _isLoading.value = false
                    onSuccess()
                } else {
                    val referrerDoc = documents.documents[0]
                    val referrerId = referrerDoc.id
                    
                    val batch = db.batch()
                    
                    // 1. Reward new user (500 pts)
                    val newUserRef = db.collection("users").document(newUserId)
                    batch.update(newUserRef, 
                        "points", com.google.firebase.firestore.FieldValue.increment(500), 
                        "referredBy", referrerId
                    )
                    
                    // 2. Reward referrer (500 pts)
                    val referrerRef = db.collection("users").document(referrerId)
                    batch.update(referrerRef, "points", com.google.firebase.firestore.FieldValue.increment(500))
                    
                    // 3. Log referral
                    val referralRecord = hashMapOf(
                        "referrerId" to referrerId,
                        "referredId" to newUserId,
                        "referredName" to newUserName,
                        "referredEmail" to newUserEmail,
                        "pointsAwarded" to 500,
                        "timestamp" to System.currentTimeMillis()
                    )
                    val referralRef = db.collection("referrals").document()
                    batch.set(referralRef, referralRecord)
                    
                    batch.commit().addOnCompleteListener {
                        _isLoading.value = false
                        onSuccess()
                    }
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                onSuccess() // Fail gracefully, user account is already OK
            }
    }

    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Omitted confusing chars like 0, O, I, 1, L
        return (1..6)
            .map { chars.random() }
            .joinToString("")
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
