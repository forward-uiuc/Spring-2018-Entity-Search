package ner.annotation;

/**
 * Simply keeps the root of any passed in annotation tree
 * @author alexaulabaugh
 */

public class RootsOnlyReconciler extends AnnotationReconciler
{

	@Override
	public void reconcileAnnotationTree(EntityAnnotation root, EntityCatalog catalog)
	{
		root.clearSubAnnotation();
	}

}
