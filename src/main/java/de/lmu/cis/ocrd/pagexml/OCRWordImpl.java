package de.lmu.cis.ocrd.pagexml;

import de.lmu.cis.ocrd.ml.features.OCRWord;

import java.util.List;

public class OCRWordImpl implements OCRWord {
	private final String word, raw;
	private final List<Double> masterOCRCharConfidences;
	private final double wordConfidence;
	private final String line;
	private final String id;

	OCRWordImpl(TextEquiv te, String line, List<Double> mConfs) {
		this.line = line;
		this.wordConfidence = te.getConfidence();
		this.word = te.getUnicodeNormalized();
		this.raw = te.getUnicode();
		this.masterOCRCharConfidences = mConfs;
		this.id = te.getParentTextRegion().getID();
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public boolean isLastInLine() {
		return line.endsWith(getWordNormalized());
	}

	@Override
	public boolean isFirstInLine() {
		return line.startsWith(getWordNormalized());
	}

	@Override
	public String getLineNormalized() {
		return line;
	}

	@Override
	public double getConfidence() {
		return wordConfidence;
	}

	@Override
	public double getCharacterConfidenceAt(int i) {
		if (i < masterOCRCharConfidences.size()) {
			return masterOCRCharConfidences.get(i);
		}
		return 0.0;
	}

	@Override
	public String getWordRaw() {
		return raw;
	}

	@Override
	public String getWordNormalized() {
		return word;
	}

	@Override
	public String toString() {
		return getWordNormalized();
	}

}
