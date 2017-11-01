package eu.synectique.verveine.extractor.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTName;

import eu.synectique.verveine.extractor.visitors.SignatureBuilderVisitor;

public class QualifiedName implements Iterable<String> {

	/**
	 * Separator in fully qualified package name
	 */
	public static final String CPP_NAME_SEPARATOR = "::";

	protected List<String> nameParts;
	
	protected boolean isAbsolute;

	// CONSTRUCTION

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
		int i=0;
		int iNameSeparator = 0;
		int iFctReturnSeparator = 0;
		int partStart = 0;
		// FIXME fails for getAisTacticalData():inline aistactical::AisTacticalData::Ref
		//       thinks it is fullyqualified
		while ( (i < fullname.length()) && (iFctReturnSeparator < SignatureBuilderVisitor.CPP_FCT_RETURN_SEPARATOR.length()) ){
			char c = fullname.charAt(i);
			i++;

			if (c == CPP_NAME_SEPARATOR.charAt(iNameSeparator)) {
				iNameSeparator++;
				iFctReturnSeparator = 0;
				if (iNameSeparator >= CPP_NAME_SEPARATOR.length()) {
					if (iNameSeparator == i) {
						isAbsolute = true;
					}
					else {
						nameParts.add(fullname.substring(partStart, i-CPP_NAME_SEPARATOR.length()));
					}
					partStart = i;
					iNameSeparator = 0;
					iFctReturnSeparator = 0;
				}
			}
			else if (c == SignatureBuilderVisitor.CPP_FCT_RETURN_SEPARATOR.charAt(iFctReturnSeparator)) {
				iFctReturnSeparator++;
				iNameSeparator = 0;
			}
			else {
				iNameSeparator = 0;
				iFctReturnSeparator = 0;
				switch (c) {
				case '<': i = parseSkip(fullname, i, '>');  break;
				case '(': i = parseSkip(fullname, i, ')');  break;
				case '[': i = parseSkip(fullname, i, ']');  break;
				}
			}
		}
		if (i > 0) { // i.e. fullname was not empty
			if (iFctReturnSeparator >= SignatureBuilderVisitor.CPP_FCT_RETURN_SEPARATOR.length()) {
				i -= iFctReturnSeparator;
			}
			nameParts.add(fullname.substring(partStart, i));
		}

	}

	protected int parseSkip(String fullname, int i, char end) {
		while ( (i < fullname.length()) && (fullname.charAt(i) != end) ) {
			char c = fullname.charAt(i);
			i++;
			switch (c) {
			case '<': i = parseSkip(fullname, i, '>');  break;
			case '(': i = parseSkip(fullname, i, ')');  break;
			case '[': i = parseSkip(fullname, i, ']');  break;
			}
		}
		return i;
	}

	// STATIC UTILITIES

	static public boolean isFullyQualified(String name) {
		return name.indexOf(QualifiedName.CPP_NAME_SEPARATOR) >= 0;
	}

	static public boolean isFullyQualified(IASTName name) {
		return isFullyQualified(name.toString());
	}

	static public String signatureFromBehaviouralFullname(String fullname) {
		if (QualifiedName.isFullyQualified(fullname)) {
			int i;
			i = fullname.indexOf('(');
			if (i < 0) {
				System.out.println("ouups "+fullname);
			}
			i = fullname.substring(0, i).lastIndexOf(QualifiedName.CPP_NAME_SEPARATOR);
			return fullname.substring(i+QualifiedName.CPP_NAME_SEPARATOR.length());
		}
		else {
			return fullname;
		}
	}

	static public String parentNameFromEntityFullname(String fullname) {
		int i;
		i = fullname.indexOf('(');
		if (i > 0) {
			fullname = fullname.substring(0, i);
		}

		i = fullname.lastIndexOf(QualifiedName.CPP_NAME_SEPARATOR);
		return fullname.substring(0, i);
	}

	// OTHER METHODS
	
	public int nbParts() {
		return nameParts.size();
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
