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
 override var name = "Nang Cuc"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override var lang = "vi"
    override val vpnStatus            = VPNStatus.MightBeNeeded
    // Enable this when your provider has a main page
    override val hasMainPage = true
  override var mainUrl = "https://nangcuc.ws"
    override val mainPage = mainPageOf(
    "/" to "Trang chủ",
    "/regions/nhat-ban/" to "Nhật Bản",
    "/regions/trung-quoc/" to "Trung Quốc",
    "/genres/au-my/" to "Âu - Mỹ",
    "/genres/trung-quoc/" to "Gái Trung Quốc",
    "/genres/khong-che/" to "Không Che",

    "/genres/viet-sub/" to "Sub Việt",
    "/dien-vien/" to "Diễn Viên"

)
  
}
