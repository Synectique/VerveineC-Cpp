package eu.synectique.verveine.extractor.parse;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;

import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.ref.AbstractRefVisitor;
import eu.synectique.verveine.extractor.ref.CDictionaryRef;

public class DefToRefDictionariesVisitor extends AbstractRefVisitor {

	private CDictionaryDef dicoDef;

	public DefToRefDictionariesVisitor(CDictionaryDef dicoDef, CDictionaryRef dicoRef) {
		super(dicoRef, /*visitNodes*/false);
		this.dicoDef = dicoDef;

		//shouldVisitAttributes = true;
		//shouldVisitDeclarations = true;
		shouldVisitNamespaces = true;
		shouldVisitTranslationUnit = true;
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		IASTName nodeName = node.getName();
		tracer.up("ICPPASTNamespaceDefinition: "+nodeName+" offset="+nodeName.getFileLocation().getNodeOffset());
		Namespace fmx = (Namespace) dicoDef.removeEntity(filename, nodeName.getFileLocation().getNodeOffset(), nodeName.getLastName().toString(), Namespace.class);
		if (fmx != null) {
			dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
		}
		return super.visit(node);
	}

}
