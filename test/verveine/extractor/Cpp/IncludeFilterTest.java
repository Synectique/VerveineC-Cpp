package verveine.extractor.Cpp;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import eu.synectique.verveine.extractor.utils.IncludeToLowerInputStream;

@SuppressWarnings("deprecation")
public class IncludeFilterTest {
	
	public static final String SRC = "// comment of include\n" +
									 "#define TOTO 0\n" +
									 "#include <stdIO.h>\n" +
									 "#include \"OtherOne.h\"\n" +
									 "void AndMore() {}\n";
	public static final String TGT = "// comment of include\n" +
									 "#define TOTO 0\n" +
									 "#include <stdio.h>\n" +
									 "#include \"otherone.h\"\n" +
									 "void AndMore() {}\n";

	public static void main(String[] args) throws IOException {
		byte[] srcBuf = new byte[SRC.length()];
		InputStream input = new IncludeToLowerInputStream( new StringBufferInputStream(SRC) );
//		InputStream input =  new StringBufferInputStream(SRC) ;

		if ( TGT.length() != input.read(srcBuf)) {
			System.err.println("too few charcaters in converted string");
			System.exit(0);
		};

		if (! TGT.equals(new String(srcBuf))) {
			System.err.println("Converted string not equal to expected string");
			System.exit(0);
		}
		
		System.out.println("Everything went accroding to plans");

		input.close();  // useless but avoid warnings in Eclipse
	}

}
