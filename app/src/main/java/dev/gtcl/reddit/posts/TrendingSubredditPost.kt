package dev.gtcl.reddit.posts

val TRENDING_SUB_REGEX = "Trending Subreddits for \\d{4}-\\d{2}-\\d{2}".toRegex()
val TITLE_REGEX = "## \\*\\*/r/\\w+\\*\\*".toRegex()
val AGE_AND_SUBS_REGEX = "A community for (.)+ subscribers\\.".toRegex()
val SUBS_AND_DESC_REGEX = "A community for (.)+ subscribers\\.(.)+---".toRegex(RegexOption.DOT_MATCHES_ALL)

class TrendingSubredditPost(post: Post?){
    var dateString = "NOT FOUND"
    val titles = arrayOf("","","","","")
    val subAgeAndCount = arrayOf("","","","","")
    val descriptions = arrayOf("","","","","")

    init {
        if(post != null && post.selftext.isNotEmpty()) {
            val dateStringTemp = TRENDING_SUB_REGEX.find(post.selftext)!!.value.replace("Trending Subreddits for ","").split("-")
            dateString = "${dateStringTemp[1]}/${dateStringTemp[2]}/${dateStringTemp[0]}"
            val titlesSequence = TITLE_REGEX.findAll(post.selftext).map { it.value.replace("##", "").replace("(\\s)*\\*\\*".toRegex(), "").replace("/r/", "") }
            val ageAndSubsSequence = AGE_AND_SUBS_REGEX.findAll(post.selftext).map { it.value }
            val subsAndDescSequence = SUBS_AND_DESC_REGEX.findAll(post.selftext).map { it.value } // TODO: update

            for(i in titles.indices){
                titles[i] = titlesSequence.elementAtOrElse(i) {""}
                subAgeAndCount[i] = ageAndSubsSequence.elementAtOrElse(i) {""}
                descriptions[i] = subsAndDescSequence.elementAtOrElse(i) {""}
            }
        }
    }
}