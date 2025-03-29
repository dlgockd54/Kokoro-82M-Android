# 필요한 라이브러리 임포트
import phonemizer # phonemizer 라이브러리 설치 필요 (pip install phonemizer), 추가적으로 brew install espeak-ng
import re         # 정규 표현식 라이브러리
import sys        # 표준 입력/출력/에러 스트림 사용

def split_num(num_match):
    num = num_match.group()
    if '.' in num:
        # 소수점 형식 확인 (예: 1.23)
        return num
    elif ':' in num:
        # 시간 형식 확인 (예: H:MM)
        try:
            h, m = [int(n) for n in num.split(':')]
            if m == 0:
                return f"{h} o'clock"
            elif m < 10:
                return f'{h} oh {m}'
            return f'{h} {m}'
        except ValueError:
            return num # ':'가 있지만 파싱 실패 시 원본 반환
    elif len(num) >= 4 and num[:4].isdigit():
        # 연도 형식 확인 (예: 1999, 2000s)
        try:
            year_part = num[:4]
            year = int(year_part)
            if year < 1100 or year % 1000 < 10: # 연도 범위 및 조건 확인
                return num
            left, right_part = year_part[:2], year_part[2:4]
            right = int(right_part)
            s = 's' if num.endswith('s') else ''
            if 100 <= year % 1000 <= 999: # 세 자리 연도 확인 (예: 1100 ~ 1999, 2100 ~ ?)
                if right == 0:
                    return f'{left} hundred{s}'
                elif right < 10:
                    return f'{left} oh {right}{s}'
            return f'{left} {right}{s}'
        except ValueError:
            return num # 연도 파싱 중 오류 발생 시 원본 반환
    return num # 위의 조건들에 맞지 않으면 원본 반환

def flip_money(m):
    match_str = m.group()
    bill = 'dollar' if match_str[0] == '$' else 'pound'
    value_part = match_str[1:].replace(',', '') # 콤마 제거

    # 예: '$1 million', '£5 thousand'
    suffix_match = re.search(r'\s+(hundred|thousand|million|billion|trillion)$', value_part, re.IGNORECASE)
    if suffix_match:
        suffix = suffix_match.group(1)
        num_part = value_part[:suffix_match.start()].strip()
        return f'{num_part} {suffix} {bill}s' # 항상 복수형으로 가정 (원본 코드 기반)

    # 예: '$1', '£5' (정수)
    if '.' not in value_part:
        try:
            amount = int(value_part)
            s = '' if amount == 1 else 's'
            return f'{amount} {bill}{s}'
        except ValueError:
            return match_str # 정수 변환 실패시 원본 반환
    # 예: '$1.50', '£10.99' (소수)
    else:
        try:
            b_str, c_str = value_part.split('.')
            b = int(b_str)
            s = '' if b == 1 else 's'
            c = int(c_str.ljust(2, '0')) # 센트/펜스 부분 2자리로 맞춤
            coins = f"cent{'' if c == 1 else 's'}" if match_str[0] == '$' else ('penny' if c == 1 else 'pence')
            return f'{b} {bill}{s} and {c} {coins}'
        except ValueError:
             return match_str # 소수 부분 파싱 실패시 원본 반환

def point_num(num_match):
    num_str = num_match.group()
    parts = num_str.split('.')
    if len(parts) == 2:
        a, b = parts
        # 숫자가 아닌 부분이 포함될 경우 처리 (예: 'version 3.1') - 원본 코드는 숫자만 가정
        if a.isdigit() and b.isdigit():
            return ' point '.join([a, ' '.join(b)])
    return num_str # 조건 불충족 시 원본 반환

