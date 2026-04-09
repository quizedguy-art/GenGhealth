package com.quizedguy.genghealth.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReferralViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _referrals = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val referrals = _referrals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadReferrals()
    }

    fun loadReferrals() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        db.collection("referrals")
            .whereEqualTo("referrerId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ReferralRecord::class.java)
                    }
                    _referrals.value = list
                }
            }
    }
}

data class ReferralRecord(
    val referredName: String = "",
    val referredEmail: String = "",
    val pointsAwarded: Int = 0,
    val timestamp: Long = 0
)
