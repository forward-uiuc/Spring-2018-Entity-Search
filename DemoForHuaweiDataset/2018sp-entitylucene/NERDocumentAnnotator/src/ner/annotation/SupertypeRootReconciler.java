package ner.annotation;

public class SupertypeRootReconciler extends AnnotationReconciler
{
	@Override
	public void reconcileAnnotationTree(EntityAnnotation root, EntityCatalog catalog)
	{
		root.clearSubAnnotation();
		root.setTypes(catalog.getSuperEntityTypes(root.getTypes()));
	}
}
