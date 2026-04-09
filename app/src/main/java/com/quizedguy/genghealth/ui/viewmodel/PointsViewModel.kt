package com.quizedguy.genghealth.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.LocalDate
import com.quizedguy.genghealth.data.UsageStatsHelper
import com.google.firebase.firestore.SetOptions

class PointsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var pointsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var usageListener: ListenerRegistration? = null

    private val _userPoints = MutableStateFlow(0)
    val userPoints = _userPoints.asStateFlow()

    private val _totalPointsEarned = MutableStateFlow(0)
    val totalPointsEarned = _totalPointsEarned.asStateFlow()

    private val _withdrawalHistory = MutableStateFlow(listOf<WithdrawalRequest>())
    val withdrawalHistory = _withdrawalHistory.asStateFlow()

    private val _dailyUsageHistory = MutableStateFlow(listOf<DailyUsageRecord>())
    val dailyUsageHistory = _dailyUsageHistory.asStateFlow()

    private val _collectingRecordIds = MutableStateFlow<Set<String>>(emptySet())
    val collectingRecordIds = _collectingRecordIds.asStateFlow()

    init {
        loadUserPoints()
        loadWithdrawalHistory()
        loadDailyUsageHistory()
    }

    private fun loadDailyUsageHistory() {
        val userId = auth.currentUser?.uid ?: return
        usageListener?.remove()
        usageListener = db.collection("daily_usage")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DailyUsageRecord::class.java)?.copy(id = doc.id)
                    }
                    _dailyUsageHistory.value = list
                }
            }
    }

    fun syncUsageHistory(context: android.content.Context) {
        val userId = auth.currentUser?.uid ?: return
        
        // Sync last 7 days
        val today = LocalDate.now()
        for (i in 0..7) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            val docId = "${userId}_$dateStr"
            
            // For days other than today (i > 0), only sync if not already in history
            if (i > 0 && _dailyUsageHistory.value.any { it.date == dateStr }) continue
            
            // For today (i == 0), don't update if already collected
            if (i == 0 && _dailyUsageHistory.value.any { it.date == dateStr && it.isCollected }) continue

            val usageMillis = UsageStatsHelper.getUsageForDate(context, date)
            val pointsPotential = calculatePoints(usageMillis)

            if (pointsPotential > 0 || i == 0) { // Always sync today, or any day with points
                val usageRef = db.collection("daily_usage").document(docId)
                
                usageRef.get().addOnSuccessListener { snapshot ->
                    val isAlreadyCollected = snapshot.getBoolean("isCollected") ?: false
                    
                    if (isAlreadyCollected) {
                        // Keep isCollected as true, only update usage data
                        usageRef.update(
                            "totalMillis", usageMillis,
                            "pointsPotential", pointsPotential
                        )
                    } else {
                        // Full merge, setting isCollected to false
                        val record = hashMapOf(
                            "userId" to userId,
                            "date" to dateStr,
                            "totalMillis" to usageMillis,
                            "pointsPotential" to pointsPotential,
                            "isCollected" to false
                        )
                        usageRef.set(record, SetOptions.merge())
                    }
                }
            }
        }
    }

    fun collectPoints(record: DailyUsageRecord) {
        val userId = auth.currentUser?.uid ?: return
        if (record.isCollected || record.pointsPotential <= 0) return
        
        // Prevent duplicate processing
        if (_collectingRecordIds.value.contains(record.id)) return
        _collectingRecordIds.value += record.id

        val batch = db.batch()
        
        // 1. Mark as collected
        val usageRef = db.collection("daily_usage").document(record.id)
        batch.update(usageRef, "isCollected", true)
        
        // 2. Increment user points
        val userRef = db.collection("users").document(userId)
        batch.update(userRef, "points", com.google.firebase.firestore.FieldValue.increment(record.pointsPotential.toLong()))
        
        batch.commit().addOnCompleteListener {
            _collectingRecordIds.value -= record.id
        }
    }

    private fun calculatePoints(millis: Long): Int {
        val hours = millis / 3600000.0
        return when {
            hours < 5 -> 200
            hours < 6 -> 100
            hours < 7 -> 50
            else -> 0
        }
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
        
        if (_userPoints.value >= 5000 && _userPoints.value >= requiredPoints) {
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

data class DailyUsageRecord(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val totalMillis: Long = 0,
    val pointsPotential: Int = 0,
    @get:JvmName("isCollected")
    val isCollected: Boolean = false
)
