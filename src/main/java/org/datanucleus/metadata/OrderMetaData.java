/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
2004 Andy Jefferson - toString(), MetaData, javadocs
2004 Andy Jefferson - added index, and indexed
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * Representation of Order MetaData - the ordering of the elements of a List.
 * This caters for 2 types of List.
 * <ul>
 * <li><b>indexed list</b> where we add a (surrogate) index column</li>
 * <li><b>ordered list</b> like in JPA where we use some ordering clause when retrieving (also as extension in JDO)</li>
 * </ul>
 */
public class OrderMetaData extends MetaData implements ColumnMetaDataContainer
{
    private static final long serialVersionUID = 2673343183786417980L;

    /** The name of the column (if specified as input) */
    protected String columnName = null;

    /** the columns  */
    protected List<ColumnMetaData> columns = null;

    /** IndexMetaData. */
    protected IndexMetaData indexMetaData;

    /** The indexing value specified as input. */
    protected IndexedValue indexed = null;

    /** Name of the field in the element that is the ordering field. */
    protected String mappedBy = null;

    /** Ordering when using an "ordered list" where the elements are retrieved in a particular order. */
    protected String ordering = null;

    /** Ordering of fields (when using "ordered List"), splitting the "ordering" apart by comma-separated parts. */
    protected FieldOrder[] fieldOrders = null;

    /**
     * Copy constructor.
     * @param omd The metadata to copy
     */
    public OrderMetaData(OrderMetaData omd)
    {
        super(null, omd);
        this.indexed = omd.indexed;
        this.columnName = omd.columnName;

        // TODO Change these to copy rather than reference
        if (omd.indexMetaData != null)
        {
            this.indexMetaData = omd.indexMetaData;
            this.indexMetaData.parent = this;
        }
        if (omd.columns != null)
        {
            for (ColumnMetaData colmd : omd.columns)
            {
                addColumn(colmd);
            }
        }
        this.mappedBy = omd.mappedBy;
        this.ordering = omd.ordering;
    }

    public OrderMetaData()
    {
    }

    /**
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects.
     * @param clr Not used
     */
    public void initialise(ClassLoaderResolver clr)
    {
        if (hasExtension(MetaData.EXTENSION_MEMBER_LIST_ORDERING))
        {
            // User has provided extension "list-ordering" meaning that we use an ordered list for this collection (like in JPA)
            String val = getValueForExtension(MetaData.EXTENSION_MEMBER_LIST_ORDERING);
            if (!StringUtils.isWhitespace(val))
            {
                this.ordering = val;
            }
        }

        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && columns != null && indexed != null && indexed != IndexedValue.FALSE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
            indexMetaData.parent = this;
            for (ColumnMetaData colmd : columns)
            {
                indexMetaData.addColumn(colmd.getName());
            }
        }

        if (mappedBy != null)
        {
            // Check that the "mapped-by" field exists in the element class
            AbstractMemberMetaData fmd = (AbstractMemberMetaData)parent;
            AbstractClassMetaData elementCmd = fmd.getCollection().element.classMetaData;
            if (elementCmd != null && !elementCmd.hasMember(mappedBy))
            {
                throw new InvalidMemberMetaDataException("044137", fmd.getClassName(), fmd.getName(), elementCmd.getFullClassName(), mappedBy);
            }
        }

