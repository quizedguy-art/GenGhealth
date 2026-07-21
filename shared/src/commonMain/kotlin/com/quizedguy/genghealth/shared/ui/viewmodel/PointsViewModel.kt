package com.quizedguy.genghealth.shared.ui.viewmodel

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import com.quizedguy.genghealth.shared.data.UsageStatsHelper
import com.quizedguy.genghealth.shared.util.RewardedAdManager
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

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

    private val _pointCreditEvent = MutableStateFlow<Int?>(null)
    val pointCreditEvent = _pointCreditEvent.asStateFlow()

    private val _lastCheckInDate = MutableStateFlow<String?>(null)
    val lastCheckInDate = _lastCheckInDate.asStateFlow()

    private val _checkInStreak = MutableStateFlow(0)
    val checkInStreak = _checkInStreak.asStateFlow()

    private val _isClaimingCheckIn = MutableStateFlow(false)
    val isClaimingCheckIn = _isClaimingCheckIn.asStateFlow()

    fun clearPointCreditEvent() {
        _pointCreditEvent.value = null
    }

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
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DailyUsageRecord::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.date }.take(10)
                    _dailyUsageHistory.value = list
                }
            }
    }

    fun syncUsageHistory(context: android.content.Context) {
        val userId = auth.currentUser?.uid ?: return
        
        // Get user's account creation timestamp to avoid syncing daily usage for pre-registration dates
        val creationTime = auth.currentUser?.metadata?.creationTimestamp
        val registrationTime = if (creationTime == null || creationTime == 0L) {
            System.currentTimeMillis()
        } else {
            creationTime
        }
        val registrationDate = Instant.ofEpochMilli(registrationTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Sync last 7 days
        val today = LocalDate.now()
        for (i in 0..7) {
            val date = today.minusDays(i.toLong())
            if (date.isBefore(registrationDate)) continue // Do not sync dates before user registration
            
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
                    if (snapshot.exists()) {
                        // Document exists, only update usage data
                        val isCollected = snapshot.getBoolean("isCollected") ?: false
                        
                        if (!isCollected) {
                            usageRef.update(
                                "totalMillis", usageMillis,
                                "pointsPotential", pointsPotential
                            )
                        } else {
                            // If already collected, only update millis for accuracy, keep potential as what was credited
                            usageRef.update("totalMillis", usageMillis)
                        }
                    } else {
                        // New document, initialize it
                        val record = hashMapOf(
                            "userId" to userId,
                            "date" to dateStr,
                            "totalMillis" to usageMillis,
                            "pointsPotential" to pointsPotential,
                            "isCollected" to false,
                            "isApproved" to false
                        )
                        usageRef.set(record)
                    }
                }
            }
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
                    }.sortedByDescending { it.createdAt }
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
                    val lastDate = snapshot.getString("lastCheckInDate")
                    val streak = snapshot.getLong("checkInStreak")?.toInt() ?: 0
                    
                    _lastCheckInDate.value = lastDate
                    _checkInStreak.value = streak
                    
                    // Notify user if points increased
                    if (points > _userPoints.value && _userPoints.value > 0) {
                        val difference = points - _userPoints.value
                        // We need a context for notification. This is a bit tricky in ViewModel.
                        // However, we can use a SharedFlow to signal the UI to show a snackbar or notification.
                        _pointCreditEvent.value = difference
                    }

                    _userPoints.value = points
                    calculateTotalEarned(_withdrawalHistory.value, points)
                }
            }
    }

    fun requestWithdrawal(context: android.content.Context, amount: Int, requiredPoints: Int, rewardName: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Daily limit check: Only 1 withdrawal per calendar day in user's timezone
        val todayLocalDate = LocalDate.now()
        val hasWithdrawalToday = _withdrawalHistory.value.any { request ->
            val requestDate = Instant.ofEpochMilli(request.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            requestDate == todayLocalDate
        }

        if (hasWithdrawalToday) {
            Toast.makeText(context, "Daily limit reached. You can only make 1 withdrawal per day.", Toast.LENGTH_LONG).show()
            return
        }

        if (_userPoints.value >= 5000 && _userPoints.value >= requiredPoints) {
            val withdrawal = hashMapOf(
                "userId" to userId,
                "amountRs" to amount,
                "rewardName" to rewardName,
                "pointsDeducted" to requiredPoints,
                "status" to "Pending",
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("withdrawals")
                .add(withdrawal)
                .addOnSuccessListener {
                    db.collection("users").document(userId)
                        .update("points", _userPoints.value - requiredPoints)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Withdrawal request submitted successfully!", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to submit request: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun collectPoints(record: DailyUsageRecord, context: android.content.Context, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (_collectingRecordIds.value.contains(record.id)) return
        
        // Add to collecting set to lock it
        _collectingRecordIds.value = _collectingRecordIds.value + record.id
        
        val usageRef = db.collection("daily_usage").document(record.id)
        usageRef.get().addOnSuccessListener { snapshot ->
            val isCollected = snapshot.getBoolean("isCollected") ?: false
            val isApproved = snapshot.getBoolean("isApproved") ?: false
            
            if (isCollected || !isApproved) {
                _collectingRecordIds.value = _collectingRecordIds.value - record.id
                onComplete(false)
                return@addOnSuccessListener
            }
            
            val activity = context as? Activity
            if (activity == null) {
                _collectingRecordIds.value = _collectingRecordIds.value - record.id
                onComplete(false)
                return@addOnSuccessListener
            }
            
            RewardedAdManager.showAdWithoutPoints(activity) { success ->
                if (success) {
                    val pointsToCollect = record.pointsPotential
                    val userRef = db.collection("users").document(userId)
                    
                    db.runTransaction { transaction ->
                        val userSnapshot = transaction.get(userRef)
                        val currentPoints = userSnapshot.getLong("points") ?: 0L
                        
                        transaction.update(userRef, "points", currentPoints + pointsToCollect)
                        transaction.update(usageRef, "isCollected", true)
                    }.addOnSuccessListener {
                        _collectingRecordIds.value = _collectingRecordIds.value - record.id
                        onComplete(true)
                    }.addOnFailureListener {
                        _collectingRecordIds.value = _collectingRecordIds.value - record.id
                        onComplete(false)
                    }
                } else {
                    _collectingRecordIds.value = _collectingRecordIds.value - record.id
                    onComplete(false)
                }
            }
        }.addOnFailureListener {
            _collectingRecordIds.value = _collectingRecordIds.value - record.id
            onComplete(false)
        }
    }

    fun getTodayCheckInDay(): Int {
        val todayStr = LocalDate.now().toString()
        val lastDateStr = _lastCheckInDate.value ?: return 1
        val currentStreak = _checkInStreak.value
        
        if (lastDateStr == todayStr) {
            return if (currentStreak <= 0) 1 else currentStreak
        }
        
        val yesterdayStr = LocalDate.now().minusDays(1).toString()
        return if (lastDateStr == yesterdayStr) {
            if (currentStreak >= 7) 1 else currentStreak + 1
        } else {
            1
        }
    }

    fun hasCheckedInToday(): Boolean {
        return _lastCheckInDate.value == LocalDate.now().toString()
    }

    fun getPointsForDay(day: Int): Int {
        return when (day) {
            1 -> 10
            2 -> 20
            3 -> 30
            4 -> 40
            5 -> 50
            6 -> 60
            7 -> 100
            else -> 10
        }
    }

    fun claimDailyCheckIn(activity: Activity, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (hasCheckedInToday() || _isClaimingCheckIn.value) {
            onComplete(false)
            return
        }

        _isClaimingCheckIn.value = true
        val targetDay = getTodayCheckInDay()
        val basePoints = getPointsForDay(targetDay)
        
        RewardedAdManager.showAdWithoutPoints(activity) { adSuccess ->
            if (adSuccess) {
                claimDirectlyTransaction(userId, targetDay, basePoints) { success ->
                    _isClaimingCheckIn.value = false
                    onComplete(success)
                }
            } else {
                _isClaimingCheckIn.value = false
                onComplete(false)
            }
        }
    }

    private fun claimDirectlyTransaction(
        userId: String,
        targetDay: Int,
        basePoints: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)
        val todayStr = LocalDate.now().toString()
        
        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentPoints = userSnapshot.getLong("points") ?: 0L
            
            transaction.update(userRef, mapOf(
                "points" to currentPoints + basePoints,
                "lastCheckInDate" to todayStr,
                "checkInStreak" to targetDay
            ))
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener { e ->
            Log.e("PointsViewModel", "Failed to claim check-in transaction: ${e.message}")
            onComplete(false)
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
    val rewardName: String = "",
    val pointsDeducted: Int = 0,
    val status: String = "Pending",
    val createdAt: Long = 0,
    val giftCardCode: String? = null,
    val processedAt: Long? = null
)

data class DailyUsageRecord(
    @get:Exclude
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val totalMillis: Long = 0,
    val pointsPotential: Int = 0,
    @get:PropertyName("isCollected")
    @set:PropertyName("isCollected")
    var isCollected: Boolean = false,
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false
)
