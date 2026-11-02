package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private static final int[] WALLPAPER_DIMS = {0, 20, 40, 60};
    private final TextView[] dimChoices = new TextView[WALLPAPER_DIMS.length];
    private static final int[] SKY_INTENSITIES = {0, 30, 60, 100};
    private final TextView[] skyChoices = new TextView[SKY_INTENSITIES.length];

    private TextView wallpaperChoice;
    private TextView introVideoChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView focusOvershootChoice;
    private TextView alwaysShowNamesChoice;
    private TextView showStatsChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;

    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private int wallpaperDim;
    private int skyIntensity;
    private boolean introVideo;
    private boolean animations;
    private boolean advancedEffects;
    private boolean focusOvershoot;
    private boolean alwaysShowNames;
    private boolean showStats;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);

        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });

        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        wallpaperDim = config.wallpaperDim;
        skyIntensity = config.skyIntensity;
        introVideo = config.introVideo;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        focusOvershoot = config.focusOvershoot;
        alwaysShowNames = config.alwaysShowNames;
        showStats = config.showStats;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }

        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }

        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }

        int[] dimIds = {R.id.dim0, R.id.dim20, R.id.dim40, R.id.dim60};
        for (int i = 0; i < dimIds.length; i++) {
            final int value = WALLPAPER_DIMS[i];
            dimChoices[i] = findViewById(dimIds[i]);
            dimChoices[i].setOnClickListener(v -> {
                if (wallpaperDim == value) return;
                wallpaperDim = value;
                choices.edit().putInt(Config.WALLPAPER_DIM_KEY, value).apply();
                changed();
                refresh();
            });
        }

        int[] skyIds = {R.id.sky0, R.id.sky30, R.id.sky60, R.id.sky100};
        for (int i = 0; i < skyIds.length; i++) {
            final int value = SKY_INTENSITIES[i];
            skyChoices[i] = findViewById(skyIds[i]);
            skyChoices[i].setOnClickListener(v -> {
                if (skyIntensity == value) return;
                skyIntensity = value;
                choices.edit().putInt(Config.SKY_INTENSITY_KEY, value).apply();
                changed();
                refresh();
            });
        }

        // Keep the numeric choices increasing in the Hebrew reading direction.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
            reverseRow(dimChoices);
            reverseRow(skyChoices);
        }

        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());

        introVideoChoice = findViewById(R.id.introVideoChoice);
        introVideoChoice.setOnClickListener(v -> {
            introVideo = !introVideo;
            choices.edit().putBoolean(Config.INTRO_VIDEO_KEY, introVideo).apply();
            changed();
            refresh();
        });

        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });

        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        focusOvershootChoice = findViewById(R.id.focusOvershootChoice);
        focusOvershootChoice.setOnClickListener(v -> {
            focusOvershoot = !focusOvershoot;
            choices.edit().putBoolean(Config.FOCUS_OVERSHOOT_KEY, focusOvershoot).apply();
            changed();
            refresh();
        });

        alwaysShowNamesChoice = findViewById(R.id.alwaysShowNamesChoice);
        alwaysShowNamesChoice.setOnClickListener(v -> {
            alwaysShowNames = !alwaysShowNames;
            choices.edit().putBoolean(Config.ALWAYS_SHOW_NAMES_KEY, alwaysShowNames).apply();
            changed();
            refresh();
        });

        showStatsChoice = findViewById(R.id.showStatsChoice);
        showStatsChoice.setOnClickListener(v -> {
            showStats = !showStats;
            choices.edit().putBoolean(Config.SHOW_STATS_KEY, showStats).apply();
            changed();
            refresh();
        });

        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }

        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY)
                    .remove(Config.INTRO_VIDEO_KEY).remove(Config.ALWAYS_SHOW_NAMES_KEY)
                    .remove(Config.SHOW_STATS_KEY).remove(Config.SKY_INTENSITY_KEY)
                    .remove(Config.FOCUS_OVERSHOOT_KEY).remove(Config.WALLPAPER_DIM_KEY).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            wallpaperDim = current.wallpaperDim;
            skyIntensity = current.skyIntensity;
            introVideo = current.introVideo;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            focusOvershoot = current.focusOvershoot;
            alwaysShowNames = current.alwaysShowNames;
            showStats = current.showStats;
            changed();
            refresh();
        });

        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });

        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));

        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        List<File> prabhasFiles = WallpaperPlayer.ensurePrabhasWallpapers(this);
        File dir = Config.dir(this);
        List<File> themeImages = dir == null ? Collections.emptyList()
                : WallpaperRotation.images(new File(dir, "wallpapers"));
        List<File> images = new ArrayList<>(prabhasFiles);
        for (File f : themeImages) {
            if (!images.contains(f)) images.add(f);
        }
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile() && !images.contains(single)) images.add(0, single);

        String[] labels = new String[images.size() + 2];
        labels[0] = getString(R.string.wallpaper_theme_default);
        for (int i = 0; i < images.size(); i++) {
            String fname = images.get(i).getName();
            String title = fname;
            for (int p = 0; p < WallpaperPlayer.PRABHAS_WALLPAPERS.length; p++) {
                if (WallpaperPlayer.PRABHAS_WALLPAPERS[p].equals(fname)) {
                    title = WallpaperPlayer.PRABHAS_TITLES[p];
                    break;
                }
            }
            labels[i + 1] = title;
        }
        labels[images.size() + 1] = getString(R.string.wallpaper_none);

        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = 0;
        if ("none".equals(current)) {
            checked = labels.length - 1;
        } else if (current != null) {
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).getName().equals(current)) {
                    checked = i + 1;
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    } else if (which == labels.length - 1) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which - 1).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        for (int i = 0; i < dimChoices.length; i++) {
            dimChoices[i].setSelected(wallpaperDim == WALLPAPER_DIMS[i]);
        }
        for (int i = 0; i < skyChoices.length; i++) {
            skyChoices[i].setSelected(skyIntensity == SKY_INTENSITIES[i]);
        }

        introVideoChoice.setText(getString(R.string.intro_video_state,
                getString(introVideo ? R.string.on : R.string.off)));
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);

        focusOvershootChoice.setText(getString(R.string.focus_overshoot_state,
                getString(focusOvershoot ? R.string.on : R.string.off)));
        alwaysShowNamesChoice.setText(getString(R.string.always_show_names_state,
                getString(alwaysShowNames ? R.string.on : R.string.off)));
        showStatsChoice.setText(getString(R.string.show_stats_state,
                getString(showStats ? R.string.on : R.string.off)));

        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        String label = getString(R.string.wallpaper_theme_default);
        if ("none".equals(wallpaperCurrent)) {
            label = getString(R.string.wallpaper_none);
        } else if (wallpaperCurrent != null) {
            for (int p = 0; p < WallpaperPlayer.PRABHAS_WALLPAPERS.length; p++) {
                if (WallpaperPlayer.PRABHAS_WALLPAPERS[p].equals(wallpaperCurrent)) {
                    label = WallpaperPlayer.PRABHAS_TITLES[p];
                    break;
                }
            }
            if (label.equals(getString(R.string.wallpaper_theme_default))) {
                label = wallpaperCurrent;
            }
        }
        wallpaperChoice.setText(getString(R.string.wallpaper_state, label));

        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains(Config.INTRO_VIDEO_KEY)
                || choices.contains(Config.ALWAYS_SHOW_NAMES_KEY) || choices.contains(Config.SHOW_STATS_KEY)
                || choices.contains(Config.SKY_INTENSITY_KEY) || choices.contains(Config.FOCUS_OVERSHOOT_KEY)
                || choices.contains(Config.WALLPAPER_DIM_KEY);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