def normalize_text(text):
    print(f"[Debug] Normalize Input type: {type(text)}, value: '{text}'", file=sys.stderr)
    if not isinstance(text, str):
         print("[Debug] Warning: Input is not a string, converting.", file=sys.stderr)
         text = str(text)

    try:
        # 기본적인 문자 교체
        text = text.replace(chr(8216), "'").replace(chr(8217), "'")
        text = text.replace('«', '"').replace('»', '"')
        text = text.replace(chr(8220), '"').replace(chr(8221), '"')
        print(f"[Debug] After basic replace: '{text}'", file=sys.stderr)

        # 아시아권 구두점 처리
        for p_asia, p_eng in zip('、。！，：；？', ',.!,:;?'):
            text = text.replace(p_asia, p_eng + ' ')
        print(f"[Debug] After Asia punct: '{text}'", file=sys.stderr)

        # 여러 종류의 공백 및 비표준 공백 처리
        text = re.sub(r'[^\S\n]+', ' ', text)
        text = re.sub(r' +', ' ', text)
        print(f"[Debug] After whitespace cleanup: '{text}'", file=sys.stderr)

        # 개행 사이 공백 제거
        text = re.sub(r'(?<=\n) +(?=\n)', '', text)

        # 약어 확장
        print("[Debug] Applying abbreviations...", file=sys.stderr)
        text = re.sub(r'\bDr\.(?= [A-Z])', 'Doctor', text)
        text = re.sub(r'\bMr\.(?= [A-Z])', 'Mister', text)
        text = re.sub(r'\bMs\.(?= [A-Z])', 'Miss', text)
        text = re.sub(r'\bMrs\.(?= [A-Z])', 'Mrs', text)
        text = re.sub(r'\betc\.(?![a-zA-Z])', 'etc', text)
        print(f"[Debug] After abbreviations: '{text}'", file=sys.stderr)

        # 구어체 표현
        print("[Debug] Applying yeah...", file=sys.stderr)
        text = re.sub(r'\b(y)eah?\b', r"\1e'a", text, flags=re.IGNORECASE)
        print(f"[Debug] After yeah: '{text}'", file=sys.stderr)

        # 숫자/시간/연도/통화/소수 처리
        print("[Debug] Applying flip_money...", file=sys.stderr)
        text = re.sub(r'(?i)[$£]\d{1,3}(?:,\d{3})*(?:\.\d+)?(?:\s+(?:hundred|thousand|million|billion|trillion))?\b|[$£]\d+(?:\.\d{1,2})?\b', flip_money, text)
        if not isinstance(text, str): raise TypeError(f"flip_money returned non-string: {type(text)}")
        print(f"[Debug] After flip_money: '{text}'", file=sys.stderr)

        print("[Debug] Applying split_num...", file=sys.stderr)
        text = re.sub(r'\b\d{1,2}:\d{2}\b|(?<!\.)\b\d{4}s?\b', split_num, text)
        if not isinstance(text, str): raise TypeError(f"split_num returned non-string: {type(text)}")
        print(f"[Debug] After split_num: '{text}'", file=sys.stderr)

        print("[Debug] Applying point_num...", file=sys.stderr)
        text = re.sub(r'\b\d+\.\d+\b', point_num, text)
        if not isinstance(text, str): raise TypeError(f"point_num returned non-string: {type(text)}")
        print(f"[Debug] After point_num: '{text}'", file=sys.stderr)

        # 기타 숫자 관련 처리
        print("[Debug] Applying other num replace...", file=sys.stderr)
        text = re.sub(r'(?<=\d),(?=\d)', '', text)
        text = re.sub(r'(?<=\d)-(?=\d)', ' to ', text)
        text = re.sub(r'(?<=\d)S', ' S', text)
        print(f"[Debug] After other num replace: '{text}'", file=sys.stderr)

        # 소유격/축약형 관련 처리
        print("[Debug] Applying possessives...", file=sys.stderr)
        text = re.sub(r"(?<=[BCDFGHJ-NP-TV-Z])'s\b", "'S", text)
        text = re.sub(r"(?<=X)'S\b", 's', text)
        print(f"[Debug] After possessives: '{text}'", file=sys.stderr)

        # 약어 내 점 처리
        print("[Debug] Applying abbr dots...", file=sys.stderr)
        text = re.sub(r'(?:[A-Z]\.){2,}', lambda m: m.group().replace('.', '-'), text)
        text = re.sub(r'(?<=[A-Z])\.(?=[A-Z])', '-', text, flags=re.IGNORECASE)
        print(f"[Debug] After abbr dots: '{text}'", file=sys.stderr)

        # 최종 정리
        print("[Debug] Final cleanup...", file=sys.stderr)
        text = re.sub(r' +', ' ', text)
        print(f"[Debug] Final normalized text: '{text.strip()}'", file=sys.stderr)
        return text.strip()

    except Exception as e:
        print(f"[Debug] Error occurred within normalize_text!", file=sys.stderr)
        print(f"[Debug] Current text value: '{text}'", file=sys.stderr)
        # 오류를 다시 발생시켜 traceback을 확인하도록 함
        raise e

