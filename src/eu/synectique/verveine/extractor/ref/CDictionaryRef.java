package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.IBinding;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.NamedEntity;

public class CDictionaryRef extends Dictionary<IBinding> {

	public CDictionaryRef(Repository famixRepo) {
		super(famixRepo);
	}

	public void remapEntityToKey(IBinding key, NamedEntity ent) {
		super.mapEntityToKey(key, ent);
	}
	
}
