package org.datanucleus.store.types;

public abstract class ElementContainerAdapter<C> implements ContainerAdapter<C>
{
    protected C container;
    
    public ElementContainerAdapter(C container)
    {
        this.container = container;
    }

    @Override
    public C getContainer()
    {
        return container;
    }
    
    protected void setContainer(C container)
    {
        this.container = container;
    }
    
    public abstract void add(Object newElement);
	
    public abstract void remove(Object element);
}
