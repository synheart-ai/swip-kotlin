package ai.synheart.swip.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * ML Model loader for SVM models
 */
class ModelLoader(private val context: Context) {

    /**
     * Load SVM model from assets
     */
    fun loadSvmModel(modelName: String = "svm_linear_v1_0.json"): SvmModel {
        val inputStream = context.assets.open("ml/$modelName")
        val reader = InputStreamReader(inputStream)
        val gson = Gson()
        return gson.fromJson(reader, SvmModel::class.java)
    }
}

/**
 * SVM Model data class matching JSON structure
 */
data class SvmModel(
    @SerializedName("type") val type: String,
    @SerializedName("version") val version: String,
    @SerializedName("feature_order") val featureOrder: List<String>,
    @SerializedName("scaler_mean") val scalerMean: List<Double>,
    @SerializedName("scaler_scale") val scalerScale: List<Double>,
    @SerializedName("classes") val classes: List<String>,
    @SerializedName("weights") val weights: List<List<Double>>,
    @SerializedName("bias") val bias: List<Double>,
    @SerializedName("model_hash") val modelHash: String,
    @SerializedName("export_time_utc") val exportTimeUtc: String,
    @SerializedName("training_commit") val trainingCommit: String,
    @SerializedName("data_manifest_id") val dataManifestId: String
)
