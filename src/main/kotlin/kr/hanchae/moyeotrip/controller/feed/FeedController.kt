package kr.hanchae.moyeotrip.controller.feed

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedCommentRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.controller.feed.response.FeedCommentResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedLikeResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedPageResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedResponse
import kr.hanchae.moyeotrip.service.feed.FeedService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(
    private val feedService: FeedService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createFeed(
        @LoginUserId userId: Long,
        @Valid @RequestPart("request") request: CreateFeedRequest,
        @RequestPart("images") images: List<MultipartFile>,
    ): ResponseEntity<FeedResponse> = ResponseEntity.status(HttpStatus.CREATED).body(feedService.createFeed(userId, request, images))

    @GetMapping
    fun getFeeds(
        @LoginUserId userId: Long,
        @RequestParam(defaultValue = "DISCOVER") tab: FeedTab,
        @RequestParam(required = false) beforeFeedId: Long?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): FeedPageResponse = feedService.getFeeds(userId, tab, beforeFeedId, limit)

    @GetMapping("/{feedId}")
    fun getFeed(
        @LoginUserId userId: Long,
        @PathVariable feedId: Long,
    ): FeedResponse = feedService.getFeed(userId, feedId)

    @PostMapping("/{feedId}/like")
    fun toggleLike(
        @LoginUserId userId: Long,
        @PathVariable feedId: Long,
    ): FeedLikeResponse = feedService.toggleLike(userId, feedId)

    @GetMapping("/{feedId}/comments")
    fun getComments(
        @LoginUserId userId: Long,
        @PathVariable feedId: Long,
    ): List<FeedCommentResponse> = feedService.getComments(userId, feedId)

    @PostMapping("/{feedId}/comments")
    fun createComment(
        @LoginUserId userId: Long,
        @PathVariable feedId: Long,
        @Valid @RequestBody request: CreateFeedCommentRequest,
    ): ResponseEntity<FeedCommentResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(feedService.createComment(userId, feedId, request))
}
