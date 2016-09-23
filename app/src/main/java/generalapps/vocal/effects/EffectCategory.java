package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.BitDepthProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.effects.FlangerEffect;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.filters.LowPassSP;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.resample.RateTransposer;
import be.tarsos.dsp.util.PitchConverter;
import generalapps.vocal.Audio;
import generalapps.vocal.BitCrusher;
import generalapps.vocal.PitchShifterNew;
import generalapps.vocal.R;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectCategory {
    public EffectCategory(int iconId, List<Effect> effects){
        mIconId = iconId;
        mEffects = effects;
        for(Effect effect : effects){
            effect.category = this;
        }
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

    public int indexOf(Effect effect){
        return mEffects.indexOf(effect);
    }

    public static EffectCategory none = new EffectCategory(R.drawable.effect_category_none, new ArrayList<>(Arrays.asList(Effect.none)));

    public static EffectCategory filters = new EffectCategory(R.drawable.effect_category_pass, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.filter_lowpass, "LowPass", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new LowPassSP(500, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.filter_highpass, "HighPass", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new HighPass(10000, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_lowpass_big, "BigLowPass", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new LowPassFS(15000, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.filter_bandpass, "BandPass", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new BandPass(10000, 10000, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_autowah, "AutoWah BandPass", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    final BandPass filter = new BandPass(5000, 2000, dispatcher.getFormat().getSampleRate());
                    final GainProcessor gain = new GainProcessor(5);
                    dispatcher.addAudioProcessor(new AudioProcessor() {
                        @Override
                        public boolean process(AudioEvent audioEvent) {
                            float freq = (float)(5000 + 10000*audioEvent.getRMS());
                            filter.setFrequency(freq);
                            Log.i("EffectCategory", "auto-wah freq: " + freq);
                            filter.process(audioEvent);
                            gain.process(audioEvent);
                            return true;
                        }

                        @Override
                        public void processingFinished() {

                        }
                    });
                }
            })
    )));

    public static EffectCategory basicEffects = new EffectCategory(R.drawable.effect_category_basic, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_echo, "Echo", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new DelayEffect(0.1, 0.5, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_delay, "Delay", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new DelayEffect(0.01, 0.4, dispatcher.getFormat().getSampleRate()));
                    dispatcher.addAudioProcessor(new DelayEffect(0.02, 0.4, dispatcher.getFormat().getSampleRate()));
                }
            }),
            new Effect(R.drawable.effect_flanger, "Flanger", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new FlangerEffect(0.0007, 0.5, dispatcher.getFormat().getSampleRate(), 1000));
                }
            }),
            new Effect(R.drawable.effect_bitcrush, "Bitcrush", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    dispatcher.addAudioProcessor(new BitCrusher(4, 8));
                }
            })
    )));

    public static EffectCategory pitchEffects = new EffectCategory(R.drawable.effect_category_pitch, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_pitch_1up, "PitchUp 1", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
//                    RateTransposer transposer = new RateTransposer(1.5);
//                    //WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(1.5, dispatcher.getFormat().getSampleRate()));
//                    WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(new WaveformSimilarityBasedOverlapAdd.Parameters(1.1, dispatcher.getFormat().getSampleRate(),
//                            200, 10, 12));
//                    wsola.setDispatcher(dispatcher);
//                    dispatcher.addAudioProcessor(wsola);
//                    dispatcher.addAudioProcessor(transposer);
                    int size = 1024;
                    PitchShifterNew shifter = new PitchShifterNew(1.1, dispatcher.getFormat().getSampleRate(), size, size - (size/8));
                    dispatcher.addAudioProcessor(shifter);
                }
            })
    )));

    public static EffectCategory synthEffects = new EffectCategory(R.drawable.effect_category_synth, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_synth, "Sine Synth", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    TarsosDSPAudioFormat format = dispatcher.getFormat();
                    PitchProcessor.PitchEstimationAlgorithm algo = PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;
                    generalapps.vocal.PitchResyntheziser prs = new generalapps.vocal.PitchResyntheziser(format.getSampleRate(), true, true, 1);
                    final generalapps.vocal.SineGenerator generator = new generalapps.vocal.SineGenerator(1.0, PitchConverter.absoluteCentToHertz(70));
//                    dispatcher.addAudioProcessor(new PitchProcessor(algo, format.getSampleRate(), bufferSize, true ? new PitchDetectionHandler() {
//                        @Override
//                        public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
//                            //generator.frequency = pitchDetectionResult.getPitch();
//                            Log.i("Effect Category", "Frequency: " + generator.frequency);
//                            generator.gain = audioEvent.getRMS();
//                        }
//                    } : prs));
                    dispatcher.addAudioProcessor(new AudioProcessor() {
                        @Override
                        public boolean process(AudioEvent audioEvent) {
                            generator.gain = audioEvent.getRMS();
                            //Log.i("EffectCategory", "gain: " + generator.gain);
                            audioEvent.clearFloatBuffer();
                            return true;
                        }

                        @Override
                        public void processingFinished() {

                        }
                    });
                    dispatcher.addAudioProcessor(generator);
                }
            }),
            new Effect(R.drawable.effect_synth_combined, "Sine Synth+Original", new Audio.AudioEffectApplier() {
                @Override
                public void Apply(AudioDispatcher dispatcher, int bufferSize) {
                    final generalapps.vocal.SineGenerator generator = new generalapps.vocal.SineGenerator(1.0, PitchConverter.absoluteCentToHertz(10));
                    dispatcher.addAudioProcessor(new AudioProcessor() {
                        @Override
                        public boolean process(AudioEvent audioEvent) {
                            generator.gain = audioEvent.getRMS()*0.2;
                            //Log.i("EffectCategory", "gain: " + generator.gain);
                            return true;
                        }

                        @Override
                        public void processingFinished() {

                        }
                    });
                    dispatcher.addAudioProcessor(generator);
                }
            })
    )));

    public static List<EffectCategory> list = Arrays.asList(none, synthEffects, basicEffects, filters);
}