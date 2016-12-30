package eu.synectique.verveine.extractor.utils;

import java.io.InputStream;

public class IncludeWithHExtensionFilterStream extends AbstractIncludeFilterStream {

	public IncludeWithHExtensionFilterStream(InputStream in) {
		super(in);
	}

	@Override
	protected int convertChar(int c) {
		return 0;
	}

}
