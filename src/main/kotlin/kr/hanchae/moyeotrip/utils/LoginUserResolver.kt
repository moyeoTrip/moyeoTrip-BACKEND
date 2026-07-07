package kr.hanchae.moyeotrip.utils

import kr.hanchae.moyeotrip.config.security.SecurityUser
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class LoginUserResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginUserId::class.java) && parameter.parameterType == Long::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val authentication = SecurityContextHolder.getContext().authentication?.principal
        // 로그인이 안됐다면 필터에서 짤렸을거라 null일 수  없긴 할텐데 혹시나 몰라 일단 null 체크를 해줌
        return (authentication as? SecurityUser)?.user?.id ?: throw BaseException(
            ErrorCode.UNAUTHORIZED,
            ErrorCode.UNAUTHORIZED.errorMessage,
        )
    }
}
