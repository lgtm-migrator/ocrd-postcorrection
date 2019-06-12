package de.lmu.cis.ocrd.cli;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lmu.cis.ocrd.ml.features.*;
import de.lmu.cis.ocrd.pagexml.*;
import de.lmu.cis.ocrd.profile.*;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import weka.core.Instance;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractMLCommand extends AbstractIOCommand {
	public static class Profiler {
		String type = "", executable = "", config = "", cacheDir = "";

		public Path getCacheFilePath(String ifg, AdditionalLexicon additionalLex) {
			String suffix = ".json.gz";
			if (additionalLex.use()) {
				suffix = "_" + additionalLex.toString() + suffix;
			}
			return Paths.get(cacheDir, ifg + suffix);
		}
	}

	public static class TrainingResource {
		public String evaluation = "", model = "", training = "", features = "", result = "";
	}

	public static class DLETrainingResource extends TrainingResource {
		public String dynamicLexicon = "";
	}

	public static class Parameter {
		public DLETrainingResource dleTraining;
		public TrainingResource rrTraining;
		public TrainingResource dmTraining;
		public Profiler profiler;
		public String model;
		String trigrams = "";
		int nOCR = 0;
		int maxCandidates = 0;
		List<String> filterClasses;
	}

	private Parameter parameter;
	private List<Page> pages;

	List<Page> getPages() {
		return pages;
	}

	public Parameter getParameter() {
		return parameter;
	}

	void setParameter(CommandLineArguments args) throws Exception {
		parameter = args.mustGetParameter(Parameter.class);
	}

	void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	FeatureClassFilter getFeatureClassFilter() {
		return new FeatureClassFilter(parameter.filterClasses);
	}

	static JsonObject[] getFeatures(String features) throws Exception {
		final Path path = Paths.get(features);
		JsonObject[] os;
		try (InputStream is = new FileInputStream(path.toFile())) {
			final String json = IOUtils.toString(is, Charset.forName("UTF-8"));
			os = new Gson().fromJson(json, JsonObject[].class);
		}
		return os;
	}

	Map<OCRToken, List<Ranking>> calculateRankings(List<OCRToken> tokens, Iterator<Instance> is, BinaryPredictor c) throws Exception {
		Map<OCRToken, List<Ranking>> rankings = new HashMap<>();
		for (OCRToken token : tokens) {
			boolean first = true;
			for (Candidate candidate : token.getAllProfilerCandidates()) {
				if (!is.hasNext()) {
					throw new Exception("instances and tokens out of sync");
				}
				final Instance instance = is.next();
				Logger.debug("instance of {}: {}", token.toString(), instanceToString(instance));
				final BinaryPrediction p = c.predict(instance);
				Logger.debug("prediction for {}: {}", token.toString(), p.toString());
				final double ranking = p.getPrediction() ? p.getConfidence() : -p.getConfidence();
				Logger.debug("confidence for {}: {}", token.toString(), p.getPrediction());
				Logger.debug("ranking for {}: {}", token.toString(), ranking);
				if (Double.isNaN(ranking)) {
					Logger.warn("ranking for {} is NaN; skipping", token.toString());
					continue;
				}
				if (first) {
					rankings.put(token, new ArrayList<>());
					first = false;
				}
				rankings.get(token).add(new Ranking(candidate, ranking));
			}
			if (rankings.containsKey(token)) {
				rankings.get(token).sort((lhs, rhs) -> Double.compare(rhs.ranking, lhs.ranking));
			}
		}
		return rankings;
	}

	private static String instanceToString(Instance instance) {
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < instance.numAttributes(); i++) {
			sj.add(Double.toString(instance.value(i)));
		}
		return "{" + sj.toString() + "}";
	}


	public static Path tagPath(String path, int n) {
		return Paths.get(path.replaceFirst("(\\..*?)$", "_" + n + "$1"));
	}

	protected interface WordOperation {
		void apply(Word word, String mOCR) throws Exception;
	}

	private static void eachLongWord(Page page, WordOperation f) throws Exception {
		for (Line line : page.getLines()) {
			for (Word word : line.getWords()) {
				String mOCR = word.getUnicodeNormalized().get(0);
				if (mOCR.length() > 3) {
					f.apply(word, mOCR);
				}
			}
		}
	}

	List<OCRToken> readTokens(METS mets, String ifg, AdditionalLexicon additionalLex) throws Exception {
		Logger.debug("read tokens ifg = {}, additional lex {}", ifg, additionalLex.toString());
		List<OCRToken> tokens = new ArrayList<>();
		final int gtIndex = parameter.nOCR;
		pages = openPages(openFiles(mets, ifg));
		final Profile profile = openProfile(ifg, pages, additionalLex);
		for (Page page: pages) {
			eachLongWord(page, (word, mOCR)->{
				final List<TextEquiv> tes = word.getTextEquivs();
				if (gtIndex < tes.size() &&
						tes.get(gtIndex).getDataTypeDetails().contains("OCR-D-GT")) {
					OCRTokenImpl t = new OCRTokenImpl(word, parameter.nOCR, parameter.maxCandidates, profile);
					if (t.getGT().isPresent()) {
						tokens.add(t);
					}
				}
			});
		}
		return tokens;
	}

	private List<METS.File> openFiles(METS mets, String ifg) {
		return mets.findFileGrpFiles(ifg);
	}

	private List<Page> openPages(List<METS.File> files) throws Exception {
		List<Page> pages = new ArrayList<>(files.size());
		for (METS.File file: files) {
			try (InputStream is = file.openInputStream()) {
				pages.add(Page.parse(Paths.get(file.getFLocat()), is));
			}
		}
		return pages;
	}

	private Profile openProfile(String ifg, List<Page> pages, AdditionalLexicon additionalLex) throws Exception {
		Path cached = parameter.profiler.getCacheFilePath(ifg, additionalLex);
		if (parameter.profiler.cacheDir == null || "".equals(parameter.profiler.cacheDir)) {
			cached = null;
		}
		return openProfileMaybeCached(cached, pages, additionalLex);
	}

	private Profile openProfileMaybeCached(Path cached, List<Page> pages, AdditionalLexicon additionalLex) throws Exception {
		if (cached != null && cached.toFile().exists()) {
			Logger.debug("opening cached profile: {}", cached.toString());
			return new FileProfiler(cached).profile();
		}
		if (cached != null && cached.getParent().toFile().mkdirs()) {
			Logger.debug("created cache directory {}", cached.getParent().toString());
		}
		Profile profile = getProfiler(pages, additionalLex).profile();
		Charset utf8 = Charset.forName("UTF-8");
		if (cached == null) {
			return profile;
		}
		Logger.debug("caching profile: {}", cached.toString());
		try (Writer w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(cached.toFile())), utf8))) {
			w.write(profile.toJSON());
			w.write('\n');
		}
		return profile;
	}

	private de.lmu.cis.ocrd.profile.Profiler getProfiler(List<Page> pages, AdditionalLexicon additionalLex) throws Exception {
		switch (parameter.profiler.type) {
			case "local":
				Logger.debug("using a local profiler: {} {}", parameter.profiler.executable, parameter.profiler.config);
				return new FileGrpProfiler(pages, new LocalProfilerProcess(
						Paths.get(parameter.profiler.executable),
						Paths.get(parameter.profiler.config),
						additionalLex));
			case "file":
				Logger.debug("using a file profiler: {}", parameter.profiler.config);
				return new FileProfiler(Paths.get(parameter.profiler.config));
			case "url":
				throw new Exception("Profiler type url: not implemented");
			default:
				throw new Exception ("Invalid profiler type: " + parameter.profiler.type);
		}
	}
}
