# Kokoro-82M-Android
## Speech로 변환할 텍스트 토큰화하기
python 디렉터리의 generate_tokens.py 파일 내용에 변환하고 싶은 텍스트를 넣고 실행하여 토큰 배열을 얻을 수 있습니다.

generate_tokens.py 파일을 실행하기 위해서는 phonemizer, espeak-ng를 설치 해야합니다.

```shell
pip install phonemizer
brew install espeak-ng
```

## ONNX 모델 선택하기
assets/onnx 경로의 onnx 파일 중 추론에 사용할 모델 파일을 고르면 됩니다.

추가로, 모델의 크기가 커서 git에 추가할 수 없는 파일이 있습니다.  
[여기](https://huggingface.co/onnx-community/Kokoro-82M-ONNX/tree/main/onnx)서 받으세요.

## 음성 스타일 모델 선택하기
assets/voices 경로의 bin 파일 중 speech로 변환하기 위해 사용할 음성 모델 파일을 고르면 됩니다.

[여기](https://huggingface.co/onnx-community/Kokoro-82M-ONNX/tree/main/voices)서 추가 모델을 찾을 수 있습니다.
