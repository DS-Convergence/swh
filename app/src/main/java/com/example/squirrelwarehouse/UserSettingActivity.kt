package com.example.squirrelwarehouse

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_user_info.*
import kotlinx.android.synthetic.main.activity_user_setting.*
import kotlinx.android.synthetic.main.activity_user_setting.back_btn

class UserSettingActivity : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    private var firestore : FirebaseFirestore? = null
    var storage : FirebaseStorage?=null
    var uid : String? = null
    var nickname : String? = null
    //var introduce : String? = null
    var email : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_setting)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid
        var user = auth.currentUser

        firestore?.collection("Users")?.document("user_${uid}")?.get()?.addOnSuccessListener { doc ->
            nickname = doc?.data?.get("nickname").toString()
            email = doc?.data?.get("email").toString()
            var introduce = doc?.data?.get("introduce").toString()
            nick_edit_tv.setText(nickname)
            email_edit_tv.text = email
            if(introduce != "null"){
                introduce_edit_tv.setText(introduce)
            }
            else{
                introduce_edit_tv.setText("소개를 작성해주세요")
            }
            var storageRef = storage?.reference?.child("images")?.child(doc?.data?.get("userProPic").toString())
            storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(applicationContext)
                        .load(uri)
                        .into(propic_img)
                Log.v("IMAGE","Success")
            }?.addOnFailureListener { //이미지 로드 실패시
                Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                Log.v("IMAGE","failed")

            }

        }
        back_btn.setOnClickListener {
            var intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
            finish()
        }
        edit_setting_btn.setOnClickListener {
            //닉네임 무조건 업데이트
            firestore?.collection("Users")?.document("user_${uid}")
                ?.update("nickname",nick_edit_tv.text.toString())
                ?.addOnSuccessListener {
                    Log.d("로그-1-success-record받기-","nickname ${nickname}")
                }
            //비밀번호 변경 코드
            if(new_pw_tv.text != null && new_pw_check_tv.text != null){//비밀번호 칸이 입력되어있다면
                if(new_pw_tv.text.toString()==new_pw_check_tv.text.toString()){//비밀번호 확인이 되면
                    if(new_pw_tv.text.toString().length in 1..5){
                        Toast.makeText(this, "비밀번호는 6자리 이상이어야 합니다.",Toast.LENGTH_SHORT).show()
                    }
                    else if(new_pw_tv.text.toString() != ""){
                        user.updatePassword(new_pw_tv.text.toString())//비밀번호 업데이트 코드
                        finish()
                    }
                }
                else{
                    new_pw_tv.setText("")
                    new_pw_check_tv.setText("")
                    Toast.makeText(this, "입력한 새 비밀번호를 확인해주세요.",Toast.LENGTH_SHORT).show()
                }
            }

            //한줄 소개 무조건 업데이트
            firestore?.collection("Users")?.document("user_${uid}")
                    ?.update("introduce",introduce_edit_tv.text.toString())
                    ?.addOnSuccessListener {
                        //Log.d("로그-1-success-record받기-","introduce ${introduce}")
                    }

            var intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}