package com.example.geng_health.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class PointsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var pointsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    private val _userPoints = MutableStateFlow(0)
    val userPoints = _userPoints.asStateFlow()

    private val _totalPointsEarned = MutableStateFlow(0)
    val totalPointsEarned = _totalPointsEarned.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow(listOf<WithdrawalRequest>())
    val withdrawalHistory = _withdrawalHistory.asStateFlow()

    init {
        loadUserPoints()
        loadWithdrawalHistory()
    }

    private fun loadWithdrawalHistory() {
        val userId = auth.currentUser?.uid ?: return
        historyListener?.remove()
        historyListener = db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val request = doc.toObject(WithdrawalRequest::class.java)
                            request?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _withdrawalHistory.value = list
                    calculateTotalEarned(list, _userPoints.value)
                }
            }
    }

    private fun calculateTotalEarned(history: List<WithdrawalRequest>, currentPoints: Int) {
        val historicalSpend = history.filter { it.status == "Approved" || it.status == "Pending" }.sumOf { it.pointsDeducted }
        _totalPointsEarned.value = historicalSpend + currentPoints
    }

    private fun loadUserPoints() {
        val userId = auth.currentUser?.uid ?: return
        
        pointsListener?.remove()
        pointsListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val points = snapshot.getLong("points")?.toInt() ?: 0
                    _userPoints.value = points
                    calculateTotalEarned(_withdrawalHistory.value, points)
                }
            }
    }

    fun requestWithdrawal(amountRs: Int) {
        val userId = auth.currentUser?.uid ?: return
        val requiredPoints = amountRs * 10
        
        if (_userPoints.value >= requiredPoints) {
            val withdrawal = hashMapOf(
                "userId" to userId,
                "amountRs" to amountRs,
                "pointsDeducted" to requiredPoints,
                "status" to "Pending",
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("withdrawals")
                .add(withdrawal)
                .addOnSuccessListener {
                    db.collection("users").document(userId)
                        .update("points", _userPoints.value - requiredPoints)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pointsListener?.remove()
        historyListener?.remove()
    }
}

data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val amountRs: Int = 0,
    val pointsDeducted: Int = 0,
    val status: String = "Pending",
    val createdAt: Long = 0,
    val giftCardCode: String? = null,
    val processedAt: Long? = null
)
