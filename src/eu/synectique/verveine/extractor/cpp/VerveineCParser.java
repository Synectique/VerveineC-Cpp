package eu.synectique.verveine.extractor.cpp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class VerveineCParser {

	private IParserLogService log;
	private Map<String,String> definedSymbols;
	private String[] includePaths;
	private IScannerInfo info;
	private IncludeFileContentProvider provider;
	private IIndex indexer;
	
	Map<IBinding,IASTName> dico;

	public void parse(String projName) {
		dico = new HashMap<IBinding,IASTName>();
		
        log = ParserFactory.createDefaultLogService();
        definedSymbols = new HashMap<String,String>();
        includePaths = new String[0];
        info = new ScannerInfo(definedSymbols,includePaths);
        provider = IncludeFileContentProvider.getEmptyFilesProvider();
        
        ICProject project = createProject(projName);
        indexer = createIndex(project);
		//System.out.println("VerveineCParser created all helpers for project: " + project.getElementName());

		try {
			System.out.println("got description of project: "+ project.getProject().getDescription().getName());
			for (String nat : project.getProject().getDescription().getNatureIds()) {
				System.out.println("project nature: " + nat);
			}
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			findTU(project.getChildren()); //getAllSourceRoots());
		} catch (CModelException e) {
			System.err.println("Got CModelException (\""+ e.getMessage() +"\") while trying to getAllSourceRoots");
		}	
	}
	
	protected void findTU(ICElement[] elts) {
		for (ICElement icElement : elts) {
			if (icElement instanceof ICContainer) {
				try {
					findTU( ((ICContainer) icElement).getChildren());
				} catch (CModelException e) {
					System.err.println("Got CModelException (\""+ e.getMessage() +"\") while trying to getChildren of "+icElement.getElementName());
				}
			}
			else if (icElement instanceof ITranslationUnit) {
				System.out.println("found TU:" + icElement.getElementName());
				try {
					visitTU( ((ITranslationUnit)icElement).getAST());
				} catch (CoreException e) {
					System.err.println("Got CoreException (\""+ e.getMessage() +"\") while getting AST of: "+ icElement.getElementName() );
					e.printStackTrace();
					System.err.println("I give up, this is too difficult  ;-(");
					System.exit(2);
				}
			}
			else {
				// don't know what it is, don't know what to do with it
			}
		}
	}

	private IIndex createIndex(ICProject proj) {
		IIndex index = null;
		try {
			IIndexManager manager = CCorePlugin.getIndexManager();
			manager.reindex(proj);
			manager.joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor());
			index = manager.getIndex(proj);
			index.acquireReadLock();
		}
		catch (CoreException excp) {
			System.err.println(excp.getMessage());			
		} catch (InterruptedException excp) {
			System.err.println(excp.getMessage());			
		}

		return index;
	}

	private ICProject createProject(String projName) {
		IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		System.out.println("workspace located in: "+root.getLocation());
		
		IProject projEclipse = root.getProject(projName);
		ICProject proj;

		try {
			if (! projEclipse.exists()) {
				System.out.println("now creating project");
				projEclipse.create(NULL_PROGRESS_MONITOR);
			}
			projEclipse.open(NULL_PROGRESS_MONITOR);
			projEclipse.refreshLocal(IResource.DEPTH_INFINITE, NULL_PROGRESS_MONITOR);
			
			System.out.println("IProject located in: "+projEclipse.getRawLocation());
		}
		catch (final CoreException e) {
			System.err.println("Got CoreException (\""+ e.getMessage() +"\") while trying to create IProject: "+ projName);
		}

		proj = CoreModel.getDefault().getCModel().getCProject(projEclipse.getName());
		try {
			proj.open(NULL_PROGRESS_MONITOR);
			proj.makeConsistent(NULL_PROGRESS_MONITOR);
		}
		catch (final CoreException e) {
			System.err.println("Got CoreException (\""+ e.getMessage() +"\") while trying to create CppProject: "+ projName);
		}

		return proj;
	}

	private void parseSourceFile(String filename) {
        FileContent reader = FileContent.createForExternalFileLocation(filename);

        System.out.println("\n ---------- parsing: " + filename +" ----------");

		try {
			IASTTranslationUnit ast;
			ast = GPPLanguage.getDefault().getASTTranslationUnit(reader, info, provider, indexer, 0, log);
			visitTU(ast);
		} catch (CoreException e) {
			System.err.println("Got CoreException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
			e.printStackTrace();
			System.err.println("I give up, this is too difficult  ;-(");
			System.exit(2);
		}
		catch (IllegalArgumentException e) {
			System.err.println("Got IllegalArgumentException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
			e.printStackTrace();
			System.err.println("I give up, this is too difficult  ;-(");
			System.exit(2);
		}

	}

	private void visitTU(IASTTranslationUnit ast) {
        ast.accept(new MainVisitor(dico));
	}
}
