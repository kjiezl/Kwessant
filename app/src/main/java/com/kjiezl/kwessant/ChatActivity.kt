package com.kjiezl.kwessant

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase


class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference

    private lateinit var userStatusRef: DatabaseReference
    private lateinit var statusTextView: TextView

    var receiverRoom: String? = null
    var senderRoom: String? =  null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)


        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        val backArrow: ImageView = findViewById(R.id.backArrow)

        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        val toolbarTitle: TextView = findViewById(R.id.toolbarTitle)
        toolbarTitle.text = name

        backArrow.setOnClickListener {
            super.onBackPressed()
            finish()
        }

        userStatusRef = FirebaseDatabase.getInstance().getReference("user").child(receiverUid!!)

        statusTextView = findViewById(R.id.chat_status)

        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sendButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList, chatRecyclerView)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = false
        chatRecyclerView.layoutManager = layoutManager
        chatRecyclerView.adapter = messageAdapter

        userStatusRef.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                updateStatusUI(status)
            }

            override fun onCancelled(error: DatabaseError) {
                //
            }
        })

        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for(postSnapshot in snapshot.children){
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()

                    scrollToBottom()
                }

                override fun onCancelled(error: DatabaseError) {
                    //
                }
            })

        sendButton.setOnClickListener{
            val message = messageBox.text.toString()
            val messageObject = Message(message, senderUid, "sent")

            val senderMessageRef = mDbRef.child("chats").child(senderRoom!!).child("messages").push()
            val receiverMessageRef = mDbRef.child("chats").child(receiverRoom!!).child("messages").push()

            if(isOnline()){
                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateLatestMessage(senderUid, receiverUid, message)
                            updateLatestMessage(receiverUid, senderUid, message)
                            mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                                .setValue(messageObject)
                        } else {
                            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                        }
                    }

                messageBox.setText("")
            } else{
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatusUI(status: String?) {
        if (status == "online") {
            statusTextView.text = "Online"
            statusTextView.setTextColor(resources.getColor(R.color.green))
        } else {
            statusTextView.text = "Offline"
            statusTextView.setTextColor(resources.getColor(R.color.sentColor))
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun updateLatestMessage(uid: String?, chatPartnerUid: String?, latestMessage: String) {
        if (uid != null && chatPartnerUid != null) {
            val currentUserLatestMessageRef = mDbRef.child("user").child(uid).child("latestMessage")
            val chatPartnerLatestMessageRef = mDbRef.child("user").child(chatPartnerUid).child("latestMessage")

            currentUserLatestMessageRef.setValue(latestMessage)
            chatPartnerLatestMessageRef.setValue(latestMessage)
        }
    }

    private fun scrollToBottom() {
        chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }

    override fun onResume() {
        super.onResume()
        StatusUtility.updateStatus(this, "online")
    }

    override fun onPause() {
        super.onPause()
        StatusUtility.updateStatus(this, "offline")
    }
}