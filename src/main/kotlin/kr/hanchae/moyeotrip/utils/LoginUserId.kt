package kr.hanchae.moyeotrip.utils

// 현재 사용자의 정보를 컨트롤러 메서드에서 쉽게 사용할 수 있도록 하는 어노테이션
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginUserId
