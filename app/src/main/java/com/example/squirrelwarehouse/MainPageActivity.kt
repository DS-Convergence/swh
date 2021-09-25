package com.example.squirrelwarehouse

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.squirrelwarehouse.models.Favorite
import com.example.squirrelwarehouse.models.Product
import com.example.squirrelwarehouse.models.StayTime
import com.example.squirrelwarehouse.models.UserModelFS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.main_page.*
import kotlinx.android.synthetic.main.product_detail.*


class MainPageActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var firestore = FirebaseFirestore.getInstance()
    private var storage = FirebaseStorage.getInstance()
    private lateinit var auth: FirebaseAuth

    private var bnnPosition = 0

    private var isFabOpen = false

    val handler = Handler(Looper.getMainLooper()) {
        setpager()
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)


        // 버튼 기능 구현
        var gotoMore = gotomore
        gotoMore.setOnClickListener {
            val intent = Intent(this, MainMore::class.java)
            startActivityForResult(intent, 0)
        }

        var chatBtn = fabChat
        gotochat.visibility = View.INVISIBLE
        chatBtn.setOnClickListener {
            val intent = Intent(this, LatestMessageActivity::class.java)
            startActivityForResult(intent, 0)
        }

        var gotoMap = gotomap
        gotoMap.setOnClickListener {
            val intent = Intent(this, ItemLoc::class.java)
            startActivityForResult(intent, 0)
        }

        var floatingBtn = fabMain
        floatingBtn.setOnClickListener {
            toggleFab()
        }

        var writeBtn = fabWrite
        writeBtn.setOnClickListener {
            val intent = Intent(this, ProductFormActivity::class.java)
            startActivityForResult(intent, 0)
        }

        var mypageBtn = fabMyPage
        mypageBtn.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivityForResult(intent, 0)
        }

        var searchBar = searchBar
        searchBar.queryHint = "검색어를 입력하세요"
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchItem(query!!)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })


        // 파워람쥐 뷰페이저 구현
        pwrgTV.setOnClickListener {
            val intent = Intent(this, UserInfoActivity::class.java)  // 물건주인의 마이페이지로 이동
            intent.putExtra("UserId","5Bf4S5mm7hRhvu3LbdPUbCI8hMh1")
            startActivity(intent)
        }

        val vpAdapter = MainViewpagerAdapter()
        VPpoweruser.adapter = vpAdapter

        val thread=Thread(PagerRunnable())
        // TODO: 파워람쥐 이미지 로드 오류 수정 必 - context null
        thread.start()


        // 메인페이지 물품 보기
        // 최근 업데이트
        // 최근 업데이트 - 더보기
        var moreUpdate = moreUpdate
        moreUpdate.setOnClickListener {
            val intent = Intent(this, UpdateMoreActivity::class.java)
            //intent.putExtra("updateList",updateList)
            startActivityForResult(intent, 0)
        }


        // 카테고리 : 지정 카테고리 항목 보여주기
        // 카테고리 - 더보기
        var moreCate = moreCate
        moreCate.setOnClickListener {
            val intent = Intent(this, CateMoreActivity::class.java)
            intent.putExtra("cateName","디지털/가전")
            startActivityForResult(intent, 0)
        }


        // 추천
        // 추천 - 더보기
        var moreRcmd = moreRcmd
        var ubr = UserBasedRcmd("user_" + FirebaseAuth.getInstance().currentUser!!.uid.toString())  // 현재 유저 아이디 필요
        /*
        rcmd1.visibility = View.INVISIBLE
        rcmd2.visibility = View.INVISIBLE
        rcmd3.visibility = View.INVISIBLE   // 오류 수정하고 지우기
         */
        moreRcmd.setOnClickListener {
            // Log.v("RcmdList", UserBasedRcmd("user_ifbnimzN2RM61ZfbfeJ48ZBdu9j2").getRcmd().toString())
            // var ubr = UserBasedRcmd("user_l0kyyYR3SNfT1zJsdrAvHYy6M3J2")  // 현재 유저 아이디 필요 -> 주석으로 둘 것

            //TODO:오류 수정 필요
            ubr.getRcmd()
        }


        // 성공,,,,,,
        // 최신 업데이트
        var products = ArrayList<Product>()
        firestore?.collection("Product")?.orderBy("uploadTime", Query.Direction.DESCENDING)?.limit(3)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    //products.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    // 데이터 받아오기
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(Product::class.java)
                        if (item != null) {
                            products.add(item)
                            Log.v("PRODUCTS",products.size.toString())
                            Log.v("PRODUCTS",item.productName.toString())
                        }

                    }

                    // 일단 다 invisible
                    up_title1.visibility = View.INVISIBLE
                    up_title2.visibility = View.INVISIBLE
                    up_title3.visibility = View.INVISIBLE

                    // 데이터가 3개보다 적을 수 있기 때문에 if문을 이렇게 작성함
                    // 이것보다 더 좋은 방법이 있다면 그거 사용해도 무방.
                    if(products.size >= 1) {
                        up_title1.visibility = View.VISIBLE

                        up_title1.text = products.get(0).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(products.get(0).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(up_thbm1)
                            Log.v("IMAGE","Success")
                        }
                    }
                    if(products.size >= 2) {
                        up_title2.visibility = View.VISIBLE

                        up_title2.text = products.get(1).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(products.get(1).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(up_thbm2)
                            Log.v("IMAGE","Success")
                        }
                    }
                    if(products.size >= 3) {
                        up_title3.visibility = View.VISIBLE

                        up_title3.text = products.get(2).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(products.get(2).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(up_thbm3)
                            Log.v("IMAGE","Success")
                        }
                    }
                }


        update1.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data",products!![0].userId+"_"+products!![0].uploadTime)
            startActivityForResult(intent, 0)
        }

        update2.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data",products!![1].userId+"_"+products!![1].uploadTime)
            startActivityForResult(intent, 0)
        }

        update3.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data",products!![2].userId+"_"+products!![2].uploadTime)
            startActivityForResult(intent, 0)
        }




        // 특정 카테고리 물품만
        var cateProds = ArrayList<Product>()
        var category = "디지털/가전"
        cateTextView.text = category
        firestore?.collection("Product")?.whereEqualTo("category", category)?.orderBy("uploadTime", Query.Direction.DESCENDING)?.limit(3)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    //cateProds.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    // 데이터 받아오기
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(Product::class.java)
                        if (item != null) {
                            cateProds.add(item)
                            Log.v("PRODUCTS", cateProds.size.toString())
                            Log.v("PRODUCTS", item.productName.toString())
                        }
                    }

                    ct_title1.visibility = View.INVISIBLE
                    ct_title2.visibility = View.INVISIBLE
                    ct_title3.visibility = View.INVISIBLE

                    if(cateProds.size >= 1) {
                        ct_title1.visibility = View.VISIBLE

                        ct_title1.text = cateProds.get(0).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(cateProds.get(0).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(ct_thbm1)
                            Log.v("IMAGE","Success")
                        }
                    }
                    if(cateProds.size >= 2) {
                        ct_title2.visibility = View.VISIBLE

                        ct_title2.text = cateProds.get(1).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(cateProds.get(1).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(ct_thbm2)
                            Log.v("IMAGE","Success")
                        }
                    }
                    if(cateProds.size >= 3) {
                        ct_title3.visibility = View.VISIBLE

                        ct_title3.text = cateProds.get(2).productName.toString()

                        var storageRef = storage?.reference?.child("product")?.child(cateProds.get(2).imageURI.toString())
                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                            Glide.with(applicationContext)
                                    .load(uri)
                                    .into(ct_thbm3)
                            Log.v("IMAGE","Success")
                        }
                    }



                }

        // 여기서 오류남. ArrayList가 또 0이라고함. 위에꺼는 안그런데,,
        // https://stackoverflow.com/questions/50123649/does-tasks-whenallsuccess-guarantee-the-order-in-which-i-pass-tasks-to-it 이거 해보기
        // 아무래도 생명주기와 관련이 있지 않을가,,,,, 아닌듯,,,
        cate1.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data",cateProds!![0].userId+"_"+cateProds!![0].uploadTime)
            startActivityForResult(intent, 0)
        }
        cate2.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data",cateProds!![1].userId+"_"+cateProds!![1].uploadTime)
            startActivityForResult(intent, 0)
        }
        cate3.setOnClickListener {
            var intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("data", cateProds!![2].userId + "_" + cateProds!![2].uploadTime)
            startActivityForResult(intent, 0)
        }

    }

    private fun searchItem(query: String) {
        var intent = Intent(this, SearchResult::class.java)
        startActivityForResult(intent, 0)
    }

    private fun toggleFab() {
        if(isFabOpen) {
            // FAB 액션 열기
            ObjectAnimator.ofFloat(fabMain, View.ROTATION, -135f, 0f).apply { start() }
            ObjectAnimator.ofFloat(fabWrite, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(fabMyPage, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(fabChat, "translationY", 0f).apply { start() }
        } else {
            // FAB 액션 닫기
            ObjectAnimator.ofFloat(fabMain, View.ROTATION, 0f, -135f).apply { start() }
            ObjectAnimator.ofFloat(fabWrite, "translationY", -170f).apply { start() }
            ObjectAnimator.ofFloat(fabMyPage, "translationY", -340f).apply { start() }
            ObjectAnimator.ofFloat(fabChat, "translationY", -510f).apply { start() }
        }

        isFabOpen = !isFabOpen
    }

    // 배너 페이지 변경
    fun setpager() {
        if(bnnPosition==3) bnnPosition=0
        VPpoweruser.setCurrentItem(bnnPosition,true)
        bnnPosition+=1
    }

    // n초마다 배너 넘기기
    inner class PagerRunnable : Runnable {
        override fun run() {
            while(true){
                Thread.sleep(3000)
                handler.sendEmptyMessage(0)
            }
        }
    }

    // TODO:오류 수정 필요
    inner class UserBasedRcmd {
        lateinit var user : String
        // var userIndex = 0
        // var data = arrayOf(datas)
        var sim = ArrayList<ArrayList<Double>>()    // 유사도
        var users = ArrayList<String>()
        var product = ArrayList<String>()
        var fav = ArrayList<ArrayList<String>>()
        var rcmdList = ArrayList<String>()   // 물건 정보 들어갈 배열

        var dataArr = ArrayList<ArrayList<Int>>()   // 전체 유저의 선호도 데이터

        var st = ArrayList<Map<String, Int>>() // staytime 넣을


        constructor(userId : String) {

            this.user = userId
            //var data : ArrayList<ArrayList<Int>> = getData()    // product와 favorite 비교해서 선호데이터 받기

            // user
            firestore?.collection("Users")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //userArr.clear()
                if (querySnapshot == null) return@addSnapshotListener

                // 데이터 받아오기
                for (snapshot in querySnapshot!!.documents) {
                    var item = snapshot.toObject(UserModelFS::class.java)
                    users.add(item!!.uid.toString())
                    Log.v("RcmdList", "user: " + item!!.uid.toString())
                }


                // product
                firestore?.collection("Product")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    //productArr.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    // 데이터 받아오기
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(Product::class.java)
                        product.add(item!!.userId.toString() + "_" + item!!.uploadTime.toString())
                        //Log.v("RcmdList", item!!.productName.toString())
                    }

                    // stayTime
                    firestore?.collection("StayTime")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        if (querySnapshot == null) return@addSnapshotListener

                        // 데이터 받아오기
                        for (snapshot in querySnapshot!!.documents) {
                            var item = snapshot.toObject(StayTime::class.java)
                            //fav.add(item!!.products as java.util.ArrayList<String>)
                            // 새로 가입한경우 null일 수 있음. 그 경우 고려해야함.
                            st.add(item!!.transform as Map<String, Int>)
                            Log.v("stayTimeSize", item!!.transform!!.size.toString())

                            // ArrayList<Map<String,Int>> 형식으로 받고
                            // 리스트에서 맵 하나씩 꺼내서 product에 있는 물건이랑 하나하나 비교.
                            // containKey(product)사용해서 있으면 getValue사용해서 값 넣기
                            //item!!.transform!!.containsKey("물건id")
                        }


                        for(map in st) {
                            var starr = ArrayList<Int>()
                            for(i in 0..product.size-1) {
                                if(map.containsKey(product.get(i))) {
                                    starr.add(map.getValue(product.get(i)))
                                }
                                else {
                                    starr.add(0)
                                }
                                Log.v("RcmdListStarr", "starr: " + product.get(i) + " "+ starr.get(i))
                            }
                            dataArr.add(starr)
                            Log.v("RcmdListStarr", "starr: 끝")
                        }


                        // favorite
                        firestore?.collection("Favorite")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                            //favArr.clear()
                            if (querySnapshot == null) return@addSnapshotListener

                            // 데이터 받아오기
                            for (snapshot in querySnapshot!!.documents) {
                                var item = snapshot.toObject(Favorite::class.java)
                                // 새로 가입한경우 null일 수 있음. 그 경우 고려해야함.
                                fav.add(item!!.products as java.util.ArrayList<String>)
                            }

                            //Log.v("RcmdList", "유저개수: " + users.size)
                            //Log.v("RcmdList", "물건개수: " + product.size)
                            //Log.v("RcmdList", "좋아요개수: " + fav.size)


                            // 물건 있는지 없는지 0 1 행렬
                            for(i in 0..fav.size-1) {
                                var arr = dataArr.get(i)   // 한 사람의 선호도 데이터
                                for(j in 0..product.size-1) {
                                    // Log.v("RcmdList", "물건개수: " + j)
                                    if(fav.get(i).contains(product.get(j)))
                                        arr[j] = 10

                                    Log.v("RcmdListFav", " fav 포함: " +product.get(j) + " " + arr.get(j))
                                }
                                dataArr.set(i, arr)

                                //Log.v("RcmdList", "dataArr개수: " + dataArr.size)
                                //Log.v("RcmdList", "dataArr개수: " + dataArr.get(i).size)
                            }



                            // 유사도 행렬
                            for (i in 0..users.size-1) {
                                var simArr = ArrayList<Double>()
                                for(j in 0..users.size-1) {
                                    if(i==j)  // 나 자신과의 유사도 계산이면 1이나오기 때문에 0으로 넣어줌.
                                        simArr.add(0.0)
                                    else {
                                        var result = cosineSimilarity(dataArr.get(i),dataArr.get(j))
                                        //var result = pearsonSimilarity(dataArr.get(i),dataArr.get(j))
                                        if(!result.isNaN())  // 한명의 유저가 아무 물건도 보지 않았을 경우, NaN이 나옴.
                                            simArr.add(result)
                                        else
                                            simArr.add(0.0)

                                    }


                                    Log.v("RcmdList", "sim : " + simArr.get(j).toString())
                                }
                                sim.add(simArr)

                                Log.v("RcmdList", "sim개수: " + sim.size)
                            }



                            // 현재 유저와 다른 유저와의 유사도만 뽑아내기
                            var index1 = 0  // 현재 유저의 순서?인덱스? 뽑아내기
                            while (index1 < users.size) {
                                if (users.get(index1).equals(user.substring(5))) //userIndex = i;
                                    break
                                index1++
                            }
                            Log.v("RcmdList", "user: " + users.get(index1))


                            // 현재 유저와의 유사도 확인
                            for(i in 0..users.size-1) {
                                Log.v("sim", sim.get(index1).get(i).toString())
                            }

/*
                            // 유사도 제일 높은 사람의 정보 출력하기
                            var max = sim.get(index1).get(0)
                            var index2 = 0
                            for (j in 0..users.size-1) {
                                if (max < sim.get(index1).get(j)) {
                                    max = sim.get(index1).get(j)
                                    index2 = j
                                }
                            }
                            Log.v("RcmdList", "user: " + users.get(index2))

 */

                            // 유사도 0.35이상인 유저
                            // 내가 보지 않았지만, 상대는 관심있는 물건 출력
                            for(i in 0..users.size-1) {
                                if(sim.get(index1).get(i) >= 0.35) {
                                    // 0.35가 넘는 유저 한명씩 비교하면서 결과list에 추가
                                    for (j in 0..dataArr.get(index1).size-1) {
                                        // 물건에 대해 현재 유저가 좋아요를 누르지 않았으며, 상대유저의 선호도가 어느정도 높은 경우
                                        if (dataArr.get(index1).get(j) != 10 && dataArr.get(i).get(j) > 5) {
                                            if(!product.get(j).contains(user.substring(5))) {
                                                // 현재 유저의 물건일 경우 제외
                                                if(!rcmdList.contains(product.get(j)))
                                                    // 물건이 이미 들어있을 경우 제외
                                                    rcmdList.add(product.get(j))
                                                    Log.v("RcmdList", "추천물품 : "+product.get(j))
                                            }
                                        }
                                    }
                                }
                            }

/*
                            // 내가 보지 않았지만, 상대는 관심있는 물건, 인덱스 출력
                            for (j in 0..dataArr.get(index1).size-1) {
                                if (dataArr.get(index1).get(j) != 10 && dataArr.get(index2).get(j) >= 2) {
                                    if(!product.get(j).contains(user.substring(5))) {
                                        rcmdList.add(product.get(j))
                                        Log.v("RcmdList", "추천물품 : "+product.get(j))

                                        /*
                                        firestore?.collection("Product")?.document(product.get(j))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                            task ->
                                            if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                                var product = task.result.toObject(Product::class.java)
                                                rc_title1.text = product?.productName
                                                // 사진 불러오기
                                                var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                                storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                                    Glide.with(applicationContext)
                                                            .load(uri)
                                                            .into(rc_thbm1)
                                                    Log.v("IMAGE", "Success")
                                                }?.addOnFailureListener { //이미지 로드 실패시
                                                    Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                                    Log.v("IMAGE", "failed")
                                                }
                                            }
                                        }*/
                                    }
                                }
                            }
