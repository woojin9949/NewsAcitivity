package fastcampus.projects.newsacitivity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import fastcampus.projects.Adapter.NewsAdapter
import fastcampus.projects.model.NewsRss
import fastcampus.projects.model.transform
import fastcampus.projects.newsacitivity.databinding.ActivityMainBinding
import fastcampus.projects.service.NewsService
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter
    private var retrofit = Retrofit.Builder()
        .baseUrl("https://news.google.com/")
        .addConverterFactory(
            TikXmlConverterFactory.create(
                //모든 정보를 다 안가져와도 Exception이 안뜨게 설정!!
                TikXml.Builder()
                    .exceptionOnUnreadXml(false)
                    .build()
            )
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        newsAdapter = NewsAdapter()

        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
        }

        val newsService = retrofit.create(NewsService::class.java)
        newsService.mainFeed().enqueue(object : Callback<NewsRss> {
            override fun onResponse(call: Call<NewsRss>, response: Response<NewsRss>) {

                //jsoup을 통해 해당 링크에 헤더에 있는 (meta태그)의 property가  og:title과 og:image 정보를 가져올 수 있다
                if (response.isSuccessful) {
                    //response로 받는 객체들(NewsItem)을 NewsModel객체로 transform (NewsModel에 썼음)
                    val list = response.body()?.channel?.items.orEmpty().transform()
                    newsAdapter.submitList(list)
                    //링크 잘 나옴
                    list.forEach {
                        Log.d("Testt", "" + it.link)
                        Thread {
                            val jsoup = Jsoup.connect(it.link).get()
                            val elements = jsoup.select("meta[property^=og:]")
                            val ogImageNode = elements.find { node ->
                                node.attr("property") == "og:image"
                            }
                            it.imageUrl = ogImageNode?.attr("content")
                        }.start()
                    }

                }
            }

            override fun onFailure(call: Call<NewsRss>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}