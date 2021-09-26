# datanucleus-core

DataNucleus core persistence support - the basis for anything in DataNucleus.

This is built using Maven, by executing `mvn clean install` which installs the built jar in your local Maven repository.

## KeyFacts

__License__ : Apache 2 licensed  
__Issue Tracker__ : http://github.com/datanucleus/datanucleus-core/issues  
__Javadocs__ : [6.0](http://www.datanucleus.org/javadocs/core/6.0/), [5.2](http://www.datanucleus.org/javadocs/core/5.2/), [5.1](http://www.datanucleus.org/javadocs/core/5.1/), [5.0](http://www.datanucleus.org/javadocs/core/5.0/), [4.1](http://www.datanucleus.org/javadocs/core/4.1/), [4.0](http://www.datanucleus.org/javadocs/core/4.0/)  
__Download__ : [Maven Central](https://repo1.maven.org/maven2/org/datanucleus/datanucleus-core)  
__Dependencies__ : See file [pom.xml](pom.xml)  
__Support__ : [DataNucleus Support Page](http://www.datanucleus.org/support.html)  

----  

## Persistence Process
The primary classes involved in the persistence process are

* *PersistenceNucleusContext* - maps across to a PMF/EMF, and provides access to the StoreManager and ExecutionContext(s) (PersistenceNucleusContextImpl). 
* *ExecutionContext* - maps across to a PM/EM, and handles the transaction (ExecutionContextImpl)  
* *StateManager* - manages access to a persistent object (StateManagerImpl)  
* *StoreManager* - manages access to the datastore (see the datastore plugins, e.g RDBMSStoreManager)  
* *MetaDataManager* - manages the metadata for the class(es), so how it is persisted  


### Persistence : Retrieve of Objects

    MyClass myObj = (MyClass)pm.getObjectById(id);
    myObj.getSomeSet().add(newVal);

* calls wrapper (see _org.datanucleus.store.types.wrappers.XXX_ or _org.datanucleus.store.types.wrappers.backed.XXX_)
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



### Persistence : Pessimistic Transactions

All persist, remove, field update calls go to the datastore straight away. 
Flush() doesn't have the same significance here as it does for optimistic, except in that it will queue "update" requests until there are more than say 3 objects waiting.
This means that multiple setters can be called on a single object and we get one UPDATE statement.


#### persist
Calls ExecutionContext.persistObject which calls EC.persistObjectWork.  
Creates a StateManager (StateManagerImpl - SM). Adds the object to EC.dirtySMs.  
Calls SM.makePersistent which calls SM.internalMakePersistent which will pass the persist through to the datastore plugin.  
Calls PersistenceHandler.insertObject, which will do any necessary cascade persist (coming back through EC.persistObjectInternal, EC.indirectDirtySMs).  


#### remove
Calls ExecutionContext.deleteObject, which calls ExecutionContext.deleteObjectWork.  
This will add the object to EC.dirtySMs.  
Calls SM.deletePersistent.  
Calls SM.internalDeletePersistent which will pass the delete through to the datastore plugin.  
Calls PersistenceHandler.deleteObject, which will do any necessary cascade delete (coming back through EC.deleteObjectInternal, EC.indirectDirtySMs).  


#### update field
Calls SM.setXXXField which calls SM.updateField and, in turn, EC.makeDirty.  
The update is then queued internally until EC.flushInternal is triggered (e.g 3 changes waiting).  


#### Collection.add
Calls SCO wrapper.add which will add the element locally.  
If a backing store is present (RDBMS) then passes it through to the backingStore.add().  


#### Collection.remove/clear
Calls SCO wrapper.remove/clear which will add the element locally.  
If a backing store is present (RDBMS) then passes it through to the backingStore.remove()/clear().  
If no backing store is present and cascade delete is true then does the cascade delete, via EC.deleteObjectInternal.  


### Persistence : Optimistic Transactions

All persist, remove, field update calls are queued.
Flush() processes all remove/add/updates that have been queued.
Call ExecutionContext.getOperationQueue() to see the operations that are queued up waiting to flush.


#### persist
Calls ExecutionContext.persistObject which calls EC.persistObjectWork.  
Creates a StateManager (StateManagerImpl - SM). Adds the object to EC.dirtySMs.  
Calls SM.makePersistent. Uses PersistFieldManager to process all reachable objects.  


#### remove
Calls ExecutionContext.deleteObject, which calls ExecutionContext.deleteObjectWork.  
Creates a StateManager as required. Adds the object to EC.dirtySMs.  
Calls SM.deletePersistent. Uses DeleteFieldManager to process all reachable objects.


#### update field
Calls SM.setXXXField which calls SM.updateField and, in turn, EC.makeDirty.  
The update is then queued internally until EC.flushInternal is triggered.  


#### Collection.add
Calls SCO wrapper.add which will add the element locally.  
Adds a queued operation to the queue for addition of this element.  


#### Collection.remove/clear
Calls SCO wrapper.remove/clear which will add the element locally.  
Adds a queued operation to the queue for removal of this element.  

----  

### Flush Process

When a set of mutating operations are required to be flushed (e.g transaction commit) the _FlushProcess_ for the *StoreManager*
is executed. At the start of the flush process we have a set of primary objects that were directly modified by the user and passed in to calls,
as well as a set of secondary objects that were connected to primary objects by relationships, and were also modified. A "modification" could mean
insert, update, delete. 

An RDBMS uses a _org.datanucleus.flush.FlushOrdered_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/flush/FlushOrdered.html).
Other datastores typically use a _org.datanucleus.flush.FlushNonReferential_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/flush/FlushNonReferential.html).

----  

### MetaData Process

The _MetaDataManager_ is responsible for loading and providing access to the metadata for all persistable classes. 
MetaData can come from Java annotations, XML metadata files, or via the JDO MetaData API.

Each class is represented via a _org.datanucleus.metadata.ClassMetaData_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/metadata/ClassMetaData.html).
This in turn has a Collection of 
_org.datanucleus.metadata.FieldMetaData_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/metadata/FieldMetaData.html) andy/or
_org.datanucleus.metadata.PropertyMetaData_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/metadata/PropertyMetaData.html)
depending whether the metadata is specified on a field or on a getter/setter method.
Fields/properties are numbered alphabetically, with the _absolute_ field number starting at the root class in an inheritance tree
and the _relative_ field number starting in the current class.

