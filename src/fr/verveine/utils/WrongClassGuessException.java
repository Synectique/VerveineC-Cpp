package fr.verveine.utils;

import eu.synectique.verveine.core.gen.famix.NamedEntity;

/**
 * An exception to flag entities wrongly typed during parsing (e.g. a Namespace that was expected as a Class.
 * It often happens because the entity was first created without really knowing what it is.
 * 
 * Class also defines a static method to log such errors 
 */
public class WrongClassGuessException extends Exception {
	private static final long serialVersionUID = 1L;

	public WrongClassGuessException(String message) {
		super(message);
	}

	static public <T extends NamedEntity> void reportWrongClassGuess(Class<T> clazz, NamedEntity found) {
		System.err.print(("Exception: wrong guessed type '"+found.getClass().getSimpleName()+"' instead of expected type '"+clazz.getSimpleName() + "' for entity "+found.getName()));
		if (found.getSourceAnchor() != null) {
			System.err.println(" @" + found.getSourceAnchor());
		}
		else {
			System.err.println();
		}

		for (StackTraceElement stackEntry : new Throwable().getStackTrace()) {
			if (! (stackEntry.getMethodName().equals("reportWrongClassGuess") || stackEntry.getMethodName().startsWith("getEntry")) ) {
				System.err.println(" From parser code: " + stackEntry.toString());
				break;
			}
		}
	}
 
}
