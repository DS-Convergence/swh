package com.example.squirrelwarehouse

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.squirrelwarehouse.models.Favorite
import com.example.squirrelwarehouse.models.StayTime
import com.example.squirrelwarehouse.models.User
import com.example.squirrelwarehouse.models.UserModelFS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    var firestore :FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG : String = "CreateAccount"
    var UserModelFS = UserModelFS()
    private var storage : FirebaseStorage? = null
    var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    var imgFileName = "PROPIC_" + timeStamp + "_.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        //원래 email 있던 곳
        val email = findViewById<EditText>(R.id.id_sign_up)
        val password = findViewById<EditText>(R.id.pw_sign_up)
        val passwordCheck = findViewById<EditText>(R.id.pw_sign_up_check)
        val nickname = findViewById<EditText>(R.id.nick_sign_up)
        //이메일 혈식 체크
        val email_pattern = android.util.Patterns.EMAIL_ADDRESS
        sign_up_btn.setOnClickListener {
            if (email.text.toString().isEmpty() || password.text.toString().isEmpty()){
                Toast.makeText(this, "이메일, 닉네임 또는 비밀번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
            else if(password.text.toString().length < 6){
                Toast.makeText(this, "비밀번호는 6자리 이상 이어야 합니다.", Toast.LENGTH_SHORT).show()
            }
            else if(!email_pattern.matcher(email.text.toString()).matches()){
                Toast.makeText(this, "올바른 이메일 형식이 아닙니다.", Toast.LENGTH_SHORT).show()
            }
            else if(password.text.toString() != passwordCheck.text.toString()){
                Toast.makeText(this, "비밀번호를 다시한번 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
            else {
                //회원 가입 완료처리....-> 이후 이메일 인증 구현하기(주석 해제)
                //회원 가입과 동시에 모델유저에 uid, nickname, 등 마이페이지에 들어가야할 정보 넣기(하면 표시하기)
                auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            //유저를 서버에 등록 성공했음. 이제 유저의 이메일로 인증 메일을 발송함.
                            /*아래는 이메일 인증 부분 주석처리 해둔 것*/
                            /*auth.currentUser?.sendEmailVerification()
                                    ?.addOnCompleteListener {task2 ->
                                        if (task2.isSuccessful){
                                            //이메일을 보냈으니 확인하라는 토스트메세지 띄움.
                                            Toast.makeText(this, "Registered successfully. Please check your email for verification.",Toast.LENGTH_LONG).show()
                                            //Log.d("로그-success-인증 메일 전송","성공")
                                        }
                                        else{
                                            Log.d("로그-fail-인증 메일 전송","실패 . . . . "+task2.exception?.message)
                                        }
                                    }*/


                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success")
                            Log.d("alarm", FirebaseMessaging.getInstance().token.toString())

                            //유저모델(파이어스토어)업로드 작업
                            UserModelFS.uid = auth?.currentUser?.uid
                            UserModelFS.email = email.text.toString()
                            UserModelFS.nickname = nickname.text.toString()
                            firestore?.collection("Users")?.document("user_${auth?.currentUser?.uid}")?.set(UserModelFS)

                            // 예은 코드. Favorite 컬렉션 생성
                            var array : ArrayList<String> = arrayListOf<String>()
                            var fav = Favorite(array)
                            firestore?.collection("Favorite")?.document(auth.currentUser!!.uid)?.set(fav).addOnCompleteListener { task ->
                                if(task.isSuccessful) {
                                    Log.v("Favorite", "Success")

                                    // StayTime 컬렉션 생성
                                    var map = mutableMapOf<String, Int>() // 물건 본 시간 원데이터
                                    var map2 = mutableMapOf<String, Int>() // 변환한 데이터
                                    var st = StayTime(map, map2)
                                    firestore?.collection("StayTime")?.document(auth.currentUser!!.uid)?.set(st).addOnCompleteListener { task ->
                                        if(task.isSuccessful) {
                                            Log.v("StayTime", "Success")
                                        }
                                    }
                                }
                            }
                            // 예은 코드 끝



                            uploadImageToFirebaseStorage() //은배가 추가
                            val user = auth.currentUser
                            //updateUI(user)
                            // 아니면 액티비티를 닫아 버린다.
                            //finish()
                            //overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit)

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            //updateUI(null)
                            //입력필드 초기화
                            email?.setText("")
                            password?.setText("")
                            email.requestFocus()
                        }
                    }
            }
        }
        back_btn.setOnClickListener {
            finish()
        }
        //<<<<<<<<<<<<<<<<<<EB가 추가한 코드 시작//
        selectphoto_button.setOnClickListener {
            //프로필 사진 고르는 버튼 누르면
            val intent = Intent(Intent.ACTION_PICK) //photo selector intent만들기
            intent.type = "image/*"  //우리가 원하는 intent type
            startActivityForResult(intent, 0) //비트맵을 이용해서 이미지 로딩하는 function.
        }
        //EB가 추가한 코드 끝>>>>>>>>>>>>>>>>>>>>>>>//
    }

    //<<<<<<<<<<<<<<<<<<EB가 추가한 코드 시작//
    //여기 부터 메소드 세 개 더 추가함.
    var selectedPhotoUri: Uri? = null //우리가 선택한 이미지 밖으로 빼. 다른 곳에서도 쓸꺼야!



    //selectphoto_button.setOnClickListener에서 startActivityForResult로 실행되는
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //프로필 사진으로 선택한 이미지 보이게 하는 과정
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            //proceed and check what the selected image was ...
            //선택한 이미지가 보이게 하는 과정
            selectedPhotoUri = data.data //uri는 그 이미지가 저장된 location을 나타냄.
            //bitmap으로 우리가 선택한 이미지에 access하기.
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            if(bitmap != null){
                select_photo_register.setImageBitmap(bitmap)
                selectphoto_button.alpha = 0f
            }else{
                val icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo)
                select_photo_register.setImageBitmap(icon)
                selectphoto_button.alpha = 0f
            }

            //val bitmapDrawable = BitmapDrawable(bitmap)
            //selectphoto_button.setBackgroundDrawable(bitmapDrawable)

        }
    }

    private fun uploadImageToFirebaseStorage() { //이미지 파일을 Storage에 저장하고 saveUserToFirebaseDatabase호출해서 유저를 (profileImageURl, uid,username) 파이어 realtime에 저장.
        //if(selectedPhotoUri == null) return //고른 포토의 uri없으면 그냥 return.
        //val filename = UUID.randomUUID().toString() //파일 name은 random String을 만듦. 이걸로 reference 만듦

        //getInctance()를 이용해서 FirebaseStorage에 접근.
        //ref는 파이어베이스 upload area에 대한 정보.
        val ref = FirebaseStorage.getInstance().getReference("/images/$imgFileName")

        if(selectedPhotoUri == null){
            val uri = Uri.parse("android.resource://com.example.squirrelwarehouse/drawable/logo")
            // val stream: InputStream? = contentResolver.openInputStream(uri)
            ref.putFile(uri)//selected Photo된거 uri를 file형태로 ref에 넣음.
                //이미지 업로드
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        it.toString() //it은 uri ( selectedPhotoUri)
                        saveUserToFirebaseDatabase(it.toString()) //profileImageUrl넘겨주며, 파이어 베이스에 유저 등록 메소드 호출
                    }
                }
        }else{
            ref.putFile(selectedPhotoUri!!) //selected Photo된거 uri를 file형태로 ref에 넣음.
                //이미지 업로드
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        it.toString() //it은 uri ( selectedPhotoUri)
                        Log.d("RegisterActivity", "File Location: $it")

                        //데이터 베이스에 user 저장
                        saveUserToFirebaseDatabase(it.toString()) //profileImageUrl넘겨주며, 파이어 베이스에 유저 등록 메소드 호출
                    }
                }
                .addOnFailureListener{ //실패하면
                }
        }

    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        //파이어 베이스에 유저 등록 : realtime database에 users밑에 profileImageURl, uid,username저장.
        val uid = FirebaseAuth.getInstance().uid?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid") //uid로 하위 클래스 나누기
        //val token :String
        //2021_09_02 추가

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(task.isSuccessful){
                val token = task.result
                val user = User(uid, token, nick_sign_up.text.toString(), profileImageUrl)
                ref.setValue(user)
                    .addOnSuccessListener {
                        //어디로 넘어갈 지 세원이 한테 확인하고 코드 추가하기**
                        val intent = Intent(this, LogInActivity::class.java) // **
                        //back버튼 눌렀을 때 다시 register activity 안돌아가기 위해서 flag를 둠.
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent) //LatestActivity시작

                    }
                    .addOnFailureListener{//실패하면
                    }
            }
        }

        //val token = FirebaseMessaging.getInstance().token
        //val token = FirebaseInstallations.getInstance().id.result
        //val token = FirebaseMessaging.getInstance().getToken().toString()

        /*firestore.collection("Users").document("user_${uid}")
            .update("userProPic",profileImageUrl)*/

        var storageRef = storage?.reference?.child("images")?.child(imgFileName)
        var userId = FirebaseAuth.getInstance().currentUser!!.uid
        var document = "user_" + userId
        firestore.collection("Users").document(document)
            .update("userProPic", imgFileName)

        //내가 만든 모델. models아래에 User

    }
    //EB가 추가한 코드 끝>>>>>>>>>>>>>>>>>>>>>>>//


}