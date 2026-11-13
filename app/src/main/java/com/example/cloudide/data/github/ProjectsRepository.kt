package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.firstLineOfCommit
import com.example.cloudide.data.github.dto.timeMillis
import com.example.cloudide.data.github.dto.toBuildStatus
import com.example.cloudide.data.github.dto.toDomain
import com.example.cloudide.data.local.RepoBuildDao
import com.example.cloudide.data.local.RepoBuildEntity
import com.example.cloudide.data.local.SessionStore
import com.example.cloudide.data.local.toDomain
import com.example.cloudide.domain.BuildInfo
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.domain.LoadFailure
import com.example.cloudide.domain.Repo
import com.example.cloudide.domain.RepoPage
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class ProjectsRepository @Inject constructor(
    private val api: GitHubApi,
    private val dao: RepoBuildDao,
    private val tracker: RateLimitTracker,
    private val sessionStore: SessionStore
) {

    private class CachedPage(val etag: String?, val page: RepoPage)

    private val pageCache = ConcurrentHashMap<Int, CachedPage>()

    @Volatile
    private var cacheOwner: String? = null

    /** One page of repos (sorted by last update). Uses ETag so unchanged pages cost nothing. */
    suspend fun loadRepos(page: Int): RepoPage = withContext(Dispatchers.IO) {
        guard {
            val owner = sessionStore.username.first()
            if (owner != cacheOwner) {
                pageCache.clear()
                cacheOwner = owner
            }
            throwIfRateLimited()

            val cached = pageCache[page]
            val response = api.getUserRepos("updated", PAGE_SIZE, page, cached?.etag)
            when {
                response.code() == 304 && cached != null -> cached.page
                response.isSuccessful -> {
                    val repos = response.body().orEmpty().map { it.toDomain() }
                    val hasMore = response.headers()["Link"]?.contains("rel=\"next\"") == true
                    val result = RepoPage(repos, hasMore)
                    pageCache[page] = CachedPage(response.headers()["ETag"], result)
                    result
                }
                else -> throw failureFor(response.code())
            }
        }
    }

    /** Builds saved in Room, used to fill the cards instantly. */
    suspend fun cachedBuilds(): Map<String, BuildInfo> = withContext(Dispatchers.IO) {
        dao.getAll().associate { it.fullName to it.toDomain() }
    }

    /**
     * Latest run of a repo. Returns the cached value while it is fresh (short TTL), and
     * falls back to the stale cached value when offline or rate limited.
     */
    suspend fun getBuild(repo: Repo, force: Boolean): BuildInfo = withContext(Dispatchers.IO) {
        val cached = dao.get(repo.fullName)
        val now = System.currentTimeMillis()
        if (!force && cached != null && now - cached.fetchedAt < ttlFor(cached)) {
            return@withContext cached.toDomain()
        }
        try {
            guard { fetchBuild(repo, cached, now) }
        } catch (e: LoadFailure) {
            if (cached != null && e !is LoadFailure.Unauthorized) cached.toDomain() else throw e
        }
    }

    private suspend fun fetchBuild(repo: Repo, cached: RepoBuildEntity?, now: Long): BuildInfo {
        throwIfRateLimited()
        val response = api.getLatestRun(repo.owner, repo.name, 1, cached?.etag)
        val code = response.code()

        if (code == 304 && cached != null) {
            dao.upsert(cached.copy(fetchedAt = now))
            return cached.toDomain()
        }

        if (response.isSuccessful) {
            val run = response.body()?.runs?.firstOrNull()
            val etag = response.headers()["ETag"]
            val entity = if (run == null) {
                noRuns(repo, etag, now)
            } else {
                RepoBuildEntity(
                    fullName = repo.fullName,
                    hasRun = true,
                    runId = run.id,
                    status = run.toBuildStatus().name,
                    commitMessage = run.firstLineOfCommit(),
                    branch = run.headBranch.orEmpty(),
                    runTimeMillis = run.timeMillis(),
                    etag = etag,
                    fetchedAt = now
                )
            }
            dao.upsert(entity)
            return entity.toDomain()
        }

        val failure = failureFor(code)
        // Actions disabled or not visible for this repo: show it as "No builds yet".
        if (failure is LoadFailure.Http && (code == 403 || code == 404)) {
            val entity = noRuns(repo, null, now)
            dao.upsert(entity)
            return entity.toDomain()
        }
        throw failure
    }

    private fun noRuns(repo: Repo, etag: String?, now: Long) = RepoBuildEntity(
        fullName = repo.fullName,
        hasRun = false,
        runId = 0,
        status = BuildStatus.Pending.name,
        commitMessage = "",
        branch = "",
        runTimeMillis = 0,
        etag = etag,
        fetchedAt = now
    )

    private fun ttlFor(entity: RepoBuildEntity): Long =
        if (entity.status == BuildStatus.Building.name || entity.status == BuildStatus.Pending.name) {
            ACTIVE_TTL_MS
        } else {
            IDLE_TTL_MS
        }

    private fun throwIfRateLimited() {
        tracker.limitedUntil()?.let { throw LoadFailure.RateLimited(it) }
    }

    private fun failureFor(code: Int): LoadFailure = when {
        code == 401 -> LoadFailure.Unauthorized()
        code == 403 || code == 429 ->
            tracker.limitedUntil()?.let { LoadFailure.RateLimited(it) } ?: LoadFailure.Http(code)
        else -> LoadFailure.Http(code)
    }

    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: LoadFailure) {
        throw e
    } catch (e: IOException) {
        throw LoadFailure.Offline()
    } catch (e: Exception) {
        throw LoadFailure.Unknown()
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val ACTIVE_TTL_MS = 20_000L
        const val IDLE_TTL_MS = 120_000L
    }
}