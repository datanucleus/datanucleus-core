/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.query;

import java.util.StringTokenizer;

import org.datanucleus.ClassConstants;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.Query;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Parser for handling JDOQL Single-String queries.
 * Takes a JDOQLQuery and the query string and parses it into its constituent parts, updating
 * the JDOQLQuery accordingly with the result that after calling the parse() method the JDOQLQuery
 * is populated.
 * <pre>
 * select [unique] [{result}] [into {result-class-name}]
 *                              [from {candidate-class-name} [exclude subclasses] ]
 *                              [where {filter}]
 *                              [variables {variables-clause} ]
 *                              [parameters {parameters-clause} ]
 *                              [{imports-clause}]
 *                              [group by {grouping-clause} ]
 *                              [order by {ordering-clause} ]
 *                              [range {from-range} ,{to-range}]                                       
 * </pre>
 * or
 * <pre>
 * UPDATE {candidate-class-name} SET fld1 = val[, fld2 = val2] WHERE {filter}
 * </pre>
 * or
 * <pre>
 * DELETE FROM {candidate-class-name} [exclude-subclasses] WHERE {filter}
 * </pre>
 * Note that {filter} can contain subqueries, hence containing keywords
 * <pre>
 * SELECT c FROM Customer c WHERE timeAvailable &lt; (SELECT avg(hours) FROM Employee e)
 * </pre>
 * So the "filter" for the outer query is "timeAvailable &lt; (SELECT avg(hours) FROM Employee e)"
 */
public class JDOQLSingleStringParser
{
    /** The JDOQL query to populate. */
    private Query query;

    /** The single-string query string. */
    private String queryString;

    boolean allowDelete = false;
    boolean allowUpdate = false;

    /**
     * Constructor for the Single-String parser.
     * @param query The query
     * @param queryString The Single-String query
     */
    public JDOQLSingleStringParser(Query query, String queryString)
    {
        NucleusLogger.QUERY.debug(Localiser.msg("042010", queryString));
        this.query = query;
        this.queryString = queryString;
    }

    public void setAllowDelete(boolean allow)
    {
        allowDelete = allow;
    }

    public void setAllowUpdate(boolean allow)
    {
        allowUpdate = allow;
    }

    /**
     * Method to parse the Single-String query
     */
    public void parse()
    {
        new Compiler(new Parser(queryString, allowDelete || allowUpdate)).compile();
    }

    /**
     * Compiler to process keywords contents. In the query the keywords often have
     * content values following them that represent the constituent parts of the query. This takes the keyword
     * and sets the constituent part accordingly.
     */
    private class Compiler
    {
        Parser tokenizer;

        Compiler(Parser tokenizer)
        {
            this.tokenizer = tokenizer;
        }

        private void compile()
        {
            compileSelect();
            String keyword = tokenizer.parseKeyword();
            if (keyword != null && JDOQLQueryHelper.isKeyword(keyword))
            {
                // any keyword after compiling the SELECT is an error
                throw new NucleusUserException(Localiser.msg("042011", keyword));
            }
        }

