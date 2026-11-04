package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class MyBoyfriendTvProvider : MainAPI() {
    override var mainUrl = "https://myboyfriend.tv" // Pon aquí la URL exacta de la web
    override var name = "MyBoyfriendTV"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // 1. Secciones de la pantalla de inicio
    override val mainPage = mainPageOf(
        "$mainUrl/latest/" to "Recientes",
        "$mainUrl/popular/" to "Populares"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + "?page=$page").document
        val home = document.select("article.item, div.video-block").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, .title, a")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    // 2. Barra de búsqueda
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.item, div.video-block").mapNotNull {
            it.toSearchResult()
        }
    }

    // 3. Página de detalles del video
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Video"
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val plot = document.selectFirst("div.description, .synopsis")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    // 4. Extracción del reproductor (Link de streaming)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Si la web usa un reproductor embebido (iframe):
        document.select("iframe").forEach { iframe ->
            val iframeUrl = fixUrlNull(iframe.attr("src"))
            if (iframeUrl != null) {
                loadExtractor(iframeUrl, data, subtitleCallback, callback)
            }
        }

        // Si la web usa una etiqueta <video> con link directo .mp4 o .m3u8:
        document.select("video source").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        videoUrl,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        isM3u8 = videoUrl.contains(".m3u8")
                    )
                )
            }
        }
        return true
    }
}