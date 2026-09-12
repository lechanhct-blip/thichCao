package com.example

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
  override var name = "Example provider"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override var lang = "vi"
    override val vpnStatus            = VPNStatus.MightBeNeeded
    // Enable this when your provider has a main page
    override val hasMainPage = true
 override var mainUrl = "https://javhdz.ac"
    override val mainPage = mainPageOf(
        "/video/" to "video",
        "/trending/" to "trending",
        "/category/censored-2/" to "censored",
        "/category/uncensore-3/" to "uncensore",
        "/beauty-4/" to "beauty"
    )

 override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}page/$page").document
//        val responseList  = document.select(".thumbnail").mapNotNull { it.toSearchResult() }
        val responseList  = document.select("a.movie-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)
    }
private fun Element.toSearchResult(): SearchResponse? {
        //tìm thẻ liên kết
        val anchor = this.selectFirst("a.movie-item")?: return null

        //lấy tên
        var tenphim = anchor.attr("title").ifEmpty {
            this.selectFirst(".movie-title-1")?.text()
        } ?: return null
        val duong_dan = mainUrl + anchor.attr(("href"))
        val hinh = fixUrlNull(this.selectFirst("img.public-film-item-thumb")?.attr("src"))

        val luot_xem = this.selectFirst(".meta-viewed")?.text()
        tenphim = "$tenphim | $luot_xem lượt xem"
        return newMovieSearchResponse(tenphim, duong_dan, TvType.NSFW) {
            this.posterUrl = hinh
            //this.po  = get(luot_xem)
        }
    }
    
  
    override suspend fun search(query: String): List<SearchResponse> = coroutineScope {
        // 1. Tách từ khóa theo dấu phẩy (,), dấu gạch đứng (|) hoặc dấu cộng (+)
        val keywords = query.split(",", "|", "+", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 2. Chạy tìm kiếm song song cho từng từ khóa bằng async
        val deferredResults = keywords.map { keyword ->
            async {
                val keywordResults = mutableListOf<SearchResponse>()
                val maxPages = 3//chọc vào file HTML lấy trang cuốc cùng


                for (page in 1..maxPages){
                    val searchUrl = "$mainUrl/search/$keyword/page/$page"
                    val document = try {
                        app.get(searchUrl).document
                    }catch (e: Exception){
                        null
                    } ?: break

                    val items= document.select("a.movie-item").mapNotNull { it.toSearchResult() }
                    if (items.isEmpty()) break

                    keywordResults.addAll(items)

                }
                keywordResults

            }
        }

        // 3. Chờ tất cả request hoàn thành -> gộp các danh sách -> loại bỏ phim bị trùng (theo URL)
        deferredResults.awaitAll()
            .flatten()
            .distinctBy { it.url }
    }


 override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val article = document.selectFirst("#film-content-wrapper")?: return null

        //trỏ vào đối tượng IMG
        val imgThumb = article.select("img.thumb")
        val hinh =fixUrlNull(imgThumb.attr("src"))
        val ten_phim = imgThumb.attr("alt").trim()


        val thong_tin = article.selectFirst("p")?.clone()//clone tạo bản sao mã HTML
      //  val thong_tin = article.selectFirst("p")?.ownText()
        thong_tin?.select("img")?.remove()
        val tt = thong_tin?.text()

        //lấy link video
        val scriptTag = document.select("script").find{it.html().contains("jwplayer(\"javhd\").setup")}
        val scriptConntent = scriptTag?.html()

//        val regex = """window\.atob\("([^"]+)"\)""".toRegex()
//        val matchResult = regex.find(response)

        // 1. Tải mã nguồn HTML từ trang xem phim
        val response = scriptConntent.toString()

        // 2. Sử dụng Regex để tìm chuỗi Base64 nằm bên trong window.atob("...")
        val regex = """window\.atob\("([^"]+)"\)""".toRegex()
        val matchResult = regex.find(response)
        val base64Encoded = matchResult?.groupValues[1].toString()
        val decodedUrl = decodeBase64Custom(base64Encoded)



// 4. Tạo tập phim mặc định để kích hoạt trình phát (Dành cho phim lẻ/nội dung đơn lẻ)
        val episodes = listOf(
            newEpisode(decodedUrl) {
                this.name = "Phát Video"
                this.episode = 1
                this.season = 1
            }
        )


        return newMovieLoadResponse(ten_phim, url, TvType.NSFW, decodedUrl) {
            this.posterUrl = hinh
            this.plot = thong_tin?.toString()

        }
    }




    
}
