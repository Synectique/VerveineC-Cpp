package eu.synectique.verveine.extractor.def;

import org.eclipse.cdt.core.model.ISourceRange;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.Type;

public class CDictionaryDef extends Dictionary<String> {

	public CDictionaryDef(Repository famixRepo) {
		super(famixRepo);
	}

	public NamedEntity removeEntity(String filename, int startPos, String name, Class<Namespace> clazz) {
		String key = mkKey(filename, startPos);
		NamedEntity ent = this.getEntityByKey(key);
		if (ent == null) {
			System.err.println("CDictionaryDef.findEntity could not find: "+name+"/"+clazz+" @ "+filename+"/"+startPos);
		}
		else if (!ent.getName().equals(name)) {
			System.err.println("CDictionaryDef.findEntity found name: "+ent.getName()+" -- for: "+name+"/"+clazz+" @ "+filename+"/"+startPos);
		}
		else if (! clazz.isInstance(ent)) {
			System.err.println("CDictionaryDef.findEntity found type: "+ent.getClass()+" -- for: "+name+"/"+clazz+" @ "+filename+"/"+startPos);
		}
		else {
			// this is the "normal" case (entity found and corresponding to expectations
			keyToEntity.remove(key, ent);
			entityToKey.remove(ent, key);
			return ent;
		}
		return null;
	}

	private String mkKey(String filename, ISourceRange anchor) {
		return mkKey(filename, anchor.getIdStartPos());
	}

	private String mkKey(String filename, int startPos) {
		return filename + startPos;
	}

	public Namespace ensureNamespace(String name, ScopingEntity parent) {
		Namespace fmx = super.ensureFamixNamespace(name, name);  // namespace's name is used as its own key
		fmx.setIsStub(false);
		fmx.setParentScope(parent);
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Package ensurePackage(String name, eu.synectique.verveine.core.gen.famix.Package parent) {
		eu.synectique.verveine.core.gen.famix.Package fmx = super.ensureFamixUniqEntity(eu.synectique.verveine.core.gen.famix.Package.class, /*key*/name, name);  // namespace's name is used as its own key
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

}
