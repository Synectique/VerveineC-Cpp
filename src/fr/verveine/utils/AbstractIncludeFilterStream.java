package fr.verveine.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Filter FilterInputStream reads an input stream and converts to lower case the name of all included files
 */
public abstract class AbstractIncludeFilterStream extends FilterInputStream {

	private enum AbstractIncludeFilterStage {
		SEARCHINCLUDE, ININCLUDE, SEARCHFILENAME, INFILENAME;
	}

	public static final String INCLUDE_MARKER = "#include";

	private int iBuf;
	private AbstractIncludeFilterStage abstractIncludeFilterStatus;
	private boolean giveBackSameChar;
	private int lastByte;

	public AbstractIncludeFilterStream(InputStream in) {
		super(in);
		iBuf = 0;
		abstractIncludeFilterStatus = AbstractIncludeFilterStage.SEARCHINCLUDE;
		giveBackSameChar = false;
	}

	protected boolean isInFileName() {
		return (abstractIncludeFilterStatus == AbstractIncludeFilterStage.INFILENAME);
	}

	protected void giveBackSameByte(boolean giveBack) {
		giveBackSameChar = giveBack;
	}

	protected abstract int convertChar(int c);

	@Override
	public int read() throws IOException {
		if (! giveBackSameChar) {
			lastByte = super.read();
			setStatus(lastByte);
		}

		return convertChar(lastByte);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = this.read();
		if (read < 0) {
			return read;
		}

		int i = 0;
		while ( (read>=0) && (i<len) ) {
			b[i] = (byte) read;
			i++;
			read = this.read();
		}
		return i;

	}

	@Override
	public long skip(long n) throws IOException {
		abstractIncludeFilterStatus= AbstractIncludeFilterStage.SEARCHINCLUDE;
		return super.skip(n);
	}

	protected void setStatus(int c) {
		if ( (abstractIncludeFilterStatus == AbstractIncludeFilterStage.SEARCHINCLUDE) &&
			 (c == INCLUDE_MARKER.charAt(0)) ) {   // i.e. '#'
			iBuf = 1;
			abstractIncludeFilterStatus = AbstractIncludeFilterStage.ININCLUDE;
			return;
		}
		
		if (abstractIncludeFilterStatus == AbstractIncludeFilterStage.ININCLUDE) {
			if (c != INCLUDE_MARKER.charAt(iBuf)) {
				abstractIncludeFilterStatus = AbstractIncludeFilterStage.SEARCHINCLUDE;
			}
			else {
				iBuf++;
				if (iBuf == INCLUDE_MARKER.length()) {
					abstractIncludeFilterStatus = AbstractIncludeFilterStage.SEARCHFILENAME;
				}
			}
			return;
		}
		
		if ( (abstractIncludeFilterStatus == AbstractIncludeFilterStage.SEARCHFILENAME) &&
			 ((c=='<') || (c=='"')) ) {
				abstractIncludeFilterStatus = AbstractIncludeFilterStage.INFILENAME;
				return;
			}

		if ( (abstractIncludeFilterStatus == AbstractIncludeFilterStage.INFILENAME) &&
			 ((c=='>') || (c=='"')) ) {
			abstractIncludeFilterStatus = AbstractIncludeFilterStage.SEARCHINCLUDE;
		}
	}

}