        setInitialised();
    }

    public final OrderMetaData setIndexed(IndexedValue val)
    {
        this.indexed = val;
        return this;
    }

    public final OrderMetaData setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
        this.indexMetaData.parent = this;
        return this;
    }

    /**
     * Method to create a new index metadata, set it, and return it.
     * @return The index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    public String getMappedBy()
    {
        return mappedBy;
    }

    public OrderMetaData setMappedBy(String mappedby)
    {
        this.mappedBy = StringUtils.isWhitespace(mappedby) ? null : mappedby;
        return this;
    }

    public void addColumn(ColumnMetaData colmd)
    {
        if (columns == null)
        {
            columns = new ArrayList<ColumnMetaData>();
        }
        columns.add(colmd);
        colmd.parent = this;
    }

    /**
     * Method to create a new column metadata, set it, and return it.
     * @return The column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }

    public final ColumnMetaData[] getColumnMetaData()
    {
        if (columns == null)
        {
            return null;
        }
        return columns.toArray(new ColumnMetaData[columns.size()]);
    }

    public final String getColumnName()
    {
        return columnName;
    }

    public OrderMetaData setColumnName(String column)
    {
        if (!StringUtils.isWhitespace(column))
        {
            ColumnMetaData colmd = new ColumnMetaData();
            colmd.setName(column);
            colmd.parent = this;
            addColumn(colmd);
            this.columnName = column;
        }
        else
        {
            this.columnName = null;
        }
        return this;
    }

    public final IndexMetaData getIndexMetaData()
    {
        return indexMetaData;
    }

    /**
     * Convenience method to return if the List is an "indexed List" like in JDO2.
     * @return Whether the List is indexed (if false means that it is "ordered" (like in JPA1)
     */
    public boolean isIndexedList()
    {
        return ordering == null;
    }

    public String getOrdering()
    {
        return ordering;
    }

    public OrderMetaData setOrdering(String ordering)
    {
        this.ordering = ordering;
        return this;
    }

    public FieldOrder[] getFieldOrders()
    {
        if (ordering != null && fieldOrders == null)
        {
            // Initialise list ordering since not yet done
            // "ordered List", so split the ordering into its components
            FieldOrder[] theOrders = null;
            AbstractMemberMetaData fmd = (AbstractMemberMetaData)parent;
            AbstractClassMetaData elementCmd = fmd.hasCollection() ? fmd.getCollection().element.classMetaData : fmd.getArray().element.classMetaData;
            if (elementCmd != null && ordering.equals("#PK"))
            {
                // Order using the PK of the element PC
                theOrders = new FieldOrder[elementCmd.getNoOfPrimaryKeyMembers()];
                String[] pkFieldNames = elementCmd.getPrimaryKeyMemberNames();
                int i = 0;
                for (int pkFieldNum=0;pkFieldNum<theOrders.length;pkFieldNum++)
                {
                    theOrders[i++] = new FieldOrder(pkFieldNames[pkFieldNum]);
                }
            }
            else if (elementCmd != null)
            {
                // Order using the provided definition of element PC fields
                StringTokenizer tokeniser = new StringTokenizer(ordering, ",");
                int num = tokeniser.countTokens();
                theOrders = new FieldOrder[num];
                int i = 0;
                while (tokeniser.hasMoreTokens())
                {
                    String nextToken = tokeniser.nextToken().trim();
                    String fieldName = null;
                    boolean forward = true;
                    int spacePos = nextToken.indexOf(' ');
                    if (spacePos > 0)
                    {
                        // Of the form "{field} {ordering}"
                        fieldName = nextToken.substring(0, spacePos);
                        String direction = nextToken.substring(spacePos+1).trim();
                        if (direction.equalsIgnoreCase("DESC"))
                        {
                            forward = false;
                        }
                        else if (!direction.equalsIgnoreCase("ASC"))
                        {
                            throw new InvalidMemberMetaDataException("044139", fmd.getClassName(), fmd.getName(), direction);
                        }
                    }
                    else
                    {
                        // Of the form "{field}"
                        fieldName = nextToken;
                    }

                    if (!elementCmd.hasMember(fieldName))
                    {
                        throw new InvalidMemberMetaDataException("044138", fmd.getClassName(), fmd.getName(), elementCmd.getFullClassName(), fieldName);
                    }

                    // Add the field order
                    theOrders[i] = new FieldOrder(fieldName);
                    if (!forward)
                    {
                        theOrders[i].setBackward();
                    }
                    i++;
                }
            }
            else
            {
                // List<NonPC> so use natural ordering
                theOrders = new FieldOrder[0];
                // TODO Set this to some special value?
            }
            this.fieldOrders = theOrders;
        }
        return fieldOrders;
    }

    // ------------------------------ Utilities --------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        // Field needs outputting so generate metadata
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<order");
        if (columnName != null)
        {
            sb.append(" column=\"" + columnName + "\"");
        }
        if (indexed != null)
        {
            sb.append(" indexed=\"" + indexed.toString() + "\"");
        }
        if (mappedBy != null)
        {
            sb.append(" mapped-by=\"" + mappedBy + "\"");
        }
        sb.append(">\n");

        // Add columns
        if (columns != null)
        {
            for (int i=0; i<columns.size(); i++)
            {
                ColumnMetaData c = columns.get(i);
                sb.append(c.toString(prefix + indent,indent));
            }
        }

        // Add index
        if (indexMetaData != null)
        {
            sb.append(indexMetaData.toString(prefix + indent,indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</order>\n");
        return sb.toString();
    }

    /**
     * Definition of ordering using a field.
     * Used by "ordered lists".
     */
    public static class FieldOrder implements Serializable
    {
        private static final long serialVersionUID = 338498690476594298L;
        String fieldName;
        boolean forward = true;

        public FieldOrder(String name)
        {
            fieldName = name;
        }
        public void setBackward()
        {
            forward = false;
        }
        public String getFieldName()
        {
            return fieldName;
        }
        public boolean isForward()
        {
            return forward;
        }
        public String toString()
        {
            return fieldName + " " + (forward ? "ASC" : "DESC");
        }
    }
}