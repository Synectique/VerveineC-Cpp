package fr.verveine.utils;

import java.io.InputStream;

public class IncludeWithHExtensionFilterStream extends AbstractIncludeFilterStream {

	public static final String ADDED_EXT = ".h";

	private enum AddExtFilterStage {
		OUTSIDEFILENAME, SEARCHDOT, DOTFOUND, ADDINGEXT;
	}

	private AddExtFilterStage addExtFilterStatus;

	private int iAdded;

	public IncludeWithHExtensionFilterStream(InputStream in) {
		super(in);
		addExtFilterStatus = AddExtFilterStage.OUTSIDEFILENAME;
		iAdded = 0;
	}

	@Override
	protected int convertChar(int c) {
		int toReturn = c;   // default to returning the character c

		if (isInFileName()) {
			switch (addExtFilterStatus) {
			case OUTSIDEFILENAME:
				addExtFilterStatus = AddExtFilterStage.SEARCHDOT;
				// return c
				break;

			case SEARCHDOT:
				 if (c == '.') {
					 addExtFilterStatus = AddExtFilterStage.DOTFOUND;
				 }
				// return c
				 break;

			case DOTFOUND:
			case ADDINGEXT:  // actually not possible here
				// return c
				break;
			}
		}
		else {  // i.e. ! isInFileName()
			switch (addExtFilterStatus) {
			case DOTFOUND:
				addExtFilterStatus = AddExtFilterStage.OUTSIDEFILENAME;
				// no break; to return c
			case OUTSIDEFILENAME:
				// return c
				break;

			case SEARCHDOT:
				addExtFilterStatus = AddExtFilterStage.ADDINGEXT;
				iAdded=0;
				giveBackSameByte(true);
				// no break; to return extension
			case ADDINGEXT:
				if (iAdded < ADDED_EXT.length()) {
					toReturn = ADDED_EXT.charAt(iAdded);
					iAdded++;
				}
				else {
					addExtFilterStatus = AddExtFilterStage.OUTSIDEFILENAME;
					giveBackSameByte(false);
				}
				break;
			}
		}

		return toReturn;
	}

}
