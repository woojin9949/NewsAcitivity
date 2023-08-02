package fastcampus.projects.newsacitivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
    private var retrofit =
        Retrofit.Builder().baseUrl("https://news.google.com/").addConverterFactory(
            TikXmlConverterFactory.create(
                //모든 정보를 다 안가져와도 Exception이 안뜨게 설정!!
                TikXml.Builder().exceptionOnUnreadXml(false).build()
            )
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        newsAdapter = NewsAdapter { url ->
            startActivity(Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", url)
            })
        }

        val newsService = retrofit.create(NewsService::class.java)

        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
        }

        binding.feedChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.feedChip.isChecked = true
            newsService.mainFeed().submitList()
        }

        binding.politicsChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.politicsChip.isChecked = true

            newsService.politicsNews().submitList()
        }
        binding.economyChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.economyChip.isChecked = true

            newsService.economyNews().submitList()
        }
        binding.societyChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.societyChip.isChecked = true

            newsService.societyNews().submitList()
        }

        binding.sportsChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.sportsChip.isChecked = true

            newsService.sportsNews().submitList()
        }
        binding.scienceChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.scienceChip.isChecked = true

            newsService.itNews().submitList()
        }
        binding.healthChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.healthChip.isChecked = true

            newsService.healthNews().submitList()
        }
        binding.searchTextInputEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.chipGroup.clearCheck()

                binding.searchTextInputEditText.clearFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                newsService.search(binding.searchTextInputEditText.text.toString()).submitList()

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false

        }

        binding.feedChip.isChecked = true
        newsService.mainFeed().submitList()
    }

    //재사용을 위해 Call<NewsRss>식으로
    private fun Call<NewsRss>.submitList() {
        this.enqueue(object : Callback<NewsRss> {
            override fun onResponse(call: Call<NewsRss>, response: Response<NewsRss>) {

                //jsoup을 통해 해당 링크에 헤더에 있는 (meta태그)의 property가  og:title과 og:image 정보를 가져올 수 있다
                if (response.isSuccessful) {
                    //response로 받는 객체들(NewsItem)을 NewsModel객체로 transform (NewsModel에 썼음)
                    val list = response.body()?.channel?.items.orEmpty().transform()
                    newsAdapter.submitList(list)

                    //검색했을 시에 해당 검색어 관련 기사 리스트에 없을 시에 애니메이션 실행 visibility
                    binding.notFoundAnimationView.isVisible = list.isEmpty()
                    //링크 잘 나옴
                    list.forEachIndexed { index, news ->
                        Thread {
                            try {
                                val jsoup = Jsoup.connect(news.link).get()
                                val elements = jsoup.select("meta[property^=og:]")
                                val ogImageNode = elements.find { node ->
                                    node.attr("property") == "og:image"
                                }
                                news.imageUrl = ogImageNode?.attr("content")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            //메인 쓰레드에선 불가 하므로 runOnUiThread
                            runOnUiThread {
                                newsAdapter.notifyItemChanged(index)
                            }
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