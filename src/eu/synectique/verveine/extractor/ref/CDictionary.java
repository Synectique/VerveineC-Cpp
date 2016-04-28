package eu.synectique.verveine.extractor.ref;

import java.util.Collection;
import java.util.Map;

import javax.lang.model.UnknownEntityException;

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
import eu.synectique.verveine.core.gen.famix.UnknownVariable;

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

	protected NamedEntity secureGetEntity(IIndexBinding key) {
		if (key == null) {
			return null;
		}
		else {
			return keyToEntity.get(key);
		}
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
		Namespace fmx;
		fmx = (Namespace) secureGetEntity(key);
		if (fmx == null) {
			super.ensureFamixNamespace(key, name);
		}
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
		fmx = (Class) secureGetEntity(key);
		if (fmx == null) {
			fmx = super.ensureFamixClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterizableClass ensureParameterizableClass(IIndexBinding key, String name, ContainerEntity owner) {
		ParameterizableClass fmx;
		fmx = (ParameterizableClass) secureGetEntity(key);
		if (fmx == null) {
			fmx = super.ensureFamixParameterizableClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public Function ensureFunction(IIndexBinding key, String name, String sig, ContainerEntity parent) {
		Function fmx;
		fmx = (Function) secureGetEntity(key);
		if (fmx == null) {
			fmx = super.ensureFamixFunction(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);
		}

		return fmx;
	}

	public Method ensureMethod(IIndexBinding key, String name, String sig, Type parent) {
		Method fmx;
		fmx = (Method) secureGetEntity(key);
		if (fmx == null) {
			fmx = super.ensureFamixMethod(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);
		}

		return fmx;
	}

	public Attribute ensureAttribute(IIndexBinding key, String name, Type parent) {
		Attribute fmx;
		fmx = (Attribute) secureGetEntity(key);
		if (fmx == null) {
			fmx = super.ensureFamixAttribute(key, name, /*type*/null, parent, /*persistIt*/true);
		}

		return fmx;
	}

	/**
	 * Create an UnknownVariable. parent currently not used
	 */
	public UnknownVariable createFamixUnknownVariable(String name, NamedEntity parent) {
		UnknownVariable fmx;
		
		fmx = ensureFamixEntity(UnknownVariable.class, /*key*/null, name, /*persistIt*/true);
		fmx.setIsStub(true);
		
		return fmx;
	}


}
