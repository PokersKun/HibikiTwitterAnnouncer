package dataClass

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("translations" ) var translations : List<Translations> = arrayListOf()

)