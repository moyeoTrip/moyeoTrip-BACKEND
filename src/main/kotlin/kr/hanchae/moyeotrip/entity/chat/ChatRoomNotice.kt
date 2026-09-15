package kr.hanchae.moyeotrip.entity.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "chat_room_notices")
class ChatRoomNotice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    val author: User,
    @Column(length = 1000)
    var content: String?,
    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    var pinned: Boolean = false,
) : BaseModifiableEntity() {
    /**
     * BE-30 · 공지에 **수정시각**이 생겼다(`updatedDateTime`, `BaseModifiableEntity`).
     *
     * 예전에는 `BaseTimeEntity` 를 상속해 생성시각만 있었다. 그래서 두 사람이 같은 공지를 고치면
     * **나중에 저장한 쪽이 앞사람 수정을 말없이 덮어썼다** — 충돌을 감지할 값 자체가 없었다
     * (QA NOTICE-021). 이제 수정할 때 호출부가 **자기가 본 수정시각**을 함께 보내고,
     * 서버가 지금 값과 견줘 다르면 409 로 거절한다.
     */
    fun updateContent(content: String) {
        this.content = content
    }

    fun updatePinned(pinned: Boolean) {
        this.pinned = pinned
    }
}
