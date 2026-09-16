package com.MissAV

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
import com.lagradost.cloudstream3.network.WebViewResolver

class MissAV : MainAPI() { // All providers must be an instance of MainAPI

    override var name = "Kho Phim 3 9:15"
    override val supportedTypes = setOf(TvType.Movie, TvType.NSFW)
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override var lang = "vi"
    override val vpnStatus            = VPNStatus.MightBeNeeded
    // Enable this when your provider has a main page
    override val hasMainPage = true
    val subtitleCatUrl = "https://www.subtitlecat.com"

override var mainUrl = "https://missav.ws"
    override val mainPage = mainPageOf(
        "/dm539/vi/new/" to "ĐỀ XUẤT",
        "/dm635/vi/release/" to "MỚI CẬP NHẬT",
        "/dm106/vi/genres/Plot/" to "CỐT TRUYỆN",
        //"/dm55/vi/genres/4K/" to "4K",
        "/dm772/vi/genres/Ntr/" to "LÉN LÚT"
    )

 private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7"
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


        var url = "$mainUrl${request.data}?page=$page"//if (page <= 1) "$mainUrl/vi/new" else "$mainUrl/vi/new?page=$page"

        // Chỉ dùng WebViewResolver khi chạy trên thiết bị Android thật (không bị crash trên JUnit Test PC)
        val response = try {
            app.get(
                url,
                headers = headers,
                interceptor = WebViewResolver(Regex("""missav\.ws"""))
            )
        } catch (e: NoClassDefFoundError) {
            // Fallback cho Unit Test JVM trên PC (không có Android WebView)
            app.get(url, headers = headers)
        }
//
        var document = response.document




        //var document = doc
        if (request.name=="ĐỀ XUẤT") {


            //document = app.get("$mainUrl/video/page/$page").document
            //lay tông trang JAVHD
            val docNavi = document.selectFirst("span.relative.z-0.inline-flex.shadow-sm")
//docNavi?.childNode(25).text ?:1
            val number = docNavi?.select("a.relative.inline-flex")?.get(10)?.text()
            //val number = Regex("""\.\.\.\s*(\d+)""").find(pageList?:"")?.groupValues?.get(1)

// Kết quả: "70" (kiểu String) hoặc null nếu không thấy

// Ép kiểu sang Int để sử dụng
            val totalPage = number?.toIntOrNull()?:3 // 70
//
            val nPage = (5..(totalPage-5).coerceAtLeast(5)).random()
//
//
            url = "$mainUrl${request.data}?page=$nPage"
           document = app.get(url).document
        }

        val responseList  = document.select("div.thumbnail.group").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)
}


   override suspend fun search(query: String, page: Int): SearchResponseList {
        val doc = app.get("$mainUrl/vi/search/$query?page=$page").document
        val results = doc.select("div.thumbnail.group").mapNotNull { it.toSearchResult() }
        val hasNext = if (results.isEmpty()) false else true
        return newSearchResponseList(results, hasNext)
    }

override suspend fun load(url: String): LoadResponse? {

val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
        "Referer" to url
    )

    // 1. Tải HTML trang chi tiết
    val document = app.get(url, headers = headers).document

