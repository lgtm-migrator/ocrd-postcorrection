package de.lmu.cis.ocrd.parsers;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import de.lmu.cis.ocrd.SimpleDocument;

public class ABBYYZipParser implements Parser {
	private final static Pattern num = Pattern.compile(".*?(\\p{Digit}+).*?");

	private static int getPageID(Path path) throws Exception {
		Matcher m = num.matcher(path.getFileName().toString());
		if (!m.matches()) {
			throw new Exception("cannot extract pageid from file name: " + path.getFileName().toString());
		}
		return Integer.parseInt(m.group(1));
	}

	private SimpleDocument doc;
	private final ZipFile zip;

	public ABBYYZipParser(ZipFile zip) {
		this.zip = zip;
	}

	@Override
	public SimpleDocument parse() throws Exception {
		ArrayList<Path> pages = new ArrayList<Path>();
		for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
			ZipEntry entry = entries.nextElement();
			if (!Archive.isABBYYLine(entry.getName())) {
				continue;
			}
			pages.add(Paths.get(entry.getName()));
		}
		this.doc = new SimpleDocument().withPath(this.zip.getName());
		for (Path page : pages) {
			parsePage(this.zip, page);
		}
		return this.doc;
	}

	private void parsePage(int pid, InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		org.w3c.dom.Document xml = docBuilder.parse(is);
		this.doc.add(new ABBYYXMLParser(xml, pid).parse());
	}

	private void parsePage(ZipFile zip, Path path) throws Exception {
		final int pid = getPageID(path);
		try (InputStream is = zip.getInputStream(zip.getEntry(path.toString()))) {
			parsePage(pid, is);
		}
	}
}
