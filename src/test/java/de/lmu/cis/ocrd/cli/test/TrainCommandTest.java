package de.lmu.cis.ocrd.cli.test;

import de.lmu.cis.ocrd.cli.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TrainCommandTest {
	private final String tmp = "src/test/resources/workspace/dump";
	private final String parameter = "src/test/resources/workspace/config.json";
	private final String mets = "src/test/resources/workspace/mets.xml";
	private final String inputFileGroupTrain = "OCR-D-PROFILED";
	private final String inputFileGroupEval = "OCR-D-EVAL";
	private final String logLevel = "DEBUG";

	private AbstractMLCommand.Parameter config;

	@Before
	public void init() {
		new File(tmp).mkdirs();
	}

	// @After
	// public void deinit() throws Exception {
	// 	FileUtils.deleteDirectory(new File(tmp));
	// }

	@Test
	public void test() throws Exception {
		train();
		evalDLE();
	}

	private void train() throws Exception {
		String[] args = {
				"-c", "train",
				"--mets", mets,
				"--parameter", parameter,
				"-I", inputFileGroupTrain,
				"--log-level", logLevel,
		};
		CommandLineArguments cla = CommandLineArguments.fromCommandLine(args);

		Command cmd = new TrainCommand();
		cmd.execute(cla);
		// 3 runs (dle, rr, dm), 2 files for each run with 2 OCRs
		assertThat(countFilesInDir(tmp), is(3*2*2));
	}

	private void evalDLE() throws Exception {
		String[] args = {
			"-c", "eval-dle",
			"--mets", mets,
			"--parameter", parameter,
			"-I", inputFileGroupEval,
			"--log-level", logLevel,
		};
		CommandLineArguments cla = CommandLineArguments.fromCommandLine(args);
		Command cmd = new EvaluateDLECommand();
		cmd.execute(cla);
		// existing files (see above) + 2 lexicon, 2 eval and 2 result files
		assertThat(countFilesInDir(tmp), is(3*2*2 + (2+2+2)));
	}

	private static int countFilesInDir(String dir) throws IOException {
		return (int) Files.walk(Paths.get(dir)).count() - 1; // do not count dir
	}

}
