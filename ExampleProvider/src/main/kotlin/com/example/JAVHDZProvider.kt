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
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class JAVHDZ : MainAPI() { // All providers must be an instance of MainAPI
  override var name = "JAVHDZ_nt"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override var lang = "vi"
    override val vpnStatus            = VPNStatus.MightBeNeeded
    // Enable this when your provider has a main page
    override val hasMainPage = true
 override var mainUrl = "https://javhdz.ac"

    override val mainPage = mainPageOf(
        "/video/" to "ĐỀ XUẤT",
        "/video/" to "video",
        "/trending/" to "trending",
        "/category/censored-2/" to "censored",
        "/category/uncensore-3/" to "uncensore",
        "/category/beauty-4/" to "beauty"
    )

 override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
   var document = app.get("$mainUrl${request.data}page/$page").document     
        if (request.name=="ĐỀ XUẤT") {
            
           /// document = app.get("$mainUrl/video/page/$page").document
            //lay tông trang JAVHD
            val docNavi = document.selectFirst("div.navigation")
            val number = docNavi?.select("a.page-numbers")?.get(4)?.text()
            //val number = Regex("""\.\.\.\s*(\d+)""").find(pageList?:"")?.groupValues?.get(1)

// Kết quả: "70" (kiểu String) hoặc null nếu không thấy

// Ép kiểu sang Int để sử dụng
            val totalPage = number?.toIntOrNull()?:3 // 70
            //val nn = pageNumber
            // val numberrandom = (1..100 ).random()


            //val pageNumber = number?.toIntOrNull() ?: 100
            val nPage = (3..(totalPage-3).coerceAtLeast(3)).random()


            document = app.get("$mainUrl${request.data}page/$nPage").document
        }
        
        //val document = app.get("$mainUrl${request.data}page/$page").document
//        val responseList  = document.select(".thumbnail").mapNotNull { it.toSearchResult() }
        val responseList  = document.select("a.movie-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)
    }

 override fun getVideoInterceptor(extractorLink: ExtractorLink): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Referer", mainUrl)
                .build()
            chain.proceed(request)
        }
    }
 
private fun Element.toSearchResult(): SearchResponse? {
        //tìm thẻ liên kết
        val anchor = this.selectFirst("a.movie-item")?: return null

        //lấy tên
        var tenphim = anchor.attr("title").ifEmpty {
            this.selectFirst(".movie-title-1")?.text()
        } ?: return null
        val duong_dan = fixUrlNull(anchor.attr("href"))?:""
        val hinh = fixUrlNull(this.selectFirst("img.public-film-item-thumb")?.attr("src"))

        val luot_xem = this.selectFirst(".meta-viewed")?.text()
        tenphim = "$tenphim | $luot_xem lượt xem"
        return newMovieSearchResponse(tenphim, duong_dan, TvType.NSFW) {
            this.posterUrl = hinh
            //this.po  = get(luot_xem)
        }
    }



 override suspend fun search(query: String, page: Int): SearchResponseList {
        val doc = app.get("$mainUrl/search/$query/page/$page").document
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
        val keywords = query.split(",", "|", "+")
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

//val decodedUrl = extractMediaUrl(url)

// 4. Tạo tập phim mặc định để kích hoạt trình phát (Dành cho phim lẻ/nội dung đơn lẻ)
        // val episodes = listOf(
        //     newEpisode(decodedUrl) {
        //         this.name = "Phát Video"
        //         this.episode = 1
        //         this.season = 1
        //     }
        // )


        return newMovieLoadResponse(ten_phim, url, TvType.NSFW, decodedUrl) {
            this.posterUrl = hinh
            this.plot = thong_tin?.toString()

        }
    }