        private void compileSelect()
        {
            boolean update = false;
            boolean delete = false;
            if (allowUpdate && (tokenizer.parseKeyword("UPDATE") || tokenizer.parseKeyword("update")))
            {
                update = true;
                query.setType(Query.BULK_UPDATE);
            }
            else if (allowDelete && (tokenizer.parseKeyword("DELETE") || tokenizer.parseKeyword("delete")))
            {
                delete = true;
                query.setType(Query.BULK_DELETE);
            }
            else if (tokenizer.parseKeyword("SELECT") || tokenizer.parseKeyword("select"))
            {
                // Do nothing
            } 
            else
            {
                throw new NucleusUserException(Localiser.msg("042012"));
            }

            if (update)
            {
                compileUpdate();
            }
            else if (delete)
            {
                if (tokenizer.parseKeyword("FROM") || tokenizer.parseKeyword("from"))
                {
                    compileFrom();
                }
            }
            else
            {
                if (tokenizer.parseKeyword("UNIQUE") || tokenizer.parseKeyword("unique"))
                {
                    compileUnique();
                }
                compileResult();
                if (tokenizer.parseKeyword("INTO") || tokenizer.parseKeyword("into"))
                {
                    compileInto();
                }
                if (tokenizer.parseKeyword("FROM") || tokenizer.parseKeyword("from"))
                {
                    compileFrom();
                }
            }

            if (tokenizer.parseKeyword("WHERE") || tokenizer.parseKeyword("where"))
            {
                compileWhere();
            }
            if (tokenizer.parseKeyword("VARIABLES") || tokenizer.parseKeyword("variables"))
            {
                compileVariables();
            }
            if (tokenizer.parseKeyword("PARAMETERS") || tokenizer.parseKeyword("parameters"))
            {
                compileParameters();
            }
            if (tokenizer.parseKeyword("import"))
            {
                compileImport();
            }
            if (tokenizer.parseKeyword("GROUP") || tokenizer.parseKeyword("group"))
            {
                compileGroup();
            }
            if (tokenizer.parseKeyword("ORDER") || tokenizer.parseKeyword("order"))
            {
                compileOrder();
            }
            if (tokenizer.parseKeyword("RANGE") || tokenizer.parseKeyword("range"))
            {
                compileRange();
            }
        }
        
        private void compileUnique()
        {
            query.setUnique(true);
        }

