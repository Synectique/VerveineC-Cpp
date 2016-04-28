package eu.synectique.verveine.extractor.def;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.model.ISourceRange;

import ch.akuhn.fame.Repository;
import ch.akuhn.util.Tab;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.IndexedFileAnchor;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.SourceAnchor;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
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
	public <T extends NamedEntity> T resolveEntity(String filename, int startPos, String name, Class<T> clazz) {
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

		tracer.msg("CDictionaryDef.resolveEntity found: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);

		return (T)ent;
	}

	public ScopingEntity resolveScopingEntity(String name, ScopingEntity parent) {
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

		return (ScopingEntity) fmx;
	}

	/**
	 * Generates a key for an entity from its position in the file containing it
	 * @param filename of the file where the entity is declared
	 * @param anchor -- a range object that knows where the entity starts and stops in the file
	 * @return the key generated
	 */
	private String mkKey(String filename, ISourceRange anchor) {
		return mkKey(filename, anchor.getIdStartPos());
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

	/**
	 * Adds location information to a Famix Entity.
	 * Location informations are: <b>name</b> of the source file and <b>position</b> in this file.
	 * @param fmx -- Famix Entity to add the anchor to
	 * @param filename -- name of the file being visited
	 * @param ast -- ASTNode, where the information are extracted
	 * @return the Famix SourceAnchor added to fmx. May be null in case of incorrect/null parameter
	 */
	public SourceAnchor addSourceAnchor(SourcedEntity fmx, String filename, IASTFileLocation anchor) {
		IndexedFileAnchor fa = null;

		if ( (fmx == null) || (anchor == null) ) {
			return null;
		}

		// position in source file
		int beg = anchor.getNodeOffset();
		int end = beg + anchor.getNodeLength();

		// create the Famix SourceAnchor
		fa = new IndexedFileAnchor();
		fa.setStartPos(beg);
		fa.setEndPos(end);
		fa.setFileName(filename);

		fmx.setSourceAnchor(fa);
		famixRepo.add(fa);

		return fa;
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

	public eu.synectique.verveine.core.gen.famix.Class ensureClass(String filename, ISourceRange anchor, String name, ContainerEntity parent) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = super.ensureFamixClass(mkKey(filename, anchor), name, parent, /*persistIt*/true);
		
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class ensureClass(String filename, int startPos, String name, ContainerEntity parent) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = super.ensureFamixClass(mkKey(filename, startPos), name, parent, /*persistIt*/true);
		
		return fmx;
	}

	public ParameterizableClass ensureParameterizableClass(String filename, ISourceRange anchor, String name, ContainerEntity parent) {
		ParameterizableClass fmx;
		fmx = super.ensureFamixParameterizableClass(mkKey(filename, anchor), name, parent, /*persistIt*/true);
		
		return fmx;
	}

	public ParameterizableClass ensureParameterizableClass(String filename, int startPos, String name, ContainerEntity parent) {
		ParameterizableClass fmx;
		fmx = super.ensureFamixParameterizableClass(mkKey(filename, startPos), name, parent, /*persistIt*/true);
		
		return fmx;
	}

	public Function ensureFunction(String filename, ISourceRange anchor, String name, String sig, ContainerEntity parent) {
		Function fmx;
		fmx = super.ensureFamixFunction(mkKey(filename, anchor), name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Function ensureFunction(String filename, int startPos, String name, String sig, ContainerEntity parent) {
		Function fmx;
		fmx = super.ensureFamixFunction(mkKey(filename, startPos), name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Method ensureMethod(String filename, ISourceRange anchor, String name, String sig, Type parent) {
		Method fmx;
		fmx = super.ensureFamixMethod(mkKey(filename, anchor), name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Method ensureMethod(String filename, int startPos, String name, String sig, Type parent) {
		Method fmx;
		fmx = super.ensureFamixMethod(mkKey(filename, startPos), name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Attribute ensureAttribute(String filename, ISourceRange anchor, String name, Type parent) {
		Attribute fmx;
		fmx = super.ensureFamixAttribute(mkKey(filename, anchor), name, /*type*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Attribute ensureAttribute(String filename, int startPos, String name, Type parent) {
		Attribute fmx;
		fmx = super.ensureFamixAttribute(mkKey(filename, startPos), name, /*type*/null, parent, /*persistIt*/true);

		return fmx;
	}

}
