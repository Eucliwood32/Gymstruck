package com.example.gymstruck.data

import com.example.gymstruck.domain.ShortVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface YouTubeSearchService {
    /**
     * 키워드로 짧은(4분 미만) 영상을 검색한다.
     * 키 미설정·네트워크 오류·쿼터 초과 등은 [Result.failure]로 반환한다.
     */
    suspend fun searchShorts(query: String, maxResults: Int = 15): Result<List<ShortVideo>>
}

/**
 * YouTube Data API v3 `search.list` 기반 구현.
 * 새 네트워크 라이브러리 없이 HttpURLConnection + 안드로이드 내장 org.json 사용.
 */
class YouTubeDataApiSearchService(
    private val apiKey: String,
) : YouTubeSearchService {

    override suspend fun searchShorts(query: String, maxResults: Int): Result<List<ShortVideo>> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("YOUTUBE_API_KEY가 설정되지 않았습니다. local.properties를 확인하세요."),
                )
            }
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val url = buildString {
                append("https://www.googleapis.com/youtube/v3/search")
                append("?part=snippet")
                append("&type=video")
                append("&videoDuration=short")
                append("&videoEmbeddable=true") // 임베드(앱 내 재생) 가능한 영상만
                append("&maxResults=").append(maxResults.coerceIn(1, 50))
                append("&q=").append(URLEncoder.encode(query, "UTF-8"))
                append("&key=").append(apiKey)
            }

            runCatching {
                val body = fetch(url)
                parse(body)
            }
        }

    private fun fetch(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("YouTube API 오류 ($code): $err")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): List<ShortVideo> {
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
        val result = ArrayList<ShortVideo>(items.length())
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
            if (videoId.isBlank()) continue
            val snippet = item.optJSONObject("snippet")
            val thumb = snippet
                ?.optJSONObject("thumbnails")
                ?.optJSONObject("medium")
                ?.optString("url")
                .orEmpty()
            result.add(
                ShortVideo(
                    videoId = videoId,
                    title = snippet?.optString("title").orEmpty(),
                    thumbnailUrl = thumb,
                    channelTitle = snippet?.optString("channelTitle").orEmpty(),
                ),
            )
        }
        return result
    }
}
