package verveine.extractor.Cpp;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;


import eu.synectique.verveine.extractor.utils.IncludeToLowerInputStream;

@SuppressWarnings("deprecation")
public class IncludeFilterTest {
	
	public static final String src  = "// comment of include\n#define TOTO 0\n#include <stdIO.h>\n#include \"OtherOne.h\"\nvoid AndMore() {}\n";
	public static final String dest = "// comment of include\n#define TOTO 0\n#include <stdio.h>\n#include \"otherone.h\"\nvoid AndMore() {}\n";

	public static void main(String[] args) throws IOException {
		byte[] srcBuf = new byte[src.length()];
		InputStream input = new IncludeToLowerInputStream( new StringBufferInputStream(src) );

		assert( dest.length() == input.read(srcBuf));

		for (int i=0; i < dest.length(); i++) {
			assert(srcBuf[i] == (byte) dest.charAt(i));
		}

		input.close();  // useless but avoid warnings in Eclipse
	}

}
