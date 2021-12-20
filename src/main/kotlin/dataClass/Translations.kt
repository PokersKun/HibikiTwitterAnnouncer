package dataClass

import com.google.gson.annotations.SerializedName


data class Translations (

  @SerializedName("translatedText" ) var translatedText : String? = null

)