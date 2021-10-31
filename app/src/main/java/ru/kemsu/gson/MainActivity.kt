package ru.kemsu.gson

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        val samples = arrayListOf<String>()
        val recyclerView: RecyclerView = findViewById(R.id.rView)
        val url = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=ff49fcd4d4a08aa6aafb6ea3de826464&tags=cat&format=json&nojsoncallback=1\n"

        Thread {
            val connection = URL(url).openConnection() as HttpURLConnection
            val jsonDate = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val wrapPhotos: Wrapper = Gson().fromJson(jsonDate, Wrapper::class.java)
            val fPage: PhotoPage = Gson().fromJson(wrapPhotos.photos, PhotoPage::class.java)
            val photos = Gson().fromJson(fPage.photo, Array<Photo>::class.java).toList()

            for (counter in photos.indices) {
                if (counter.mod(5) == 4) {
                    Timber.d(photos[counter].toString())
                }
                samples.add("https://farm${photos[counter].farm}.staticflickr.com/${photos[counter].server}/${photos[counter].id}_${photos[counter].secret}_z.jpg\n")
            }
            connection.disconnect()

            runOnUiThread()
            {
                recyclerView.layoutManager = GridLayoutManager(this, 2)
                recyclerView.adapter = Adapter(this, samples, this)
            }
        }.start()
    }

    fun onCellClickListener(data: String) {
        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Photo link", data)
        Timber.i(data)
        clipboard.setPrimaryClip(clip)
    }

}


class Adapter(private val context: Context,
              private val arrayList: ArrayList<String>,
              private val cellClickListener: MainActivity
): RecyclerView.Adapter<Adapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.rview_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrayList[position]
        val bitmapF = BitmapFactory.decodeStream(URL(data).openStream())
        holder.image.setImageBitmap(bitmapF)

        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(data)
        }
    }
}

data class Photo(
    val id: Long,
    val owner: String = "",
    val secret: String = "",
    val server: Int = 127001,
    val farm: Int = 0,
    val title: String = "",
    val isPublic: Int = 1,
    val isFriend: Int = 0,
    val isFamily: Int = 0
)

data class PhotoPage(
    val page: Int = 1,
    val pages: Int = 1,
    val perPage: Int = 100,
    val total: Int = 100,
    val photo: JsonArray
)

data class  Wrapper(
    val photos: JsonObject,
    val stat: String = "ok"
)