        private void compileResult()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() > 0)
            {
                query.setResult(content);
            }
        }

        private void compileUpdate()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // No UPDATE clause
                throw new NucleusUserException(Localiser.msg("043010"));
            }
            query.setFrom(content);
            query.setCandidateClassName(content);

            if (tokenizer.parseKeyword("set") || tokenizer.parseKeyword("SET"))
            {
                content = tokenizer.parseContent(false);
                query.setUpdate(content.trim());
            }
            else
            {
                // UPDATE clause has no "SET ..." !
                throw new NucleusUserException(Localiser.msg("043011"));
            }
        }

        private void compileInto()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "INTO", "<result class>"));
            }

            String resultClassName = content.trim();
            query.setResultClassName(resultClassName);
        }

        private void compileFrom()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "FROM", "<candidate class>"));
            }

            if (content.indexOf(' ') > 0)
            {
                // Subquery accepts "<candidate-expr> alias"
                query.setFrom(content.trim());
            }
            else
            {
                // Content is "<candidate-class-name>"
                query.setCandidateClassName(content);
            }

            if (tokenizer.parseKeyword("EXCLUDE") || tokenizer.parseKeyword("exclude"))
            {
                if (!tokenizer.parseKeyword("SUBCLASSES") && !tokenizer.parseKeyword("subclasses"))
                {
                    throw new NucleusUserException(Localiser.msg("042015", "SUBCLASSES", "EXCLUDE"));
                }
                content = tokenizer.parseContent(false);
                if (content.length() > 0)
                {
                    throw new NucleusUserException(Localiser.msg("042013", "EXCLUDE SUBCLASSES", content));
                }
                query.setSubclasses(false);
            }
        }

        private void compileWhere()
        {
            String content = tokenizer.parseContent(true);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "WHERE", "<filter>"));
            }

            if (content.indexOf("SELECT ") > 0 || content.indexOf("select ") > 0)
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
            int subqueryNum = 1;
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
                        // Check for SELECT/select form of JDOQL subquery
                        String remains = content.substring(i+1).trim();
                        if (remains.startsWith("select") || remains.startsWith("SELECT"))
                        {
                            // JDOQL subquery, so find closing brace and process it
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
                            subqueryProcessed = true;
                            subqueryNum++;
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

        private void compileVariables()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "VARIABLES", "<variable declarations>"));
            }
            query.declareExplicitVariables(content);
        }

        private void compileParameters()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "PARAMETERS", "<parameter declarations>"));
            }
            query.declareExplicitParameters(content);
        }

        private void compileImport()
        {
            StringBuilder content = new StringBuilder("import " + tokenizer.parseContent(false));
            while (tokenizer.parseKeyword("import"))
            {
                content.append("import ").append(tokenizer.parseContent(false));
            }
            query.declareImports(content.toString());
        }

        private void compileGroup()
        {
            String content = tokenizer.parseContent(false);
            if (!tokenizer.parseKeyword("BY") && !tokenizer.parseKeyword("by"))
            {
                // GROUP must be followed by BY
                throw new NucleusUserException(Localiser.msg("042015", "BY", "GROUP"));
            }

            content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "GROUP BY", "<grouping>"));
            }
            query.setGrouping(content);
        }

        private void compileOrder()
        {
            String content = tokenizer.parseContent(false);
            if (!tokenizer.parseKeyword("BY") && !tokenizer.parseKeyword("by"))
            {
                // ORDER must be followed by BY
                throw new NucleusUserException(Localiser.msg("042015", "BY", "ORDER"));
            }

            content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "ORDER BY", "<ordering>"));
            }
            query.setOrdering(content);
        }

        private void compileRange()
        {
            String content = tokenizer.parseContent(false);
            if (content.length() == 0)
            {
                // content cannot be empty
                throw new NucleusUserException(Localiser.msg("042014", "RANGE", "<range>"));
            }
            query.setRange(content);
        }
    }

    /**
     * Tokenizer that provides access to current token.
     */
    private static class Parser
    {
        final boolean extended;

        final String queryString;

        int queryStringPos = 0;

        /** tokens */
        final String[] tokens;

        /** keywords */
        final String[] keywords;

        /** current token cursor position */
        int tokenIndex = -1;

        /**
         * Constructor
         * @param str String to parse
         */
        public Parser(String str, boolean extended)
        {
            this.queryString = str;
            this.extended = extended;

            StringTokenizer tokenizer = new StringTokenizer(str);
            tokens = new String[tokenizer.countTokens()];
            keywords = new String[tokenizer.countTokens()];
            int i = 0;
            while (tokenizer.hasMoreTokens())
            {
                tokens[i] = tokenizer.nextToken();
                if ((extended && JDOQLQueryHelper.isKeywordExtended(tokens[i])) ||
                    (!extended && JDOQLQueryHelper.isKeyword(tokens[i])))
                {
                    keywords[i] = tokens[i];
                }
                i++;
            }
        }

        /**
         * Parse the content until a keyword is found
         * @param allowSubentries Whether to permit subentries (in parentheses) in this next block
         * @return the content
         */
        public String parseContent(boolean allowSubentries)
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
                    // e.g SELECT ... FROM ... WHERE param1 < (SELECT ... FROM ...)
                    // and the "WHERE" part is "param1 < (SELECT ... FROM ...)"
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

                if (level == 0 && 
                    ((extended && JDOQLQueryHelper.isKeywordExtended(tokens[tokenIndex])) ||
                     (!extended && JDOQLQueryHelper.isKeyword(tokens[tokenIndex]))))
                {
                    // Keyword encountered, and not part of any subquery so end of content block
                    tokenIndex--;
                    break;
                }

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

            return content;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is
         * skipped in one tick if a keyword is found
         * @param keyword the searched keyword
         * @return true if the keyword
         */
        public boolean parseKeyword(String keyword)
        {
            if (tokenIndex < tokens.length - 1)
            {
                tokenIndex++;
                if (keywords[tokenIndex] != null)
                {
                    if (keywords[tokenIndex].equals(keyword))
                    {
                        // Move query position to end of last processed token
                        queryStringPos = 
                            queryString.indexOf(keywords[tokenIndex], queryStringPos) + 
                            keywords[tokenIndex].length()+1;
                        return true;
                    }
                }
                tokenIndex--;
            }
            return false;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is
         * skipped in one tick if a keyword is found
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