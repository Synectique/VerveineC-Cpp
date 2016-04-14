package eu.synectique.verveine.extractor.ref;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;

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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDecltypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.ASTAmbiguousNode;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.utils.Tracer;

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

	private CDictionaryDef dicoDef;

	public MainRefVisitor(CDictionaryDef dicoDef, CDictionaryRef dicoRef) {
		super(dicoRef, /*visitNodes*/true);
		this.dicoDef = dicoDef;
		tracer = new Tracer();
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ======================================================================================================

	@Override
	public void visit(ITranslationUnit tu) {
		context = new EntityStack2();    // "reseting" context
		super.visit(tu);
	}

	/**
	 * Removes the package corresponding to elt from dicoDef.<BR>
	 * No need to put it in dicoRef as it is not used here
	 */
	@Override
	public void visit(ICContainer elt) {
		dicoDef.removeUniqEntity(elt.getElementName(), eu.synectique.verveine.core.gen.famix.Package.class);
		super.visit(elt);
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		IASTName nodeName = node.getName();
		IBinding bnd = nodeName.resolveBinding();

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		Namespace fmx = dicoDef.removeUniqEntity(nodeName.getLastName().toString(), Namespace.class);
		if (fmx == null) {
			return PROCESS_SKIP;
		}
		dico.remapEntityToKey(nodeName.resolveBinding(), fmx);		
		this.context.push(fmx);

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		tracer.down();
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
			tracer.msg("IASTExpression ("+node.getClass().getSimpleName()+") @ "+node.getFileLocation().getStartingLineNumber());
		}

		return super.visit(node);
	}

	@Override
	public int visit(IASTInitializer node) {
//		tracer.up("IASTInitializer ");
		return super.visit(node);
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
	public int visit(IASTComment node) {
//		tracer.up("IASTComment "+node.toString());
		return super.visit(node);
	}

	@Override
	public int leave(IASTComment node) {
//		tracer.down();
		return super.leave(node);
	}


	// ABSTRACT_REF_VISITOR VISITING METODS ON AST =======================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		tracer.msg("    -> ICPPASTCompositeTypeSpecifier");
		IASTName nodeName = node.getName();
		IASTImageLocation loc;
		IBinding bnd;
		Class fmx;
		
		if (nodeName == null) {
			System.err.println("ERROR nodeName=null");
			return PROCESS_CONTINUE;
		}

		loc = nodeName.getImageLocation();
		bnd = nodeName.resolveBinding();
		if ( (loc == null) || (bnd == null) ) {
			System.err.println("ERROR loc/bnd=null");
			return PROCESS_SKIP;
		}

		fmx = dicoDef.removeEntity(simpleFilename(loc), loc.getNodeOffset(), nodeName.toString(), eu.synectique.verveine.core.gen.famix.Class.class);
		if (fmx == null) {
			System.err.println("ERROR fmx=null");
			return PROCESS_SKIP;
		}

		dico.remapEntityToKey(nodeName.resolveBinding(), fmx);

		this.context.push(fmx);
//		dico.addSourceAnchor(fmx, node, /*oneLineAnchor*/false);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();

		return PROCESS_CONTINUE;		
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		IASTFunctionDeclarator func = (IASTFunctionDeclarator)node;
		IASTName nodeName = func.getName();
		IASTImageLocation loc;
		IBinding bnd;
		BehaviouralEntity fmx = null;

		tracer.msg("    -> IASTFunctionDeclarator");

		if (nodeName == null) {
			return PROCESS_CONTINUE;
		}

		loc = nodeName.getImageLocation();
		bnd = nodeName.resolveBinding();
		
		if ( (loc == null) || (bnd==null) ) {
			return PROCESS_SKIP;
		}

		if (bnd instanceof ICPPMethod) {   // C++ method
			fmx = dicoDef.removeEntity(simpleFilename(loc), loc.getNodeOffset(), nodeName.toString(), Method.class);
		}
		else {                    //   C function ?
			fmx = dicoDef.removeEntity(simpleFilename(loc), loc.getNodeOffset(), nodeName.toString(), Function.class);
		}

		if (fmx == null) {
			return PROCESS_SKIP;
		}

		dico.remapEntityToKey(bnd, fmx);

		fmx.setSignature(bnd.toString());

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}
	
	@Override
	protected int leave(IASTFunctionDeclarator node) {
/*		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof eu.synectique.verveine.core.gen.famix.Type) &&
			 (top.getName().equals(node.getName().toString())) ) {
			BehaviouralEntity fmx = (BehaviouralEntity) context.pop();
		}*/
		return PROCESS_CONTINUE;
	}

	@Override
	protected int visit(ICPPASTDeclarator node) {
		IASTName nodeName = null;
		IASTImageLocation loc = null;
		Attribute fmx = null;

		nodeName = node.getName();
		loc = nodeName.getImageLocation();
		if (nodeName != null) {
			loc = nodeName.getImageLocation();
			if (loc != null) {
				fmx = dicoDef.removeEntity(simpleFilename(loc), loc.getNodeOffset(), nodeName.toString(), Attribute.class);
				if (fmx != null) {
					dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
				}
			}
		}
		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTDeclarator node) {
		// TODO Auto-generated method stub
		return 0;
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

	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
//		tracer.msg("    -> ICASTCompositeTypeSpecifier");
		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
//		tracer.msg("    -> IASTEnumerationSpecifier");
		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
//		tracer.msg("    -> ICPPASTNamedTypeSpecifier");
		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	// UTILITIES =======================================================================================================

	private String simpleFilename(IASTImageLocation loc) {
		if (loc == null) {
			return null;
		}

		String name = loc.getFileName();
		int i = name.lastIndexOf(File.separatorChar);
		
		if (i<0) {
			return name;
		}
		else {
			return name.substring(i+1);
		}
	}

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
