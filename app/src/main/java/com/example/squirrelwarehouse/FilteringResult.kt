package com.example.squirrelwarehouse

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.squirrelwarehouse.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.listview.view.*
import kotlinx.android.synthetic.main.listview_form.*
import kotlinx.android.synthetic.main.listview_form.back_btn
import kotlinx.android.synthetic.main.product_detail.*
import java.text.SimpleDateFormat

class FilteringResult : AppCompatActivity() {
    private var firestore : FirebaseFirestore? = null
    private var storage : FirebaseStorage? = null

    private var valarr : Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.listview_form)

        back_btn.setOnClickListener {
            finish()
        }

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        /*
        if(intent.hasExtra("category")) {
            page_title.text = intent.getStringExtra("category")
            listView.adapter = ResultViewRecyclerViewAdapter()
            listView.layoutManager = LinearLayoutManager(this)
        }
        else {
            Toast.makeText(this,"카테고리 미선택",Toast.LENGTH_LONG).show()
        }
         */

        valarr = intent.getSerializableExtra("valList") as Array<String>
        for(i in 0..2) {
            Log.v("valList", i.toString()+" - "+ valarr!![i])
        }

        if(valarr!!.size!=0) {
            page_title.text = "검색결과"
            // Log.v("filtering result", "arr size: "+valarr.size) 로그 출력이 안 됨
        }
        else
            page_title.text = "검색조건 미선택"


        listView.adapter = ResultViewRecyclerViewAdapter()
        listView.layoutManager = LinearLayoutManager(this)

    }

    inner class ResultViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var products: ArrayList<Product> = arrayListOf()

        init {
            firestore?.collection("Product")?.orderBy("uploadTime", Query.Direction.DESCENDING)
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        products.clear()
                        if (querySnapshot == null) return@addSnapshotListener

                        var cat1 = valarr!!.get(0)
                        var cat2 = valarr!!.get(1)
                        var loc = valarr!!.get(2)
                        var strloc : List<String>? = null

                        // 데이터 받아오기
                        for (snapshot in querySnapshot!!.documents) {
                            var item = snapshot.toObject(Product::class.java)

                            // 위도, 경도 데이터 > 한글 주소
                            var geocoder = Geocoder(this@FilteringResult)
                            var list : List<Address>? = null
                            if(item?.region != null)
                                list = geocoder.getFromLocation(item!!.region!!.latitude, item!!.region!!.longitude, 10)

                            if(list != null) {
                                if(list.size!=0) {
                                    strloc = list.get(0).toString().split(" ")
                                    Log.v("loc", strloc[1] + " " + strloc[2])
                                }
                            }

                            Log.v("loc", loc)

                            // 필터링
                            if(cat1.equals("") || cat1.equals(item!!.category)) {  // 일반카테고리
                                if(cat2.equals("") || cat2.equals(item!!.categoryHobby)) {  // 취미카테고리
                                    if(loc.equals("") || ((strloc!!.get(1).equals(loc) || strloc!!.get(2).equals(loc)) && item!!.region !=null)) {  // 위치
                                        if(item!!.status.equals("대여 전")) {  // 위치 null값과 대여 전인 물건
                                            products.add(item!!)
                                            // Log.v("products", "Success, size: " + products.size)
                                        }
                                    }
                                }
                            }
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view =
                    LayoutInflater.from(parent.context).inflate(R.layout.listview, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        }

        override fun getItemCount(): Int {
            return products.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView

            viewHolder.titleTV.text = products!![position].productName
            viewHolder.timeTV.text = products!![position].uploadTime
            viewHolder.detailTV.text = products!![position].productDetail

            // 시간 데이터 포맷
            var sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
            var date = sdf.parse(products[position]?.uploadTime)
            sdf = SimpleDateFormat("yyyy.MM.dd HH:mm")
            var dateStr = sdf.format(date)
            viewHolder.timeTV.text = dateStr

            // 사진 불러오기
            var storageRef = storage?.reference?.child("product")?.child(products!![position].imageURI.toString())
            storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(applicationContext)
                        .load(uri)
                        .into(viewHolder.thumb)
                //Log.v("IMAGE","Success")

            }

            viewHolder.setOnClickListener {
                Intent(this@FilteringResult, ProductDetailActivity::class.java).apply {
                    putExtra("data", products!![position].userId + "_" + products!![position].uploadTime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.run { startActivity(this) }

            }

            viewHolder.setOnClickListener {

                // 이거를 쓰려면 product이름을 바꿔야함. userid+uploadTime 이런식으로로
                // 지금은 오류남. 암튼 이 코드로 했을 때 다음 페이지로 넘어가는 건 확실함. 해봄.
                Intent(this@FilteringResult, ProductDetailActivity::class.java).apply {
                    //putExtra("data", "아무래도 물건 넣을때 이름을 줘야지 되겠어~~~~~")
                    putExtra("data", products!![position].userId+"_"+products!![position].uploadTime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.run { startActivity(this)}

            }

        }
    }

    /*
    inner class ResultViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var products:ArrayList<Product> = arrayListOf()

        init {
            firestore?.collection("Product")?.whereEqualTo("category", intent.getStringExtra("category"))?.orderBy("uploadTime", Query.Direction.DESCENDING)
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        products.clear()
                        if (querySnapshot == null) return@addSnapshotListener

                        // 데이터 받아오기
                        for (snapshot in querySnapshot!!.documents) {
                            var item = snapshot.toObject(Product::class.java)
                            products.add(item!!)
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view =
                    LayoutInflater.from(parent.context).inflate(R.layout.listview, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        }

        override fun getItemCount(): Int {
            return products.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView

            viewHolder.titleTV.text = products!![position].productName
            viewHolder.timeTV.text = products!![position].uploadTime
            viewHolder.detailTV.text = products!![position].productDetail


            // 사진 불러오기
            /*var storageRef = storage?.reference?.child("product")?.child(products!![position].imageURI.toString())
            storageRef?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(applicationContext)
                        .load(uri)
                        .into(viewHolder.thumb)
            }*/

            viewHolder.setOnClickListener {
                Intent(this@FilteringResult, ProductDetailActivity::class.java).apply {
                    //putExtra("data", "아무래도 물건 넣을때 이름을 줘야지 되겠어~~~~~")
                    putExtra("data", products!![position].userId+"_"+products!![position].uploadTime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.run { startActivity(this)}
            }

        }
    }

     */
}