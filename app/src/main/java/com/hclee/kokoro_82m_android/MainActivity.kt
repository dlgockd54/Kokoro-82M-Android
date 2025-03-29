package com.hclee.kokoro_82m_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.hclee.kokoro_82m_android.ui.theme.Kokoro82MAndroidTheme
import com.hclee.kokoro_82m_android.ui.theme.TtsPerformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val ttsPerformer = TtsPerformer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kokoro82MAndroidTheme {
                MainContent(
                    onTtsStartButtonClicked = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            ttsPerformer.start(
                                context = this@MainActivity,
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    onTtsStartButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onTtsStartButtonClicked,
        ) {
            Text("TTS 시작")
        }
    }
}

@Preview
@Composable
private fun MainContentPreview() {
    Kokoro82MAndroidTheme {
        MainContent(
            onTtsStartButtonClicked = {},
        )
    }
}
