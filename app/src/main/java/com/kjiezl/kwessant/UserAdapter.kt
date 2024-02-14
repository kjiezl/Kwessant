package com.kjiezl.kwessant

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserAdapter(val context: Context, val userList: ArrayList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    private val userStatusRef = FirebaseDatabase.getInstance().getReference("user")

    private val mDbRef = FirebaseDatabase.getInstance().getReference()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.textName.text = currentUser.name
        updateUI(holder, currentUser.status)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val chatRoom = if (currentUserId!! < currentUser.uid!!)
            currentUserId + currentUser.uid
        else
            currentUser.uid + currentUserId

        FirebaseDatabase.getInstance().getReference("chats").child(chatRoom).child("messages")
            .orderByKey().limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                // Inside onDataChange for retrieving last message
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Messages exist, retrieve the last message
                        for (messageSnapshot in snapshot.children) {
                            val lastMessage = messageSnapshot.child("message").getValue(String::class.java)
                            holder.textLatestMessage.text = lastMessage ?: "No messages"
                            // Update the User object with the last message
                            currentUser.latestMessage = lastMessage ?: "No messages"
                        }
                    } else {
                        // No messages found
                        holder.textLatestMessage.text = "No messages"
                        // Update the User object with a default value
                        currentUser.latestMessage = "No messages"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        userStatusRef.child(currentUser.uid!!).child("status").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                updateUI(holder, status)
            }

            override fun onCancelled(error: DatabaseError) {
                //
            }
        })

        holder.itemView.setOnClickListener{
            val intent = Intent(context, ChatActivity::class.java)

            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)

            context.startActivity(intent)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val textName = itemView.findViewById<TextView>(R.id.txt_name)
        val textStatus = itemView.findViewById<TextView>(R.id.txt_status)
        val textLatestMessage = itemView.findViewById<TextView>(R.id.txt_latest_message)
    }

    private fun updateUI(holder: UserViewHolder, status: String?) {
        if (status == "online") {
            holder.textStatus.text = "Online"
            holder.textStatus.setTextColor(context.resources.getColor(R.color.green))
        } else {
            holder.textStatus.text = "Offline"
            holder.textStatus.setTextColor(context.resources.getColor(R.color.sentColor))
        }
    }
}