package com.spaceline.client.engine;

import com.spaceline.common.module.ModuleManager;
import com.spaceline.client.module.hud.ArmorBarModule;
import com.spaceline.client.module.hud.ArmorStatusModule;
import com.spaceline.client.module.hud.AttackIndicatorModule;
import com.spaceline.client.module.hud.BossBarModule;
import com.spaceline.client.module.hud.ComboDisplayModule;
import com.spaceline.client.module.hud.CoordinatesModule;
import com.spaceline.client.module.hud.CpsModule;
import com.spaceline.client.module.hud.CustomCrosshairModule;
import com.spaceline.client.module.hud.DamageIndicatorModule;
import com.spaceline.client.module.hud.DeathInfoModule;
import com.spaceline.client.module.hud.DirectionModule;
import com.spaceline.client.module.hud.FpsModule;
import com.spaceline.client.module.hud.HeartsModule;
import com.spaceline.client.module.hud.HitIndicatorModule;
import com.spaceline.client.module.hud.ItemCounterModule;
import com.spaceline.client.module.hud.KeystrokesModule;
import com.spaceline.client.module.hud.MousestrokesModule;
import com.spaceline.client.module.hud.PackDisplayModule;
import com.spaceline.client.module.hud.PingModule;
import com.spaceline.client.module.hud.PlaytimeModule;
import com.spaceline.client.module.hud.PotionEffectsModule;
import com.spaceline.client.module.hud.ReachDisplayModule;
import com.spaceline.client.module.hud.SaturationModule;
import com.spaceline.client.module.hud.ScoreboardModule;
import com.spaceline.client.module.hud.ServerAddressModule;
import com.spaceline.client.module.hud.SpeedMeterModule;
import com.spaceline.client.module.hud.StopwatchModule;
import com.spaceline.client.module.hud.SystemResourcesModule;
import com.spaceline.client.module.hud.TablistModule;
import com.spaceline.client.module.hud.TierTaggerModule;
import com.spaceline.client.module.hud.TimeModule;
import com.spaceline.client.module.hud.TntTimerModule;
import com.spaceline.client.module.hud.TotemCounterModule;
import com.spaceline.client.module.hud.TpsModule;
import com.spaceline.client.module.hud.UhcOverlayModule;
import com.spaceline.client.module.movement.JumpResetModule;
import com.spaceline.client.module.movement.ToggleSprintModule;
import com.spaceline.client.module.performance.CullLogsModule;
import com.spaceline.client.module.social.HypixelUtilitiesModule;
import com.spaceline.client.module.social.NickHiderModule;
import com.spaceline.client.module.social.TeamTrackerModule;
import com.spaceline.client.module.social.VoiceModule;
import com.spaceline.client.module.utility.AutoPerspectiveModule;
import com.spaceline.client.module.utility.AutoTextModule;
import com.spaceline.client.module.utility.AutohideHudModule;
import com.spaceline.client.module.utility.BackupsModule;
import com.spaceline.client.module.utility.CameraModule;
import com.spaceline.client.module.utility.DropPreventionModule;
import com.spaceline.client.module.utility.FovChangerModule;
import com.spaceline.client.module.utility.InventoryModule;
import com.spaceline.client.module.utility.ItemDespawnModule;
import com.spaceline.client.module.utility.KeybindSearchModule;
import com.spaceline.client.module.utility.PerspectiveModule;
import com.spaceline.client.module.utility.ReconnectModule;
import com.spaceline.client.module.utility.ScreenshotToolsModule;
import com.spaceline.client.module.utility.SnaplookModule;
import com.spaceline.client.module.utility.SoundFiltersModule;
import com.spaceline.client.module.utility.UiScalingModule;
import com.spaceline.client.module.utility.ZoomModule;
import com.spaceline.client.module.visual.AnimationsModule;
import com.spaceline.client.module.visual.BlockIndicatorModule;
import com.spaceline.client.module.visual.BlockOverlayModule;
import com.spaceline.client.module.visual.ColorSaturationModule;
import com.spaceline.client.module.visual.CustomAdvancementsModule;
import com.spaceline.client.module.visual.CustomFogModule;
import com.spaceline.client.module.visual.DarkModeModule;
import com.spaceline.client.module.visual.FullbrightModule;
import com.spaceline.client.module.visual.ItemInfoModule;
import com.spaceline.client.module.visual.ItemPhysicsModule;
import com.spaceline.client.module.visual.LootBeamsModule;
import com.spaceline.client.module.visual.MobOverlayModule;
import com.spaceline.client.module.visual.MotionBlurModule;
import com.spaceline.client.module.visual.NametagsModule;
import com.spaceline.client.module.visual.PlayerModelModule;
import com.spaceline.client.module.visual.ShulkerTooltipsModule;
import com.spaceline.client.module.visual.SubtitlesModule;
import com.spaceline.client.module.visual.TitleTweakerModule;
import com.spaceline.client.module.visual.ToastControlModule;
import com.spaceline.client.module.visual.TooltipsModule;
import com.spaceline.client.module.visual.ViewModelModule;
import com.spaceline.client.module.world.HorsesModule;
import com.spaceline.client.module.world.LightLevelOverlayModule;
import com.spaceline.client.module.world.TimeChangerModule;
import com.spaceline.client.module.world.WaypointsModule;
import com.spaceline.client.module.world.WeatherChangerModule;

