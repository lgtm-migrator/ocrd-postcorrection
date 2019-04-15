package de.lmu.cis.ocrd.ml.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lmu.cis.ocrd.ml.features.*;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
public class DefaultFeatureFactoryTest {
	private static Optional<Feature> makeFeature(String json) throws Exception {
		final FeatureFactory featureFactory = FeatureFactory.getDefault()
				.withArgumentFactory(new EmptyArgumentFactory());
		return featureFactory.create(new Gson().fromJson(json, JsonObject.class));
	}

	@Test
	public void testMinOCRConfidenceFeature() throws Exception {
		final String json = "{\"type\":\"de.lmu.cis.ocrd.ml.features.MinOCRCharacterConfidenceFeature\",\"name\":\"MinOCRConfidence\",\"ocrIndex\":0}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
		assertThat(feature.get().getName(), is("MinOCRConfidence"));
		assertThat(feature.get() instanceof MinOCRCharacterConfidenceFeature, is(true));
	}

	@Test
	public void testMaxOCRConfidenceFeature() throws Exception {
		final String json = "{\"type\":\"de.lmu.cis.ocrd.ml.features.MaxOCRCharacterConfidenceFeature\",\"name\":\"MaxOCRConfidence\",\"ocrIndex\":0}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
		assertThat(feature.get().getName(), is("MaxOCRConfidence"));
		assertThat(feature.get() instanceof MaxOCRCharacterConfidenceFeature, is(true));
	}

	@Test
	public void testMinCharNGramsFeature() throws Exception {
		final String json = "{\"type\":\"de.lmu.cis.ocrd.ml.features.MinCharNGramsFeature\",\"name\":\"MinCharNGrams\"}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
		assertThat(feature.get().getName(), is("MinCharNGrams"));
		assertThat(feature.get() instanceof MinCharNGramsFeature, is(true));
	}

	@Test
	public void testMaxCharNGramsFeature() throws Exception {
		final String json = "{\"type\":\"de.lmu.cis.ocrd.ml.features.MaxCharNGramsFeature\",\"name\":\"MaxCharNGrams\"}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
		assertThat(feature.get().getName(), is("MaxCharNGrams"));
		assertThat(feature.get() instanceof MaxCharNGramsFeature, is(true));
	}

	@Test
	public void testHighestProfilerVoteWeightFeature() throws Exception {
		final String json = "{\"type\":\"de.lmu.cis.ocrd.ml.features.HighestRankedCandidateVoteWeightFeature\",\"name\":\"ProfilerHighestVoteWeight\"}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
		assertThat(feature.get().getName(), is("ProfilerHighestVoteWeight"));
		assertThat(feature.get() instanceof HighestRankedCandidateVoteWeightFeature, is(true));
	}

	@Test
	public void testDeactivateFeatureTrue() throws Exception {
		final String json = "{\"deactivate\":true,\"type\":\"de.lmu.cis.ocrd.ml.features.MaxCharNGramsFeature\",\"name\":\"MaxCharNGrams\"}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(false));
	}

	@Test
	public void testDeactivateFeatureFalse() throws Exception {
		final String json = "{\"deactivate\":false,\"type\":\"de.lmu.cis.ocrd.ml.features.MaxCharNGramsFeature\",\"name\":\"MaxCharNGrams\"}";
		final Optional<Feature> feature = makeFeature(json);
		assertThat(feature.isPresent(), is(true));
	}
}
