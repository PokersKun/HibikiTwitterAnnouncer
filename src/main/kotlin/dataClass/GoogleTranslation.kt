package dataClass
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable

data class GoogleTranslation(
    @JsonProperty("data")
    val `data`: Data
)

data class Data(
    @JsonProperty("translations")
    val translations: List<Translation>
)

data class Translation(
    @JsonProperty("detectedSourceLanguage")
    val detectedSourceLanguage: String,
    @JsonProperty("translatedText")
    val translatedText: String
)

object TranslationDeserializer : ResponseDeserializable<GoogleTranslation> {
    override fun deserialize(content: String) =
        jacksonObjectMapper().readValue<GoogleTranslation>(content)
}
