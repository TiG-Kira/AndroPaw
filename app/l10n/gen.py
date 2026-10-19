#!/usr/bin/env python3
"""Генерирует res/values*/strings.xml и res/xml/locales_config.xml из langs/*.py.
Запуск: python app/l10n/gen.py  (английский — язык по умолчанию, values/)."""
import os, re, runpy, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "res")
sys.path.insert(0, HERE)
from keys import KEYS  # noqa: E402

# код языка → каталог ресурсов Android
DIRS = {
    "en": "values", "ru": "values-ru", "uk": "values-uk", "de": "values-de", "fr": "values-fr",
    "es": "values-es", "pt-BR": "values-pt-rBR", "it": "values-it", "pl": "values-pl", "tr": "values-tr",
    "ar": "values-ar", "fa": "values-fa", "hi": "values-hi", "id": "values-in", "vi": "values-vi",
    "zh-CN": "values-zh-rCN", "ja": "values-ja", "ko": "values-ko",
}


def esc(s: str) -> str:
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if s.startswith("@") or s.startswith("?"):
        s = "\\" + s
    return s


def load(code):
    return runpy.run_path(os.path.join(HERE, "langs", code + ".py"))["S"]


# Один <string name="…">…</string> целиком (многострочные значения тоже захватываются).
ENTRY = re.compile(r'[ \t]*<string\b[^>]*\bname="([^"]+)"[^>]*>.*?</string>\n?', re.S)


def merge_with_existing(path, generated):
    """l10n-таблицы покрывают не все строки: часть UI-строк живёт только в
    values*/strings.xml (new_machine, vmos_*, m_*, as_* …). Раньше gen.py
    перезаписывал файл целиком и молча выбрасывал их — компилятор потом
    ругался 'Unresolved reference'. Теперь существующий файл — база,
    сгенерированные ключи перекрывают его, остальные дописываются."""
    merged = {}
    if os.path.isfile(path):
        for m in ENTRY.finditer(open(path, encoding="utf-8").read()):
            merged[m.group(1)] = m.group(0)
    for m in ENTRY.finditer("".join(line + "\n" for line in generated)):
        merged[m.group(1)] = m.group(0)
    return "".join(merged.values())


def main():
    en = load("en")
    missing = [k for k in KEYS if k not in en]
    assert not missing, f"en: нет ключей {missing}"
    for code, d in DIRS.items():
        s = load(code)
        lack = [k for k in KEYS if k not in s]
        extra = [k for k in s if k not in KEYS]
        if lack or extra:
            print(f"{code}: нет {lack} лишние {extra}")
        out = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- создано app/l10n/gen.py, правьте langs/*.py -->", "<resources>"]
        if code == "en":
            out.append('    <string name="app_name" translatable="false">AndroPaw</string>')
        for k in KEYS:
            if k in s:
                out.append(f'    <string name="{k}">{esc(s[k])}</string>')
        path = os.path.join(RES, d)
        os.makedirs(path, exist_ok=True)
        target = os.path.join(path, "strings.xml")
        # out[0] — <?xml …?>, out[1] — комментарий, out[2] — <resources>, далее строки
        body = merge_with_existing(target, out[3:])
        with open(target, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(out[:3]) + "\n" + body + "</resources>\n")
    tags = [c for c in DIRS]
    xml = ['<?xml version="1.0" encoding="utf-8"?>', '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">']
    xml += [f'    <locale android:name="{t}" />' for t in tags]
    xml.append("</locale-config>\n")
    os.makedirs(os.path.join(RES, "xml"), exist_ok=True)
    with open(os.path.join(RES, "xml", "locales_config.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(xml))
    print(f"готово: {len(DIRS)} языков, {len(KEYS)} строк")


if __name__ == "__main__":
    main()
