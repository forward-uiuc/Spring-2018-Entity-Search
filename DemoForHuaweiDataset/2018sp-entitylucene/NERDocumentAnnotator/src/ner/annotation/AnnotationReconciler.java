package ner.annotation;

public abstract class AnnotationReconciler
{
	public abstract void reconcileAnnotationTree(EntityAnnotation root, EntityCatalog catalog);
}
