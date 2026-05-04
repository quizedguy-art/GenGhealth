package com.quizedguy.genghealth.ui.viewmodel

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

    private val _withdrawalHistory = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawalHistory = _withdrawalHistory.asStateFlow()

    private val _usageRecords = MutableStateFlow<List<DailyUsageRecord>>(emptyList())
    val usageRecords = _usageRecords.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        db.collection("withdrawals")
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
                    _pendingWithdrawals.value = list.filter { it.status == "Pending" }
                    _withdrawalHistory.value = list.filter { it.status != "Pending" }
                }
            }

        db.collection("daily_usage")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(DailyUsageRecord::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _usageRecords.value = list
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

    fun creditUsagePoints(record: DailyUsageRecord, points: Int) {
        // Update user points
        db.collection("users").document(record.userId).get().addOnSuccessListener { userSnap ->
            val currentPoints = userSnap.getLong("points") ?: 0L
            db.collection("users").document(record.userId)
                .update("points", currentPoints + points)
            
            // Mark usage as collected
            db.collection("daily_usage").document(record.id)
                .update(mapOf(
                    "isCollected" to true,
                    "pointsPotential" to points
                ))
        }
    }
}
