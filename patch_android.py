"""Après `npx cap add android` : icônes, bannière TV, manifeste, lecteur natif ExoPlayer."""
import os, re, shutil
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = HERE
ANDROID = os.path.join(ROOT, 'android')
APP = os.path.join(ANDROID, 'app')
MAIN = os.path.join(APP, 'src', 'main')
RES = os.path.join(MAIN, 'res')
MANIFEST = os.path.join(MAIN, 'AndroidManifest.xml')
JAVA_DIR = os.path.join(MAIN, 'java', 'com', 'jbmtv', 'app')
BG = (3, 36, 194)
logo = Image.open(os.path.join(ROOT, 'resources', 'logo.png')).convert('RGBA')

# ---------- icônes ----------
legacy = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
foreground = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}
for d, size in legacy.items():
    folder = os.path.join(RES, 'mipmap-' + d)
    if not os.path.isdir(folder):
        continue
    logo.resize((size, size), Image.LANCZOS).save(os.path.join(folder, 'ic_launcher.png'))
    mask = Image.new('L', (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    rnd = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    rnd.paste(logo.resize((size, size), Image.LANCZOS), (0, 0), mask)
    rnd.save(os.path.join(folder, 'ic_launcher_round.png'))
    fs = foreground[d]
    fg = Image.new('RGBA', (fs, fs), (0, 0, 0, 0))
    inner = int(fs * 0.68)
    fg.paste(logo.resize((inner, inner), Image.LANCZOS), ((fs - inner) // 2, (fs - inner) // 2))
    fg.save(os.path.join(folder, 'ic_launcher_foreground.png'))

bgxml = os.path.join(RES, 'values', 'ic_launcher_background.xml')
if os.path.exists(bgxml):
    with open(bgxml, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#0324C2</color>\n</resources>\n')

# ---------- bannière Android TV ----------
os.makedirs(os.path.join(RES, 'drawable'), exist_ok=True)
banner = Image.new('RGB', (320, 180), BG)
px = banner.load()
for y in range(180):
    for x in range(320):
        t = (x + y) / 500
        px[x, y] = (int(1 + 2 * t), int(80 - 44 * t), int(251 - 57 * t))
lg = logo.resize((140, 140), Image.LANCZOS)
banner.paste(lg, (90, 20), lg)
banner.save(os.path.join(RES, 'drawable', 'banner.png'))

# ---------- manifeste ----------
with open(MANIFEST, encoding='utf-8') as f:
    m = f.read()
if 'android.software.leanback' not in m:
    feats = ('    <uses-feature android:name="android.software.leanback" android:required="false" />\n'
             '    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />\n\n    ')
    m = m.replace('<application', feats + '<application', 1)
if 'android:banner' not in m:
    m = m.replace('<application', '<application\n        android:banner="@drawable/banner"', 1)
if 'usesCleartextTraffic' not in m:
    m = m.replace('<application', '<application\n        android:usesCleartextTraffic="true"', 1)
if 'LEANBACK_LAUNCHER' not in m:
    m = re.sub(r'(<category android:name="android.intent.category.LAUNCHER"\s*/>)',
               r'\1\n                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />', m, count=1)
if 'screenOrientation' not in m:
    m = m.replace('<activity', '<activity\n            android:screenOrientation="sensorLandscape"', 1)
with open(MANIFEST, 'w', encoding='utf-8') as f:
    f.write(m)

# ---------- lecteur natif ExoPlayer ----------
os.makedirs(JAVA_DIR, exist_ok=True)
for name in ('MainActivity.java', 'JbmPlayerPlugin.java'):
    shutil.copyfile(os.path.join(HERE, name), os.path.join(JAVA_DIR, name))

gradle = os.path.join(APP, 'build.gradle')
with open(gradle, encoding='utf-8') as f:
    g = f.read()
if 'media3-exoplayer' not in g:
    deps = ('\n    implementation "androidx.media3:media3-exoplayer:1.3.1"'
            '\n    implementation "androidx.media3:media3-exoplayer-hls:1.3.1"'
            '\n    implementation "androidx.media3:media3-datasource:1.3.1"'
            '\n    implementation "androidx.media3:media3-ui:1.3.1"')
    g, n = re.subn(r'(\ndependencies\s*\{)', r'\1' + deps.replace('\\', '\\\\'), g, count=1)
    if n != 1:
        raise SystemExit('Bloc dependencies introuvable dans build.gradle')
    with open(gradle, 'w', encoding='utf-8') as f:
        f.write(g)

print('Icônes, bannière TV, manifeste et lecteur natif prêts.')
