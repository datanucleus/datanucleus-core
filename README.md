# datanucleus-core

DataNucleus core persistence support - the basis for anything in DataNucleus.

This is built using Maven, by executing `mvn clean install` which installs the built jar in your local Maven repository.

## KeyFacts

__License__ : Apache 2 licensed  
__Issue Tracker__ : http://www.datanucleus.org/servlet/jira/browse/NUCCORE  
__RoadMap__ : http://issues.datanucleus.org/browse/NUCCORE?report=com.atlassian.jira.plugin.system.project:roadmap-panel  
__Javadocs__ : [5.0](http://www.datanucleus.org/javadocs/core/5.0/), [4.1](http://www.datanucleus.org/javadocs/core/4.1/), [4.0](http://www.datanucleus.org/javadocs/core/4.0/), [3.2](http://www.datanucleus.org/javadocs/core/3.2/), [3.1](http://www.datanucleus.org/javadocs/core/3.1/), [3.0](http://www.datanucleus.org/javadocs/core/3.0/), [2.2](http://www.datanucleus.org/javadocs/core/2.2/), [2.1](http://www.datanucleus.org/javadocs/core/2.1/), [2.0](http://www.datanucleus.org/javadocs/core/2.0/), [1.1](http://www.datanucleus.org/javadocs/core/1.1/), [1.0](http://www.datanucleus.org/javadocs/core/1.0/)  
__Download(Releases)__ : [Maven Central](http://central.maven.org/maven2/org/datanucleus/datanucleus-core)  
__Download(Nightly)__ : [Nightly Builds](http://www.datanucleus.org/downloads/maven2-nightly/org/datanucleus/datanucleus-core)  
__Dependencies__ : See file [pom.xml](pom.xml)  

----  

## Persistence Process
The primary classes involved in the persistence process are
*ExecutionContext* - maps across to a PM/EM, and handles the transaction (ExecutionContextImpl)  
*ObjectProvider* - manages access to a persistent object (StateManagerImpl)  
*StoreManager* - manages access to the datastore (see the datastore plugins, e.g RDBMSStoreManager)  
*MetaDataManager* - manages the metadata for the class(es), so how it is persisted  

###Retrieve of Objects

    MyClass myObj = (MyClass)pm.getObjectById(id);
    myObj.getSomeSet().add(newVal);

* calls backing store wrapper (see _org.datanucleus.store.types.backed.XXX_ or _org.datanucleus.store.types.simple.XXX_)
* if optimistic txns then queues up til flush/commit
* otherwise will call backing store for the wrapper (RDBMS) which updates the DB, or will mark the field as dirty (non-RDBMS) and the field is sent to the datastore at the next convenient place.


    Query q = pm.newQuery("SELECT FROM " + MyClass.class.getName());
    List<MyClass> results = (List<MyClass>)q.execute();

* Makes use of QueryManager to create an internal Query object (wrapped by a JDO/JPA Query object). This may be something like org.datanucleus.store.rdbms.query.JDOQLQuery specific to 
the datastore.
* The query is compiled generically. This involves converting each component of the query (filter, ordering, grouping, result etc) into Node trees, and then converting that into Expression trees. 
This is then stored in a QueryCompilation, and can be cached.
* The query is then converted into a datastore-specific compilation. In the case of RDBMS this will be an RDBMSCompilation, and will be an SQL string (and associated parameter/result lookups).
* The query is executed in the datastore and/or in-memory. The in-memory evaluator is in datanucleus-core under org.datanucleus.query.evaluator.memory. 
The execution process will return a QueryResult (which is a List).
* Operations on the QueryResult such as "iterator()" will result in lazy loading of results from the underlying ResultSet (in the case of RDBMS)



<a name="pessimistic"/>
###Persist Of Objects : Pessimistic Transactions

All persist, remove, field update calls go to the datastore straight away. 
Flush() doesn't have the same significance here as it does for optimistic, except in that it will queue "update" requests until there are more than say 3 objects waiting.
This means that multiple setters can be called on a single object and we get one UPDATE statement.


####persist
Calls ExecutionContext.persistObject which calls EC.persistObjectWork.  
Creates an ObjectProvider (StateManagerImpl - OP). Adds the object to EC.dirtyOPs.  
Calls OP.makePersistent which calls OP.internalMakePersistent which will pass the persist through to the datastore plugin.  
Calls PersistenceHandler.insertObject, which will do any necessary cascade persist (coming back through EC.persistObjectInternal, EC.indirectDirtyOPs).  


####remove
Calls ExecutionContext.deleteObject, which calls ExecutionContext.deleteObjectWork.  
This will add the object to EC.dirtyOPs.  
Calls OP.deletePersistent.  
Calls OP.internalDeletePersistent which will pass the delete through to the datastore plugin.  
Calls PersistenceHandler.deleteObject, which will do any necessary cascade delete (coming back through EC.deleteObjectInternal, EC.indirectDirtyOPs).  


####update field
Calls OP.setXXXField which calls OP.updateField and, in turn, EC.makeDirty.  
The update is then queued internally until EC.flushInternal is triggered (e.g 3 changes waiting).  


####Collection.add
Calls SCO wrapper.add which will add the element locally.  
If a backing store is present (RDBMS) then passes it through to the backingStore.add().  


####Collection.remove/clear
Calls SCO wrapper.remove/clear which will add the element locally.  
If a backing store is present (RDBMS) then passes it through to the backingStore.remove()/clear().  
If no backing store is present and cascade delete is true then does the cascade delete, via EC.deleteObjectInternal.  


<a name="optimistic"/>
###Persist Of Objects : Optimistic Transactions

All persist, remove, field update calls are queued.
Flush() processes all remove/add/updates that have been queued.
Call ExecutionContext.getOperationQueue() to see the operations that are queued up waiting to flush.


####persist
Calls ExecutionContext.persistObject which calls EC.persistObjectWork.  
Creates an ObjectProvider (StateManagerImpl - OP). Adds the object to EC.dirtyOPs.  
Calls OP.makePersistent. Uses PersistFieldManager to process all reachable objects.  


####remove
Calls ExecutionContext.deleteObject, which calls ExecutionContext.deleteObjectWork.  
Creates an ObjectProvider as required. Adds the object to EC.dirtyOPs.  
Calls OP.deletePersistent. Uses DeleteFieldManager to process all reachable objects.


####update field
Calls OP.setXXXField which calls OP.updateField and, in turn, EC.makeDirty.  
The update is then queued internally until EC.flushInternal is triggered.  


####Collection.add
Calls SCO wrapper.add which will add the element locally.  
Adds a queued operation to the queue for addition of this element.  


####Collection.remove/clear
Calls SCO wrapper.remove/clear which will add the element locally.  
Adds a queued operation to the queue for removal of this element.  


