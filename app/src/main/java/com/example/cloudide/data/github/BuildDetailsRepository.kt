package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.firstLineOfCommit
import com.example.cloudide.data.github.dto.timeMillis
import com.example.cloudide.data.github.dto.toBuildStatus
import com.example.cloudide.domain.BuildArtifact
import com.example.cloudide.domain.BuildRunSummary
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BuildDetailsRepository @Inject constructor(
    private val api: GitHubApi,
) {

    /** Fetches the logs of one job. Returns the plain text, truncated by the caller. */
    suspend fun getLogs(owner: String, repo: String, jobId: Long): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getJobLogs(owner, repo, jobId)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code()}"))
                }
                val body = response.body() ?: return@withContext Result.failure(
                    Exception("Empty log body")
                )
                val text = body.string()
                Result.success(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Lists the non-expired artifacts of a finished run. */
    suspend fun listArtifacts(owner: String, repo: String, runId: Long): Result<List<BuildArtifact>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getArtifactsForRun(owner, repo, runId)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code()}"))
                }
                val artifacts = response.body()?.artifacts.orEmpty()
                    .filter { !it.expired }
                    .map {
                        BuildArtifact(
                            id = it.id,
                            name = it.name,
                            sizeInBytes = it.sizeInBytes,
                            expired = it.expired,
                        )
                    }
                Result.success(artifacts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Downloads the artifact ZIP and extracts the first .apk into [destDir].
     * The auth header is NOT forwarded on the cross-origin redirect (see AuthInterceptor).
     */
    suspend fun downloadApk(
        owner: String,
        repo: String,
        artifactId: Long,
        destDir: File,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destDir.mkdirs()
            val zipFile = File(destDir, "artifact-$artifactId.zip")
            val response = api.downloadArtifactZip(owner, repo, artifactId)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            }
            val body = response.body() ?: return@withContext Result.failure(
                Exception("Empty artifact body")
            )
            val total = body.contentLength()
            var written = 0L
            body.byteStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress(written.toFloat() / total)
                    }
                }
            }
            val apk = extractFirstApk(zipFile, destDir)
            zipFile.delete()
            if (apk == null) {
                Result.failure(Exception("No APK found inside the artifact"))
            } else {
                Result.success(apk)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractFirstApk(zip: File, destDir: File): File? {
        ZipInputStream(zip.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                    val dest = File(destDir, File(entry.name).name)
                    FileOutputStream(dest).use { fos -> zis.copyTo(fos) }
                    return dest
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    /** Lists recent runs of a repo, newest first. */
    suspend fun listRuns(owner: String, repo: String): Result<List<BuildRunSummary>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.listRuns(owner, repo)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code()}"))
                }
                val runs = response.body()?.runs.orEmpty().map {
                    BuildRunSummary(
                        id = it.id,
                        status = it.toBuildStatus(),
                        branch = it.headBranch.orEmpty(),
                        commitMessage = it.firstLineOfCommit(),
                        timeMillis = it.timeMillis(),
                    )
                }
                Result.success(runs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}