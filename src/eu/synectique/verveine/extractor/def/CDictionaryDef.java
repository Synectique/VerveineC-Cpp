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
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.Tracer;

public class CDictionaryDef extends Dictionary<String> {

	private ITracer tracer;

	public CDictionaryDef(Repository famixRepo) {
		super(famixRepo);
		tracer = new NullTracer();
	}

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

	public <T extends NamedEntity> void listAll(Class<T> clazz) {
		for (Map.Entry<String, NamedEntity> kv : keyToEntity.entrySet()) {
			if (clazz.isInstance(kv.getValue())) {
				System.err.println("list all "+clazz.getSimpleName()+": "+kv.getValue().getName()+" @ "+kv.getKey());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends NamedEntity> T removeEntity(String filename, int startPos, String name, Class<T> clazz) {
		String key = mkKey(filename, startPos);
		NamedEntity ent = this.getEntityByKey(key);
		if (ent == null) {
			tracer.msg("CDictionaryDef.removeEntity could not find: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);
		}
		else if (!ent.getName().equals(name)) {
			tracer.msg("CDictionaryDef.removeEntity found name: "+ent.getName()+" -- for: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);
		}
		else if (! clazz.isInstance(ent)) {
			tracer.msg("CDictionaryDef.removeEntity found type: "+ent.getClass().getSimpleName()+" -- for: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);
		}
		else {
			// this is the "normal" case (entity found and corresponds to expectations)
			tracer.msg("CDictionaryDef.removeEntity found: "+name+"/"+clazz.getSimpleName()+" @ "+filename+"/"+startPos);
			keyToEntity.remove(key, ent);
			entityToKey.remove(ent, key);
			return (T)ent;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends NamedEntity> T removeUniqEntity(String name, Class<T> clazz) {
		NamedEntity ent = this.getEntityByKey(name);
		if (ent == null) {
			tracer.msg("CDictionaryDef.removeUniqEntity could not find: "+name+"/"+clazz.getSimpleName());
		}
		else if (!ent.getName().equals(name)) {
			tracer.msg("CDictionaryDef.removeUniqEntity found name: "+ent.getName()+" -- for: "+name+"/"+clazz.getSimpleName());
		}
		else if (! clazz.isInstance(ent)) {
			tracer.msg("CDictionaryDef.removeUniqEntity found type: "+ent.getClass().getSimpleName()+" -- for: "+name+"/"+clazz.getSimpleName());
		}
		else {
			tracer.msg("CDictionaryDef.removeUniqEntity found: "+name+"/"+clazz.getSimpleName());
			keyToEntity.remove(name, ent);
			entityToKey.remove(ent, name);
			return (T)ent;
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
