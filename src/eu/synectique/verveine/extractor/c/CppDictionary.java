package eu.synectique.verveine.extractor.c;

import org.eclipse.cdt.core.dom.ast.IBinding;
import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;

/**
 * A {@link eu.synectique.verveine.Dictionary} specialized for Java
 * @author anquetil
 */
public class CppDictionary extends Dictionary<IBinding> {

	/**
	 * A property added to CompilationUnits to record the name of the source file they belong to.
	 * Used to create FileAnchors
	 */
	public static final String SOURCE_FILENAME_PROPERTY = "verveine-source-filename";

  	public void mapKey(IBinding bnd, NamedEntity fmx) {
		super.mapEntityToKey(bnd, fmx);
	}

	/**
	 * @param famixRepo
	 */
	public CppDictionary(Repository famixRepo) {
		super(famixRepo);
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
	
}