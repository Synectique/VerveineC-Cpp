package eu.synectique.verveine.extractor.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTName;

public class QualifiedName implements Iterable<String> {

	/**
	 * Separator in fully qualified package name
	 */
	public static final String CPP_NAME_SEPARATOR = "::";

	protected List<String> nameParts;
	
	protected boolean isAbsolute;

	public QualifiedName(String name) {
		nameParts = new ArrayList<String>();
		isAbsolute = false;
		parse( name);
	}

	public QualifiedName(IASTName name) {
		this(name.toString());
	}

	protected QualifiedName(List<String> nameParts) {
		this.nameParts = nameParts;
	}
	
	protected void parse(String fullname) {
		int i;
		int iSeparator = 0;
		int partStart = 0;
		
		for (i=0; i < fullname.length(); i++) {
			char c = fullname.charAt(i);
	
			if (c == CPP_NAME_SEPARATOR.charAt(iSeparator)) {
				iSeparator++;
				if (iSeparator >= CPP_NAME_SEPARATOR.length()) {
					if (iSeparator == i+1) {
						isAbsolute = true;
					}
					else {
						nameParts.add(fullname.substring(partStart, i-CPP_NAME_SEPARATOR.length()+1));
					}
					partStart = i+1;
					iSeparator = 0;
				}
			}
			else {
				iSeparator = 0;
			}
		}
		if (i > 0) { // i.e. fullname was not empty
			nameParts.add(fullname.substring(partStart, i));
		}

	}

	public int nbParts() {
		return nameParts.size();
	}

	static public boolean isFullyQualified(String name) {
		return name.indexOf(QualifiedName.CPP_NAME_SEPARATOR) >= 0;
	}

	static public boolean isFullyQualified(IASTName name) {
		return isFullyQualified(name.toString());
	}

	public boolean isFullyQualified() {
		return nbParts() > 1;
	}

	protected void setAbsoluteQualified(boolean abs) {
		isAbsolute = abs;
	}

	public boolean isAbsoluteQualified() {
		return isAbsolute;
	}

	public boolean isEmpty() {
		return nameParts.isEmpty();
	}

	/**
	 * Returns the last part of a fully qualified name
	 */
	public String unqualifiedName() {
		if (nameParts.isEmpty()) {
			return "";
		}
		else {
			return nameParts.get(nbParts()-1);
		}
	}

	/**
	 * "Opposite" of unqualifiedName, returns the qualifying part of a fully qualified name
	 */
	public QualifiedName nameQualifiers() {
		QualifiedName ret;
		if (! isFullyQualified()) {
			ret= new QualifiedName("");
		}
		else {
			ret = new QualifiedName(nameParts.subList(0, nbParts()-1));
		}
		ret.setAbsoluteQualified(isAbsolute);
		return ret;
	}

	@Override
	public Iterator<String> iterator() {
		return nameParts.iterator();
	}

	@Override
	public String toString() {
		String full= "";
		boolean isFirst;
		
		isFirst = ! isAbsolute; // if isAbsolute, we want to add "::" before the first part, so consider it is not the first
		for (String part : nameParts) {
			if (isFirst) {
				isFirst = false;
				full = part;
			}
			else {
				full += CPP_NAME_SEPARATOR + part;
			}
		}
		return full;
	}
}
