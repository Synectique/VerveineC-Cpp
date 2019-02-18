package verveine.extractor.Cpp;

import fr.verveine.utils.QualifiedName;

/*
 * NOT A JUNIT TEST -- run as a normal application
 */
public class QualifiedNameTest {

	public static void main(String[] args) throws Error {

		testEmpty();
		testUnqualified();
		testQualified();
		testQualifiedAbsolute();
		testForeach();
		testNameQualifiers();
		testQualifiedTemplate();
		testUnqualifiedFct();
		testQualifiedFct();
		System.out.println("Everything went as planned!");
	}

	protected static void testEmpty() {
		QualifiedName qual = new QualifiedName("");
		if (qual.isFullyQualified()) {
			throw new Error("Should not be qualified");
		}
		if (qual.nbParts() != 0) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! qual.isEmpty()) {
			throw new Error("Should be empty");
		}
		if (! "".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "".equals(qual.toString())) {
			throw new Error("Wrong unqualified name: "+qual.toString());
		}
	}

	protected static void testUnqualified() {
		QualifiedName qual = new QualifiedName("toto");
		if (qual.isFullyQualified()) {
			throw new Error("Should not be qualified");
		}
		if (qual.nbParts() != 1) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "toto".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "toto".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

	protected static void testQualified() {
		QualifiedName qual = new QualifiedName("toto::titi::tutu()");
		if (! qual.isFullyQualified()) {
			throw new Error("Should be qualified");
		}
		if (qual.isAbsoluteQualified()) {
			throw new Error("Should not be absolute qualified");
		}
		if (qual.nbParts() != 3) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "tutu()".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "toto::titi::tutu()".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

	protected static void testQualifiedAbsolute() {
		QualifiedName qual = new QualifiedName("::toto::titi");
		if (! qual.isFullyQualified()) {
			throw new Error("Should be qualified");
		}
		if (! qual.isAbsoluteQualified()) {
			throw new Error("Should be absolute qualified");
		}
		if (qual.nbParts() != 2) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "titi".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "::toto::titi".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

	protected static void testNameQualifiers() {
		QualifiedName qual = new QualifiedName("toto::titi::tutu()").nameQualifiers();
		if (! qual.isFullyQualified()) {
			throw new Error("Should be qualified");
		}
		if (qual.nbParts() != 2) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "toto::titi".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
		if (qual.isAbsoluteQualified()) {
			throw new Error("Should not be absolute qualified");
		}

		qual = new QualifiedName("::toto::titi").nameQualifiers();
		if (! qual.isAbsoluteQualified()) {
			throw new Error("Should be absolute qualified");
		}

		qual = new QualifiedName("::toto").nameQualifiers();
		if (! qual.isAbsoluteQualified()) {
			throw new Error("Should be absolute qualified");
		}

	}

	protected static void testForeach() {
		QualifiedName qual = new QualifiedName("toto::titi::tutu()");
		int i = 1;
		String expected = null;
		for (String part : qual) {
			switch (i) {
			case 1: expected = "toto";   break;
			case 2: expected = "titi";   break;
			case 3: expected = "tutu()"; break;
			}
			if (! expected.equals(part)) {
				throw new Error(expected +" != "+part);
			}
			i++;
		}
	}

	protected static void testQualifiedTemplate() {
		QualifiedName qual = new QualifiedName("toto::titi<bla::blih>");
		if (! qual.isFullyQualified()) {
			throw new Error("Should be qualified");
		}
		if (qual.nbParts() != 2) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "titi<bla::blih>".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "toto::titi<bla::blih>".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

	protected static void testUnqualifiedFct() {
		QualifiedName qual = new QualifiedName("toto()->int");
		if (qual.isFullyQualified()) {
			throw new Error("Should not be qualified");
		}
		if (qual.nbParts() != 1) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "toto()".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "toto()".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

	protected static void testQualifiedFct() {
		QualifiedName qual = new QualifiedName("toto::titi::tutu()->int");
		if (! qual.isFullyQualified()) {
			throw new Error("Should be qualified");
		}
		if (qual.isAbsoluteQualified()) {
			throw new Error("Should not be absolute qualified");
		}
		if (qual.nbParts() != 3) {
			throw new Error("Wrong part count: "+qual.nbParts());
		}
		if (! "tutu()".equals(qual.unqualifiedName())) {
			throw new Error("Wrong unqualified name: "+qual.unqualifiedName());
		}
		if (! "toto::titi::tutu()".equals(qual.toString())) {
			throw new Error("Wrong toString: "+qual.toString());
		}
	}

}
