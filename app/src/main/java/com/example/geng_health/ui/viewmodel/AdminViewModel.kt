package com.example.geng_health.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pendingWithdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val pendingWithdrawals = _pendingWithdrawals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadPendingWithdrawals()
    }

    private fun loadPendingWithdrawals() {
        _isLoading.value = true
        db.collection("withdrawals")
            .whereEqualTo("status", "Pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(WithdrawalRequest::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _pendingWithdrawals.value = list
                }
            }
    }

    fun approveRequest(id: String, code: String) {
        db.collection("withdrawals").document(id)
            .update(mapOf(
                "status" to "Approved",
                "giftCardCode" to code,
                "processedAt" to System.currentTimeMillis()
            ))
    }

    fun rejectRequest(id: String) {
        db.collection("withdrawals").document(id)
            .update(mapOf(
                "status" to "Rejected",
                "processedAt" to System.currentTimeMillis()
            ))
    }
}
