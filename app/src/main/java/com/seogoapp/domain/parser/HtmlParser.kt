package com.seogoapp.domain.parser

import com.google.gson.Gson
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 마스토돈 타래 백업 HTML 파싱기 (한참/longwhile@crepe 포맷 기준)
 *
 * HTML 구조 예시:
 * ```html
 * <div class="toot">
 *   <div class="username">비센테 (@vicente)</div>
 *   <div class="content">
 *     <p>내용 <span class="h-card"><a>@mention</a></span> 계속</p>
 *   </div>
 *   <div class="media"><a href="https://cdn.../image.jpg">이미지</a></div>
 * </div>
 * ```
 */
object HtmlParser {

    private val gson = Gson()

    // "표시명 (@handle)" 또는 "표시명 (@handle@domain)" 패턴
    private val usernamePattern = Regex("""^(.+?)\s+\(@([^)@]+)(?:@[^)]+)?\)$""")

    data class ParseResult(
        val title: String,              // 파일명에서 추출 (파서가 결정하지 않음)
        val previewText: String,        // mention 제거 후 60자
        val characters: List<String>,   // 표시명 목록 (중복 제거)
        val tootCount: Int,
        val hasMedia: Boolean,
        val mediaUrls: List<String>,
        val contentHtml: String         // 원본 HTML 전체
    )

    /**
     * HTML 문자열을 받아 ParseResult 반환.
     * 파싱 실패 시 null 반환 → 호출부에서 수동 입력 화면으로 전환.
     */
    fun parse(htmlContent: String): ParseResult? {
        return try {
            val doc: Document = Jsoup.parse(htmlContent)

            val toots = doc.select("div.toot")
            if (toots.isEmpty()) return null

            val tootCount = toots.size

            // ── 등장 캐릭터 추출 ──
            val characters = toots
                .mapNotNull { toot ->
                    val rawUsername = toot.selectFirst(".username")?.text()?.trim()
                        ?: toot.selectFirst(".display-name")?.text()?.trim()
                        ?: return@mapNotNull null
                    parseDisplayName(rawUsername)
                }
                .distinct()

            // ── 미리보기 텍스트: 첫 번째 .content p ──
            val previewText = run {
                val firstContent = toots.firstOrNull()
                    ?.selectFirst(".content")
                    ?: toots.firstOrNull()?.selectFirst(".status__content")

                val contentClone = firstContent?.clone()
                // mention 링크 제거
                contentClone?.select("span.h-card")?.remove()
                contentClone?.select("a.mention")?.remove()

                contentClone?.selectFirst("p")?.text()
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    ?.take(80)
                    ?: ""
            }

            // ── 이미지 URL 추출 ──
            val mediaUrls = doc.select("div.media a[href], div.attachments a[href]")
                .map { it.attr("href") }
                .filter { it.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)(\\?.*)?$", RegexOption.IGNORE_CASE)) }
                .distinct()

            ParseResult(
                title = "",             // 호출부에서 파일명으로 설정
                previewText = previewText,
                characters = characters,
                tootCount = tootCount,
                hasMedia = mediaUrls.isNotEmpty(),
                mediaUrls = mediaUrls,
                contentHtml = htmlContent
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * "비센테 (@vicente)" → "비센테"
     * 매칭 실패 시 원본 문자열 반환.
     */
    fun parseDisplayName(raw: String): String {
        val match = usernamePattern.matchEntire(raw.trim())
        return match?.groupValues?.get(1)?.trim() ?: raw.trim()
    }

    fun toJson(list: List<String>): String = gson.toJson(list)

    @Suppress("UNCHECKED_CAST")
    fun fromJson(json: String): List<String> =
        runCatching { gson.fromJson(json, List::class.java) as List<String> }
            .getOrDefault(emptyList())
}
