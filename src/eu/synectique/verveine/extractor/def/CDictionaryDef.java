package eu.synectique.verveine.extractor.def;

import java.util.Map;

import org.eclipse.cdt.core.model.ISourceRange;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;

public class CDictionaryDef extends Dictionary<String> {

	private ITracer tracer;

	public CDictionaryDef(Repository famixRepo) {
		super(famixRepo);
		tracer = new NullTracer();
	}

	/**
	 * Prints the number of entities for different types
	 * Kinda debuging method
	 */
	public void sizes() {
		int ns = 0;
		int pk = 0;
		int cl = 0;
		int mt = 0;
		int at = 0;
		int ot = 0;
		for (NamedEntity ent : keyToEntity.values()) {
			if (ent instanceof Namespace)											ns++;
			else if (ent instanceof eu.synectique.verveine.core.gen.famix.Package)	pk++;
			else if (ent instanceof eu.synectique.verveine.core.gen.famix.Class)	cl++;
			else if (ent instanceof Method)											mt++;
			else if (ent instanceof Attribute)										at++;
			else																	ot++;
		}			
		System.err.println("CDictionaryDef ns="+ns+", pk="+pk+", cl="+cl+", mt="+mt+", at="+at+", ot="+ot);
	}

	/**
	 * Print a list of all entities of a given type
	 * Kinda debugging method
	 */
	public <T extends NamedEntity> void listAll(Class<T> clazz) {
		for (Map.Entry<String, NamedEntity> kv : keyToEntity.entrySet()) {
			if (clazz.isInstance(kv.getValue())) {
				System.err.println("list all "+clazz.getSimpleName()+": "+kv.getValue().getName()+" @ "+kv.getKey());
			}
		}
	}
	
	/**
	 * Computes moose name for a ScopingEntity
	 * This is a convenient method to call {@link #mooseName(Namespace)} or {@link #mooseName(Package)}
	 * and to make Java type checker happy
	 */
	protected String mooseName(ScopingEntity ent, String name) {
		if (ent instanceof Package) {
			return mooseName((Package)ent, name);
		}
		if (ent instanceof Namespace) {
			return mooseName((Namespace)ent, name);
		}
		return name;
	}

	/**
	 * Computes moose name for a Namespace
	 * MooseName is the concatenation of the moosename of the parent Namescape with the simple name of the Namescape
	 */
	protected String mooseName(Namespace parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentScope(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}
	
	/**
	 * Computes moose name for a Package
	 * MooseName is the concatenation of the moosename of the parent Package with the simple name of the Package
	 */
	protected String mooseName(Package parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentPackage(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}

	protected String concatMooseName(String prefix, String name) {
		return prefix + "::" + name;
	}

	@SuppressWarnings("unchecked")
	public <T extends NamedEntity> T removeEntity(String filename, int startPos, String name, Class<T> clazz) {
		String key = mkKey(filename, startPos);
		NamedEntity ent = this.getEntityByKey(key);

		if (ent == null) {
			/* Debugging code
			  for (Map.Entry<String,NamedEntity> kv : keyToEntity.entrySet()) {
				if (name.endsWith(kv.getValue().getName()) && clazz.isInstance(kv.getValue())) {
					System.err.println("Found compatible entity:"+kv.getValue()+" @ "+kv.getKey());
				}
			}*/
			return null;
		}
		if (!ent.getName().equals(name)) {
			return null;
		}
		if (! clazz.isInstance(ent)) {
			return null;
		}
		
		// this is the "normal" case (entity found and corresponds to expectations)
		tracer.msg("CDictionaryDef.removeEntity found: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);
		keyToEntity.remove(key, ent);
		entityToKey.remove(ent, key);
		return (T)ent;
	}

	public ScopingEntity removeScopingEntity(String name, ScopingEntity parent) {
		String fullName = mooseName(parent, name);
		NamedEntity fmx = this.getEntityByKey(fullName);
		if (fmx == null) {
			return null;
		}
		if ( ! (fmx instanceof ScopingEntity) ) {
			return null;
		}
		if (!fmx.getName().equals(name)) {   // is this test really needed ?
			return null;
		}

		keyToEntity.remove(fullName, fmx);
		entityToKey.remove(fmx, fullName);
		return (ScopingEntity) fmx;
	}

	/**
	 * Generates a key for an entity from its position in the file containing it
	 * @param filename of the file where the entity is declared
	 * @param anchor -- a range object that knows where the entity starts and stops in the file
	 * @return the key generated
	 */
	private String mkKey(String filename, ISourceRange anchor) {
		return mkKey(filename, anchor.getStartPos());
	}

	/**
	 * Generates a key for an entity from its position in the file containing it
	 * @param filename of the file where the entity is declared
	 * @param startPos in the file containing the entity
	 * @return the key generated
	 */
	private String mkKey(String filename, int startPos) {
		return filename + startPos;
	}

	public Namespace ensureNamespace(String name, ScopingEntity parent) {
		Namespace fmx = super.ensureFamixNamespace(name, mooseName((Namespace) parent, name));
		fmx.setIsStub(false);
		fmx.setParentScope(parent);
		return fmx;
	}

	public Package ensurePackage(String name, Package parent) {
		Package fmx = super.ensureFamixEntity(Package.class, /*key*/mooseName(parent, name), name, /*persistIt*/true);
		fmx.setIsStub(false);
		fmx.setParentPackage(parent);
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class createClass(String filename, ISourceRange anchor, String name, ContainerEntity parent) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = super.ensureFamixClass(mkKey(filename, anchor), name, parent, /*persistIt*/true);
		
		return fmx;
	}

	public Method createMethod(String filename, ISourceRange anchor, String name, Type parent) {
		Method fmx;
		fmx = super.ensureFamixMethod(mkKey(filename, anchor), name, /*signature*/name, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Attribute createAttribute(String filename, ISourceRange anchor, String name, Type parent) {
		Attribute fmx;
		fmx = super.ensureFamixAttribute(mkKey(filename, anchor), name, /*type*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public boolean assertEmpty() {
		return keyToEntity.isEmpty();
	}

}
