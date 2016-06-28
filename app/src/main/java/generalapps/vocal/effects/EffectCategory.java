package generalapps.vocal.effects;

import android.printservice.PrinterDiscoverySession;
import android.support.annotation.DrawableRes;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.effects.FlangerEffect;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.synthesis.PitchResyntheziser;
import be.tarsos.dsp.synthesis.SineGenerator;
import generalapps.vocal.Audio;
import generalapps.vocal.R;
import generalapps.vocal.Recorder;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectCategory {
    public EffectCategory(int iconId, List<Effect> effects){
        mIconId = iconId;
        mEffects = effects;
    }

    public EffectCategory(int iconId){
        mIconId = iconId;
    }

    @DrawableRes public int mIconId;
    public List<Effect> mEffects;

    public boolean hasChildren(){
        return mEffects != null && 0 < mEffects.size();
    }

    public Effect get(int i){
        return mEffects.get(i);
    }

    public int size(){
        return mEffects.size();
    }

    public static List<EffectCategory> getDefault(){
        return Arrays.asList(EffectCategory.none, EffectCategory.synthEffects, EffectCategory.basicEffects, EffectCategory.filters);
    }

    public static EffectCategory none = new EffectCategory(R.drawable.effect_category_none);

    public static EffectCategory micTest = new EffectCategory(R.drawable.effect_category_voice_modulation, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_category_none),
            new Effect(R.drawable.effect_category_voice_modulation))));

    public static EffectCategory filters = new EffectCategory(R.drawable.effect_category_filter, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_filter_lowpass, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new LowPassFS(1000, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_filter_highpass, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new HighPass(10000, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_filter_doublepass, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new HighPass(10000, dispatcher.getFormat().getSampleRate()));
                    dispatcher.addAudioProcessor(new LowPassFS(1000, dispatcher.getFormat().getSampleRate()));
                }
            })
    )));

    public static EffectCategory basicEffects = new EffectCategory(R.drawable.effect_category_basic, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_basic_delay, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new DelayEffect(1, 0.5, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_basic_flanger, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new FlangerEffect(5, 0.1, dispatcher.getFormat().getSampleRate(), 1000));
                }
            })
    )));

    public static EffectCategory synthEffects = new EffectCategory(R.drawable.effect_category_synth, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_synth_basic, new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    TarsosDSPAudioFormat format = dispatcher.getFormat();
                    PitchProcessor.PitchEstimationAlgorithm algo = PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET;
                    generalapps.vocal.PitchResyntheziser prs = new generalapps.vocal.PitchResyntheziser(format.getSampleRate(), true, true);
                    final generalapps.vocal.SineGenerator generator = new generalapps.vocal.SineGenerator(1.0, 0);
                    dispatcher.addAudioProcessor(new PitchProcessor(algo, format.getSampleRate(), bufferSize, false ? new PitchDetectionHandler() {
                        @Override
                        public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                            if(pitchDetectionResult.isPitched()){
                                generator.frequency = pitchDetectionResult.getPitch();
                                generator.gain = audioEvent.getRMS();
                            }
                        }
                    } : prs));
                    dispatcher.addAudioProcessor(generator);
                }
            })
    )));
}