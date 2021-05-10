/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
   ...
**********************************************************************/
package org.datanucleus.store.query.compiler;

/**
 * Enum of node types.
 */
public enum NodeType
{
    LITERAL,    /** literal node type **/
    INVOKE,     /** invoke node type. Such as method/function invocation. **/
    NAME,       /** type name node type. Used for aliases in from clause. **/
    IDENTIFIER, /** identifier node type **/
    OPERATOR,   /** operator node type. **/
    CREATOR,    /** operator node type. **/
    CLASS,      /** class node type (e.g use in a "from" clause for a candidate). **/
    PARAMETER,  /** parameter node type **/
    CAST,       /** cast node type **/
    ARRAY,      /** array node type **/
    SUBQUERY,   /** subquery node type (EXISTS, ANY, SOME, ALL, etc) **/
    TYPE,       /** "type" node type (JPQL, like "instanceof" **/
    PRIMARY,    /** "primary" node type where we have something like an OPERATOR being passed to INVOKE. */
    CASE;       /** Case node type (JPQL) **/
}