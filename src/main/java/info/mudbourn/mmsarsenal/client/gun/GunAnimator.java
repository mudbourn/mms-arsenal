package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsrendercommon.client.geo.BonePose;
import info.mudbourn.mmsrendercommon.client.geo.GeoAnimation;
import info.mudbourn.mmsrendercommon.client.geo.MolangScope;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// One gun's animation controller with GeckoLib 4 semantics: a queue of stages, loop, hold and play-once modes, and keyframe events.
public final class GunAnimator {

    // How a stage ends: loop forever, freeze on its last frame, or move to the next stage.
    public enum Loop {
        LOOP,
        HOLD_ON_LAST_FRAME,
        PLAY_ONCE
    }

    public enum State {
        RUNNING,
        PAUSED,
        STOPPED
    }

    // One named animation in a sequence and how it ends.
    public record Stage(String name, Loop loop) {
    }

    // A sequence of stages, like GeckoLib's RawAnimation; equal sequences do not restart each other.
    public record Sequence(List<Stage> stages) {

        public static Sequence of(Stage... stages) {
            return new Sequence(List.of(stages));
        }
    }

    private final Map<String, GeoAnimation> animations;
    private final Deque<Stage> queue = new ArrayDeque<>();
    private Sequence sequence;
    private Stage stage;
    private GeoAnimation animation;
    private State state = State.STOPPED;
    private double stageStart;
    private double lastSeconds;
    private float speed = 1.0F;
    private boolean forceReset;

    public GunAnimator(Map<String, GeoAnimation> animations) {
        this.animations = animations;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    // Restarts the current sequence the next time it is set, as GeckoLib's forceAnimationReset does.
    public void forceReset() {
        this.forceReset = true;
    }

    // Clears the controller so the next sequence starts fresh.
    public void reset() {
        this.sequence = null;
        this.stage = null;
        this.animation = null;
        this.state = State.STOPPED;
        this.queue.clear();
    }

    // Sets the sequence to play, switching at once unless it is already the one playing.
    public void set(Sequence next, double nowTicks) {
        if (next.equals(this.sequence) && !this.forceReset) {
            return;
        }
        this.forceReset = false;
        this.sequence = next;
        this.queue.clear();
        this.queue.addAll(next.stages());
        this.startNext(nowTicks);
    }

    public boolean hasCurrent() {
        return this.stage != null;
    }

    public boolean isPlaying(String name) {
        return this.stage != null && this.stage.name().equals(name);
    }

    public boolean finished() {
        return this.state == State.STOPPED;
    }

    public State state() {
        return this.state;
    }

    // Advances the timeline to now, firing keyframe events crossed on the way, and returns every animated bone's pose.
    public Map<String, BonePose> sample(double nowTicks, MolangScope scope, Consumer<String> sounds, Consumer<String> effects) {
        Map<String, BonePose> poses = new HashMap<>();
        if (this.animation == null) {
            return poses;
        }
        double seconds = this.seconds(nowTicks);
        if (this.state == State.RUNNING && seconds >= this.animation.length()) {
            this.fireCues(this.lastSeconds, this.animation.length(), sounds, effects);
            switch (this.stage.loop()) {
                case LOOP -> {
                    this.stageStart = nowTicks;
                    this.lastSeconds = -1.0;
                    seconds = 0.0;
                }
                case HOLD_ON_LAST_FRAME -> this.state = State.PAUSED;
                case PLAY_ONCE -> {
                    if (this.queue.isEmpty()) {
                        this.state = State.STOPPED;
                    } else {
                        this.startNext(nowTicks);
                        seconds = 0.0;
                    }
                }
            }
        }
        if (this.animation == null || this.state == State.STOPPED) {
            return poses;
        }
        if (this.state == State.PAUSED) {
            seconds = this.animation.length();
        } else {
            this.fireCues(this.lastSeconds, seconds, sounds, effects);
            this.lastSeconds = seconds;
        }
        float time = (float) Math.min(seconds, this.animation.length());
        for (String bone : this.animation.animatedBones()) {
            BonePose pose = this.animation.sampleAt(bone, time, scope);
            if (pose != null) {
                poses.put(bone, pose);
            }
        }
        return poses;
    }

    private double seconds(double nowTicks) {
        return (nowTicks - this.stageStart) * this.speed / 20.0;
    }

    private void startNext(double nowTicks) {
        this.stage = this.queue.poll();
        this.animation = this.stage == null ? null : this.animations.get(this.stage.name());
        this.stageStart = nowTicks;
        this.lastSeconds = -1.0;
        this.state = this.animation == null ? State.STOPPED : State.RUNNING;
    }

    private void fireCues(double from, double to, Consumer<String> sounds, Consumer<String> effects) {
        List<String> crossedSounds = new ArrayList<>();
        for (GeoAnimation.SoundCue cue : this.animation.sounds()) {
            if (cue.time() > from && cue.time() <= to) {
                crossedSounds.add(cue.sound().getPath());
            }
        }
        crossedSounds.forEach(sounds);
        for (GeoAnimation.EffectCue cue : this.animation.effects()) {
            if (cue.time() > from && cue.time() <= to) {
                effects.accept(cue.effect());
            }
        }
    }
}