----

### Query Process

DataNucleus provides a _generic_ query processing engine. It provides for compilation of __string-based query languages__. 
Additionally it allows _in-memory evaluation_ of these queries. This is very useful when providing support for new datastores which either
don't have a native query language and so the only alternative is for DataNucleus to evaluate the queries, or where it will take some time 
to map the compiled query to the equivalent query in the native language of the datastore.

#### Query : Input Processing

When a user invokes a query, using the JDO/JPA APIs, they are providing either

* A single-string query made up of keywords and clauses
* A query object that has the clauses specified directly

The first step is to convert these two forms into the constituent clauses. It is assumed that a string-based query is of the form

	SELECT {resultClause} FROM {fromClause} WHERE {filterClause}
	GROUP BY {groupingClause} HAVING {havingClause}
	ORDER BY {orderClause}]]></source>

The two primary supported query languages have helper classes to provide this migration from the _single-string query form_ into the individual clauses. 
These can be found in _org.datanucleus.store.query.JDOQLSingleStringParser_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/JDOQLSingleStringParser.html)
and _org.datanucleus.store.query.JPQLSingleStringParser_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/JPQLSingleStringParser.html).

#### Query : Compilation

So we have a series of clauses and we want to compile them. So what does this mean? Well, in simple terms, we are going to convert the individual clauses 
from above into expression tree(s) so that they can be evaluated. The end result of a compilation is a _org.datanucleus.store.query.compiler.QueryCompilation_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/compiler/QueryCompilation.html).

