package ai.synheart.swip.ml

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream

@RunWith(MockitoJUnitRunner::class)
class ModelLoaderTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var assetManager: AssetManager

    private lateinit var modelLoader: ModelLoader

    @Before
    fun setup() {
        `when`(context.assets).thenReturn(assetManager)
        modelLoader = ModelLoader(context)
    }

    @Test
    fun `test load SVM model with valid JSON`() {
        // Given valid JSON model data
        val validJson = """
        {
            "type": "linear_svm",
            "version": "1.0",
            "feature_order": ["mean_hr", "std_hr", "min_hr", "max_hr", "sdnn", "rmssd"],
            "scaler_mean": [72.5, 8.2, 65.0, 85.0, 45.3, 32.1],
            "scaler_scale": [12.0, 5.5, 8.0, 15.0, 18.7, 12.4],
            "classes": ["Amused", "Calm", "Stressed"],
            "weights": [
                [0.12, -0.33, 0.08, -0.19, 0.5, 0.3],
                [-0.21, 0.55, -0.07, 0.1, -0.4, -0.3],
                [0.02, -0.12, 0.1, 0.05, 0.2, 0.1]
            ],
            "bias": [-0.2, 0.3, 0.1],
            "model_hash": "abc123def456",
            "export_time_utc": "2024-01-15T10:30:00Z",
            "training_commit": "main@abc123",
            "data_manifest_id": "manifest_v1"
        }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(validJson.toByteArray())
        `when`(assetManager.open("ml/svm_linear_v1_0.json")).thenReturn(inputStream)

        // When loading model
        val model = modelLoader.loadSvmModel()

        // Then model should be loaded correctly
        assertEquals("linear_svm", model.type)
        assertEquals("1.0", model.version)
        assertEquals(6, model.featureOrder.size)
        assertEquals(3, model.classes.size)
        assertEquals("Amused", model.classes[0])
        assertEquals("Calm", model.classes[1])
        assertEquals("Stressed", model.classes[2])
        assertEquals(3, model.weights.size)
        assertEquals(6, model.weights[0].size)
        assertEquals(3, model.bias.size)
    }

    @Test
    fun `test load model with custom name`() {
        // Given a custom model name
        val customJson = """
        {
            "type": "linear_svm",
            "version": "2.0",
            "feature_order": ["mean_hr"],
            "scaler_mean": [70.0],
            "scaler_scale": [10.0],
            "classes": ["Happy"],
            "weights": [[0.5]],
            "bias": [0.1],
            "model_hash": "custom123",
            "export_time_utc": "2024-01-15T10:30:00Z",
            "training_commit": "main@xyz",
            "data_manifest_id": "manifest_v2"
        }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(customJson.toByteArray())
        `when`(assetManager.open("ml/custom_model.json")).thenReturn(inputStream)

        // When loading with custom name
        val model = modelLoader.loadSvmModel("custom_model.json")

        // Then should load custom model
        assertEquals("linear_svm", model.type)
        assertEquals("2.0", model.version)
    }

    @Test
    fun `test model scaler parameters are correct`() {
        // Given model with known scaler parameters
        val validJson = """
        {
            "type": "linear_svm",
            "version": "1.0",
            "feature_order": ["feature1", "feature2"],
            "scaler_mean": [100.0, 200.0],
            "scaler_scale": [10.0, 20.0],
            "classes": ["A", "B"],
            "weights": [[0.1, 0.2], [0.3, 0.4]],
            "bias": [0.5, 0.6],
            "model_hash": "test123",
            "export_time_utc": "2024-01-15T10:30:00Z",
            "training_commit": "main@test",
            "data_manifest_id": "manifest_test"
        }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(validJson.toByteArray())
        `when`(assetManager.open("ml/svm_linear_v1_0.json")).thenReturn(inputStream)

        // When loading model
        val model = modelLoader.loadSvmModel()

        // Then scaler parameters should be correct
        assertEquals(100.0, model.scalerMean[0], 0.001)
        assertEquals(200.0, model.scalerMean[1], 0.001)
        assertEquals(10.0, model.scalerScale[0], 0.001)
        assertEquals(20.0, model.scalerScale[1], 0.001)
    }

    @Test
    fun `test model metadata is preserved`() {
        // Given model with metadata
        val validJson = """
        {
            "type": "linear_svm",
            "version": "1.0",
            "feature_order": ["feature"],
            "scaler_mean": [0.0],
            "scaler_scale": [1.0],
            "classes": ["Class"],
            "weights": [[0.5]],
            "bias": [0.0],
            "model_hash": "hash_abc123",
            "export_time_utc": "2024-01-15T10:30:00Z",
            "training_commit": "main@commit123",
            "data_manifest_id": "manifest_unique_id"
        }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(validJson.toByteArray())
        `when`(assetManager.open("ml/svm_linear_v1_0.json")).thenReturn(inputStream)

        // When loading model
        val model = modelLoader.loadSvmModel()

        // Then metadata should be preserved
        assertEquals("hash_abc123", model.modelHash)
        assertEquals("2024-01-15T10:30:00Z", model.exportTimeUtc)
        assertEquals("main@commit123", model.trainingCommit)
        assertEquals("manifest_unique_id", model.dataManifestId)
    }

    @Test(expected = Exception::class)
    fun `test load model throws exception for missing file`() {
        // Given missing model file
        `when`(assetManager.open(anyString())).thenThrow(RuntimeException("File not found"))

        // When loading model
        // Then should throw exception
        modelLoader.loadSvmModel("missing.json")
    }

    @Test
    fun `test model weights dimensions match classes`() {
        // Given model with multiple classes
        val validJson = """
        {
            "type": "linear_svm",
            "version": "1.0",
            "feature_order": ["f1", "f2"],
            "scaler_mean": [0.0, 0.0],
            "scaler_scale": [1.0, 1.0],
            "classes": ["Class1", "Class2", "Class3"],
            "weights": [
                [0.1, 0.2],
                [0.3, 0.4],
                [0.5, 0.6]
            ],
            "bias": [0.1, 0.2, 0.3],
            "model_hash": "test",
            "export_time_utc": "2024-01-15T10:30:00Z",
            "training_commit": "main",
            "data_manifest_id": "manifest"
        }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(validJson.toByteArray())
        `when`(assetManager.open("ml/svm_linear_v1_0.json")).thenReturn(inputStream)

        // When loading model
        val model = modelLoader.loadSvmModel()

        // Then dimensions should match
        assertEquals(model.classes.size, model.weights.size)
        assertEquals(model.classes.size, model.bias.size)
        assertEquals(model.featureOrder.size, model.weights[0].size)
    }
}

