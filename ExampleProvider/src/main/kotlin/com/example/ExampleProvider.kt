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
    
  
    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf()
    }
}
