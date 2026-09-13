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
import com.lagradost.cloudstream3.utils.newExtractorLink

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
  override var name = " Cao Cao"
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
        "/category/beauty-4/" to "beauty"
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



 override suspend fun search(query: String, page: Int): SearchResponseList {
        val doc = app.get("`$mainUrl/search/$query/page/$page`").document
//        val json = app.get("$mainUrl/search/$query/page/$page").text
//        val html = JSONObject(json).getString("html")
//        val document = Jsoup.parse(html)1

        val results = doc.select("a.movie-item").mapNotNull { it.toSearchResult() }
        val hasNext = if (results.isEmpty()) false else true
        return newSearchResponseList(results, hasNext)
    }

    
  /*
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
*/

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


        return newMovieLoadResponse(ten_phim, url, TvType.NSFW, url) {
            this.posterUrl = hinh
            this.plot = thong_tin?.toString()

        }
    }



 /*
 override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document


        val scriptTag = doc.select("script").find{it.html().contains("jwplayer(\"javhd\").setup")}?: return false
        val scriptConntent = scriptTag.html()

//        val regex = """window\.atob\("([^"]+)"\)""".toRegex()
//        val matchResult = regex.find(response)

        // 1. Tải mã nguồn HTML từ trang xem phim
        val response = app.get(data).text

        // 2. Sử dụng Regex để tìm chuỗi Base64 nằm bên trong window.atob("...")
        val regex = """window\.atob\("([^"]+)"\)""".toRegex()
        val matchResult = regex.find(response)



        if (matchResult != null) {
            // Lấy ra chuỗi mã hóa (VD: aHR0cHM6Ly9wMTYtc2cu...)
            val base64Encoded = matchResult.groupValues[1]

            // 3. Giải mã Base64 sang chuỗi URL gốc công khai
            //val decodedUrl = String(Base64.decode(base64Encoded, Base64.DEFAULT))
//            val decodedBytes = android.util.Base64.decode(base64Encoded, android.util.Base64.DEFAULT)
//            val decodedUrl = String(decodedBytes, Charsets.UTF_8)



            val decodedUrl = decodeBase64Custom(base64Encoded)

            // 4. Kiểm tra xem link giải mã được có phải là định dạng m3u8 không
            val isM3u8 = decodedUrl.contains(".m3u8")


//            runAllAsync(
//                {
//                    val episodeList = doc.select(".button_style .button_choice_server")
//                    episodeList.amap { item ->
//                        val link = item.attr("data-embed")
//                        loadExtractor(base64Decode(link),subtitleCallback,callback)
//                    }
//                },
//
//            )



             callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = "Server VIP (JW)",
                    url = decodedUrl,
//                    referer = data, // Thêm referer để tránh lỗi 403 Forbidden nếu website chặn hotlink
//                    quality = Qualities.Unknown.value, // Hệ thống m3u8 (Auto) sẽ tự nhận diện độ phân giải
//                    isM3u8 = isM3
                )
            )
            return true
        }




        /*
                runAllAsync(
                    {
                        val episodeList = doc.select(".button_style .button_choice_server")
                        episodeList.amap { item ->
                            val link = item.attr("data-embed")
                            loadExtractor(base64Decode(link),subtitleCallback,callback)
                        }
                    },
                    {
                       // getExternalSubtitile(doc, subtitleCallback)
                    }
                )
        */
        return true
    }
*/





 
    // Hàm bổ trợ giải mã Base64 thuần giúp tương thích hoàn toàn với Unit Test và mọi phiên bản Android
    private fun decodeBase64Custom(input: String): String {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val cleanedInput = input.replace("=", "")
        val bytes = ArrayList<Byte>()
        var buffer = 0
        var bufferLength = 0

        for (char in cleanedInput) {
            val value = base64Chars.indexOf(char)
            if (value < 0) continue // Bỏ qua ký tự không hợp lệ

            buffer = (buffer shl 6) or value
            bufferLength += 6

            if (bufferLength >= 8) {
                bufferLength -= 8
                val byteValue = (buffer shr bufferLength) and 0xFF
                bytes.add(byteValue.toByte())
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }
 


    
}
