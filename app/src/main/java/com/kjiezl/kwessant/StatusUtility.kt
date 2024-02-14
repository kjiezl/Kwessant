package com.kjiezl.kwessant

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class StatusUtility {
    companion object {

        fun updateStatus(context: Context, status: String) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userStatusRef = FirebaseDatabase.getInstance().getReference("user/${currentUser.uid}/status")
                userStatusRef.setValue(status)
            }
        }
    }
}