def get_vocab():
    _pad = "$"
    _punctuation = ';:,.!?¡¿—…"«»“” ' # 공백 포함됨
    _letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    _letters_ipa = "ɑɐɒæɓʙβɔɕçɗɖðʤəɘɚɛɜɝɞɟʄɡɠɢʛɦɧħɥʜɨɪʝɭɬɫɮʟɱɯɰŋɳɲɴøɵɸθœɶʘɹɺɾɻʀʁɽʂʃʈʧʉʊʋⱱʌɣɤʍχʎʏʑʐʒʔʡʕʢǀǁǂǃˈˌːˑʼʴʰʱʲʷˠˤ˞↓↑→↗↘'̩'ᵻ"
    symbols = [_pad] + list(_punctuation) + list(_letters) + list(_letters_ipa)
    # 문자열이나 리스트로 변경하여 enumerate 사용
    symbol_string = "".join(symbols)
    dicts = {char: i for i, char in enumerate(symbol_string)}
    return dicts

VOCAB = get_vocab()

def tokenize(ps):
    # VOCAB 딕셔너리를 사용하여 음소 문자열(ps)을 토큰 ID 리스트로 변환
    # VOCAB에 없는 문자는 무시됨
    return [token_id for char in ps if (token_id := VOCAB.get(char)) is not None]

# Phonemizer 초기화 (시스템에 espeak-ng 설치 필요)
try:
    phonemizers = {
        'a': phonemizer.backend.EspeakBackend(language='en-us', preserve_punctuation=True, with_stress=True),
        'b': phonemizer.backend.EspeakBackend(language='en-gb', preserve_punctuation=True, with_stress=True),
    }
    print("Phonemizer backends initialized successfully.", file=sys.stderr)
except ImportError:
    print("Error: phonemizer library not found. Please install it (`pip install phonemizer`).", file=sys.stderr)
    sys.exit(1) # 프로그램 종료
except Exception as e:
    print(f"Error initializing phonemizer backend (espeak-ng). Is espeak-ng installed and accessible?", file=sys.stderr)
    print(f"Error details: {e}", file=sys.stderr)
    sys.exit(1) # 프로그램 종료

