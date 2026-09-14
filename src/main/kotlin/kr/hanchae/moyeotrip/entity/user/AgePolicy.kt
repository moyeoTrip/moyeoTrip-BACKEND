package kr.hanchae.moyeotrip.entity.user

/**
 * 서비스 전체의 **나이 기준 한 곳**.
 *
 * 가입 최소 연령과 모집의 연령 조건 하한은 **같아야 한다** — 하한이 더 높으면
 * 가입은 되는데 연령 조건이 걸린 모집에는 영원히 못 들어가는 사람이 생긴다.
 * 실제로 가입 연령만 19세로 내렸을 때 모집 하한이 20세로 남아 그 구멍이 났었다.
 * 값을 두 군데 적으면 또 갈라지므로 여기 하나만 둔다.
 */
object AgePolicy {
    const val MINIMUM_SIGNUP_AGE = 19
}
