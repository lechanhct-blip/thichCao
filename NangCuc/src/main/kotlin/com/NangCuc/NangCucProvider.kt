package com.NangCuc

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class NangCuc : MainAPI() {
 override var name = "Kho Phim 2"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override var lang = "vi"
    override val vpnStatus            = VPNStatus.MightBeNeeded
    // Enable this when your provider has a main page
    override val hasMainPage = true
    override var mainUrl = "https://daebaknews.co"
    override val mainPage = mainPageOf(
     "/" to "ĐỀ XUẤT",
     "/" to "Trang chủ",
     "/regions/nhat-ban/" to "Nhật Bản",
     "/regions/trung-quoc/" to "Trung Quốc",
     "/genres/au-my/" to "Âu - Mỹ",
     "/genres/trung-quoc/" to "Gái Trung Quốc",
     "/genres/khong-che/" to "Không Che",
     "/genres/viet-sub/" to "Sub Việt"

     )

 override fun getVideoInterceptor(extractorLink: ExtractorLink): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Referer", mainUrl)
                .build()
            chain.proceed(request)
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        //val document = app.get("$mainUrl${request.data}page/$page").document

        var document = app.get("$mainUrl${request.data}page/$page").document
        if (request.name=="ĐỀ XUẤT") {
            //document = app.get("$mainUrl/page/$page").document

            val docNavi = document.selectFirst("ul.pagination")
            val number = docNavi?.select("li.page-item a")?.get(3)?.text()
            val totalPage = number?.toIntOrNull()?:5 // 70
            //val nn = pageNumber
            // val numberrandom = (1..100 ).random()

            //val pageNumber = number?.toIntOrNull() ?: 100
            val nPage = (5..(totalPage-5).coerceAtLeast(5)).random()
            document = app.get("$mainUrl${request.data}page/$nPage").document
        }
     
//        val responseList  = document.select(".thumbnail").mapNotNull { it.toSearchResult() }
        val responseList  = document.select("div.flw-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)
    }




    private fun Element.toSearchResult(): SearchResponse? {
//        val title = this.select(".video-title").text()
        //val title = this.selectFirst(".movie-item m-block")?.attr("title").toString()

        //tìm thẻ liên kết
        val anchor = this.selectFirst("div.flw-item")?: return null


        val hinh = fixUrlNull(anchor.selectFirst("img.film-poster-img")?.attr("data-src"))

        //lấy tên
        var tenphim = anchor.selectFirst("img.film-poster-img")?.attr("title").toString()

        val duong_dan = anchor.selectFirst("a.film-poster-ahref")?.attr("href").toString()

        return newMovieSearchResponse(tenphim, duong_dan, TvType.NSFW) {
            this.posterUrl = hinh
            //this.po  = get(luot_xem)
        }
    }




    override suspend fun search(query: String, page: Int): SearchResponseList {
        val doc = app.get("$mainUrl/page/$page/?s=$query").document
        val results = doc.select("div.flw-item").mapNotNull { it.toSearchResult() }
        val hasNext = if (results.isEmpty()) false else true
        return newSearchResponseList(results, hasNext)
    }


override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val article = document.selectFirst(".watch-block-area")?: return null

        //trỏ vào đối tượng IMG
       // val imgThumb = article.selectFirst("button.film-preview-thumb img")
        val hinh =fixUrlNull(article.selectFirst("button.film-preview-thumb img")?.attr("src"))
        val ten_phim = article.selectFirst("h1.video-title")?.text()?:""


        val thong_tin = article.selectFirst(".fancybox p")?.text()
        //thong_tin?.select("img")?.remove()
//val aa = thong_tin?.length
val embedUrl = article.selectFirst(".list_link li")?.attr("data-link")


//val luot_xem =article.selectFirst(".block-view span")?.text()

    return newMovieLoadResponse(ten_phim, url, TvType.NSFW, embedUrl) {
        this.posterUrl = hinh
        this.plot = thong_tin
    }

// 4. Tạo tập phim mặc định để kích hoạt trình phát (Dành cho phim lẻ/nội dung đơn lẻ)
//        val episodes = listOf(
//            newEpisode(url) {
//                this.name = "Phát Video"
//                this.episode = 1
//                this.season = 1
//            }
//        )

    }

override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {        
    // 4. Kiểm tra và trả về link cho trình phát
    if (data.startsWith("http")) {
        val extractor = newExtractorLink(
            source = this.name,
            name = "Server VIP",
            url = data
        )
        callback.invoke(extractor)
        return true
    }
    return false
  }








    
  
}
