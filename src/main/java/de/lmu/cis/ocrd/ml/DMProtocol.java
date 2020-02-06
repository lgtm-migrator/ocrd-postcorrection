package de.lmu.cis.ocrd.ml;

import com.google.gson.Gson;
import de.lmu.cis.ocrd.util.StringCorrector;
import org.pmw.tinylog.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMProtocol implements Protocol {
    public static class Value {
        public GroundTruth gt;
        public String normalized = "";
        public String ocr = "";
        public String cor = "";
        public List<Ranking> rankings;
        public double confidence = 0;
        public boolean taken = false;
    }

    public static class GroundTruth {
        public String gt;
        public boolean present;

        public GroundTruth(OCRToken token) {
            present = token.getGT().isPresent();
            gt = token.getGT().orElse("");
        }
    }

    public static class Protocol {
        public final Map<String, Value> corrections = new HashMap<>();
    }

    private Protocol protocol = new Protocol();
    private Map<OCRToken, List<Ranking>> rankings;

    public DMProtocol() {}
    public DMProtocol(Map<OCRToken, List<Ranking>> rankings) {
        this.rankings = rankings;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public void read(InputStream is) {
        protocol = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), protocol.getClass());
    }
    @Override
    public void write(OutputStream out) throws Exception {
        out.write(new Gson().toJson(protocol).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void protocol(OCRToken token, String correction, double confidence, boolean taken) {
        Logger.debug("putting token into dm protocol: {} {} {} {}", token, correction, confidence, taken);
        final OCRWord word = token.getMasterOCR();
        final Value val = new Value();
        val.gt = new GroundTruth(token);
        val.normalized = word.getWordNormalized().toLowerCase();
        val.ocr = word.getWordRaw();
        val.cor = new StringCorrector(val.ocr).correctWith(correction);
        val.confidence = confidence;
        val.taken = taken;
        val.rankings = rankings.get(token);
        protocol.corrections.put(token.getMasterOCR().getID(), val);
    }
}
