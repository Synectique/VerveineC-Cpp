package eu.synectique.verveine.extractor.utils;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Constants that need accessing from different classes
 * @author anquetil
 *
 */
public class Constants {

	/**
	 * local variable to keep eclipse platform quiet
	 */
	public static final IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	public static final List<String> EMPTY_STRING_LIST = new LinkedList<String>();

}