*/

                            // 일단 다 invisible
                            rc_title1.visibility = View.INVISIBLE
                            rc_title2.visibility = View.INVISIBLE
                            rc_title3.visibility = View.INVISIBLE

                            Log.v("RcmdList", "추천물품 개수 : "+rcmdList.size)

                            // 데이터가 3개보다 적을 수 있기 때문에 if문을 이렇게 작성함
                            // 이것보다 더 좋은 방법이 있다면 그거 사용해도 무방.
                            if(rcmdList.size >= 1) {
                                firestore?.collection("Product")?.document(rcmdList.get(0))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title1.text = product?.productName
                                        rc_title1.visibility = View.VISIBLE
                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm1)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                            }
                            if(rcmdList.size >= 2) {
                                firestore?.collection("Product")?.document(rcmdList.get(1))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title2.text = product?.productName
                                        rc_title2.visibility = View.VISIBLE
                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm2)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                            }
                            if(rcmdList.size >= 3) {
                                firestore?.collection("Product")?.document(rcmdList.get(2))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title3.text = product?.productName
                                        rc_title3.visibility = View.VISIBLE
                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm3)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                                Log.v("RcmdList", "배열 크기: "+rcmdList.size)
                            }

                            // 일단 다 invisible
                            rc_title1.visibility = View.INVISIBLE
                            rc_title2.visibility = View.INVISIBLE
                            rc_title3.visibility = View.INVISIBLE

                            Log.v("RcmdList", "추천물품 개수 : "+rcmdList.size)

                            // 데이터가 3개보다 적을 수 있기 때문에 if문을 이렇게 작성함
                            // 이것보다 더 좋은 방법이 있다면 그거 사용해도 무방.
                            if(rcmdList.size >= 1) {
                                firestore?.collection("Product")?.document(rcmdList.get(0))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title1.text = product?.productName
                                        rc_title1.visibility = View.VISIBLE

                                        rcmd1.setOnClickListener {
                                            var intent = Intent(this@MainPageActivity, ProductDetailActivity::class.java)
                                            intent.putExtra("data",product?.userId+"_"+product?.uploadTime)
                                            startActivityForResult(intent, 0)
                                        }

                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm1)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                            }
                            if(rcmdList.size >= 2) {
                                firestore?.collection("Product")?.document(rcmdList.get(1))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title2.text = product?.productName
                                        rc_title2.visibility = View.VISIBLE

                                        rcmd2.setOnClickListener {
                                            var intent = Intent(this@MainPageActivity, ProductDetailActivity::class.java)
                                            intent.putExtra("data",product?.userId+"_"+product?.uploadTime)
                                            startActivityForResult(intent, 0)
                                        }

                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm2)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                            }
                            if(rcmdList.size >= 3) {
                                firestore?.collection("Product")?.document(rcmdList.get(2))?.get()?.addOnCompleteListener { // 넘겨온 물건 id를 넣어주면 됨.
                                    task ->
                                    if (task.isSuccessful) { // 데이터 가져오기를 성공하면
                                        var product = task.result.toObject(Product::class.java)
                                        rc_title3.text = product?.productName
                                        rc_title3.visibility = View.VISIBLE

                                        rcmd3.setOnClickListener {
                                            var intent = Intent(this@MainPageActivity, ProductDetailActivity::class.java)
                                            intent.putExtra("data",product?.userId+"_"+product?.uploadTime)
                                            startActivityForResult(intent, 0)
                                        }

                                        // 사진 불러오기
                                        var storageRef = storage?.reference?.child("product")?.child(product?.imageURI.toString())
                                        storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                                            Glide.with(applicationContext)
                                                    .load(uri)
                                                    .into(rc_thbm3)
                                            Log.v("IMAGE", "Success")
                                        }?.addOnFailureListener { //이미지 로드 실패시
                                            Toast.makeText(applicationContext, "실패", Toast.LENGTH_SHORT).show()
                                            Log.v("IMAGE", "failed")
                                        }
                                    }
                                }
                            }
                        }



                    }

                }
            }