So if you think about a typical query you may have

	SELECT field1, field2 FROM MyClass

This has 2 result expressions - field1, and field2 (where they are each a "PrimaryExpression" meaning a representation of a field).
The query compilation of a particular clauses has 2 stages

1. Compilation into a Node tree, with operations between the nodes
2. Compilation of the Node tree into an Expression tree of supported expressions

and compilation is performed by a JavaQueryCompiler, so look at _org.datanucleus.store.query.compiler.JDOQLCompiler_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/compiler/JDOQLCompiler.html)
and _org.datanucleus.store.query.compiler.JPQLCompiler_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/compiler/JPQLCompiler.html).
These each have a Parser that performs the extraction of the different components of the clauses and generation of the Node tree. 
Once a Node tree is generated it can then be converted into the compiled Expression tree; this is handled inside the JavaQueryCompiler.

The other part of a query compilation is the _org.datanucleus.store.query.compiler.SymbolTable_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/compiler/SymbolTable.html)
which is a lookup table (map) of identifiers and their value. So, for example, an input parameter will have a name, so has an entry in 
the table, and its value is stored there. This is then used during evaluation.

#### Query : Evaluation In-datastore

Intuitively it is more efficient to evaluate a query within the datastore since it means that fewer actual result objects need 
instantiating in order to determine the result objects. To evaluate a compiled query in the datastore there needs to be a compiler 
for taking the generic expression compilation and converting it into a native query. Additionally it should be noted that you aren't 
forced to evaluate the whole of the query in the datastore, maybe just the filter clause. This would be done where the datastore 
native language maybe only provides a limited amount of query capabilities. For example with db4o we evaluated the _filter_ and 
_ordering_ in the datastore, using their SODA query language. The remaining clauses can be evaluated on the resultant objects 
_in-memory_ (see below). Obviously for a datastore like RDBMS it should be possible to evaluate the whole query in-datastore.

#### Query : Evaluation In-memory

