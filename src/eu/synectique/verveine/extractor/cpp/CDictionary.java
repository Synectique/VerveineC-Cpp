package eu.synectique.verveine.extractor.cpp;

import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IBinding;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;

public class CDictionary extends Dictionary<IBinding> {

	/**
	 * A property added to CompilationUnits to record the name of the source file they belong to.
	 * Used to create FileAnchors
	 */
	public static final String SOURCE_FILENAME_PROPERTY = "verveine-source-filename";

	/**
	 * @param famixRepo
	 */
	public CDictionary(Repository famixRepo) {
		super(famixRepo);
	}

  	public void mapKey(IBinding bnd, NamedEntity fmx) {
		super.mapEntityToKey(bnd, fmx);
	}

	/**
	 * Returns a Famix Parameter associated with the IBinding.
	 * The Entity is created if it does not exist.<br>
	 * Params: see {@link Dictionary#ensureFamixParameter(Object, String, Type, eu.synectique.verveine.core.gen.famix.BehaviouralEntity, boolean)}.
	 * @param persistIt -- whether to persist or not the entity eventually created
	 * @return the Famix Entity found or created. May return null if "bnd" is null or in case of a Famix error
	 */
	public Parameter ensureFamixParameter(IBinding bnd, String name, Type typ, Method owner, boolean persistIt) {
		Parameter fmx = null;

		// --------------- to avoid useless computations if we can
		fmx = (Parameter)getEntityByKey(bnd);
		if (fmx != null) {
			return fmx;
		}

		// --------------- name
		if (name == null) {
			if (bnd == null) {
				return null;
			}
			else {
				name = bnd.getName();
			}
		}

		// --------------- owner
		/*if (owner == null) {
			if (bnd == null) {
				owner = ensureFamixStubMethod("<"+name+"_owner>");
			}
			else {
				owner = ensureFamixMethod(bnd.getDeclaringMethod(), persistIt);
			}
		}*/
		
		// --------------- type
		/*if (typ == null) {
			if (bnd == null) {
				typ = null;  // what else ?
			}
			else {
				typ = this.ensureFamixType(bnd.getType(), /*ctxt* /owner.getParentType(), /*alwaysPersist?* /persistIt);  // context of the parameter def = the class definition
			}
		}*/

		// --------------- recover from name ?
		/*for (Parameter candidate : getEntityByName(Parameter.class, name) ) {
			if ( matchAndMapVariable(bnd, name, owner, candidate) ) {
				fmx = candidate;
				break;
			}
		}*/

		if (fmx == null) {
			fmx = super.createFamixParameter(bnd, name, typ, owner, persistIt);
		}
		
		if (fmx != null) {
			fmx.setParentBehaviouralEntity(owner);
			fmx.setDeclaredType(typ);	
		}

		return fmx;
	}

	/**
	 * Returns a Famix Method associated with the IBinding. The Entity is created if it does not exist.
	 * Params: see {@link Dictionary#ensureFamixMethod(Object, String, String, Type, Type, boolean)}.
	 * @return the Famix Entity found or created. May return null if "bnd" is null or in case of a Famix error
	 */
	public Method ensureFamixMethod(IBinding bnd, String name, Collection<String> paramTypes, Type ret, Type owner, int modifiers, boolean persistIt) {
		Method fmx = null;
		String sig = "";
		boolean first;
		boolean delayedRetTyp;

		// --------------- to avoid useless computations if we can
		fmx = (Method)getEntityByKey(bnd);
		if (fmx != null) {
			return fmx;
		}

		// --------------- name
		if (name == null) {
			if (bnd == null) {
				return null;
			}
			else {
				name = bnd.getName();
			}
		}

		// --------------- signature
		first = true;
		/*if (bnd != null) {
			for (IBinding parBnd : bnd.getParameterTypes()) {
				if (first) {
					sig = parBnd.getName();
					first = false;
				}
				else {
					sig += "," + parBnd.getName();
				}
			}
		}
		else*/ if (paramTypes != null) {
			for (String t : paramTypes) {
				if (first) {
					sig = t;
					first = false;
				}
				else {
					sig += "," + t;
				}
			}				
		}
		else {
			sig += "???";
		}
		sig = name + "(" + sig + ")";
/*
		// --------------- recover from name ?
		for (Method candidate : this.getEntityByName(Method.class, name)) {
			if ( matchAndMapMethod(bnd, sig, ret, owner, candidate) ) {
				fmx = candidate;
				break;
			}
		}
*/
		if (fmx == null) {
			fmx = super.ensureFamixMethod(bnd, name, sig, ret, owner, persistIt);
		}

		if (fmx != null) {
			//setNamedEntityModifiers(fmx, modifiers);
			//fmx.setHasClassScope(Modifier.isStatic(modifiers));
		}

		return fmx;
	}

}