/*
            // 유사도 행렬
            for (i in 0..users.size-1) {
                var simArr = ArrayList<Double>()
                for(j in 0..users.size-1) {
                    if(i==j)
                        simArr.add(0.0)
                    else
                        simArr.add(cosineSimilarity(data.get(i),data.get(j)))

                    Log.v("RcmdList", simArr.get(j).toString())
                }
                sim.add(simArr)
            }

            // 현재 유저와 다른 유저와의 유사도만 뽑아내기
            var index1 = 0
            while (index1 < users.size) {
                if (users.get(index1) == user) //userIndex = i;
                    break
                index1++
            }

            // 유사도 제일 높은 사람의 정보 출력하기
            var max = sim.get(index1).get(0)
            var index2 = 0
            for (j in 0..users.size-1) {
                if (max < sim.get(index1).get(j)) {
                    max = sim.get(index1).get(j)
                    index2 = j
                }
            }

            // 내가 보지 않았지만, 상대는 관심있는 물건, 인덱스 출력
            for (j in 0..data.get(index1).size-1) {
                if (data.get(index1).get(j) == 0 && data.get(index2).get(j) != 0) {
                    rcmdList.add(product.get(j))
                    Log.v("RcmdList", "추천물품 : "+product.get(j))
                }
            }*/
        }

        fun getRcmd() {
            val intent = Intent(this@MainPageActivity, RcmdMoreActivity::class.java)
            intent.putExtra("rcmdList",rcmdList)
            startActivityForResult(intent, 0)
        }

        private fun getData() : ArrayList<ArrayList<Int>> {
            // 파이어베이스에서 데이터 불러와야 함
            // 지금은 일단 더미 데이터로 해보기
            // favorite 데이터랑 product 데이터 가져와야 함
            // data, users
            // sim = Array(data.size) { DoubleArray(data.size) }

            Log.v("RcmdList", "getData()호출")

            var productArr = ArrayList<String>()
            firestore?.collection("Product")?.orderBy("uploadTime", Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //productArr.clear()
                if (querySnapshot == null) return@addSnapshotListener

                // 데이터 받아오기
                for (snapshot in querySnapshot!!.documents) {
                    var item = snapshot.toObject(Product::class.java)
                    productArr.add(item!!.userId.toString() + "_" + item!!.uploadTime.toString())
                    Log.v("RcmdList", item!!.productName.toString())
                }
            }
            product = productArr

            //var userArr = ArrayList<String>()
            firestore?.collection("Users")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //userArr.clear()
                if (querySnapshot == null) return@addSnapshotListener

                // 데이터 받아오기
                for (snapshot in querySnapshot!!.documents) {
                    var item = snapshot.toObject(UserModelFS::class.java)
                    users.add(item!!.uid.toString())
                    Log.v("RcmdList", "user: " + item!!.uid.toString())
                }

                Log.v("RcmdList", "유저개수: " + users.size)
            }
            //users = userArr
            Log.v("RcmdList", "유저개2수: " + users.size)

            var favArr = ArrayList<ArrayList<String>>()
            firestore?.collection("Favorite")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //favArr.clear()
                if (querySnapshot == null) return@addSnapshotListener

                // 데이터 받아오기
                for (snapshot in querySnapshot!!.documents) {
                    var item = snapshot.toObject(Favorite::class.java)
                    favArr.add(item!!.products as java.util.ArrayList<String>)
                }
            }

            var data1 = ArrayList<ArrayList<Int>>()   // 전체 유저의 선호도 데이터
            var arr = ArrayList<Int>()   // 한 사람의 선호도 데이터
            for(i in 0..favArr.size-1) {
                //arr.clear()
                for(j in 0..productArr.size-1) {
                    if(favArr.get(i).contains(productArr.get(j)))
                        arr.add(1)
                    else
                        arr.add(0)
                }
                data1.add(arr)
            }

            return data1
        }

        private fun cosineSimilarity(user1: ArrayList<Int>, user2: ArrayList<Int>): Double {
            // 코사인 유사도 계산
            var sum = 0.0
            var sum1 = 0.0
            var sum2 = 0.0
            for (i in 0..user1.size-1) {
                sum += (user1[i] * user2[i]).toDouble()
                sum1 += Math.pow(user1[i].toDouble(), 2.0)
                sum2 += Math.pow(user2[i].toDouble(), 2.0)
            }
            return sum / (Math.sqrt(sum1) * Math.sqrt(sum2))
        }

        private fun pearsonSimilarity(user1: ArrayList<Int>, user2: ArrayList<Int>): Double {
            // 피어슨 유사도 계산
            var avg1 = 0.0
            var avg2 = 0.0

            // 평균 구하기

            for (i in 0 until user1.size) {
                avg1 += user1.get(i)
                avg2 += user2.get(i)
            }

            avg1 = avg1 / user1.size
            avg2 = avg2 / user2.size


            // xy 분산
            var sum1 = 0.0
            var sum2 = 0.0
            var sum12 = 0.0

            for (i in 0 until user1.size) {
                sum1 += Math.pow(user1.get(i) - avg1, 2.0).toInt()
                sum2 += Math.pow(user2.get(i) - avg2, 2.0).toInt()
                sum12 += (user1.get(i) - avg1) * (user2.get(i) - avg2)
            }

            return sum12 / (Math.sqrt(sum1.toDouble()) * Math.sqrt(sum2.toDouble()))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 변경된 데이터를 불러오기 위해 자신의 액티비티를 다시 호출
        var intent = Intent(this, MainPageActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

}