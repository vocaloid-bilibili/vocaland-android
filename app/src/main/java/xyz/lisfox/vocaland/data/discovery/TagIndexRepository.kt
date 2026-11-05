package xyz.lisfox.vocaland.data.discovery

class TagIndexRepository(
    private val discovery: DiscoveryRepository = DiscoveryRepository(),
) {
    suspend fun loadWall(
        scope: String?,
        sort: String,
    ): Result<TagWallData> = discovery.loadTagWall(scope, sort)

    suspend fun loadFiltered(
        scope: String?,
        category: String?,
        search: String?,
        sort: String,
        page: Int,
    ): Result<TagFlatPage> = discovery.loadTagList(
        scope = scope,
        category = category,
        search = search,
        sort = sort,
        page = page,
    )
}