def phonemize(text, lang='a', norm=True):
    if not phonemizers:
        raise RuntimeError("Phonemizer backends are not initialized.")
    if lang not in phonemizers:
        raise RuntimeError(f"Phonemizer backend for language '{lang}' not available.")

    backend = phonemizers[lang]

    if norm:
        # 정규화 먼저 수행
        normalized_text = normalize_text(text)
        print(f"Normalized Text: \"{normalized_text}\"", file=sys.stderr) # 정규화 결과 확인용 출력
    else:
        normalized_text = text

    if not normalized_text: # 정규화 결과가 비어있으면 phonemize 호출 불필요
        return ""

    # phonemizer 실행
    ps_list = backend.phonemize([normalized_text], strip=True) # strip=True 추가 고려
    ps = ps_list[0] if ps_list else ''
    print(f"Raw Phonetic String: \"{ps}\"", file=sys.stderr) # 원본 음소 결과 확인용 출력

    # 후처리 로직 (kokoro.py 기준)
    ps = ps.replace('kəkˈoːɹoʊ', 'kˈoʊkəɹoʊ').replace('kəkˈɔːɹəʊ', 'kˈəʊkəɹəʊ')
    ps = ps.replace('ʲ', 'j').replace('r', 'ɹ').replace('x', 'k').replace('ɬ', 'l') # 문자 치환
    # 정규식 기반 후처리
    ps = re.sub(r'(?<=[a-zɹː])(?=hˈʌndɹɪd)', ' ', ps) # hundred 앞 공백 추가
    ps = re.sub(r' z(?=[;:,.!?¡¿—…"«»“” ]|$)', 'z', ps) # 문장 끝 z 처리

    # 언어별 후처리 (en-us)
    if lang == 'a':
        ps = re.sub(r'(?<=nˈaɪn)ti(?!ː)', 'di', ps) # 예: ninety -> ninedy

    # 최종 필터링 (VOCAB에 있는 문자만 남김)
    original_len = len(ps)
    ps = ''.join(filter(lambda p: p in VOCAB, ps))
    filtered_len = len(ps)
    if original_len != filtered_len:
         print(f"Filtered out {original_len - filtered_len} characters not in VOCAB.", file=sys.stderr)

    return ps.strip()


# --- 메인 실행 부분 ---

if __name__ == "__main__":
    # 1. 입력 텍스트와 언어 설정
    # 예시 텍스트:
    # input_text = "This costs $1.99."
    # input_text = "The year was 1984s."
    # input_text = "Mr. Smith arrived at 3:05." 
    # input_text = "Please generate tokens."
    # input_text = "Life is full of ups and downs. Live as if you were to die tomorrow. Believe in yourself. No sweat, No sweet."
    
    # 토큰 길이 261
    # input_text = "We assume you have received the usual lecture from the local System Administrator. It usually boils down to these three things: 1. Respect the privacy of others. 2. Think before you type. 3. With great power comes great responsibility."

    # 토큰 길이 589
    # input_text = "I have a dream that one day this nation will rise up and live out the true meaning of its creed, \"We hold these truths to be self-evident, that all men are created equal.\" I have a dream that one day on the red hills of Georgia, the sons of former slaves and the sons of former slave owners will be able to sit down together at the table of brotherhood. I have a dream that one day even the state of Mississippi, a state sweltering with the heat of injustice, sweltering with the heat of oppression, will be transformed into an oasis of freedom and justice."

    # 토큰 길이 379
    input_text = "I have a dream that one day this nation will rise up and live out the true meaning of its creed, \"We hold these truths to be self-evident, that all men are created equal.\" I have a dream that one day on the red hills of Georgia, the sons of former slaves and the sons of former slave owners will be able to sit down together at the table of brotherhood."

    language_code = 'a' # 'a' for en-us, 'b' for en-gb

    print(f"Processing Input Text: \"{input_text}\"", file=sys.stderr)
    print(f"Language Code: {language_code}", file=sys.stderr)
    print("-" * 30, file=sys.stderr) # 구분선

    # 2. Phonemize 및 Tokenize 실행
    try:
        # 텍스트를 음소 문자열로 변환
        phonetic_string = phonemize(input_text, lang=language_code, norm=True)
        print(f"Final Phonetic String for Tokenization: \"{phonetic_string}\"", file=sys.stderr)

        # 음소 문자열을 토큰 ID 리스트로 변환
        tokens = tokenize(phonetic_string)

        # 3. 최종 결과 (토큰 배열) 출력
        # 표준 출력(stdout)으로는 오직 토큰 배열만 출력합니다.
        # 다른 정보성 메시지는 표준 에러(stderr)로 출력했습니다.
        print()
        print("변환된 토큰: ")
        print(tokens)

    except RuntimeError as e:
        print(f"\nError during processing: {e}", file=sys.stderr)
        sys.exit(1) # 오류 시 종료
    except Exception as e:
        print(f"\nAn unexpected error occurred: {e}", file=sys.stderr)
        sys.exit(1) # 오류 시 종료