Evaluation of queries in-memory assumes that we have a series of "candidate" objects. These are either user-input to the query itself, 
or retrieved from the datastore. We then use the in-memory evaluator _org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator_
[[Javadoc]](http://www.datanucleus.org/javadocs/core/latest/org/datanucleus/store/query/inmemory/InMemoryExpressionEvaluator.html).
This takes in each candidate object one-by-one and evaluates whichever of the query clauses are desired to be evaluated. 
For example we could just evaluate the filter clause. Evaluation makes use of the values of the fields of the candidate objects 
(and related objects) and uses the SymbolTable for values of parameters etc. Where a candidate fails a particular clause 
in the filter then it is excluded from the results.

#### Query : Results

There are two primary ways to return results to the user.

* Instantiate all into memory and return a (java.util.)List. This is the simplest, but obviously can impact on memory footprint.
* Return a wrapper to a List, and intercept calls so that you can load objects as they are accessed. This is more complex, 
but has the advantage of not imposing a large footprint on the application.

To make use of the second route, consider extending the class _org.datanucleus.store.query.AbstractQueryResult_ and implement the key methods.
Also, for the iterator, you can extend _org.datanucleus.store.query.AbstractQueryResultIterator_.

----  


### Types : Second-Class Objects

When a persistable class is persisted and has a field of a (mutable) second-class type (Collection, Map, Date, etc) then DataNucleus needs to know when the
user calls operations on it to change the contents of the object. To do this, at the first reference to the field once enlisted in a transaction, DataNucleus 
will replace the field value with a _proxy wrapper_ wrapping the real object. This has no effect for the user in that the field is still castable to 
the same type as they had in that field, but all operations are intercepted.


### Types : Container fields and caching of Values

By default when a container field is replaced by a second-class object (SCO) wrapper it will be enabled to cache the values in that field. This means that once 
the values are loaded in that field there will be no need to make any call to the datastore unless changing the container. This gives significant speed-up 
when compared to relaying all calls via the datastore. You can change to <b>not</b> use caching by setting either

* Globally for the PersistenceManagerFactory - this is controlled by setting the persistence property 
__org.datanucleus.cache.collections__. Set it to false to pass through to the datastore.
* For the specific Collection/Map - add a MetaData &lt;collection&gt; or &lt;map&gt; extension 
_cache_ setting it to false to pass through to the datastore.

This is implemented in a typical SCO proxy wrapper by using the SCOUtils method _useContainerCache()_ which determines if caching is required, and by having a 
method _load()_ on all proxy wrapper container classes.


#### Types : Container fields and Lazy Loading

JDO and JPA provide mechanisms for specifying whether fields are loaded lazily (when required) or whether they are loaded eagerly (when the object is first met). 
DataNucleus follows these specifications but also allows the user to override the lazy loading for a SCO container. For example if a collection field was marked 
as being part of the default fetch group it should be loaded eagerly which means that when the owning object is instantiated the collection is loaded up too. 
If the user overrides the lazy loading for that field in that situation to make it lazy, DataNucleus will instantiate the owning object and instantiate the 
collection but leave it marked as "to be loaded" and the elements will be loaded up when needed. You can change the lazy loading setting via

* Globally for the PMF/EMF - this is controlled by setting the persistence property __org.datanucleus.cache.collections.lazy__. 
Set it to true to use lazy loading, and set it to false to load the elements when the collection/map is initialised.
* For the specific Collection/Map - add a MetaData &lt;collection&gt; or &lt;map&gt; extension __cache-lazy-loading__. 
Set it to true to use lazy loading, and false to load once at initialisation.


#### Types : SCO fields and Queuing operations

When DataNucleus is using an optimistic transaction it attempts to delay all datastore operations until _commit_ is called on the transaction or _flush_ is 
called on the PersistenceManager/EntityManager. This implies a change to operation of SCO proxy wrappers in that they must __queue__ up all mutating operations 
(add, clear, remove etc) until such a time as they need to be sent to the datastore. The ExecutionContext has the queue for this purpose.

All code for the queued operations are stored under _org.datanucleus.flush_.


#### Types : Simple SCO interceptors

There are actually two sets of SCO wrappers in DataNucleus. The first set provide lazy loading, queueing, etc and have a "backing store" where the operations
can be fed through to the datastore as they are made (for RDBMS). The second set are simple wrappers that intercept operations and mark the field as dirty in 
the StateManager. This second set are for use with all (non-RDBMS) datastores that don't utilise backing stores and just want to know when the field is dirty
and hence should be written.

All code for the backed SCO wrappers are stored under _org.datanucleus.store.types.wrappers.backed_.
All code for the simple SCO wrappers are stored under _org.datanucleus.store.types.wrappers_.

----  

### Schema

#### MultiTenancy

The handling for multi-tenancy code is present in each of the store plugins but is controlled from 

* __org.datanucleus.metadata.AbstractClassMetaData.getMultitenancyMetaData__ : returns details of any multi-tenancy discriminator null if the class does not need it).
* __org.datanucleus.ExecutionContext.getTenantId__ : returns the tenant id to use for multi-tenancy (for any write operations, and optionally any read operations)
* __org.datanucleus.PersistenceNucleusContext.getTenantReadIds__ : returns the tenant ids to use in any read operations (overriding the tenant id above when specified).

The metadata of the class defines whether it has a tenancy discriminator (i.e you have to explicitly add the metadata to get a discriminator). 


### CDI Integration

DataNucleus allows use of CDI injected resources into attribute converter classes (JDO and JPA) as well as JPA lifecycle listeners.
The basis for this is the specification of the persistence property *datanucleus.cdi.bean.manager*. 
If this is set then when creating an instance of the objected with injected resources, we call 

    CDIHandler.createObjectWithInjectedDependencies(cls);


