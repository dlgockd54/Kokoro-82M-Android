package com.hclee.kokoro_82m_android.ui.theme

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.time.measureTime

class TtsPerformer {

    suspend fun start(context: Context) {
        val result = runOnnxInference(context = context)

        if (result != null) {
            // 오디오 데이터(FloatArray) 처리 (예: AudioTrack으로 재생)
            saveAudioToFile(context, result)
        } else {
            // 추론 실패 처리
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "추론 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun runOnnxInference(context: Context): FloatArray? {
        // 1. 토큰 정의
        val tokens = longArrayOf(
            43,
            102,
            16,
            50,
            72,
            64,
            16,
            70,
            16,
            46,
            123,
            156,
            51,
            158,
            55,
            16,
            81,
            156,
            72,
            62,
            65,
            157,
            138,
            56,
            16,
            46,
            156,
            47,
            102,
            16,
            81,
            102,
            61,
            16,
            56,
            156,
            47,
            102,
            131,
            83,
            56,
            16,
            65,
            102,
            54,
            16,
            123,
            156,
            43,
            102,
            68,
            16,
            157,
            138,
            58,
            16,
            72,
            56,
            46,
            16,
            54,
            156,
            43,
            102,
            64,
            16,
            156,
            43,
            135,
            62,
            16,
            81,
            83,
            16,
            62,
            123,
            156,
            63,
            158,
            16,
            55,
            156,
            51,
            158,
            56,
            102,
            112,
            16,
            138,
            64,
            16,
            102,
            62,
            61,
            16,
            53,
            123,
            156,
            51,
            158,
            46,
            3,
            16,
            11,
            65,
            51,
            158,
            16,
            50,
            156,
            57,
            135,
            54,
            46,
            16,
            81,
            51,
            158,
            68,
            16,
            62,
            123,
            156,
            63,
            158,
            119,
            61,
            16,
            62,
            83,
            44,
            51,
            16,
            61,
            156,
            86,
            54,
            48,
            156,
            86,
            64,
            102,
            46,
            83,
            56,
            62,
            3,
            16,
            81,
            72,
            62,
            16,
            156,
            76,
            158,
            54,
            16,
            55,
            156,
            86,
            56,
            16,
            69,
            158,
            123,
            16,
            53,
            123,
            51,
            158,
            156,
            47,
            102,
            125,
            177,
            46,
            16,
            156,
            51,
            158,
            53,
            65,
            83,
            54,
            4,
            11,
            16,
            43,
            102,
            16,
            50,
            72,
            64,
            16,
            70,
            16,
            46,
            123,
            156,
            51,
            158,
            55,
            16,
            81,
            156,
            72,
            62,
            65,
            157,
            138,
            56,
            16,
            46,
            156,
            47,
            102,
            16,
            76,
            56,
            81,
            83,
            16,
            123,
            156,
            86,
            46,
            16,
            50,
            156,
            102,
            54,
            68,
            16,
            138,
            64,
            16,
            46,
            147,
            156,
            76,
            158,
            123,
            46,
            147,
            83,
            3,
            16,
            81,
            83,
            16,
            61,
            156,
            138,
            56,
            68,
            16,
            138,
            64,
            16,
            48,
            156,
            76,
            158,
            123,
            55,
            85,
            16,
            61,
            54,
            156,
            47,
            102,
            64,
            68,
            16,
            72,
            56,
            46,
            16,
            81,
            83,
            16,
            61,
            156,
            138,
            56,
            68,
            16,
            138,
            64,
            16,
            48,
            156,
            76,
            158,
            123,
            55,
            85,
            16,
            61,
            54,
            156,
            47,
            102,
            64,
            16,
            156,
            57,
            135,
            56,
            85,
            68,
            16,
            65,
            102,
            54,
            16,
            44,
            51,
            158,
            16,
            156,
            47,
            102,
            44,
            83,
            54,
            16,
            62,
            83,
            16,
            61,
            156,
            102,
            62,
            16,
            46,
            157,
            43,
            135,
            56,
            16,
            62,
            83,
            92,
            157,
            86,
            81,
            85,
            123,
            16,
            72,
            62,
            16,
            81,
            83,
            16,
            62,
            156,
            47,
            102,
            44,
            83,
            54,
            16,
            138,
            64,
            16,
            44,
            123,
            156,
            138,
            81,
            85,
            50,
            157,
            135,
            46,
            4
        )

        println("token length: ${tokens.size}")

        // 2. 토큰 길이 검증
        require(tokens.size <= 510) { "Token length exceeds 510: ${tokens.size}" }

        // 3. ONNX Runtime 환경 및 세션 생성
        val ortEnvironment = OrtEnvironment.getEnvironment(
            OrtLoggingLevel.ORT_LOGGING_LEVEL_VERBOSE,
        )
        val sessionOptions = OrtSession.SessionOptions()

        // 모델 파일 로드 (assets 경로 주의)
//        val modelFileName = "model_q8f16.onnx"
        val modelFileName = "model_quantized.onnx"
//        val modelFileName = "model_uint8.onnx"
//        val modelFileName = "model_q4.onnx"
//        val modelFileName = "model.onnx"
        val modelFilePath = "onnx/$modelFileName"

//        val modelBytes = context.assets
//            .open(modelFilePath)
//            .readBytes()

        val modelBytes: ByteBuffer
        val modelLoadDuration = measureTime {
            println("_hc onnx model load 시작")
            modelBytes = context.assets
                .open(modelFilePath)
                .run { // memory direct allocation
                    val size = available()
                    val buffer = ByteBuffer.allocateDirect(size)
                    val bytes = ByteArray(8192)
                    var read: Int

                    while (read(bytes).also { read = it } != -1) {
                        buffer.put(bytes, 0, read)
                    }
                    buffer.rewind()
                    buffer
                }
            println("_hc onnx model load 완료")
        }

        println("_hc model load 소요 시간: ${modelLoadDuration.inWholeMilliseconds}ms")

        val session = ortEnvironment.createSession(modelBytes, sessionOptions)

        println("_hc ort session 생성 완료")

        try {
            // 4. Style 벡터 로드 및 선택 (ref_s)
            val voiceFileName = "voices/am_adam.bin"
//            val voiceFileName = "voices/af.bin"
            val voiceBytes = context.assets.open(voiceFileName).readBytes()
            val voiceBuffer =
                ByteBuffer.wrap(voiceBytes).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val allVoices = FloatArray(voiceBuffer.remaining())
            voiceBuffer.get(allVoices)

            val styleVectorSize = 256 // Python 코드의 reshape(-1, 1, 256) 에서 256
            val numVoices = allVoices.size / styleVectorSize // 전체 voice 개수 추정

            // Python의 voices[len(tokens)] 로직 구현
            // 주의: Python의 voices 구조 (reshaped to (-1, 1, 256)) 와 인덱싱 방식을 정확히 확인해야 함.
            // 여기서는 len(tokens) 번째 voice의 style vector (256개 float)를 가져온다고 가정합니다.
            val styleStartIndex = tokens.size * styleVectorSize
            if (styleStartIndex + styleVectorSize > allVoices.size) {
                throw IndexOutOfBoundsException("Token length ${tokens.size} is out of bounds for the available voices (max index ${numVoices - 1})")
            }
            val refS = allVoices.copyOfRange(styleStartIndex, styleStartIndex + styleVectorSize)


            // 5. 입력 텐서 준비

            // input_ids: 패딩 추가 및 텐서 생성 [1, num_tokens + 2]
            val paddedTokens = longArrayOf(0L) + tokens + longArrayOf(0L)
            val inputIdsShape = longArrayOf(1, paddedTokens.size.toLong())
            val inputIdsBuffer = LongBuffer.wrap(paddedTokens)
            val inputIdsTensor =
                OnnxTensor.createTensor(ortEnvironment, inputIdsBuffer, inputIdsShape)

            // style: ref_s 텐서 생성 [1, 256]
            val styleShape = longArrayOf(1, styleVectorSize.toLong())
            val styleBuffer = FloatBuffer.wrap(refS)
            val styleTensor = OnnxTensor.createTensor(ortEnvironment, styleBuffer, styleShape)

            // speed: [1.0f] 텐서 생성 [1]
            val speedShape = longArrayOf(1)
            val speedBuffer = FloatBuffer.wrap(floatArrayOf(1.0f))
            val speedTensor = OnnxTensor.createTensor(ortEnvironment, speedBuffer, speedShape)

            // 입력 맵 구성
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "style" to styleTensor,
                "speed" to speedTensor
            )

            // 6. 추론 실행
            val results: OrtSession.Result
            val inferenceDuration = measureTime {
                println("_hc 추론 시작 🤔🤔")
                results = session.run(inputs)
                println("_hc 추론 완료")
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "추론 성공 🥳🥳", Toast.LENGTH_SHORT).show()
            }

            println("_hc 추론 소요 시간: ${inferenceDuration.inWholeMilliseconds}ms")

            // 7. 결과 처리
            val resultOptional = results.get("waveform")

            if (resultOptional.isEmpty) {
                return null
            }

            val audioOutputTensor = resultOptional.get() as? OnnxTensor
            val audioData = audioOutputTensor?.floatBuffer?.let { buffer ->
                val array = FloatArray(buffer.remaining())
                buffer.get(array)
                array
            } // -1.0 ~ 1.0 사이의 값을 갖는 float 배열, 디지털 오디오 처리 분야에서 부동 소수점 형식으로 오디오 데이터를 다룰 떄, 진폭을 -1.0과 1.0 사이로 정규화 하는 것은 일반적인 관행

            // 사용한 텐서 및 결과 리소스 해제 (중요!)
            inputIdsTensor.close()
            styleTensor.close()
            speedTensor.close()
            results.close() // OrtSession.Result 닫기

            return audioData

        } catch (e: Exception) {
            // 오류 처리
            e.printStackTrace()
            return null
        } finally {
            // 세션 닫기
            session.close()
            // 필요하다면 OrtEnvironment 도 닫을 수 있지만, 앱 전체에서 공유하는 것이 일반적
            // ortEnvironment.close()
        }
    }

    /**
     * FloatArray 형식의 오디오 데이터를 WAV 파일로 저장합니다.
     *
     * @param context Context 객체
     * @param audioData FloatArray 형식의 오디오 데이터 (-1.0 ~ 1.0 범위 가정)
     * @param sampleRate 오디오 샘플링 레이트 (Hz) - 모델 출력에 맞게 수정 필요
     * @param numChannels 오디오 채널 수 (1: Mono, 2: Stereo) - 모델 출력에 맞게 수정 필요
     * @param outputFileName 저장할 파일 이름 (예: "output_audio.wav")
     * @return 저장 성공 시 File 객체, 실패 시 null
     */
    private fun saveAudioToFile(
        context: Context,
        audioData: FloatArray,
        sampleRate: Int = 24000, // 허깅페이스 샘플 코드와 동일하게 24000Hz 사용
        numChannels: Int = 1,     // 중요: 실제 모델의 채널 수로 변경하세요!
        outputFileName: String = "output_audio.wav"
    ): File? {
        val bitsPerSample: Short = 16 // 16-bit PCM으로 가정

        try {
            // 1. 저장 경로 설정 (앱별 외부 저장소의 Music 디렉토리)
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            if (storageDir == null || (!storageDir.exists() && !storageDir.mkdirs())) {
                println("Error: Cannot access external storage directory.")
                return null
            }
            val outputFile = File(storageDir, outputFileName)

            // 2. FloatArray -> ShortArray -> ByteArray 변환 (16-bit PCM), 16-bit PCM은 -32768과 32767(-2^15 ~ 2^15-1) 사이의 정수 값을 사용
            val shortData = ShortArray(audioData.size) // Short 2바이트 정수형
            for (i in audioData.indices) {
                // Float (-1.0 ~ 1.0) -> Short (-32768 ~ 32767)
                val shortVal = (audioData[i] * 32767.0f)
                    .coerceIn(-32768.0f, 32767.0f)
                    .toInt()
                    .toShort() // -1 ~ 1 범위에 32767 을 곱하여 16비트의 최대 표현 범위로 확장
                shortData[i] = shortVal
            }

            val byteBuffer = ByteBuffer.allocate(shortData.size * 2) // Short는 2바이트
                .order(ByteOrder.LITTLE_ENDIAN) // WAV는 보통 Little Endian
            byteBuffer.asShortBuffer().put(shortData)
            val pcmData = byteBuffer.array()

            // 3. WAV 헤더 생성 및 파일 쓰기
            FileOutputStream(outputFile).use { fileOutputStream ->
                DataOutputStream(fileOutputStream).use { dataOutputStream ->
                    writeWavHeader(
                        dataOutputStream,
                        pcmData.size,
                        sampleRate,
                        numChannels,
                        bitsPerSample
                    )
                    dataOutputStream.write(pcmData)
                }
            }

            println("Audio saved successfully to: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: IOException) {
            println("Error saving audio file: ${e.message}")
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            println("An unexpected error occurred during saving: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * WAV 파일 헤더를 DataOutputStream에 씁니다.
     */
    @Throws(IOException::class)
    private fun writeWavHeader(
        out: DataOutputStream,
        pcmDataSize: Int,
        sampleRate: Int,
        numChannels: Int,
        bitsPerSample: Short
    ) {
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = (numChannels * bitsPerSample / 8).toShort()
        val audioFormat: Short = 1 // PCM

        val headerSize = 44
        val fileSize = pcmDataSize + headerSize - 8

        // RIFF chunk
        out.writeBytes("RIFF")
        out.writeInt(Integer.reverseBytes(fileSize)) // ChunkSize (Little Endian)
        out.writeBytes("WAVE")

        // fmt sub-chunk
        out.writeBytes("fmt ")
        out.writeInt(Integer.reverseBytes(16)) // Subchunk1Size (16 for PCM)
        out.writeShort(java.lang.Short.reverseBytes(audioFormat).toInt()) // AudioFormat
        out.writeShort(java.lang.Short.reverseBytes(numChannels.toShort()).toInt()) // NumChannels
        out.writeInt(Integer.reverseBytes(sampleRate)) // SampleRate
        out.writeInt(Integer.reverseBytes(byteRate)) // ByteRate
        out.writeShort(java.lang.Short.reverseBytes(blockAlign).toInt()) // BlockAlign
        out.writeShort(java.lang.Short.reverseBytes(bitsPerSample).toInt()) // BitsPerSample

        // data sub-chunk
        out.writeBytes("data")
        out.writeInt(Integer.reverseBytes(pcmDataSize)) // Subchunk2Size
    }
}