/**
 * Registers the complete Space~line client module catalogue with a
 * {@link ModuleManager}. Generated to stay in lock-step with the module
 * classes on disk; every feature listed in the project spec is wired here so the
 * mod menu, keybinds and config persistence pick it up automatically.
 *
 * <p>There are 85 modules across all seven categories.
 */
public final class ClientModules {

    private ClientModules() {
    }

    /** Registers every built-in module. */
    public static void registerAll(ModuleManager manager) {
        manager.register(new ArmorBarModule());
        manager.register(new ArmorStatusModule());
        manager.register(new AttackIndicatorModule());
        manager.register(new BossBarModule());
        manager.register(new ComboDisplayModule());
        manager.register(new CoordinatesModule());
        manager.register(new CpsModule());
        manager.register(new CustomCrosshairModule());
        manager.register(new DamageIndicatorModule());
        manager.register(new DeathInfoModule());
        manager.register(new DirectionModule());
        manager.register(new FpsModule());
        manager.register(new HeartsModule());
        manager.register(new HitIndicatorModule());
        manager.register(new ItemCounterModule());
        manager.register(new KeystrokesModule());
        manager.register(new MousestrokesModule());
        manager.register(new PackDisplayModule());
        manager.register(new PingModule());
        manager.register(new PlaytimeModule());
        manager.register(new PotionEffectsModule());
        manager.register(new ReachDisplayModule());
        manager.register(new SaturationModule());
        manager.register(new ScoreboardModule());
        manager.register(new ServerAddressModule());
        manager.register(new SpeedMeterModule());
        manager.register(new StopwatchModule());
        manager.register(new SystemResourcesModule());
        manager.register(new TablistModule());
        manager.register(new TierTaggerModule());
        manager.register(new TimeModule());
        manager.register(new TntTimerModule());
        manager.register(new TotemCounterModule());
        manager.register(new TpsModule());
        manager.register(new UhcOverlayModule());
        manager.register(new JumpResetModule());
        manager.register(new ToggleSprintModule());
        manager.register(new CullLogsModule());
        manager.register(new HypixelUtilitiesModule());
        manager.register(new NickHiderModule());
        manager.register(new TeamTrackerModule());
        manager.register(new VoiceModule());
        manager.register(new AutoPerspectiveModule());
        manager.register(new AutoTextModule());
        manager.register(new AutohideHudModule());
        manager.register(new BackupsModule());
        manager.register(new CameraModule());
        manager.register(new DropPreventionModule());
        manager.register(new FovChangerModule());
        manager.register(new InventoryModule());
        manager.register(new ItemDespawnModule());
        manager.register(new KeybindSearchModule());
        manager.register(new PerspectiveModule());
        manager.register(new ReconnectModule());
        manager.register(new ScreenshotToolsModule());
        manager.register(new SnaplookModule());
        manager.register(new SoundFiltersModule());
        manager.register(new UiScalingModule());
        manager.register(new ZoomModule());
        manager.register(new AnimationsModule());
        manager.register(new BlockIndicatorModule());
        manager.register(new BlockOverlayModule());
        manager.register(new ColorSaturationModule());
        manager.register(new CustomAdvancementsModule());
        manager.register(new CustomFogModule());
        manager.register(new DarkModeModule());
        manager.register(new FullbrightModule());
        manager.register(new ItemInfoModule());
        manager.register(new ItemPhysicsModule());
        manager.register(new LootBeamsModule());
        manager.register(new MobOverlayModule());
        manager.register(new MotionBlurModule());
        manager.register(new NametagsModule());
        manager.register(new PlayerModelModule());
        manager.register(new ShulkerTooltipsModule());
        manager.register(new SubtitlesModule());
        manager.register(new TitleTweakerModule());
        manager.register(new ToastControlModule());
        manager.register(new TooltipsModule());
        manager.register(new ViewModelModule());
        manager.register(new HorsesModule());
        manager.register(new LightLevelOverlayModule());
        manager.register(new TimeChangerModule());
        manager.register(new WaypointsModule());
        manager.register(new WeatherChangerModule());
    }

    /** The total number of built-in modules. */
    public static int count() {
        return 85;
    }
}
