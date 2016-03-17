package eu.synectique.verveine.extractor;

import eu.synectique.verveine.extractor.c.VerveineCParser;

public class StandaloneParser {

	public static void main(String[] args) {
		VerveineCParser parser = new VerveineCParser();
		parser.setOptions(args);
		parser.parse();
		parser.emitMSE();
	}

}
