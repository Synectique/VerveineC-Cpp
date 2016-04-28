package eu.synectique.verveine.extractor.ref;

import java.util.Collection;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ISourceRange;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
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

public class CDictionary extends Dictionary<IIndexBinding> {
	
	public final static String DESTRUCTOR_KIND_MARKER = "destructor";

 	public CDictionary(Repository famixRepo) {
		super(famixRepo);
	}

	/**(
	 * Debug method
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
		System.err.println("CDictionaryRef ns="+ns+", pk="+pk+", cl="+cl+", mt="+mt+", at="+at+", ot="+ot);
	}

	/**
	 * Debug method
	 */
	public IIndexBinding findkeyfrommethodname(String name) {
		IIndexBinding key = null;
		for (Map.Entry<IIndexBinding,NamedEntity> ent :keyToEntity.entrySet()) {
			if ( (ent.getValue() instanceof Method) && (name.endsWith(ent.getValue().getName()))) {
				key = ent.getKey();
				break;
			}
		}
		return key;
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

	public Namespace ensureNamespace(IIndexBinding key, String name, ScopingEntity parent) {
		Namespace fmx = super.ensureFamixNamespace(key, name);
		fmx.setIsStub(false);
		fmx.setParentScope(parent);
		return fmx;
	}

	public Package ensurePackage(String name, Package parent) {
		Package fmx = super.ensureFamixEntity(Package.class, /*key*/null, name, /*persistIt*/true);
		fmx.setIsStub(false);
		fmx.setParentPackage(parent);
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class ensureClass(IIndexBinding key, String name, ContainerEntity owner) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = (Class) keyToEntity.get(key);
		if (fmx == null) {
			fmx = super.ensureFamixClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterizableClass ensureParameterizableClass(IIndexBinding key, String name, ContainerEntity owner) {
		ParameterizableClass fmx;
		fmx = (ParameterizableClass) keyToEntity.get(key);
		if (fmx == null) {
			fmx = super.ensureFamixParameterizableClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public Function ensureFunction(IIndexBinding key, String name, String sig, ContainerEntity parent) {
		Function fmx;
		fmx = super.ensureFamixFunction(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Method ensureMethod(IIndexBinding key, String name, String sig, Type parent) {
		Method fmx;
		fmx = super.ensureFamixMethod(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public Attribute ensureAttribute(IIndexBinding key, String name, Type parent) {
		Attribute fmx;
		fmx = super.ensureFamixAttribute(key, name, /*type*/null, parent, /*persistIt*/true);

		return fmx;
	}

	public ScopingEntity resolveNamespaceManually(String name, NamedEntity context) {
		Collection candidates;
		ScopingEntity found = null;
		
		if (context == null) {
			candidates = famixRepo.all(Namespace.class);
		}
		else {
			candidates = ((ScopingEntity)context).getChildScopes();
		}

		for (ScopingEntity scope : (Collection<ScopingEntity>)candidates) {
			if (scope.getName().equals(name)) {
				found = scope;
				break;
			}
		}

		return found;
	}

	private BehaviouralEntity resolveBehaviouralManually(String name, String signature, NamedEntity context) {
		Collection candidates = null;
		BehaviouralEntity found = null;
		
		if (context == null) {
			candidates = famixRepo.all(BehaviouralEntity.class);
		}
		else {
			
		}

		for (ScopingEntity scope : (Collection<ScopingEntity>)candidates) {
			if (scope.getName().equals(name)) {

				break;
			}
		}

		return found;
	}


}
