package eu.synectique.verveine.extractor.ref;

import java.io.File;
import java.io.RandomAccessFile;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTAttributeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.ASTAmbiguousNode;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;

public class MainRefVisitor extends AbstractRefVisitor implements ICElementVisitor {

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack2 context;

	/**
	 * The source code of the visited AST.
	 * Used to find back the contents of non-javadoc comments
	 */
	protected RandomAccessFile source;

	/**
	 * Whether a variable access is lhs (write) or not
	 */
	protected boolean inAssignmentLHS = false;
	
	public MainRefVisitor(CDictionaryRef dicoRef) {
		super(dicoRef);
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ======================================================================================================

	@Override
	public void visit(ITranslationUnit tu) {
		context = new EntityStack2();
		super.visit(tu);
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(IASTTranslationUnit node) {
//		tracer.up("IASTTranslationUnit: "+node.getFilePath());
		return super.visit(node);
	}

	@Override
	public int leave(IASTTranslationUnit node) {
//		tracer.down("IASTTranslationUnit: "+node.getFilePath());
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
/*		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		IASTName nodeName = node.getName();
		Namespace fmx = null;
//		fmx = dico.ensureFamixNamespace(nodeName.resolveBinding(), nodeName.toString());
			fmx.setIsStub(false);
		
		this.context.push(fmx);*/
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
/*		this.context.pop();
		tracer.down();*/
		return super.leave(node);
	}

	@Override
	public int visit(IASTArrayModifier node) {
//		tracer.up("IASTArrayModifier ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTArrayModifier node) {
//		tracer.down("IASTArrayModifier ");
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclarator node) {
/*		tracer.up("IASTDeclarator:");

		if (node instanceof IASTFieldDeclarator) {
			this.visit((IASTFieldDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			this.visit((IASTFunctionDeclarator)node);
		}
*/
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclarator node) {
/*		if (node instanceof IASTFieldDeclarator) {
			this.leave((IASTFieldDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			this.leave((IASTFunctionDeclarator)node);
		}
		tracer.down("IASTDeclarator: ");
		tracename(node.getName());*/
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
/*		String trace=node.getRawSignature();
		int cr = trace.indexOf('\n');
		tracer.up("IASTDeclSpecifier: "+ (cr<0 ? trace : trace.substring(0, cr)+"..."));

		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			this.visit((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			this.visit((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			this.visit((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			this.visit((ICPPASTNamedTypeSpecifier)node);
		}
*/
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclSpecifier node) {
/*		tracer.down();

		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			this.leave((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			this.leave((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			this.leave((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			this.leave((ICPPASTNamedTypeSpecifier)node);
		}
*/
		return super.leave(node);
	}

	@Override
	public int visit(IASTEnumerator node) {
		// enumeration member (i.e. a constant in an enum)
//		tracer.up("IASTEnumerator ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTEnumerator node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTExpression node) {
		if (node instanceof IASTFieldReference) {
			visit( (IASTFieldReference)node);
			return ASTVisitor.PROCESS_SKIP;
		}
		else if (node instanceof IASTFunctionCallExpression) {
			visit((IASTFunctionCallExpression)node);
			return ASTVisitor.PROCESS_SKIP;  // because we already visited the FunctionNameExpression
		}
		else if (node instanceof IASTIdExpression) {
			// ???
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTExpression node) {
		// never called
		return super.leave(node);
	}

	@Override
	public int visit(IASTInitializer node) {
//		tracer.up("IASTInitializer ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTInitializer node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
/*		tracer.msg("IASTParameterDeclaration: ");
		if (context.topMethod() != null) {
			node.accept( new ParamDeclVisitor(dico, context.topMethod()) );
		}*/
		return PROCESS_SKIP;
	}

	@Override
 	public int leave(IASTParameterDeclaration node) {
		// never actually called
		return super.leave(node);
	}

	@Override
	public int visit(IASTPointerOperator node) {
//		tracer.up("IASTPointerOperator ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTPointerOperator node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTProblem node) {
//		tracer.up("IASTProblem ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTProblem node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTStatement node) {
//		tracer.up("IASTStatement ("+node.getClass().getSimpleName()+")");
		return super.visit(node);
	}

	@Override
	public int leave(IASTStatement node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTTypeId node) {
//		tracer.up("IASTTypeId ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTTypeId node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICASTDesignator node) {
//		tracer.up("ICASTDesignator ");
		return super.visit(node);
	}

	@Override
	public int leave(ICASTDesignator node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
//		tracer.up("ICPPASTBaseSpecifier:");
//		tracename(node.getName());
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTBaseSpecifier node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTCapture node) {
//		tracer.up("ICPPASTCapture ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTCapture node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTTemplateParameter node) {
//		tracer.up("ICPPASTTemplateParameter ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTTemplateParameter node) {
//		tracer.down();
		return super.leave(node);
	}

	@SuppressWarnings("restriction")
	@Override
	public int visit(ASTAmbiguousNode node) {
//		tracer.msg("ASTAmbiguousNode ");
		return super.visit(node);
	}

	@Override
	public int visit(IASTAttribute node) {
//		tracer.up("IASTAttribute: "+node.getName());
		return super.visit(node);
	}

	@Override
	public int leave(IASTAttribute node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTAttributeSpecifier node) {
//		tracer.up("IASTAttributeSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTAttributeSpecifier node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTComment node) {
//		tracer.up("IASTComment "+node.toString());
		return super.visit(node);
	}

	@Override
	public int leave(IASTComment node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTToken node) {
//		tracer.up("IASTToken type="+node.getTokenType());
		return super.visit(node);
	}

	@Override
	public int leave(IASTToken node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTClassVirtSpecifier node) {
//		tracer.up("ICPPASTClassVirtSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTClassVirtSpecifier node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTDecltypeSpecifier node) {
//		tracer.up("ICPPASTDecltypeSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTDecltypeSpecifier node) {
//		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTVirtSpecifier node) {
//		tracer.up("ICPPASTVirtSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTVirtSpecifier node) {
//		tracer.down();
		return super.leave(node);
	}

	// ADDITIONAL VISITING METODS ON AST =======================================================================================================

	/** reference to a Field of a struct
	 */
	public void visit( IASTFieldReference node) {
		IASTName nodeName = null;
		Attribute fmx = null;
		IBinding bnd;

		nodeName = node.getFieldName();
		if (nodeName != null) {
			bnd = nodeName.resolveBinding();
			fmx = (Attribute) dico.getEntityByKey(bnd);
			if (fmx != null) {
				IASTImageLocation loc = nodeName.getImageLocation();
				tracer.msg("IASTFieldReference to:"+fmx.getName()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
			}
		}
	}

	public void visit(IASTFunctionCallExpression node) {
/*		IASTName nodeName = null;
		BehaviouralEntity fmx = null;
		IBinding bnd;

		nodeName = node.getFunctionNameExpression();
		if (nodeName != null) {
			bnd = nodeName.resolveBinding();
			fmx = (Attribute) dico.getEntityByKey(bnd);
			if (fmx != null) {
				IASTImageLocation loc = nodeName.getImageLocation();
				tracer.msg("IASTFunctionCallExpression to:"+fmx.getName()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
			}
		}*/
	}

	
	public int visit(ICASTCompositeTypeSpecifier node) {
//		tracer.msg("    -> ICASTCompositeTypeSpecifier");
		return PROCESS_CONTINUE;
	}

	public int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	/** Visiting a class definition
	 */
	public int visit(ICPPASTCompositeTypeSpecifier node) {
//		tracer.msg("    -> ICPPASTCompositeTypeSpecifier");
		IASTName nodeName = node.getName();
		IBinding bnd = nodeName.resolveBinding();
		IASTFileLocation loc = nodeName.getFileLocation();

//		tracename(nodeName);
//		traceanchor(loc);

		if ( (bnd != null) && (loc != null) && (loc.getFileName().equals(this.filename)) ) {
//			tracer.msg("creating famix class:"+nodeName.toString());
			eu.synectique.verveine.core.gen.famix.Class fmx = null;
//			fmx = dico.ensureFamixClass(bnd, nodeName.toString(), /*owner*/(ContainerEntity)context.top(), /*persistIt*/true);
			if (fmx != null) {
				fmx.setIsStub(false);

				this.context.push(fmx);
//				dico.addSourceAnchor(fmx, node, /*oneLineAnchor*/false);
			}
		}

		return PROCESS_CONTINUE;
	}

	public int leave(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;		
	}

	public int visit(IASTEnumerationSpecifier node) {
//		tracer.msg("    -> IASTEnumerationSpecifier");
		return PROCESS_CONTINUE;
	}

	public int leave(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	public int visit(ICPPASTNamedTypeSpecifier node) {
//		tracer.msg("    -> ICPPASTNamedTypeSpecifier");
		return PROCESS_CONTINUE;
	}

	public int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	public int visit(IASTFieldDeclarator node) {
//		tracer.msg("    -> IASTFieldDeclarator");
		return PROCESS_CONTINUE;
	}

	public int leave(IASTFieldDeclarator node) {
		return PROCESS_CONTINUE;
	}

	public int visit(ICPPASTFunctionDefinition node) {
//		tracer.msg("    -> CPPASTFunctionDefinition ");
		return PROCESS_SKIP;
	
	}

	public int leave(ICPPASTFunctionDefinition node) {
		return PROCESS_SKIP;
	}
	
	public void visit(IASTFunctionDeclarator node) {
		IASTFunctionDeclarator func = (IASTFunctionDeclarator)node;
		IASTName nodeName = func.getName();
		IBinding bnd = nodeName.resolveBinding();
		IASTFileLocation loc = nodeName.getFileLocation();
		BehaviouralEntity fmx = null;

//		tracer.msg("    -> IASTFunctionDeclarator");
//		tracename(nodeName);
//		traceanchor(loc);

		if ( (bnd != null) && (loc != null) && (loc.getFileName().equals(this.filename)) ) {
			boolean iscpp = (bnd instanceof ICPPMethod);
			
			if (iscpp) {
//				tracer.msg("creating famix method:"+nodeName.toString());
//				fmx = dico.ensureFamixMethod(bnd, nodeName.toString(), /*signature*/nodeName.toString()+"(", /*ret.type*/null, context.topType(), /*persitIt*/true);
			}
			else {
//				tracer.msg("creating famix function:"+nodeName.toString());
//				fmx = dico.ensureFamixFunction(bnd, nodeName.toString(), /*signature*/nodeName.toString()+"(", /*ret.type*/null, (ContainerEntity)context.top(), /*persitIt*/true);				
			}

			if (fmx != null) {
				fmx.setIsStub(false);
				this.context.push(fmx);
				if (iscpp) {
					if (bnd instanceof ICPPConstructor) {
						((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
					}
				}
			}
		}
	}
	
	protected void leave(IASTFunctionDeclarator node) {
		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof eu.synectique.verveine.core.gen.famix.Type) &&
			 (top.getName().equals(node.getName().toString())) ) {
			BehaviouralEntity fmx = (BehaviouralEntity) context.pop();
			fmx.setSignature(fmx.getSignature()+")");
		}
	}

	// more specialized trace methods

	protected void tracename(IASTName name) {
		if (name != null) {
			tracer.msg("    -> '"+name.toString()+ "' " +(name.resolveBinding()!=null ?"is":"not") +" bound");
		}
		else {
			tracer.msg("    -> null name");
		}
	}
	
	protected void traceanchor(IASTFileLocation loc) {
		if (loc != null) {
			String filename = loc.getFileName();
			filename = filename.substring(filename.lastIndexOf(File.separatorChar)+1);
			tracer.msg("    -> fileanchor=<"+filename+","+loc.getNodeOffset()+">");
		}
		else {
			tracer.msg("    -> fileanchor=<null>");
		}
	}

}
