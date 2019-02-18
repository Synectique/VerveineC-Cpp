package verveine.extractor.Cpp;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import fr.verveine.utils.IncludeToLowerFilterStream;
import fr.verveine.utils.IncludeWithHExtensionFilterStream;

@SuppressWarnings("deprecation")
public class IncludeFilterTest {
	
	public static final String SRC =
					"// comment of include\n" +
					"#define TOTO 0\n" +
					"#include <stdIO>\n" +
					"#include \"OtherOne.h\"\n" +
					"void AndMore() {}";
	public static final String TGT_LOWER =
					"// comment of include\n" +
					"#define TOTO 0\n" +
					"#include <stdio>\n" +
					"#include \"otherone.h\"\n" +
					"void AndMore() {}";
	public static final String TGT_ADD_H =
					"// comment of include\n" +
					"#define TOTO 0\n" +
					"#include <stdIO.h>\n" +
					"#include \"OtherOne.h\"\n" +
					"void AndMore() {}";
	public static final String TGT_BOTH = 
					"// comment of include\n" +
					"#define TOTO 0\n" +
					"#include <stdio.h>\n" +
					"#include \"otherone.h\"\n" +
					"void AndMore() {}";

	
	public static void main(String[] args) throws IOException {
		byte[] srcBuf = new byte[SRC.length()+2];  // +2 to give room for added ext
		InputStream input;
		int byteRead;

		input = new IncludeToLowerFilterStream( new StringBufferInputStream(SRC) );
		byteRead = input.read(srcBuf);
		reportError(byteRead, srcBuf,"to-lower", TGT_LOWER);

		input = new IncludeWithHExtensionFilterStream( new StringBufferInputStream(SRC) );
		byteRead = input.read(srcBuf);
		reportError(byteRead, srcBuf,"add-ext", TGT_ADD_H);

		input = new IncludeToLowerFilterStream( new IncludeWithHExtensionFilterStream( new StringBufferInputStream(SRC)));
		byteRead = input.read(srcBuf);
		reportError(byteRead, srcBuf,"add-ext then to-lower", TGT_BOTH);

		input = new IncludeWithHExtensionFilterStream( new IncludeToLowerFilterStream( new StringBufferInputStream(SRC)));
		byteRead = input.read(srcBuf);
		reportError(byteRead, srcBuf,"to-lower then add-ext", TGT_BOTH);
		input.close();

		System.out.println("Everything went as planned!");

	}

	protected static void reportError(int byteRead, byte[] srcBuf, String msg, String expected) {
		boolean error=false;

		if ( expected.length() != byteRead) {
			System.err.println("too few characters in converted string ("+msg+"). Expected "+expected.length()+ " received "+byteRead);
			error = true;
		}
		if (! expected.equals(new String(srcBuf, 0, byteRead))) {
			error = true;
			System.err.println("Converted string ("+ msg +") not equal to expected string");
			System.err.println("EXPECTED:\n---");
			System.err.println(expected);
			System.err.println("---\n");
			System.err.println("CONVERTED:\n---");
			System.err.println(new String(srcBuf));
			System.err.println("---\n");
			
			for (int i=0; i<expected.length(); i++) {
				if ( expected.charAt(i) != srcBuf[i]) {
					System.err.println("diff at char "+i+" expect '"+expected.charAt(i)+"', got '"+ srcBuf[i]+ "'");
					i=expected.length();
				}
			}
			
			if (error) {
				System.exit(0);
			}
		}
	}

}
