package com.example.squirrelwarehouse

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.squirrelwarehouse.models.ChatMessage
import com.example.squirrelwarehouse.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {
    companion object {
        var currentUser: User? = null
        val TAG = "chatLog"
    }
    val adapter = GroupAdapter<ViewHolder>()//새로운 어뎁터
    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerView_chat_log.adapter = adapter //새로운 object를 add할 수 있게 해주고 그럴 때마다 새롭게 refresh해줌.

        //상단 바이름 설정
        //NewMessageActivity에서 받아온 키 값으로 username 받아오기
        //username 뿐만아니라 전체 user받아올 수 있음
        toUser = intent.getParcelableExtra<User>(LatestMessageActivity.USER_KEY)
        chat_log_textview_username.text = toUser?.username //optional이니까 ?써줘야함.

        listenForMessages()

        //보내기 버튼 누리면 보내지게
        send_button_chat_log.setOnClickListener {
            Log.d(TAG, " Attempt to send message.....")
            performSendMessage() // 새로운 메소드. 어떻게 firebase의 메세지를 보낼지
        }
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        //쓴 메세지를 들을 수 있게
        val ref = FirebaseDatabase.getInstance().getReference("/user-message/$fromId/$toId")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    //채팅 메세지가 null이 아니라면
                    Log.d(TAG, chatMessage.text)//로그 창에 보내줘
                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = currentUser ?: return
                        //지금 로그인한 user의 아이디 : FirebaseAuth.getInstance().uid
                        adapter.add(ChatFromItem(chatMessage.text, currentUser))
                    } else {
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }
                }
                recyclerView_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }


    private fun performSendMessage() {
        //how do we actually send a messaage to firebase
        val text = editText_chat_log.text.toString() //우리가 쓴 메세지를 text로 얻어와

        val fromId = FirebaseAuth.getInstance().uid //나는 보내는 사람이니까 from
        val user = intent.getParcelableExtra<User>(LatestMessageActivity.USER_KEY)
        val toId = user!!.uid

        //firebase에 user-message만듦.
        val reference = FirebaseDatabase.getInstance().getReference("/user-message/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-message/$toId/$fromId").push()
        if (fromId == null) return //보내는 ID없으면 그냥 return

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000) //class변수 만들기

        reference.setValue(chatMessage)
                .addOnSuccessListener {
                    Log.d(TAG, "Saved our chat message: ${reference.key}")
                    //보내면 사라지게
                    editText_chat_log.text.clear()
                    //보내면 가장 최근 보낸 메세지 쪽으로 스크롤 위치
                    recyclerView_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
        toReference.setValue(chatMessage) //이메일로 로그인 했을 때도 여전히 뜰수 있게

        //새로보낸메세지를 위해서
        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }


    class ChatFromItem(val text: String, val user: User) : Item<ViewHolder>() {
        override fun bind(viewHolder: ViewHolder, position: Int) {
            //text받아와서 뛰우기
            //access to view holder
            viewHolder.itemView.textView_from_row.text = text

            //이미지를 로드하기. load our user image into the User image icon
            val uri = user.profileImageUrl
            val targetImageView = viewHolder.itemView.imageview_chat_from_row
            Picasso.get().load(uri).into(targetImageView) //imageview_chat_from_row  내쪽 프로필 설정
        }
        override fun getLayout(): Int {
            return R.layout.chat_from_row
        }
    }

    class ChatToItem(val text: String, val user: User) : Item<ViewHolder>() {
        //text받아와서 뛰우기
        //access to view holder
        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.textView_to_row.text = text

            //이미지를 로드하기. load our user image into the User image icon
            val uri = user.profileImageUrl
            val targetImageView = viewHolder.itemView.imageview_chat_to_row
            Picasso.get().load(uri).into(targetImageView) //imageview_chat_to_row  tkdeoqkd 프로필 설정
        }

        override fun getLayout(): Int {
            return R.layout.chat_to_row
        }
    }
}