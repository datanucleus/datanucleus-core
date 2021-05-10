/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - cater for subqueries, bulk update, bulk delete parsing
    ...
**********************************************************************/
package org.datanucleus.store.query;

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassConstants;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.Query.QueryType;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Parser for handling JPQL Single-String queries.
 * Takes a JPQLQuery and the query string and parses it into its constituent parts, updating the JPQLQuery accordingly with the result that, 
 * after calling the parse() method, the JPQLQuery is populated.
 * <pre>
 * SELECT [{result} ]
 *        [FROM {from-clause} ]
 *        [WHERE {filter}]
 *        [GROUP BY {grouping-clause} ]
 *        [HAVING {having-clause} ]
 *        [ORDER BY {ordering-clause}]
 *        [RANGE x,y]
 * e.g SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
 * </pre>
 * or
 * <pre>
 * UPDATE {update-clause}
 * WHERE {filter}
 * </pre>
 * and <i>update-clause</i> is of the form "Entity [[AS] identifier] SET {field = new_value}, ..."
 * or
 * <pre>
 * DELETE {delete-clause}
 * WHERE {filter}
 * </pre>
 * and <i>delete-clause</i> is of the form "FROM Entity [[AS] identifier]"
 * <p>
 * Note that only the {filter} and {having-clause} can strictly contain subqueries in JPQL, hence containing keywords
 * <pre>
 * SELECT c FROM Customer c WHERE NOT EXISTS (SELECT o1 FROM c.orders o1)
 * </pre>
 * So the "filter" for the outer query is "NOT EXISTS (SELECT o1 FROM c.orders o1)".
 * Note also that we allow subqueries in {result}, {from}, and {having} clauses as well (vendor extension).
 * If a subquery is contained we extract the subquery and then set it as a variable in the symbol table, and add the subquery separately.
 * Note that the <pre>[RANGE x,y]</pre> is a DataNucleus extension syntax to allow for specification of firstResult/maxResults in the query string and hence in subqueries
 * and is dependent on enabling <i>datanucleus.query.jpql.allowRange</i>.
 *
 * TODO Need to better cater for the construct "TRIM(... FROM ...)" since it reuses the FROM keyword and we don't handle that explicitly here, just working around it
 */
public class JPQLSingleStringParser
{
    /** The JPQL query to populate. */
    private Query query;

    /** The single-string query string. */
    private String queryString;

    /** Standard JPQL does not allow RANGE keyword in the JPQL. */
    private boolean allowRange = false;

