package dev.gtcl.astro.models.reddit.listing

import android.graphics.Color
import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

@Parcelize
data class Flair(
    var text: String,
    @Json(name = "text_editable")
    val textEditable: Boolean,
    val id: String,
    val richtext: List<FlairRichtext>?
) : Parcelable {

    @IgnoredOnParcel
    val randomColor = when(Random.nextInt(0, 8)){
        0 -> Color.parseColor("#d50000")
        1 -> Color.parseColor("#aa00ff")
        2 -> Color.parseColor("#304ffe")
        3 -> Color.parseColor("#0091ea")
        4 -> Color.parseColor("#00bfa5")
        5 -> Color.parseColor("#64dd17")
        6 -> Color.parseColor("#ffab00")
        else -> Color.parseColor("#dd2c00")
    }

}