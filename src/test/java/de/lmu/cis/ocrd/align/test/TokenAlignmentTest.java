package de.lmu.cis.ocrd.align.test;

import de.lmu.cis.ocrd.align.TokenAlignment;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TokenAlignmentTest {
	@Test
	public void test2alignments() {
		final String a = "abc def";
		final String b = "a b c def";
		TokenAlignment tokens = new TokenAlignment(a).add(b);
		assertThat(tokens.size(), is(2));
		assertThat(tokens.get(0).toString(), is("abc|a,b,c"));
		assertThat(tokens.get(1).toString(), is("def|def"));
	}

	@Test
	public void test3alignments() {
		final String a = "abcd ef gh";
		final String b = "ab cd efgh";
		final String c = "ab cd ef gh";
		TokenAlignment tokens = new TokenAlignment(a).add(b).add(c);
		assertThat(tokens.size(), is(3));
		assertThat(tokens.get(0).toString(), is("abcd|ab,cd|ab,cd"));
		assertThat(tokens.get(1).toString(), is("ef|efgh|ef"));
		assertThat(tokens.get(2).toString(), is("gh|efgh|gh"));
	}

	@Test
	public void testAlignToSelf() {
		final String a = "abc def ghi";
		TokenAlignment tokens = new TokenAlignment(a).add(a);
		assertThat(tokens.size(), is(3));
		assertThat(tokens.get(0).toString(), is("abc|abc"));
		assertThat(tokens.get(1).toString(), is("def|def"));
		assertThat(tokens.get(2).toString(), is("ghi|ghi"));
	}

	@Test
	public void testBug1() {
		final String a = "nen in dem Momente wo der Wagenzug anrasselte die gräßliche Zerschmettere er";
		final String b = "nen in de m Momente wo der Wagenzug anrasseltez die CgräßlichksZekschmktkckking ex";
		TokenAlignment tokens = new TokenAlignment(a).add(b);
		assertThat(tokens.size(), is(12));
		assertThat(tokens.get(0).toString(), is("nen|nen"));
		assertThat(tokens.get(3).toString(), is("Momente|Momente"));
		assertThat(tokens.get(6).toString(), is("Wagenzug|Wagenzug"));
		// TODO: add more tests for token alignment
	}
}
