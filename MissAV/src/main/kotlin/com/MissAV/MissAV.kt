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

    override var name = "MissAV"
    override val supportedTypes = setOf(TvType.NSFW)
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
        "/dm55/vi/genres/4K/" to "4K",
        "/dm772/vi/genres/Ntr/" to "LÉN LÚC"
    )

 private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7"
    )


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

    private fun Element.toSearchResult(): SearchResponse? {
        //tìm thẻ liên kết
        val anchor = this.selectFirst("div.thumbnail.group div.relative")?: return null
        val code = anchor.selectFirst("a")?.attr("alt")?:""
        val duong_dan = fixUrlNull(anchor.selectFirst("a")?.attr("href"))?:""
        val tenphim = code.uppercase() + " " + anchor.selectFirst("img")?.attr("alt")
        val hinh = fixUrlNull(anchor.selectFirst("img")?.attr("data-src"))
        return newMovieSearchResponse(tenphim, duong_dan, TvType.NSFW) {
            this.posterUrl = hinh
        }
    }













 




}