override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
/*
// 1. Thêm Header giả lập trình duyệt để tránh bị website chặn ngầm
    val responseText = app.get(
        data,
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to mainUrl
        )
    ).text
val document = Jsoup.parse(responseText)

  // 2. Tìm đoạn script chứa jwplayer
    //val scriptTag = document.lines().find { it.contains("jwplayer(\"javhd\").setup") } ?: return false
    val scriptTag = document.select("script").find { it.html().contains("jwplayer(\"javhd\").setup") } ?: return false
val scriptContent = scriptTag.html()
    // 3. Sử dụng Regex tìm chuỗi Base64
    val pattern = """window\.atob\("([^"]+)"\)""".toRegex()
    val matchResult = pattern.find(scriptContent) ?: return false

    val base64Encoded = matchResult.groupValues.getOrNull(1) ?: return false
    val decodedUrl = decodeBase64Custom(base64Encoded)
*/

  //val responseText = app.get(data).text

        // 2. Gọi hàm bóc tách chuỗi URL trực tiếp từ HTML
    //    val decodedUrl = extractMediaUrl(responseText)
        
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
    
  
        // val doc = app.get(data).document


  
        // val scriptTag = doc.select("script").find{it.html().contains("jwplayer(\"javhd\").setup")}
        // val scriptConntent = scriptTag?.html()
        // val response = scriptConntent.toString()
        // val regex = """window\.atob\("([^"]+)"\)""".toRegex()
        // val matchResult = regex.find(response)
        // val base64Encoded = matchResult?.groupValues[1].toString()
                
        //   loadExtractor(base64Decode(base64Encoded),subtitleCallback,callback)
/*
// 1. Lấy nội dung thẻ script chứa jwplayer
    val scriptTag = doc.select("script").find { it.html().contains("jwplayer(\"javhd\").setup") } ?: return false
    val scriptContent = scriptTag.html()

    // 2. Tìm chuỗi Base64 bằng Regex
    val pattern = """window\.atob\("([^"]+)"\)""".toRegex()
    val matchResult = pattern.find(scriptContent) ?: return false

    // 3. Lấy chuỗi base64 và giải mã
    val base64Encoded = matchResult.groupValues.getOrNull(1) ?: return false
    val decodedUrl = decodeBase64Custom(base64Encoded)

    // 4. Nếu giải mã ra link video hợp lệ, đẩy thẳng link về cho CloudStream
    if (decodedUrl.startsWith("http")) {
            // Dùng newExtractorLink theo đúng chuẩn SDK mới
            val extractor = newExtractorLink(
                source = this.name,
                name = "Server VIP",
                url = decodedUrl
                
            )
            callback.invoke(extractor)
            return true
        }


*/


                
        

        return false
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



// Thay thế hàm giải mã bằng thư viện java.util.Base64 chuẩn của Java để tránh lỗi tự tính toán bit
private fun decodeBase64Custom(input: String): String {
    return try {
        if (input.isEmpty()) return ""
        val cleanedInput = input.trim().replace("\n", "").replace("\r", "")
        val decodedBytes = java.util.Base64.getDecoder().decode(cleanedInput)
        String(decodedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        ""
    }
}


// --- HÀM TRÍCH LỌC VÀ BÓC TÁCH RIÊNG KHỐI JWPLAYER ---
    private fun extractMediaUrl(htmlContent: String): String {
        if (htmlContent.isBlank()) return ""

        // 1. Dùng Jsoup parse HTML và lọc CHÍNH XÁC thẻ script chứa jwplayer("javhd").setup
        val document = org.jsoup.Jsoup.parse(htmlContent)
        val scriptTag = document.select("script").find {
            it.html().contains("""jwplayer("javhd").setup""") || it.html().contains("jwplayer(\"javhd\").setup")
        } ?: return ""

        val scriptContent = scriptTag.html()

        // 2. Bắt chính xác hàm window.atob("...") nằm bên trong đoạn script jwplayer đó
        val pattern = """window\.atob\s*\(\s*["']([^"']+)["']\s*\)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(scriptContent) ?: return ""

        // 3. Trả về chuỗi URL m3u8 sau khi giải mã Base64
        val base64Encoded = matchResult.groupValues.getOrNull(1) ?: return ""
        return decodeBase64Custom(base64Encoded)
    }


   


    
}