    /**
     * Constructor for the Single-String parser.
     * @param query The query into which we populate the components of the query
     * @param queryString The Single-String query
     */
    public JPQLSingleStringParser(Query query, String queryString)
    {
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            NucleusLogger.QUERY.debug(Localiser.msg("043000", queryString));
        }
        this.query = query;
        this.queryString = queryString;
    }

    public JPQLSingleStringParser allowRange()
    {
        allowRange = true;
        return this;
    }

    /**
     * Method to parse the Single-String query
     */
    public void parse()
    {
        new Compiler(new Parser(queryString, allowRange)).compile();
    }

    /**
     * Compiler to process keywords contents. 
     * In the query the keywords often have content values following them that represent the constituent parts of the query. 
     * This takes the keyword and sets the constituent part accordingly.
     */
    private class Compiler
    {
        Parser parser;
        int subqueryNum = 1;
        
        Compiler(Parser tokenizer)
        {
            this.parser = tokenizer;
        }

        private void compile()
        {
            compileQuery();

            // any keyword after compiling the SELECT is an error
            String keyword = parser.parseKeyword();
            if (keyword != null)
            {
                if (JPQLQueryHelper.isKeyword(keyword))
                {
                    throw new NucleusUserException(Localiser.msg("043001", keyword));
                }
                // unexpected token
            }
        }

        private void compileQuery()
        {
            boolean insert = false;
            boolean update = false;
            boolean delete = false;
            if (parser.parseKeywordIgnoreCase("SELECT"))
            {
                // Do nothing
            }
            else if (parser.parseKeywordIgnoreCase("INSERT"))
            {
                insert = true;
                query.setType(QueryType.BULK_INSERT);
            }
            else if (parser.parseKeywordIgnoreCase("UPDATE"))
            {
                update = true;
                query.setType(QueryType.BULK_UPDATE);
            }
            else if (parser.parseKeywordIgnoreCase("DELETE"))
            {
                delete = true;
                query.setType(QueryType.BULK_DELETE);
            }
            else
            {
                throw new NucleusUserException(Localiser.msg("043002"));
            }

            if (update)
            {
                // UPDATE {entity [AS alias]} SET x1 = val1, x2 = val2, ...
                compileUpdate();

                if (parser.parseKeywordIgnoreCase("WHERE"))
                {
                    compileWhere();
                }
            }
            else if (insert)
            {
                // INSERT [INTO] {entity} (field1, field2, ...)
                String content = parser.parseContent(null, false);
                if (content.length() > 0)
                {
                    if (content.startsWith("INTO"))
                    {
                        // Skip optional INTO
                        content = content.substring(4).trim();
                    }

                    int bracketStart = content.indexOf('(');
                    int bracketEnd = content.indexOf(')');
                    if (bracketStart < 0 || bracketEnd < 0)
                    {
                        throw new NucleusUserException("INSERT [INTO] {entity} should be followed by '(field1, field2, ...)' but isn't");
                    }
                    String entityString = content.substring(0, bracketStart).trim();
                    String fieldString = content.substring(bracketStart+1, bracketEnd).trim();

                    query.setFrom(entityString);
                    query.setInsertFields(fieldString);
                }
                else
                {
                    throw new NucleusUserException("INSERT [INTO] should be followed by '{entity} (field1, field2, ...)' but isn't");
                }

                // Extract remains of query, and move to end of query
                String selectQuery = parser.queryString.substring(parser.queryStringPos).trim();
                while (parser.parseKeyword() != null)
                {
                }
                query.setInsertSelectQuery(selectQuery);
            }
            else if (delete)
            {
                // DELETE FROM {entity [AS alias]}
                if (parser.parseKeywordIgnoreCase("FROM"))
                {
                    compileFrom();
                }

                if (parser.parseKeywordIgnoreCase("WHERE"))
                {
                    compileWhere();
                }
            }
            else
            {
                // SELECT a, b, c FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... [RANGE ...]
                compileResult();

                if (parser.parseKeywordIgnoreCase("FROM"))
                {
                    compileFrom();
                }
                if (parser.parseKeywordIgnoreCase("WHERE"))
                {
                    compileWhere();
                }
                if (parser.parseKeywordIgnoreCase("GROUP BY"))
                {
                    compileGroup();
                }
                if (parser.parseKeywordIgnoreCase("HAVING"))
                {
                    compileHaving();
                }
                if (parser.parseKeywordIgnoreCase("ORDER BY"))
                {
                    compileOrder();
                }
                if (allowRange && parser.parseKeywordIgnoreCase("RANGE"))
                {
                    // DN extension
                    compileRange();
                }
            }
        }

        private void compileResult()
        {
            String content = parser.parseContent(null, true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("043004", "SELECT", "<result>"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0) // Case insensitive search
            {
                // Subquery (or subqueries) present so split them out and just apply the result for this query
                String substitutedContent = processContentWithSubqueries(content);
                query.setResult(substitutedContent);
            }
            else
            {
                query.setResult(content);
            }
        }

        private void compileUpdate()
        {
            String content = parser.parseContent(null, true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // No UPDATE clause
                throw new NucleusUserException(Localiser.msg("043010"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0) // Case insensitive search
            {
                // Subquery (or subqueries) present so split them out and just apply the filter for this query
                String substitutedContent = processContentWithSubqueries(content);

                String contentUpper = substitutedContent.toUpperCase();
                int setIndex = contentUpper.indexOf(" SET ");
                if (setIndex < 0)
                {
                    // UPDATE clause has no " SET ..." !
                    throw new NucleusUserException(Localiser.msg("043011"));
                }
                query.setFrom(substitutedContent.substring(0, setIndex+1).trim());
                query.setUpdate(substitutedContent.substring(setIndex+4).trim());
            }
            else
            {
                String contentUpper = content.toUpperCase();
                int setIndex = contentUpper.indexOf(" SET ");
                if (setIndex < 0)
                {
                    // UPDATE clause has no " SET ..." !
                    throw new NucleusUserException(Localiser.msg("043011"));
                }
                query.setFrom(content.substring(0, setIndex+1).trim());
                query.setUpdate(content.substring(setIndex+4).trim());
            }
        }

        private void compileFrom()
        {
            String content = parser.parseContent(null, true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() > 0)
            {
                if (content.toUpperCase().indexOf("SELECT ") > 0) // Case insensitive search
                {
                    // Subquery (or subqueries) present so split them out and just apply the filter for this query
                    String substitutedContent = processContentWithSubqueries(content);
                    query.setFrom(substitutedContent);
                }
                else
                {
                    query.setFrom(content);
                }
            }
        }

        private void compileWhere()
        {
            String content = parser.parseContent("FROM", true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("043004", "WHERE", "<filter>"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0) // Case insensitive search
            {
                // Subquery (or subqueries) present so split them out and just apply the filter for this query
                String substitutedContent = processContentWithSubqueries(content);
                query.setFilter(substitutedContent);
            }
            else
            {
                query.setFilter(content);
            }
        }

        private void compileGroup()
        {
            String content = parser.parseContent("FROM", true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("043004", "GROUP BY", "<grouping>"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0) // Case insensitive search
            {
                // Subquery (or subqueries) present so split them out and just apply the grouping for this query
                String substitutedContent = processContentWithSubqueries(content);
                query.setGrouping(substitutedContent);
            }
            else
            {
                query.setGrouping(content);
            }
        }

        private void compileHaving()
        {
            String content = parser.parseContent("FROM", true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("043004", "HAVING", "<having>"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0)
            {
                // Subquery (or subqueries) present so split them out and just apply the having for this query
                String substitutedContent = processContentWithSubqueries(content);
                query.setHaving(substitutedContent);
            }
            else
            {
                query.setHaving(content);
            }
        }

        private void compileOrder()
        {
            String content = parser.parseContent("FROM", true); // Allow subqueries (see below also search for SELECT), and "TRIM(... FROM ...)"
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("043004", "ORDER", "<ordering>"));
            }

            if (content.toUpperCase().indexOf("SELECT ") > 0)
            {
                // Subquery (or subqueries) present so split them out and just apply the having for this query
                String substitutedContent = processContentWithSubqueries(content);
                query.setOrdering(substitutedContent);
            }
            else
            {
                query.setOrdering(content);
            }
        }

        private void compileRange()
        {
            String content = parser.parseContent(null, false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "RANGE", "<range>"));
            }
            query.setRange(content);
        }

        /**
         * Method to extract the required clause, splitting out any subqueries and replacing by variables (adding subqueries to the underlying query), returning the clause to use.
         * @param content The input string
         * @return Content with subqueries substituted
         */
        private String processContentWithSubqueries(String content)
        {
            StringBuilder stringContent = new StringBuilder();
            boolean withinLiteralDouble = false;
            boolean withinLiteralSingle = false;
            for (int i=0;i<content.length();i++)
            {
                boolean subqueryProcessed = false;
                char chr = content.charAt(i);

                if (chr == '"')
                {
                    withinLiteralDouble = !withinLiteralDouble;
                }
                else if (chr == '\'')
                {
                    withinLiteralSingle = !withinLiteralSingle;
                }

                if (!withinLiteralDouble && !withinLiteralSingle)
                {
                    if (chr == '(')
                    {
                        // Check for SELECT/select form of subquery
                        String remains = content.substring(i+1).trim();
                        if (remains.toUpperCase().startsWith("SELECT"))
                        {
                            // subquery, so find closing brace and process it
                            remains = content.substring(i);
                            int endPosition = -1;
                            int braceLevel = 0;
                            for (int j=1;j<remains.length();j++) // Omit opening brace since we know about it
                            {
                                if (remains.charAt(j) == '(')
                                {
                                    braceLevel++;
                                }
                                else if (remains.charAt(j) == ')')
                                {
                                    braceLevel--;
                                    if (braceLevel < 0) // Closing brace for the subquery
                                    {
                                        endPosition = i+j;
                                        break;
                                    }
                                }
                            }
                            if (endPosition < 0)
                            {
                                throw new NucleusUserException(Localiser.msg("042017"));
                            }

                            String subqueryStr = content.substring(i+1, endPosition).trim();
                            String subqueryVarName = "DN_SUBQUERY_" + subqueryNum;

                            Query subquery = (Query)ClassUtils.newInstance(query.getClass(),
                                new Class[]{ClassConstants.STORE_MANAGER, ClassConstants.EXECUTION_CONTEXT, String.class},
                                new Object[] {query.getStoreManager(), query.getExecutionContext(), subqueryStr});
                            // TODO Set the type of the variable
                            query.addSubquery(subquery, "double " + subqueryVarName, null, null);

                            if (stringContent.length() > 0 && stringContent.charAt(stringContent.length()-1) != ' ')
                            {
                                stringContent.append(' ');
                            }
                            stringContent.append(subqueryVarName);
                            i = endPosition;
                            subqueryNum++;
                            subqueryProcessed = true;
                        }
                    }
                }
                if (!subqueryProcessed)
                {
                    stringContent.append(chr);
                }
            }

            if (withinLiteralDouble || withinLiteralSingle)
            {
                // Literal wasn't closed
                throw new NucleusUserException(Localiser.msg("042017"));
            }

            return stringContent.toString();
        }
    }

    /**
     * Tokenizer that provides access to current token.
     */
    private static class Parser
    {
        final String queryString;

        int queryStringPos = 0;

        /** tokens */
        final String[] tokens;

        /** keywords */
        final String[] keywords;

        /** current token cursor position */
        int tokenIndex = -1;

        boolean allowRange = false;

        /**
         * Constructor
         * @param str Query string
         */
        public Parser(String str, boolean allowRange)
        {
            this.allowRange = allowRange;
            queryString = str.replace('\n', ' ');

            // Parse into tokens, taking care to keep any String literals together
            List<String> tokenList = new ArrayList();
            boolean withinSingleQuote = false;
            boolean withinDoubleQuote = false;
            StringBuilder currentToken = new StringBuilder();
            for (int i=0;i<queryString.length();i++)
            {
                char chr = queryString.charAt(i);
                if (chr == '"')
                {
                    withinDoubleQuote = !withinDoubleQuote;
                    currentToken.append(chr);
                }
                else if (chr == '\'')
                {
                    withinSingleQuote = !withinSingleQuote;
                    currentToken.append(chr);
                }
                else if (chr == ' ')
                {
                    if (!withinDoubleQuote && !withinSingleQuote)
                    {
                        tokenList.add(currentToken.toString().trim());
                        currentToken = new StringBuilder();
                    }
                    else
                    {
                        currentToken.append(chr);
                    }
                }
                else
                {
                    currentToken.append(chr);
                }
            }
            if (currentToken.length() > 0)
            {
                tokenList.add(currentToken.toString());
            }

            tokens = new String[tokenList.size()];
            int i = 0;
            for (String token : tokenList)
            {
                tokens[i++] = token;
            }

            keywords = new String[tokenList.size()];
            for (i = 0; i < tokens.length; i++)
            {
                if (JPQLQueryHelper.isKeyword(tokens[i], allowRange))
                {
                    keywords[i] = tokens[i];
                }
                else if (i < tokens.length - 1 && JPQLQueryHelper.isKeyword(tokens[i] + ' ' + tokens[i + 1], allowRange))
                {
                    keywords[i] = tokens[i];
                    i++;
                    keywords[i] = tokens[i];
                }
            }
        }

        /**
         * Parse the content until a keyword is found.
         * @param keywordToIgnore Ignore this keyword if found first
         * @param allowSubentries Whether to permit subentries (in parentheses) in this next block
         * @return the content
         */
        public String parseContent(String keywordToIgnore, boolean allowSubentries)
        {
            String content = "";
            int level = 0;

            while (tokenIndex < tokens.length - 1)
            {
                tokenIndex++;

                if (allowSubentries)
                {
                    // Process this token to check level of parentheses.
                    // This is necessary because we want to ignore keywords if within a parentheses-block
                    // e.g SELECT ... FROM ... WHERE ... EXISTS (SELECT FROM ...)
                    // and the "WHERE" part is "... EXISTS (SELECT FROM ...)"
                    // Consequently subqueries will be parsed into the relevant block correctly.
                    // Assumes that subqueries are placed in parentheses
                    for (int i=0;i<tokens[tokenIndex].length();i++)
                    {
                        char c = tokens[tokenIndex].charAt(i);
                        if (c == '(')
                        {
                            level++;
                        }
                        else if (c == ')')
                        {
                            level--;
                        }
                    }
                }

                if (level == 0 && JPQLQueryHelper.isKeyword(tokens[tokenIndex], allowRange) && !tokens[tokenIndex].equals(keywordToIgnore))
                {
                    // Invalid keyword encountered and not currently in subquery block
                    tokenIndex--;
                    break;
                }
                else if (level == 0 && tokenIndex < tokens.length - 1 && JPQLQueryHelper.isKeyword(tokens[tokenIndex] + ' ' + tokens[tokenIndex + 1], allowRange))
                {
                    // Invalid keyword entered ("GROUP BY", "ORDER BY") and not currently in subquery block
                    tokenIndex--;
                    break;
                }
                else
                {
                    // Append the content from the query string from the end of the last token to the end of this token
                    int endPos = queryString.indexOf(tokens[tokenIndex], queryStringPos) + tokens[tokenIndex].length();
                    String contentValue = queryString.substring(queryStringPos, endPos);
                    queryStringPos = endPos;

                    if (content.length() == 0)
                    {
                        content = contentValue;
                    }
                    else
                    {
                        content += contentValue;
                    }
                }
            }
            return content;
        }

        /**
         * Parse the next token looking for a keyword. 
         * The cursor position is skipped in one tick if a keyword is found
         * @param keyword the searched keyword
         * @return true if the keyword
         */
        public boolean parseKeywordIgnoreCase(String keyword)
        {
            if (tokenIndex < tokens.length - 1)
            {
                tokenIndex++;
                if (keywords[tokenIndex] != null)
                {
                    if (keywords[tokenIndex].equalsIgnoreCase(keyword))
                    {
                        // Move query position to end of last processed token
                        queryStringPos = queryString.indexOf(keywords[tokenIndex], queryStringPos) + keywords[tokenIndex].length()+1;
                        return true;
                    }
                    if (keyword.indexOf(' ') > -1) // "GROUP BY", "ORDER BY"
                    {
                        if ((keywords[tokenIndex] + ' ' + keywords[tokenIndex + 1]).equalsIgnoreCase(keyword))
                        {
                            // Move query position to end of last processed token
                            queryStringPos = queryString.indexOf(keywords[tokenIndex], queryStringPos) + keywords[tokenIndex].length()+1;
                            queryStringPos = queryString.indexOf(keywords[tokenIndex+1], queryStringPos) + keywords[tokenIndex+1].length()+1;
                            tokenIndex++;
                            return true;
                        }
                    }
                }
                tokenIndex--;
            }
            return false;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is skipped in one tick if a keyword is found
         * @return the parsed keyword or null
         */
        public String parseKeyword()
        {
            if (tokenIndex < tokens.length - 1)
            {
                tokenIndex++;
                if (keywords[tokenIndex] != null)
                {
                    return keywords[tokenIndex];
                }
                tokenIndex--;
            }
            return null;
        }
    }
}