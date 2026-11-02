param(
    [Parameter(Mandatory = $true)]
    [string]$SourceRoot
)

$ErrorActionPreference = 'Stop'
$SourceRoot = (Resolve-Path $SourceRoot).Path
$player = Join-Path $SourceRoot 'launcher\app\src\main\java\com\gathin\tvhome\WallpaperPlayer.java'
$assetsSource = Join-Path $PSScriptRoot 'prabhas-wallpapers'
$assetsTarget = Join-Path $SourceRoot 'launcher\app\src\main\assets\prabhas-wallpapers'

if (-not (Test-Path $player)) {
    throw "WallpaperPlayer.java not found at '$player'. Extract the official CleanYourTV v3.0.0 source ZIP first and pass its root folder."
}
if (-not (Test-Path $assetsSource)) {
    throw "Patch assets folder not found: $assetsSource"
}

$code = [System.IO.File]::ReadAllText($player)
$newline = if ($code.Contains("`r`n")) { "`r`n" } else { "`n" }
$code = $code.Replace("`r`n", "`n")
$original = $code

function Replace-ExactlyOnce([string]$Text, [string]$Old, [string]$New, [string]$Description) {
    $count = ([regex]::Matches($Text, [regex]::Escape($Old))).Count
    if ($count -ne 1) {
        throw "Expected exactly one match for '$Description', found $count. This patch targets CleanYourTV v3.0.0; no Java changes have been written."
    }
    return $Text.Replace($Old, $New)
}

$code = Replace-ExactlyOnce $code 'files = new ArrayList<>(config.wallpapers);' 'files = loadPrabhasWallpapers(context, config.wallpapers);' 'wallpaper collection source'
$code = Replace-ExactlyOnce $code 'interval = config.wallpaperRotationMinutes * 60_000L;' 'interval = 0L; // Background changes only when Home opens; no timed slideshow.' 'rotation interval'
$code = Replace-ExactlyOnce $code 'if (config.wallpaperEnabled && !files.isEmpty()) {' 'if (!files.isEmpty()) {' 'wallpaper enabled condition'
$oldFirst = @'
        File first = WallpaperRotation.find(playable, previous);
        if (first == null) first = WallpaperRotation.pick(playable, previous, order, random);
'@
$newFirst = @'
        File first = WallpaperRotation.pick(playable, previous, random);
'@
$code = Replace-ExactlyOnce $code $oldFirst $newFirst 'initial wallpaper selection'

$oldFields = @'
    private boolean active;
    private boolean closed;
    private boolean loading;
    private String displayed = "";
'@
$newFields = @'
    private boolean active;
    private boolean closed;
    private boolean loading;
    /** Constructor has already displayed the first random wallpaper before the first onResume. */
    private boolean startedOnce;
    private String displayed = "";
'@
$code = Replace-ExactlyOnce $code $oldFields $newFields 'wallpaper state fields'

$oldStart = @'
    void start() {
        active = true;
        update();
    }
'@
$newStart = @'
    void start() {
        active = true;
        // showFirst() already chose the first picture during construction. Every later return to Home
        // chooses another picture, excluding the one shown before; update() never drives a timed slideshow.
        if (!startedOnce) {
            startedOnce = true;
            return;
        }
        showNextOnHomeOpen();
    }

    private void showNextOnHomeOpen() {
        if (!active || closed || loading || files.isEmpty()) return;
        String previous = preferences.getString("image", "");
        File next = WallpaperRotation.pick(playable(), previous, random);
        if (next == null || next.getName().equals(displayed)) return;
        loading = true;
        decoder.execute(() -> {
            Bitmap bitmap = decode(next);
            root.post(() -> {
                loading = false;
                if (closed || !active) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                if (bitmap == null) {
                    files.remove(next);
                    showNextOnHomeOpen();
                    return;
                }
                show(bitmap);
                displayed = next.getName();
                showCaption(false);
                preferences.edit().putString("image", displayed)
                        .putLong("changed", System.currentTimeMillis()).apply();
            });
        });
    }
'@
$code = Replace-ExactlyOnce $code $oldStart $newStart 'Home-entry wallpaper selection'

$helper = @'
    /** Bundled Prabhas wallpapers replace the theme's wallpaper collection, without changing any launcher UI. */
    private static List<File> loadPrabhasWallpapers(Context context, List<File> fallback) {
        String[] names = {
                "prabhas-01-bahubali.jpg",
                "prabhas-02-bahubali.jpg",
                "prabhas-03-salaar.jpg",
                "prabhas-04-fauzi.jpg",
                "prabhas-05-fauzi.jpg"
        };
        File directory = new File(context.getCacheDir(), "prabhas-home-wallpapers");
        if (!directory.isDirectory() && !directory.mkdirs()) return new ArrayList<>(fallback);
        List<File> result = new ArrayList<>();
        for (String name : names) {
            File target = new File(directory, name);
            if (!target.isFile() || target.length() == 0) {
                try (java.io.InputStream in = context.getAssets().open("prabhas-wallpapers/" + name);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                } catch (java.io.IOException failure) {
                    target.delete();
                    continue;
                }
            }
            if (target.isFile() && target.length() > 0) result.add(target);
        }
        return result.size() == names.length ? result : new ArrayList<>(fallback);
    }

'@
$marker = '    /**' + "`r`n" + '     * The picture Home opens with is decoded right here'
if (-not $code.Contains($marker)) {
    $marker = '    /**' + "`n" + '     * The picture Home opens with is decoded right here'
}
if (-not $code.Contains($marker)) {
    throw "Could not locate the showFirst documentation marker; no Java changes have been written."
}
$code = $code.Replace($marker, $helper + $marker)

if ($code -eq $original) {
    throw "No changes were applied. The source may already be patched."
}

New-Item -ItemType Directory -Force -Path $assetsTarget | Out-Null
Copy-Item -Path (Join-Path $assetsSource '*') -Destination $assetsTarget -Force
$code = $code.Replace("`n", $newline)
[System.IO.File]::WriteAllText($player, $code, [System.Text.UTF8Encoding]::new($false))

Write-Host 'Prabhas wallpaper patch applied.'
Write-Host "Java file: $player"
Write-Host "Bundled images: $assetsTarget"
Write-Host 'Only WallpaperPlayer.java and the five wallpaper assets were added/changed.'
