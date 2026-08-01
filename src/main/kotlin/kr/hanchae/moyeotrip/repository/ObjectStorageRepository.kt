package kr.hanchae.moyeotrip.repository

import io.awspring.cloud.s3.S3Template
import kr.hanchae.moyeotrip.config.properties.StorageS3Properties
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.Locale
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

    fun upload(
        path: String,
        file: MultipartFile,
    ): String {
        requireNotNull(file.originalFilename) { "파일의 원본 파일명이 필요합니다." }
        val extension =
            FilenameUtils
                .getExtension(file.originalFilename)
                .lowercase(Locale.getDefault())
        return upload(path, generateFileName(extension), file.inputStream)
    }

    fun getDownloadUrl(key: String): String = "${storageS3Properties.cdnUrl}/$key"

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

        fun generateFileName(extension: String): String = "${UUID.randomUUID()}.$extension" // 중복나지 않도록 UUID 사용
    }
}
