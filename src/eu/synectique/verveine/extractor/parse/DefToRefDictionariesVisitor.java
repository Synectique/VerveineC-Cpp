package eu.synectique.verveine.extractor.parse;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTToken;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTClassVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDecltypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.ref.AbstractRefVisitor;
import eu.synectique.verveine.extractor.ref.CDictionaryRef;
import eu.synectique.verveine.extractor.utils.Tracer;

public class DefToRefDictionariesVisitor extends AbstractRefVisitor {

	private CDictionaryDef dicoDef;

	public DefToRefDictionariesVisitor(CDictionaryDef dicoDef, CDictionaryRef dicoRef) {
		super(dicoRef, /*visitNodes*/false);
		this.dicoDef = dicoDef;
		tracer = new Tracer();

		shouldVisitNamespaces = true;
		shouldVisitTranslationUnit = true;
		shouldVisitDeclarations = true;
		shouldVisitDeclSpecifiers = true;
		shouldVisitDecltypeSpecifiers = true;
		shouldVisitDeclarators = true;
		
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		IASTName nodeName = node.getName();
		//tracer.msg("ICPPASTNamespaceDefinition: "+nodeName+" offset="+nodeName.getFileLocation().getNodeOffset());
		Namespace fmx = dicoDef.removeUniqEntity(nodeName.getLastName().toString(), Namespace.class);
		if (fmx != null) {
			dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
		}
		return super.visit(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			//this.visit((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			this.visit((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			//this.visit((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			//this.visit((ICPPASTNamedTypeSpecifier)node);
		}

		return super.visit(node);
	}

	@Override
	public int visit(IASTDeclarator node) {
		if (node instanceof ICPPASTFieldDeclarator) {
			this.visit((ICPPASTFieldDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			this.visit((IASTFunctionDeclarator)node);
		}

		return super.visit(node);
	}

	@Override
	public int visit(IASTDeclaration node) {
		if (node instanceof IASTSimpleDeclaration) {
			this.visit( (IASTSimpleDeclaration)node );
		}
		return super.visit(node);
	}

	@Override
	public int visit(IASTEnumerator node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTExpression node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTInitializer node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTName node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTPointerOperator node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTProblem node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTStatement node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTToken node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTTranslationUnit node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTTypeId node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICASTDesignator node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTCapture node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTClassVirtSpecifier node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTDecltypeSpecifier node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTTemplateParameter node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTVirtSpecifier node) {
		return super.visit(node);
	}

	// ADDITIONAL VISITING METODS ON AST =======================================================================================================

	/** Visiting a class definition
	 */
	public int visit(ICPPASTCompositeTypeSpecifier node) {
		//tracer.msg("    -> ICPPASTCompositeTypeSpecifier");
		IASTName nodeName = null;
		IASTImageLocation loc = null;
		eu.synectique.verveine.core.gen.famix.Class fmx = null;

		nodeName = node.getName();
		tracer.msg("ICPPASTCompositeTypeSpecifier: "+nodeName.toString());
		for (IASTDeclaration decl : node.getMembers()) {
			tracer.msg("  -> "+decl.getRawSignature()+"  / "+decl.getClass().getSimpleName());
		}
		
		if (nodeName != null) {
			loc = nodeName.getImageLocation();
			if (loc != null) {
				fmx = dicoDef.removeEntity(filename, loc.getNodeOffset(), nodeName.toString(), eu.synectique.verveine.core.gen.famix.Class.class);
				if (fmx != null) {
					dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
				}
			}
		}

		return PROCESS_CONTINUE;
	}

	public void visit(IASTFunctionDeclarator node) {
		IASTName nodeName = null;
		IASTImageLocation loc = null;
		BehaviouralEntity fmx = null;
		IBinding bnd;

		nodeName = node.getName();
		if (nodeName != null) {
			loc = nodeName.getImageLocation();
			if (loc != null) {
				bnd = nodeName.resolveBinding();
				
				if (bnd instanceof ICPPMethod) {   // C++ method or C function ?
					fmx = dicoDef.removeEntity(filename, loc.getNodeOffset(), nodeName.toString(), Method.class);
				}
				else {
					fmx = dicoDef.removeEntity(filename, loc.getNodeOffset(), nodeName.toString(), Function.class);
				}
				
				if (fmx != null) {
					dico.remapEntityToKey(bnd, fmx);
				}
			}
		}
	}

	public int visit(IASTSimpleDeclaration node) {
		tracer.msg("IASTSimpleDeclaration: "+node.getRawSignature());
		for (IASTDeclarator de : node.getDeclarators()) {
			tracer.msg("  -> "+de.getName().toString()+"  / "+de.getClass().getSimpleName());
		}

/*		IASTName nodeName = null;
		IASTImageLocation loc = null;
		Attribute fmx = null;

		nodeName = node.getName();
		tracer.msg("DefToRefDictionariesVisitor IASTFieldDeclarator: "+nodeName.toString());
		if (nodeName != null) {
			loc = nodeName.getImageLocation();
			if (loc != null) {
				fmx = dicoDef.removeEntity(filename, loc.getNodeOffset(), nodeName.toString(), Attribute.class);
				if (fmx != null) {
					tracer.msg("DefToRefDictionariesVisitor found Attribute: "+nodeName.toString()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
					dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
				}
				else {
					tracer.msg("DefToRefDictionariesVisitor could not find Attribute: "+nodeName.toString()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
				}
			}
		}*/
		return PROCESS_CONTINUE;
	}

	public int visit(ICPPASTFieldDeclarator node) {
		IASTName nodeName = null;
		IASTNodeLocation loc = null;
		Attribute fmx = null;

		nodeName = node.getName();
		loc = nodeName.getImageLocation();
		tracer.msg("DefToRefDictionariesVisitor ICPPASTFieldDeclarator: "+nodeName.toString()+" @ "+filename+"/"+(loc==null?"noloc":loc.getNodeOffset()));
		if (nodeName != null) {
//			loc = nodeName.getImageLocation();
			if (loc != null) {
				fmx = dicoDef.removeEntity(filename, loc.getNodeOffset(), nodeName.toString(), Attribute.class);
				if (fmx != null) {
//					tracer.msg("DefToRefDictionariesVisitor found Attribute: "+nodeName.toString()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
					dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
				}
				else {
//					tracer.msg("DefToRefDictionariesVisitor could not find Attribute: "+nodeName.toString()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
				}
			}
		}
		return PROCESS_CONTINUE;
	}

}
