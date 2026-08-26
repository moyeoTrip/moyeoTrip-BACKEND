package kr.hanchae.moyeotrip.repository

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Template
import kr.hanchae.moyeotrip.config.properties.StorageS3Properties
import org.springframework.stereotype.Repository
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.UUID

@Repository
class ObjectStorageRepository(
    private val storageS3Properties: StorageS3Properties,
    private val s3Template: S3Template,
) {
    fun upload(
        path: String,
        key: String,
        stream: InputStream,
    ): String {
        val result = s3Template.upload(storageS3Properties.bucket, path + key, stream)
        return result.filename
    }

    fun getDownloadUrl(key: String): String = "${storageS3Properties.cdnUrl}/$key"

    fun uploadGeneratedProfileImage(imageBytes: ByteArray): String =
        s3Template
            .upload(
                storageS3Properties.bucket,
                USER_PROFILE_IMAGE_PATH + generateFileName(PROFILE_IMAGE_EXTENSION),
                ByteArrayInputStream(imageBytes),
                ObjectMetadata
                    .builder()
                    .contentType(PROFILE_IMAGE_CONTENT_TYPE)
                    .contentLength(imageBytes.size.toLong())
                    .build(),
            ).filename

    fun uploadTourismImage(
        imageBytes: ByteArray,
        contentType: String,
    ): String =
        s3Template
            .upload(
                storageS3Properties.bucket,
                TOURISM_IMAGE_PATH + generateFileName(contentType.imageExtension()),
                ByteArrayInputStream(imageBytes),
                ObjectMetadata
                    .builder()
                    .contentType(contentType)
                    .contentLength(imageBytes.size.toLong())
                    .build(),
            ).filename

    fun delete(key: String) { // 참고로 delete는 실시간 반영되지 않음
        s3Template.deleteObject(storageS3Properties.bucket, key)
    }

    // WARNING: 이 메소드는 모든 객체를 삭제합니다. 주의해서 사용하세요.
    fun deleteAll() {
        s3Template.listObjects(storageS3Properties.bucket, "").parallelStream().forEach { obj ->
            delete(obj.filename)
        }
    }

    companion object {
        const val USER_PROFILE_IMAGE_PATH = "user/profile/image/"
        const val TOURISM_IMAGE_PATH = "tourism/image/"
        private const val PROFILE_IMAGE_EXTENSION = "webp"
        private const val PROFILE_IMAGE_CONTENT_TYPE = "image/webp"

        fun generateFileName(extension: String): String = "${UUID.randomUUID()}.$extension" // 중복나지 않도록 UUID 사용

        private fun String.imageExtension(): String = if (equals("image/png", ignoreCase = true)) "png" else "jpg"
    }
}
