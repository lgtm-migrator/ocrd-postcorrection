package de.lmu.cis.ocrd.ml.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lmu.cis.ocrd.ml.ModelZIP;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ModelZIPTest {
    private ModelZIP model;
    private Path tmp;
    private String[] names;

    @Before
    public void init() throws Exception {
        tmp = Files.createTempDirectory("ocrd-cis-java");
        model = new ModelZIP();
        names = new String[]{
                "a", "b", "c",
                "d", "e", "f",
                "g", "h", "i",
        };
        Path out;
        int j = 0;
        for (int i = 0; i < 3; i++) {
            out = writeTmpFile(names[j++]);
            model.addLEModel(out, i);
        }
        for (int i = 0; i < 3; i++) {
            out = writeTmpFile(names[j++]);
            model.addRRModel(out, i);
        }
        for (int i = 0; i < 3; i++) {
            out = writeTmpFile(names[j++]);
            model.addDMModel(out, i);
        }
        model.setLEFeatureSet(getLEFeatureSet());
        model.setRRFeatureSet(getRRFeatureSet());
        model.setDMFeatureSet(getDMFeatureSet());
        model.save(Paths.get(tmp.toString(), "model.zip"));
        model = ModelZIP.open(Paths.get(tmp.toString(), "model.zip"));
    }

    @After
    public void close() throws IOException {
        model.close();
        //noinspection ResultOfMethodCallIgnored
        tmp.toFile().delete();
    }

    private Path writeTmpFile(String x) throws IOException {
        final Path out = Paths.get(tmp.toString(), x);
        try (PrintWriter os = new PrintWriter(out.toString())) {
            os.print(x);
        }
        return out;
    }

    private String readStringAndClose(InputStream is) throws IOException {
        try (InputStream iis = is) {
            return IOUtils.toString(iis, StandardCharsets.UTF_8);
        }
    }

    private List<JsonObject> getLEFeatureSet() {
        List<JsonObject> ret = new ArrayList<>();
        ret.add(new Gson().fromJson("{\"leFeature\": \"leFeatureValue\"}", JsonObject.class));
        return ret;
    }

    private List<JsonObject> getRRFeatureSet() {
        List<JsonObject> ret = new ArrayList<>();
        ret.add(new Gson().fromJson("{\"rrFeature\": \"rrFeatureValue\"}", JsonObject.class));
        return ret;
    }

    private List<JsonObject> getDMFeatureSet() {
        List<JsonObject> ret = new ArrayList<>();
        ret.add(new Gson().fromJson("{\"dmFeature\": \"dmFeatureValue\"}", JsonObject.class));
        return ret;
    }

    @Test
    public void testReadDLEModel1() throws Exception {
        final String got = readStringAndClose(model.openDLEModel(0));
        assertThat(got, is(names[0]));
    }

    @Test
    public void testReadDLEModel2() throws Exception {
        final String got = readStringAndClose(model.openDLEModel(1));
        assertThat(got, is(names[1]));
    }

    @Test
    public void testReadDLEModel3() throws Exception {
        final String got = readStringAndClose(model.openDLEModel(2));
        assertThat(got, is(names[2]));
    }

    @Test
    public void testReadRRModel1() throws Exception {
        final String got = readStringAndClose(model.openRRModel(0));
        assertThat(got, is(names[3]));
    }

    @Test
    public void testReadRRModel2() throws Exception {
        final String got = readStringAndClose(model.openRRModel(1));
        assertThat(got, is(names[4]));
    }

    @Test
    public void testReadRRModel3() throws Exception {
        final String got = readStringAndClose(model.openRRModel(2));
        assertThat(got, is(names[5]));
    }

    @Test
    public void testReadDMModel1() throws Exception {
        final String got = readStringAndClose(model.openDMModel(0));
        assertThat(got, is(names[6]));
    }

    @Test
    public void testReadDMModel2() throws Exception {
        final String got = readStringAndClose(model.openDMModel(1));
        assertThat(got, is(names[7]));
    }

    @Test
    public void testReadDMModel3() throws Exception {
        final String got = readStringAndClose(model.openDMModel(2));
        assertThat(got, is(names[8]));
    }

    @Test
    public void testReadDLEFeatureSet() {
        final String want = new Gson().toJson(getLEFeatureSet());
        final String got = new Gson().toJson(model.getLEFeatureSet());
        assertThat(got, is(want));
    }

    @Test
    public void testReadRRFeatureSet() {
        final String want = new Gson().toJson(getRRFeatureSet());
        final String got = new Gson().toJson(model.getRRFeatureSet());
        assertThat(got, is(want));
    }

    @Test
    public void testReadDMFeatureSet() {
        final String want = new Gson().toJson(getDMFeatureSet());
        final String got = new Gson().toJson(model.getDMFeatureSet());
        assertThat(got, is(want));
    }
}
