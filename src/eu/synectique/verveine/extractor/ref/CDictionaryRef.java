package eu.synectique.verveine.extractor.ref;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IBinding;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ImplicitVariable;
import eu.synectique.verveine.core.gen.famix.IndexedFileAnchor;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.SourceAnchor;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;

public class CDictionaryRef extends Dictionary<IBinding> {

	public CDictionaryRef(Repository famixRepo) {
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
	public IBinding findkeyfrommethodname(String name) {
		IBinding key = null;
		for (Map.Entry<IBinding,NamedEntity> ent :keyToEntity.entrySet()) {
			if ( (ent.getValue() instanceof Method) && (name.endsWith(ent.getValue().getName()))) {
				key = ent.getKey();
				break;
			}
		}
		return key;
	}

	public void remapEntityToKey(IBinding key, NamedEntity ent) {
		super.mapEntityToKey(key, ent);
	}

	public void listKeyEntities() {
		for (Map.Entry<IBinding,NamedEntity> ent : keyToEntity.entrySet()) {
			System.err.println("CDictionaryRef key="+ent.getKey()+"/"+ent.getKey().hashCode()+"  val="+ent.getValue().getName());
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

	public eu.synectique.verveine.core.gen.famix.Class ensureClass(IBinding bnd, String name) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = (Class) keyToEntity.get(bnd);
		if (fmx == null) {
			fmx = super.ensureFamixClass(bnd, name, /*owner*/null, /*persistIt*/true);
		}
		
		return fmx;
	}

}
