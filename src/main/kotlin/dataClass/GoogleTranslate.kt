package dataClass

import com.google.gson.annotations.SerializedName


data class GoogleTranslate (

  @SerializedName("data" ) var data : Data? = Data()

)