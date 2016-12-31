package eu.synectique.verveine.extractor.utils;

import java.io.InputStream;

public class IncludeToLowerFilterStream extends AbstractIncludeFilterStream {

	public IncludeToLowerFilterStream(InputStream in) {
		super(in);
	}

	@Override
	protected int convertChar(int c) {
		if (isInFileName()) {
			return Character.toLowerCase(c);
		}
		else {
			return c;
		}
	}

}
