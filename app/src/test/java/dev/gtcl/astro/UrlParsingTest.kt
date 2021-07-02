package dev.gtcl.astro

import dev.gtcl.astro.url.UrlType
import dev.gtcl.astro.url.getUrlType
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlParsingTest {

    @Test
    fun image(){
        val url = "http://i.imgur.com/cjLrMM5.jpg"
        assertEquals(url.getUrlType(), UrlType.IMAGE)
    }

    @Test
    fun gif(){
        val url = "https://thumbs.gfycat.com/NervousSickAmericancrocodile-size_restricted.gif"
        assertEquals(url.getUrlType(), UrlType.GIF)
    }

    @Test
    fun gifv(){
        val url = "https://i.imgur.com/Ug7NSbt.gifv"
        assertEquals(url.getUrlType(), UrlType.GIFV)
    }

    @Test
    fun gfycat(){
        val url = "https://gfycat.com/deafeningslowadouri-natalia-baccino"
        assertEquals(url.getUrlType(), UrlType.GFYCAT)
    }

    @Test
    fun redgifs(){
        val url  = "https://redgifs.com/watch/lighthearteduntriedcrayfish?l"
        assertEquals(url.getUrlType(), UrlType.REDGIFS)
    }

    @Test
    fun hls(){
        val url = "https://v.redd.it/l5v6wqr9q1d51/HLSPlaylist.m3u8?a=1598326672%2CNmVhNmJlMjBhOWVkMWEzZDQ3OThiMTI2MGJmZjJhNmMxZTExNjQ4ZTYxNmY1NTYzMzAzOTI2YzcxYTJiMzI4Zg%3D%3D&amp;v=1&amp;f=sd"
        assertEquals(url.getUrlType(), UrlType.HLS)
    }

    @Test
    fun redditVideo(){
        val url = "https://v.redd.it/l5v6wqr9q1d51"
        assertEquals(url.getUrlType(), UrlType.REDDIT_VIDEO)
    }

    @Test
    fun standardVideo(){
        val url = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
        assertEquals(url.getUrlType(), UrlType.STANDARD_VIDEO)
    }

    @Test
    fun imgurAlbum(){
        val url = "https://imgur.com/gallery/sVpILmj"
        assertEquals(url.getUrlType(), UrlType.IMGUR_ALBUM)
    }

    @Test
    fun imgurAlbum2(){
        val url = "https://m.imgur.com/a/6tDq0XV"
        assertEquals(url.getUrlType(), UrlType.IMGUR_ALBUM)
    }

    @Test
    fun imgurImage(){
        val url = "http://i.imgur.com/cjLrMM5"
        assertEquals(url.getUrlType(), UrlType.IMGUR_IMAGE)
    }

    @Test
    fun subreddit(){
        val url = "https://www.reddit.com/r/funny"
        assertEquals(url.getUrlType(), UrlType.SUBREDDIT)
    }

    @Test
    fun user1(){
        val url = "https://www.reddit.com/user/Animesh77666"
        assertEquals(url.getUrlType(), UrlType.USER)
    }

    @Test
    fun user2(){
        val url = "https://www.reddit.com/u/Animesh77666"
        assertEquals(url.getUrlType(), UrlType.USER)
    }

    @Test
    fun redditComments(){
        val url = "https://www.reddit.com/r/funny/comments/ocdeld/broad_daylight_robbery/"
        assertEquals(url.getUrlType(), UrlType.REDDIT_COMMENTS)
    }

    @Test
    fun redditThread(){
        val url = "https://www.reddit.com/r/WatchItForThePlot/comments/ikj1wz/keira_knightley_little_puffy_plots_a_dangerous/g3mjo5l/"
        assertEquals(url.getUrlType(), UrlType.REDDIT_THREAD)
    }

    @Test
    fun redditGallery(){
        val url = "https://www.reddit.com/gallery/ob5spt"
        assertEquals(url.getUrlType(), UrlType.REDDIT_GALLERY)
    }

    @Test
    fun other(){
        val url = "https://www.google.com/"
        assertEquals(url.getUrlType(), UrlType.OTHER)
    }
}