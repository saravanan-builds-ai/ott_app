package `in`.sms.tringapps.feature.home.presentation.component

object HomeTestTags {
    const val LOADING = "home_loading"
    const val ERROR = "home_error"
    const val RETRY = "home_retry"
    const val HERO = "home_hero"
    const val HERO_PLAY = "home_hero_play"
    const val HERO_INFO = "home_hero_info"
    const val LIST = "home_list"
    const val SEARCH = "home_search"
    const val TRENDING = "home_trending"
    const val CONTINUE_WATCHING = "home_continue_watching"
    const val MY_LIST = "home_my_list"

    fun rail(playlistId: String) = "rail_$playlistId"
    fun seeAll(playlistId: String) = "see_all_$playlistId"
    fun rankedCard(mediaId: String) = "ranked_card_$mediaId"
}
