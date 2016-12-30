package eu.synectique.verveine.extractor.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Filter FilterInputStream reads an input stream and converts to lower case the name of all included files
 */
public abstract class AbstractIncludeFilterStream extends FilterInputStream {

	public static final String INCLUDE_MARKER = "#include";

	private byte[] includeBuffer;
	private int iBuf;
	private boolean markerFound;
	private boolean fileNameFound;

	public AbstractIncludeFilterStream(InputStream in) {
		super(in);
		includeBuffer = new byte[500];  // seems enough to store any realistic #include line
		iBuf = 0;
		markerFound = false;
		fileNameFound = false;
	}

	@Override
	public int read() throws IOException {
		return myReadChar(super.read());
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int nb = super.read(b, off, len);
		for (int i=0; i<nb; i++) {
			b[i] = (byte) myReadChar(b[i]);
		}
		return nb;
	}

	@Override
	public long skip(long n) throws IOException {
		iBuf = 0;
		markerFound = false;
		fileNameFound = false;
		return super.skip(n);
	}

	/**
	 * Analyzes chars read as they come to detect presence of INCLUDE_MARKER string and
	 * then transform them according to the specific concrete sub-class.<br>
	 * Returns the transformed (or not) character
	 */
	protected int myReadChar(int c) {
		if (! markerFound) {
			return lookingForMarker(c);
		}
		else {
			return lookingForIncludedFile(c);
		}
	}


	/**
	 * Analyzes chars read as they come to detect the start of the included filename.
	 * After this marker, passes the include file name to concrete convertChar(char)<br>
	 * Returns the transformed (or not) character
	 */
	private int lookingForIncludedFile(int c) {
		if (! fileNameFound) {
			switch (c) {
			case ' ' :
			case '\t': return c;

			case '<' :
			case '"':
				fileNameFound = true;
				return c;
			
			default:
				// cannot happen
				return c;
			}
		}
		else {
			switch (c) {
			case '>' :
			case '"':
				fileNameFound = false;
				markerFound = false;
				return c;
			
			default:
				return convertChar(c);
			}
		}
	}

	protected abstract int convertChar(int c);


	/**
	 * Analyzes chars read as they come to detect presence of INCLUDE_MARKER string.
	 * After this marker, passes the include file name to concrete convertChar(char)<br>
	 * Returns the transformed (or not) character
	 */
	protected int lookingForMarker(int c) {
		includeBuffer[iBuf] = (byte) c;
		iBuf++;
		if (! INCLUDE_MARKER.startsWith(new String(includeBuffer, 0, iBuf))) {
			if (c == INCLUDE_MARKER.charAt(0)) {   // i.e. '#'
				includeBuffer[0] = (byte) c;
				iBuf = 1;
			}
			else {
				iBuf = 0;
			}
		}
		else
			if (iBuf == INCLUDE_MARKER.length()) {
				iBuf = 0;
				markerFound = true;
				fileNameFound = false;
			}

		return c;
	}

}