// 2. Tìm thẻ script chứa đoạn eval unpack m3u8
    val scripts = document.select("script").map { it.data() }
    val targetScript = scripts.find { it.contains("eval(function(p,a,c,k,e,d)") && it.contains("surrit") }
    var m3u8Url = ""
    if (targetScript != null) {
        // 3. Dùng Regex lọc chuỗi UUID (VD: f66ccc35-3ac7-4da8-afa4-4cc4f9eab3a7)
        val uuidRegex = Regex("""([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})""")
        val matchUuid = uuidRegex.find(targetScript)?.value?:""
        if (matchUuid.isNotEmpty()) {
            m3u8Url = "https://surrit.com/$matchUuid/playlist.m3u8"
        }
    }

    
        //val document = app.get(url).document
        //val article = document.selectFirst(".watch-block-area")?: return null

        val hinh =fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        //val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: "Unknown"
        val ten_phim = document.selectFirst("Title")?.text() ?:""
        var thong_tin = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?:""

        //var thong_tin = ten_phim
        val elements = document.select(".space-y-2 div.text-secondary")

        elements.forEach { el ->
            val e = el.selectFirst(".text-secondary span")
            val txt = e?.text() ?:""

            if (txt.contains("Ngày phát hành:") or txt.contains("Nữ diễn viên:")) {


                thong_tin += "\n ► "+ txt +" "+ e?.nextElementSiblings()?.text()
            }

        }


    
    return newMovieLoadResponse(ten_phim, url, TvType.Movie, m3u8Url) {
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

// val headers = mapOf(
//         "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
//         "Referer" to data
//     )


// // 1. Tải HTML trang web
//     val responseText = app.get(data, headers = headers).text
//     val document = Jsoup.parse(responseText)

//     // 2. Tìm thẻ script chứa đoạn eval unpack m3u8
//     val scripts = document.select("script").map { it.data() }
//     val targetScript = scripts.find { it.contains("eval(function(p,a,c,k,e,d)") && it.contains("surrit") }

//     //if (targetScript != null) {
//         // 3. Dùng Regex lọc chuỗi UUID (VD: f66ccc35-3ac7-4da8-afa4-4cc4f9eab3a7)
//     val uuidRegex = Regex("""([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})""")
//     val matchUuid = uuidRegex.find(targetScript)?.value

//       //  if (!matchUuid.isNullOrEmpty()) {
//             // 4. Tái tạo URL m3u8 chính thức
//     val m3u8Url = "https://surrit.com/$matchUuid/playlist.m3u8"


                     


     runAllAsync(
            {
                 val extractor = newExtractorLink(
                    source = this.name,
                    name = "Server VIP",
                    url = data
                )
                callback.invoke(extractor)

            },
            {
                var javCode = "([a-zA-Z]+-\\d+)".toRegex().find(this.name)?.groups?.get(1)?.value
                if (javCode != null) {
                    getExternalSubtitile(javCode, subtitleCallback)    
                }
                
            }
        )

            

            

            // val extractor = newExtractorLink(
            //     source = this.name,
            //     name = "Server VIP",
            //     url = m3u8Url
            // )
            // callback.invoke(extractor)
            //return true
            

            // callback.invoke(
            //     ExtractorLink(
            //         source = this.name,
            //         name = "Surrit HLS",
            //         url = m3u8Url,
            //         referer = "https://surrit.com/",
            //         quality = Qualities.Unknown.value,
            //         isM3u8 = true
            //     )
            // )
         //   return true
       // }
   // }
    /*
var m3u8Url = ""

    // Cho WebView tải trang ngầm và bắt URL m3u8 từ Network Requests
    val webView = app.get(
        data,
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to data
        )
    )


    // Lọc tìm URL m3u8 trong response hoặc bắt request bằng WebViewResolver nếu Cloudstream hỗ trợ
    // Hoặc tìm link playlist m3u8 bằng Regex mở rộng (mã hóa tương đối)
    val m3u8Regex = Regex("""https?://[^\s"'<]+?\.(?:m3u8|mp4)[^\s"'<]*""")
    val match = m3u8Regex.find(webView.text)?.value

    if (!match.isNullOrEmpty()) {
        m3u8Url = match
    }

    */
    
/*
val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
        "Referer" to data
    )

   
// 1. Tải HTML trang chi tiết
    val response = app.get(data, headers = headers).text

    // 2. Trích xuất chuỗi mã hóa m3u8 từ script (ví dụ Regex tìm biến UUID/hls)
    // Cần kiểm tra regex khớp với cấu trúc script hiện tại của MissAV
    val m3u8Url = Regex("""https://[^\s"'<]+?\.m3u8""").find(response)?.value?:""

*/
    
    // 4. Kiểm tra và trả về link cho trình phát
    //if (m3u8Url.startsWith("http")) {
    // if (m3u8Url.isNotEmpty()) {
    //     val extractor = newExtractorLink(
    //         source = this.name,
    //         name = "Server VIP",
    //         url = m3u8Url
    //     )
    //     callback.invoke(extractor)
    //     return true
    // }




    
    return true
  }











    private fun Element.toSearchResult(): SearchResponse? {
        //tìm thẻ liên kết
        val anchor = this.selectFirst("div.thumbnail.group div.relative")?: return null
        val code = anchor.selectFirst("a")?.attr("alt")?:""
        val duong_dan = fixUrlNull(anchor.selectFirst("a")?.attr("href"))?:""
        val tenphim = code.uppercase() + " " + anchor.selectFirst("img")?.attr("alt")
        val hinh = fixUrlNull(anchor.selectFirst("img")?.attr("data-src"))
        return newMovieSearchResponse(tenphim, duong_dan, TvType.Movie) {
            this.posterUrl = hinh
        }
    }



suspend fun getExternalSubtitile(code: String, subtitleCallback: (SubtitleFile) -> Unit) {
        
                
                
                
                val query = "$subtitleCatUrl/index.php?search=$code"
                val subDoc = app.get(query, timeout = 15).document
                val subList = subDoc.select("td a")
                for(item in subList)
                {
                    if(item.text().contains(code))
                    {
                        val fullUrl = "$subtitleCatUrl/${item.attr("href")}"
                        val pDoc = app.get(fullUrl, timeout = 10).document
                        val sList = pDoc.select(".col-md-6.col-lg-4")
                        for(item in sList)
                        {
                            try {
                                val language = item.select(".sub-single span:nth-child(2)").text()
                                val text = item.select(".sub-single span:nth-child(3) a")
                                if(text.isNotEmpty() && text[0].text() == "Download")
                                {
                                    val url = "$subtitleCatUrl${text[0].attr("href")}"
                                    showToast("$language Da tim thay SUB: $url")
                                    subtitleCallback.invoke(
                                        newSubtitleFile(
                                            language.replace("\uD83D\uDC4D \uD83D\uDC4E",""),  // Use label for the name
                                            url     // Use extracted URL
                                        )
                                    )
                                }
                            } catch (_: Exception) { }
                        }

                    }
                }

                
                
                
            
    }









 




}

