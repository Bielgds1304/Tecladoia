package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.offline.LocalOfflineAiEngine
import com.example.ai.offline.TFLiteModelManager
import com.example.ai.offline.TFLiteTextProcessor
import com.example.api.TransformAction
import com.example.data.security.CryptoManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Teclado Inteligente", appName)
  }

  @Test
  fun `crypto manager encrypts and decrypts text`() {
    val sampleApiKey = "AIzaSyTestApiKey12345"
    val encrypted = CryptoManager.encrypt(sampleApiKey)
    assertTrue(encrypted.isNotEmpty())
    val decrypted = CryptoManager.decrypt(encrypted)
    assertEquals(sampleApiKey, decrypted)
  }

  @Test
  fun `offline engine corrects text without network`() = runBlocking {
    val rawText = "vc combino de comecar hj blz?"
    val result = LocalOfflineAiEngine.executeOfflineTransform(rawText, TransformAction.CORRECT)
    assertTrue(result.isSuccess)
    val corrected = result.getOrThrow()
    assertTrue(corrected.contains("você", ignoreCase = true))
    assertTrue(corrected.contains("combinou", ignoreCase = true))
  }

  @Test
  fun `tflite model manager downloads and verifies model file`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val modelManager = TFLiteModelManager(context)
    val downloadResult = modelManager.downloadTFLiteModel()
    assertTrue(downloadResult.isSuccess)
    assertTrue(modelManager.isModelReady())
    assertTrue(modelManager.modelFile.length() > 0)
  }

  @Test
  fun `tflite text processor executes offline inference`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val modelManager = TFLiteModelManager(context)
    val processor = TFLiteTextProcessor(context, modelManager)
    val inferenceResult = processor.processText("ola tudo bem", TransformAction.NATURAL_HUMAN)
    assertTrue(inferenceResult.isSuccess)
    val output = inferenceResult.getOrThrow()
    assertTrue(output.processedText.isNotEmpty())
    assertTrue(output.inferenceTimeMs >= 0)
  }
}
