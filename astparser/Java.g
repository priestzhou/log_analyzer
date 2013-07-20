/**
 * An ANTLRv3 capable Java 1.5 grammar for building ASTs.
 *
 * Note that there's also the tree grammar 'JavaTreeParser.g' that can be fed
 * with this grammer's output.
 *
 *
 * Please report any detected errors or even suggestions regarding this grammar
 * to
 *
 *          dieter [D O T] habelitz [A T] habelitz [D O T] com
 *
 *      with the subject
 *
 *          jsom grammar: [your subject note]
 *
 * To generate a parser based on this grammar you'll need ANTLRv3, which you 
 * can get from 'http://www.antlr.org'.
 *
 * 
 * Change and version log (year-month-day):
 * ----------------------------------------
 * 
 * 2008-04-03	First release version published.
 * 2008-04-16   Bug-fix (not grammar specific!): the flag 'mHasError' will be 
 				set to 'true' now if an error occurs.
 * 2008-04-16   Some changes within the lexer grammar regarding comments; 
 *              furthermore, the flag 'preserveWhitespacesAndComments' has been
 *              replaced by four flags: 'mPreserveWhitespaces', 
 *              'mPreserveJavaDocComments', 'mPreserveBlockComments' and 
 *              'mPreserveLineComments'. I.e., whitespaces and all kinds of
 *				comments can be preserved or thrown away independently.
 * 2008-05-13	Grammar rule 'enumScopeDeclarations' fixed; constants are
 *			    optional not obligatory.
 * 2008-06-11   Resolving of cast expressions fixed; very simple parenthesized
 *              expressions have been recognized as type casts.
 * 2008-08-13	Rule 'switchBlockLabels' fixed.
 * 2008-08-22   New token for new line characters introduces; in previous
 *              versions new line characters have been tokenized as whitespaces.
 * 2010-09-02   Annotation initializers within parentheses are optional not
 *				obligatory (i.e. @AnyAnnotation() is valid).
 * 2011-01-11   Separate sub-rule for each statement type.
 * 2011-01-11   Preserve empty statements.
 * 2011-03-14   Recognize generic type arguments correctly for all kinds of
 *              super constructor calls.
 * 2011-08-27   Problem with the closing brackets of nested generic type 
 *              parameter/argument lists fixed.
 * 2011-10-12   Support of hexadecimal floating point literals.
 * 2012-03-20   Another problem with closing generic type brackets fixes;
 *              formal example: AnyType<AnyGenType1<AnyGenType2>>[]
 *              - problem before the 2011-08-27 fix: the array declarator was
 *                bound to 'AnyGenType1'
 *              - problem after the fix and up to now: the array declarator was
 *                bound to 'AnyType' and 'AnyGenType1'
 *              - after this fix: the array declarator is bound to 'AnyType'
 *                only.
 * 2012-04-27   Allow semicolons within annotation scopes (like semicolons
 *              within class scopes, for instance)
 * 2012-05-17   Some other problems with generic type closing brackets found and
 *              fixed (a huge amount of tests have been written for that now).
 *
 * ----------------------------------------------------------------------------
 * license agreement
 * ----------------------------------------------------------------------------
 *
 * This grammar is published under the ...
 *
 * BSD license
 * 
 * Copyright (c) 2007-2012 by HABELITZ Software Developments
 *
 * All rights reserved.
 * 
 * http://www.habelitz.com
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY HABELITZ SOFTWARE DEVELOPMENTS ('HSD') ``AS IS'' 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL 'HSD' BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
grammar Java;

options {
    backtrack = true; 
    memoize = true;
    output = AST;
    ASTLabelType = CommonTree;
}

tokens {

    // operators and other special chars
    
    AND                     = '&'               ;
    AND_ASSIGN              = '&='              ;
    ASSIGN                  = '='               ;
    AT                      = '@'               ;
    BIT_SHIFT_RIGHT         = '>>>'             ;
    BIT_SHIFT_RIGHT_ASSIGN  = '>>>='            ;
    COLON                   = ':'               ;
    COMMA                   = ','               ;
    DEC                     = '--'              ;
    DIV                     = '/'               ;
    DIV_ASSIGN              = '/='              ;
    DOT                     = '.'               ;
    DOTSTAR                 = '.*'              ;
    ELLIPSIS                = '...'             ;
    EQUAL                   = '=='              ;
    GREATER_OR_EQUAL        = '>='              ;
    GREATER_THAN            = '>'               ;
    INC                     = '++'              ;
    LBRACK                  = '['               ;
    LCURLY                  = '{'               ;
    LESS_OR_EQUAL           = '<='              ;
    LESS_THAN               = '<'               ;
    LOGICAL_AND             = '&&'              ;
    LOGICAL_NOT             = '!'               ;
    LOGICAL_OR              = '||'              ;
    LPAREN                  = '('               ;
    MINUS                   = '-'               ;
    MINUS_ASSIGN            = '-='              ;
    MOD                     = '%'               ;
    MOD_ASSIGN              = '%='              ;
    NOT                     = '~'               ;
    NOT_EQUAL               = '!='              ;
    OR                      = '|'               ;
    OR_ASSIGN               = '|='              ;
    PLUS                    = '+'               ;
    PLUS_ASSIGN             = '+='              ;
    QUESTION                = '?'               ;
    RBRACK                  = ']'               ;
    RCURLY                  = '}'               ;
    RPAREN                  = ')'               ;
    SEMI                    = ';'               ;
    SHIFT_LEFT              = '<<'              ;
    SHIFT_LEFT_ASSIGN       = '<<='             ;
    SHIFT_RIGHT             = '>>'              ;
    SHIFT_RIGHT_ASSIGN      = '>>='             ;
    STAR                    = '*'               ;
    STAR_ASSIGN             = '*='              ;
    XOR                     = '^'               ;
    XOR_ASSIGN              = '^='              ;

    // keywords
    
    ABSTRACT                = 'abstract'        ;
    ASSERT                  = 'assert'          ;
    BOOLEAN                 = 'boolean'         ;
    BREAK                   = 'break'           ;
    BYTE                    = 'byte'            ;
    CASE                    = 'case'            ;
    CATCH                   = 'catch'           ;
    CHAR                    = 'char'            ;
    CLASS                   = 'class'           ;
    CONTINUE                = 'continue'        ;
    DEFAULT                 = 'default'         ;
    DO                      = 'do'              ;
    DOUBLE                  = 'double'          ;
    ELSE                    = 'else'            ;
    ENUM                    = 'enum'            ;
    EXTENDS                 = 'extends'         ;
    FALSE                   = 'false'           ;
    FINAL                   = 'final'           ;
    FINALLY                 = 'finally'         ;
    FLOAT                   = 'float'           ;
    FOR                     = 'for'             ;
    IF                      = 'if'              ;
    IMPLEMENTS              = 'implements'      ;
    INSTANCEOF              = 'instanceof'      ;
    INTERFACE               = 'interface'       ;
    IMPORT                  = 'import'          ;
    INT                     = 'int'             ;
    LONG                    = 'long'            ;
    NATIVE                  = 'native'          ;
    NEW                     = 'new'             ;
    NULL                    = 'null'            ;
    PACKAGE                 = 'package'         ;
    PRIVATE                 = 'private'         ;
    PROTECTED               = 'protected'       ;
    PUBLIC                  = 'public'          ;
    RETURN                  = 'return'          ;
    SHORT                   = 'short'           ;
    STATIC                  = 'static'          ;
    STRICTFP                = 'strictfp'        ;
    SUPER                   = 'super'           ;
    SWITCH                  = 'switch'          ;
    SYNCHRONIZED            = 'synchronized'    ;
    THIS                    = 'this'            ;
    THROW                   = 'throw'           ;
    THROWS                  = 'throws'          ;
    TRANSIENT               = 'transient'       ;
    TRUE                    = 'true'            ;
    TRY                     = 'try'             ;
    VOID                    = 'void'            ;
    VOLATILE                = 'volatile'        ;
    WHILE                   = 'while'           ;
    
    // tokens for imaginary nodes
    
    ANNOTATION_INIT_ARRAY_ELEMENT;
    ANNOTATION_INIT_BLOCK;
    ANNOTATION_INIT_DEFAULT_KEY;
    ANNOTATION_INIT_KEY_LIST;
    ANNOTATION_LIST;
    ANNOTATION_METHOD_DECL;
    ANNOTATION_SCOPE;
    ANNOTATION_TOP_LEVEL_SCOPE;
    ARGUMENT_LIST;
    ARRAY_DECLARATOR;
    ARRAY_DECLARATOR_LIST;
    ARRAY_ELEMENT_ACCESS;
    ARRAY_INITIALIZER;
    BLOCK_SCOPE;
    CAST_EXPR;
    CATCH_CLAUSE_LIST;
    CLASS_CONSTRUCTOR_CALL;
    CLASS_INSTANCE_INITIALIZER;
    CLASS_STATIC_INITIALIZER;
    CLASS_TOP_LEVEL_SCOPE;
    CONSTRUCTOR_DECL;
    ENUM_TOP_LEVEL_SCOPE;
    EXPR;
    EXTENDS_BOUND_LIST;
    EXTENDS_CLAUSE;
    FOR_CONDITION;
    FOR_EACH;
    FOR_INIT;
    FOR_UPDATE;
    FORMAL_PARAM_LIST;
    FORMAL_PARAM_STD_DECL;
    FORMAL_PARAM_VARARG_DECL;
    FUNCTION_METHOD_DECL;
    GENERIC_TYPE_ARG_LIST;
    GENERIC_TYPE_PARAM_LIST;
    INTERFACE_TOP_LEVEL_SCOPE;
    IMPLEMENTS_CLAUSE;
    LABELED_STATEMENT;
    LOCAL_MODIFIER_LIST;
    JAVA_SOURCE;
    METHOD_CALL;
    MODIFIER_LIST;
    PARENTESIZED_EXPR;
    POST_DEC;
    POST_INC;
    PRE_DEC;
    PRE_INC;
    QUALIFIED_TYPE_IDENT;
    STATIC_ARRAY_CREATOR;
    SUPER_CONSTRUCTOR_CALL;
    SWITCH_BLOCK_LABEL_LIST;
    THIS_CONSTRUCTOR_CALL;
    THROWS_CLAUSE;
    TYPE;
    UNARY_MINUS;
    UNARY_PLUS;
    VAR_DECLARATION;
    VAR_DECLARATOR;
    VAR_DECLARATOR_LIST;
    VOID_METHOD_DECL;
}

@header {
// Should be replaced for individual needs.
package astparser;

import org.antlr.runtime.misc.IntArray;
}

@members {
    
private boolean mMessageCollectionEnabled = false;
private List<String> mMessages;

/** Extend ANTLR's IntArray a little bit. */
private class IntStack extends IntArray {

    /** Returns the value on top of the stack but doesn't remove it. */
    int peek() {
    
        if (p >= 0) {
            return data[p];
        }
        
        return -1;
    }
    
    /** @return  The number of integers on the stack. */
    int stackSize() {
    
        return p + 1;
    }
}
private IntStack mReusableGenTypeClosingBrackets = new IntStack();

/**
 * Switches error message collection on or of.
 * <p>
 * The standard destination for parser error messages is <code>
 * System.err</code>. However, if <code>true</code> gets passed to this method 
 * this default behavior will be switched off and all error messages will be 
 * collected instead of writing them to an output stream.
 * <p>
 * The default value is <code>false</code>.
 *
 * @param pNewState  <code>true</code> if error messages should be collected.
 */
public void enableErrorMessageCollection(boolean pNewState) {

    mMessageCollectionEnabled = pNewState;
    if (mMessages == null && mMessageCollectionEnabled) {
        mMessages = new ArrayList<String>();
    }
}

/**
 * Collects an error message or passes the error message to <code>
 * super.emitErrorMessage(...)</code>.
 * <p>
 * The actual behavior depends on whether collecting error messages has been 
 * enabled or not.
 *
 * @param pMessage  The error message.
 */
@Override
public void emitErrorMessage(String pMessage) {

    if (mMessageCollectionEnabled) {
        mMessages.add(pMessage);
    } else {
        super.emitErrorMessage(pMessage);
    }
}

/**
 * Returns collected error messages.
 * <p>
 * This method returns a copy of the internal list of error messages and clears
 * the internal list.
 * <p>
 * Note that calling this method equals to a <code>getMessages(null)</code>
 * call.
 *
 * @see #getMessages(List)
 *
 * @return  A list holding collected error messages or <code>null</code> if
 *          collecting error messages is disabled. Of course, the returned list 
 *		    may be empty if no error message has been emitted.
 */
public List<String> getMessages() {

	return getMessages(null);
}

/**
 * Returns collected error messages.
 *
 * @param pUserList  If this argument is not <code>null</code> the error
 *                   messages will be added to this list and it's this list that
 *                   will be returned. Otherwise a copy of the internal list of 
 *                   error messages will be created and returned. The internal
 *                   list will be cleared just before the method returns.
 *
 * @return  A list holding collected error messages. If collecting error 
 *          messages is disabled <code>null</code> will be returned, even if the 
 *			argument <code>pUserList</code> is not <code>null</code>.
 */
public List<String> getMessages(List<String> pUserList) {

	if (!mMessageCollectionEnabled) {
	    return null;
	}
	List<String> result = pUserList;
	if (result == null) {
		result = new ArrayList<String>();
	}
	result.addAll(mMessages);
    mMessages.clear();
    
    return result;
}

/**
 * Tells if there is any collected error messages.
 *
 * @return  <code>true</code> if there is at least one collected error message.
 */
public boolean hasErrors() {

    return mMessages != null && mMessages.size() > 0;
}
} // @members

@lexer::header {
package astparser;
}

@lexer::members {

/** 
 *  If <code>true</code> whitespaces will be preserved within the hidden 
 *  channel, otherwise the appropriate tokens will be thrown away.
 *  <p>
 *  Note that new line characters will be handled explicitly and will therefore
 *  not be seen as whitespaces.
 *  <p>
 *  The default value of this flag is <code>false</code>.
 */
public boolean mPreserveWhitespaces = false;

/** 
 *  If <code>true</code> new line characters will be preserved within the hidden 
 *  channel, otherwise the appropriate tokens will be thrown away.
 *  <p>
 *  Note that for new line characters the token text of the token  <code>NL
 *  </code> will be set to <code>System.getProperty("line.separator")</code>
 *  unless this flag is set to <code>false</code>, off course.
 *  <p>
 *  The default value of this flag is <code>false</code>.
 */
public boolean mPreserveNewlineCharacters = false;

/** 
 *  If <code>true</code> javadoc comments will be preserved within the hidden 
 *  channel, otherwise the appropriate tokens will be thrown away.
 *  <p>
 *  The default value of this flag is <code>false</code>.
 */
public boolean mPreserveJavaDocComments = false;

/** 
 *  If <code>true</code> (non javadoc) block comments will be preserved within the 
 *  hidden channel, otherwise the appropriate tokens will be thrown away.
 *  <p>
 *  The default value of this flag is <code>false</code>.
 */
public boolean mPreserveBlockComments = false;

/** 
 *  If <code>true</code> single line comments will be preserved within the hidden 
 *  channel, otherwise the appropriate tokens will be thrown away.
 *  <p>
 *  The default value of this flag is <code>false</code>.
 */
public boolean mPreserveLineComments = false;

private static final String NEW_LINE = System.getProperty("line.separator");

private boolean mMessageCollectionEnabled = false;
private List<String> mMessages;

/**
 * Switches error message collection on or of.
 * <p>
 * The standard destination for parser error messages is <code>
 * System.err</code>. However, if <code>true</code> gets passed to this method 
 * this default behavior will be switched off and all error messages will be 
 * collected instead of writing them to an output stream.
 * <p>
 * The default value is <code>false</code>.
 *
 * @param pNewState  <code>true</code> if error messages should be collected.
 */
public void enableErrorMessageCollection(boolean pNewState) {

    mMessageCollectionEnabled = pNewState;
    if (mMessages == null && mMessageCollectionEnabled) {
        mMessages = new ArrayList<String>();
    }
}

/**
 * Collects an error message or passes the error message to <code>
 * super.emitErrorMessage(...)</code>.
 * <p>
 * The actual behavior depends on whether collecting error messages has been 
 * enabled or not.
 *
 * @param pMessage  The error message.
 */
@Override
public void emitErrorMessage(String pMessage) {

    if (mMessageCollectionEnabled) {
        mMessages.add(pMessage);
    } else {
        super.emitErrorMessage(pMessage);
    }
}

/**
 * Returns collected error messages.
 * <p>
 * This method returns a copy of the internal list of error messages and clears
 * the internal list.
 * <p>
 * Note that calling this method equals to a <code>getMessages(null)</code>
 * call.
 *
 * @see #getMessages(List)
 *
 * @return  A list holding collected error messages or <code>null</code> if
 *          collecting error messages is disabled. Of course, the returned list 
 *		    may be empty if no error message has been emitted.
 */
public List<String> getMessages() {

	return getMessages(null);
}

/**
 * Returns collected error messages.
 *
 * @param pUserList  If this argument is not <code>null</code> the error
 *                   messages will be added to this list and it's this list that
 *                   will be returned. Otherwise a copy of the internal list of 
 *                   error messages will be created and returned. The internal
 *                   list will be cleared just before the method returns.
 *
 * @return  A list holding collected error messages. If collecting error 
 *          messages is disabled <code>null</code> will be returned, even if the 
 *			argument <code>pUserList</code> is not <code>null</code>.
 */
public List<String> getMessages(List<String> pUserList) {

	if (!mMessageCollectionEnabled) {
	    return null;
	}
	List<String> result = pUserList;
	if (result == null) {
		result = new ArrayList<String>();
	}
	result.addAll(mMessages);
    mMessages.clear();
    
    return result;
}

/**
 * Tells if there is any collected error messages.
 *
 * @return  <code>true</code> if there is at least one collected error message.
 */
public boolean hasErrors() {

    return mMessages != null && mMessages.size() > 0;
}

} // @lexer::members

// Starting point for parsing a Java file.
javaSource
    :   compilationUnit
        ->  ^(JAVA_SOURCE compilationUnit)
    ;

compilationUnit
    :   annotationList 
        packageDeclaration? 
        importDeclaration* 
        typeDecls*
    ;

typeDecls
    :   typeDeclaration
    |   SEMI!
    ;

packageDeclaration
    :   PACKAGE^ qualifiedIdentifier SEMI!  
    ;
    
importDeclaration
    :   IMPORT^ STATIC? qualifiedIdentifier DOTSTAR? SEMI!
    ;
    
typeDeclaration
    :   modifierList!
        (   classTypeDeclaration[$modifierList.tree]
        |   interfaceTypeDeclaration[$modifierList.tree]
        |   enumTypeDeclaration[$modifierList.tree]
        |   annotationTypeDeclaration[$modifierList.tree]
        )
    ;
    
classTypeDeclaration[CommonTree modifiers]
    :   CLASS IDENT genericTypeParameterList? classExtendsClause? implementsClause? classBody
        ->  ^(CLASS {$modifiers} IDENT genericTypeParameterList? classExtendsClause? implementsClause? classBody)
    ;
    
classExtendsClause
    :   EXTENDS type
        ->  ^(EXTENDS_CLAUSE[$EXTENDS, "EXTENDS_CLAUSE"] type)
    ;   
    
interfaceExtendsClause
    :   EXTENDS typeList
        ->  ^(EXTENDS_CLAUSE[$EXTENDS, "EXTENDS_CLAUSE"] typeList)
    ;   
    
implementsClause
    :   IMPLEMENTS typeList
        ->  ^(IMPLEMENTS_CLAUSE[$IMPLEMENTS, "IMPLEMENTS_CLAUSE"] typeList)
    ;
        
genericTypeParameterList
    :   LESS_THAN genericTypeParameter (COMMA genericTypeParameter)* genericTypeListClosing
        ->  ^(GENERIC_TYPE_PARAM_LIST[$LESS_THAN, "GENERIC_TYPE_PARAM_LIST"] genericTypeParameter+)
    ;

genericTypeListClosing
    // The handling of closing nested generic type parameter/argument lists is a 
    // little bit 'unorthodox'. Because more than one closing brackets can be 
    // tokenized as 'BIT_SHIFT_RIGHT' or 'SHIFT_RIGHT' or 'GREATER_THAN' or even
    // as a mix of these tokens depending on the amount of closing brackets and
    // whitespaces between them each single closing bracket must related to the
    // appropriate generic type parameter/argument virtually.
    //
    // This will be done by this rule by stacking the closing bracket 
    // information for each outer generic type parameters/argument list because
    // all possible closing bracket tokens are bound to the most inner generic
    // type parameter/argument list by this grammar.
    //
    // Therefore, if this rule fetches a closing bracket token that belongs to
    // more than one generic type parameter/argument it stacks the current 
    // position of the tokens 'n-1' times for the outer generic type 
    // parameter/argument lists and uses these buffered positions to rewind the
    // token stream to virtualize the closing of the outer generic type
    // parameters/arguments.
    //
    @init {
        TokenStream tokenStream = getTokenStream();
        if (mReusableGenTypeClosingBrackets.stackSize() == 0) {
            int tokenType = tokenStream.LA(1);
            if (tokenType == SHIFT_RIGHT) {
                // Remember the position of this token for a further closing.
                int marker = tokenStream.mark();
                mReusableGenTypeClosingBrackets.push(marker);
            } else if (tokenType == BIT_SHIFT_RIGHT) {
                // Remember the position of this token for two further virtual
                // closings..
                mReusableGenTypeClosingBrackets.push(tokenStream.mark());
                mReusableGenTypeClosingBrackets.push(tokenStream.mark());
            }
        } else {
            // Decrease the stack.
            mReusableGenTypeClosingBrackets.pop();
        } 
    }
    :   GREATER_THAN
    |  	SHIFT_RIGHT
    |   BIT_SHIFT_RIGHT
    ;
    finally {
        if (mReusableGenTypeClosingBrackets.stackSize() > 0) {
            // Rewind the stream for a virtual generic type parameter/argument
            // closing but let the position in the stack in order to avoid that
            // the stack is empty when entering this rule again.
            tokenStream.rewind(mReusableGenTypeClosingBrackets.peek());
        }  
    }

genericTypeParameter
    :   IDENT bound?
        ->  ^(IDENT bound?)
    ;
        
bound
    :   EXTENDS type (AND type)*
        ->  ^(EXTENDS_BOUND_LIST[$EXTENDS, "EXTENDS_BOUND_LIST"] type+)
    ;

enumTypeDeclaration[CommonTree modifiers]
    :   ENUM IDENT implementsClause? enumBody
        ->  ^(ENUM {$modifiers} IDENT implementsClause? enumBody)
    ;
    
enumBody
    :   LCURLY enumScopeDeclarations RCURLY
        ->  ^(ENUM_TOP_LEVEL_SCOPE[$LCURLY, "ENUM_TOP_LEVEL_SCOPE"] enumScopeDeclarations*)
    ;

enumScopeDeclarations
    :   enumConstants? (COMMA!)? enumClassScopeDeclarations?
    ;

enumClassScopeDeclarations
    :   SEMI classScopeDeclarations*
        ->  ^(CLASS_TOP_LEVEL_SCOPE[$SEMI, "CLASS_TOP_LEVEL_SCOPE"] classScopeDeclarations*)
    ;

enumConstants
    :   enumConstant (COMMA! enumConstant)*
    ;
    
enumConstant
    :   annotationList IDENT^ arguments? classBody?
    ;
    
interfaceTypeDeclaration[CommonTree modifiers]
    :   INTERFACE IDENT genericTypeParameterList? interfaceExtendsClause? interfaceBody
        ->  ^(INTERFACE {$modifiers} IDENT genericTypeParameterList? interfaceExtendsClause? interfaceBody)
    ;
    
typeList
    :   type (COMMA! type)*
    ;
    
classBody
    :   LCURLY classScopeDeclarations* RCURLY
        ->  ^(CLASS_TOP_LEVEL_SCOPE[$LCURLY, "CLASS_TOP_LEVEL_SCOPE"] classScopeDeclarations*)
    ;
    
interfaceBody
    :   LCURLY interfaceScopeDeclarations* RCURLY
        ->  ^(INTERFACE_TOP_LEVEL_SCOPE[$LCURLY, "CLASS_TOP_LEVEL_SCOPE"] interfaceScopeDeclarations*)
    ;

classScopeDeclarations
    :   block           ->  ^(CLASS_INSTANCE_INITIALIZER block)
    |   STATIC block    ->  ^(CLASS_STATIC_INITIALIZER[$STATIC, "CLASS_STATIC_INITIALIZER"] block)
    |   modifierList
        (   genericTypeParameterList?
            (   type IDENT formalParameterList arrayDeclaratorList? throwsClause? (block | SEMI)
                ->  ^(FUNCTION_METHOD_DECL modifierList genericTypeParameterList? type IDENT formalParameterList arrayDeclaratorList? throwsClause? block?)
            |   VOID IDENT formalParameterList throwsClause? (block | SEMI)
                ->  ^(VOID_METHOD_DECL modifierList genericTypeParameterList? IDENT formalParameterList throwsClause? block?)
            |   ident=IDENT formalParameterList throwsClause? block
                ->  ^(CONSTRUCTOR_DECL[$ident, "CONSTRUCTOR_DECL"] modifierList genericTypeParameterList? formalParameterList throwsClause? block)
            )
        |   type classFieldDeclaratorList SEMI
            ->  ^(VAR_DECLARATION modifierList type classFieldDeclaratorList)
        )
    |   typeDeclaration
    |   SEMI!
    ;
            
interfaceScopeDeclarations
    :   modifierList
        (   genericTypeParameterList?
            (   type IDENT formalParameterList arrayDeclaratorList? throwsClause? SEMI
                ->  ^(FUNCTION_METHOD_DECL modifierList genericTypeParameterList? type IDENT formalParameterList arrayDeclaratorList? throwsClause?)
            |   VOID IDENT formalParameterList throwsClause? SEMI
                ->  ^(VOID_METHOD_DECL modifierList genericTypeParameterList? IDENT formalParameterList throwsClause?)
            )
        |   type interfaceFieldDeclaratorList SEMI
            ->  ^(VAR_DECLARATION modifierList type interfaceFieldDeclaratorList)
        )
    |   typeDeclaration
    |   SEMI!
    ;

classFieldDeclaratorList
    :   classFieldDeclarator (COMMA classFieldDeclarator)*
        ->  ^(VAR_DECLARATOR_LIST classFieldDeclarator+)
    ;

classFieldDeclarator
    :   variableDeclaratorId (ASSIGN variableInitializer)?
        ->  ^(VAR_DECLARATOR variableDeclaratorId variableInitializer?)
    ;
    
interfaceFieldDeclaratorList
    :   interfaceFieldDeclarator (COMMA interfaceFieldDeclarator)*
        ->  ^(VAR_DECLARATOR_LIST interfaceFieldDeclarator+)
    ;

interfaceFieldDeclarator
    :   variableDeclaratorId ASSIGN variableInitializer
        ->  ^(VAR_DECLARATOR variableDeclaratorId variableInitializer)
    ;
    
variableDeclaratorId
    :   IDENT^ arrayDeclaratorList?
    ;

variableInitializer
    :   arrayInitializer
    |   expression
    ;

arrayDeclarator
    :   LBRACK RBRACK
        ->  ^(ARRAY_DECLARATOR)
    ;

arrayDeclaratorList
    :   arrayDeclarator+
        ->  ^(ARRAY_DECLARATOR_LIST arrayDeclarator+)   
    ;
    
arrayInitializer
    :   LCURLY (variableInitializer (COMMA variableInitializer)* COMMA?)? RCURLY
        ->  ^(ARRAY_INITIALIZER[$LCURLY, "ARRAY_INITIALIZER"] variableInitializer*)
    ;

throwsClause
    :   THROWS qualifiedIdentList
        ->  ^(THROWS_CLAUSE[$THROWS, "THROWS_CLAUSE"] qualifiedIdentList)
    ;

modifierList
    :   modifier*   
        ->  ^(MODIFIER_LIST modifier*)
    ;

modifier
    :   PUBLIC
    |   PROTECTED
    |   PRIVATE
    |   STATIC
    |   ABSTRACT
    |   NATIVE
    |   SYNCHRONIZED
    |   TRANSIENT
    |   VOLATILE
    |   STRICTFP
    |   localModifier
    ;

localModifierList
    :   localModifier*
        -> ^(LOCAL_MODIFIER_LIST localModifier*)
    ;
    
localModifier
    :   FINAL
    |   annotation
    ;

type
    :   simpleType
    |   objectType 
    ;
    
simpleType
    :   primitiveType arrayDeclaratorList?
        ->  ^(TYPE primitiveType arrayDeclaratorList?)  
    ;
    
objectType
    :   qualifiedTypeIdent arrayDeclaratorList?
        ->  ^(TYPE qualifiedTypeIdent arrayDeclaratorList?)
    ;

// TODO  Remove this rule.
//objectTypeSimplified
//    :   qualifiedTypeIdentSimplified arrayDeclaratorList?
//        ->  ^(TYPE qualifiedTypeIdentSimplified arrayDeclaratorList?)
//    ;

qualifiedTypeIdent
    :   typeIdent (DOT typeIdent)*
        ->  ^(QUALIFIED_TYPE_IDENT typeIdent+) 
    ;

qualifiedTypeIdentSimplified
    :   typeIdentSimplified (DOT typeIdentSimplified)*
        ->  ^(QUALIFIED_TYPE_IDENT typeIdentSimplified+) 
    ;

typeIdent
    :   IDENT^ genericTypeArgumentList? 
    ;

typeIdentSimplified
    :   IDENT^ genericTypeArgumentListSimplified?
    ;

primitiveType
    :   BOOLEAN
    |   CHAR
    |   BYTE
    |   SHORT
    |   INT
    |   LONG
    |   FLOAT
    |   DOUBLE
    ;

genericTypeArgumentList
    :   LESS_THAN genericTypeArgument (COMMA genericTypeArgument)* genericTypeListClosing
        ->  ^(GENERIC_TYPE_ARG_LIST[$LESS_THAN, "GENERIC_TYPE_ARG_LIST"] genericTypeArgument+)
    ;

genericTypeArgument
    :   type
    |   QUESTION genericWildcardBoundType?
        ->  ^(QUESTION genericWildcardBoundType?)
    ;
    
genericWildcardBoundType
    :   (EXTENDS | SUPER)^ type
    ;

genericTypeArgumentListSimplified
    :   LESS_THAN genericTypeArgumentSimplified (COMMA genericTypeArgumentSimplified)* genericTypeListClosing
        ->  ^(GENERIC_TYPE_ARG_LIST[$LESS_THAN, "GENERIC_TYPE_ARG_LIST"] genericTypeArgumentSimplified+)
    ;
    
genericTypeArgumentSimplified
    :   type
    |   QUESTION
    ;
    
qualifiedIdentList
    :   qualifiedIdentifier (COMMA! qualifiedIdentifier)*
    ;
    
formalParameterList
    :   LPAREN 
        (   // Contains at least one standard argument declaration and optionally a variable argument declaration.
            formalParameterStandardDecl (COMMA formalParameterStandardDecl)* (COMMA formalParameterVarArgDecl)? 
            ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"] formalParameterStandardDecl+ formalParameterVarArgDecl?) 
            // Contains a variable argument declaration only.
        |   formalParameterVarArgDecl
            ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"] formalParameterVarArgDecl) 
            // Contains nothing.
        |   ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"]) 
        )
        RPAREN
    ;
    
formalParameterStandardDecl
    :   localModifierList type variableDeclaratorId
        ->  ^(FORMAL_PARAM_STD_DECL localModifierList type variableDeclaratorId)
    ;
    
formalParameterVarArgDecl
    :   localModifierList type ELLIPSIS variableDeclaratorId
        ->  ^(FORMAL_PARAM_VARARG_DECL localModifierList type variableDeclaratorId)
    ;
    
qualifiedIdentifier
    :   (   IDENT               ->  IDENT
        )
        (   DOT ident=IDENT     ->  ^(DOT $qualifiedIdentifier $ident)
        )*
    ;
    
// ANNOTATIONS

annotationList
    :   annotation*
        ->  ^(ANNOTATION_LIST annotation*)
    ;

annotation
    :   AT^ qualifiedIdentifier annotationInit?
    ;
    
annotationInit
    :   LPAREN annotationInitializers? RPAREN
        ->  ^(ANNOTATION_INIT_BLOCK[$LPAREN, "ANNOTATION_INIT_BLOCK"] annotationInitializers?)
    ;

annotationInitializers
    :   annotationInitializer (COMMA annotationInitializer)*
        ->  ^(ANNOTATION_INIT_KEY_LIST annotationInitializer+)
    |   annotationElementValue // implicit initialization of the annotation field 'value'
        ->  ^(ANNOTATION_INIT_DEFAULT_KEY annotationElementValue)
    ;
    
annotationInitializer
    :   IDENT^ ASSIGN! annotationElementValue
    ;
    
annotationElementValue
    :   annotationElementValueExpression
    |   annotation
    |   annotationElementValueArrayInitializer
    ;
    
annotationElementValueExpression
    :   conditionalExpression
        ->  ^(EXPR conditionalExpression)
    ;
    
annotationElementValueArrayInitializer
    :   LCURLY (annotationElementValue (COMMA annotationElementValue)*)? (COMMA)? RCURLY
        ->  ^(ANNOTATION_INIT_ARRAY_ELEMENT[$LCURLY, "ANNOTATION_ELEM_VALUE_ARRAY_INIT"] annotationElementValue*)
    ;
    
annotationTypeDeclaration[CommonTree modifiers]
    :   AT INTERFACE IDENT annotationBody
        -> ^(AT {$modifiers} IDENT annotationBody)
    ;
    
annotationBody
    :   LCURLY annotationScopeDeclarations* RCURLY
        ->  ^(ANNOTATION_TOP_LEVEL_SCOPE[$LCURLY, "CLASS_TOP_LEVEL_SCOPE"] annotationScopeDeclarations*)
    ;
    
annotationScopeDeclarations
    :   modifierList type
        (   IDENT LPAREN RPAREN annotationDefaultValue? SEMI
            ->  ^(ANNOTATION_METHOD_DECL modifierList type IDENT annotationDefaultValue?)
        |   classFieldDeclaratorList SEMI
            ->  ^(VAR_DECLARATION modifierList type classFieldDeclaratorList)
        )
    |   typeDeclaration
    |   SEMI!
    ;
    
annotationDefaultValue
    :   DEFAULT^ annotationElementValue
    ;

// STATEMENTS / BLOCKS

block
    :   LCURLY blockStatement* RCURLY
        ->  ^(BLOCK_SCOPE[$LCURLY, "BLOCK_SCOPE"] blockStatement*)
    ;

blockStatement
    :   localVariableDeclaration SEMI!
    |   typeDeclaration
    |   statement
    ;
    
localVariableDeclaration
        // Because the initializer of a local variable is optional local
        // variable declarators are equal to class field declarators.
    :   localModifierList type classFieldDeclaratorList
        ->  ^(VAR_DECLARATION localModifierList type classFieldDeclaratorList)
    ;
    
        
statement
    :   block
    |   assertStatement
    |   ifStatement   
    |   forStatement
    |	forEachStatement
    |   whileStatement
    |   doWhileStatement
    |   tryStatement
    |   switchStatement
    |   synchronizedStatement
    |   returnStatement
    |   throwStatement
    |   breakStatement
    |   continueStatement
    |   labeledStatement
    |   expressionStatement
    ;           

assertStatement
	:	ASSERT expr1=expression 
        (   COLON expr2=expression SEMI	-> ^(ASSERT $expr1 $expr2)
        |   SEMI                        -> ^(ASSERT $expr1)
        )
	;
        
ifStatement
	:	IF parenthesizedExpression ifStat=statement 
        (   ELSE elseStat=statement -> ^(IF parenthesizedExpression $ifStat $elseStat)
        |                           -> ^(IF parenthesizedExpression $ifStat)
        )
	;
        
forStatement
	:	FOR LPAREN forInit SEMI forCondition SEMI forUpdater RPAREN statement
		-> ^(FOR forInit forCondition forUpdater statement)
	;
        
forEachStatement
	:	FOR LPAREN localModifierList type IDENT COLON expression RPAREN statement
		-> ^(FOR_EACH[$FOR, "FOR_EACH"] localModifierList type IDENT expression statement)
	;
        
whileStatement
	:	WHILE parenthesizedExpression statement
		-> ^(WHILE parenthesizedExpression statement)
	;
        
doWhileStatement
	:	DO statement WHILE parenthesizedExpression SEMI
		-> ^(DO statement parenthesizedExpression)
	;
        
tryStatement
	:	TRY block (catches finallyClause? | finallyClause)
		-> ^(TRY block catches? finallyClause?)
	;
        
switchStatement
	:	SWITCH parenthesizedExpression LCURLY switchBlockLabels RCURLY
		-> ^(SWITCH parenthesizedExpression switchBlockLabels)
	;
        
synchronizedStatement
	:	SYNCHRONIZED parenthesizedExpression block
		-> ^(SYNCHRONIZED parenthesizedExpression block)
	;
        
returnStatement
	:	RETURN expression? SEMI -> ^(RETURN expression?)
	;
        
throwStatement
	:	THROW expression SEMI -> ^(THROW expression)
	;
        
breakStatement
	:	BREAK IDENT? SEMI -> ^(BREAK IDENT?)
	;
        
continueStatement
	:	CONTINUE IDENT? SEMI -> ^(CONTINUE IDENT?)
	;
        
labeledStatement
	:	IDENT COLON statement -> ^(LABELED_STATEMENT IDENT statement)
	;
        
expressionStatement
	:	expression SEMI!
    |   SEMI // Preserve empty statements.
	;
        
catches
    :   catchClause+
        ->  ^(CATCH_CLAUSE_LIST catchClause+)
    ;
    
catchClause
    :   CATCH^ LPAREN! formalParameterStandardDecl RPAREN! block
    ;

finallyClause
    :   FINALLY block
        ->  block
    ;

switchBlockLabels
    :   preCases=switchCaseLabels switchDefaultLabel? postCases=switchCaseLabels
        ->  ^(SWITCH_BLOCK_LABEL_LIST $preCases? switchDefaultLabel? $postCases?)
    ;
    
switchCaseLabels
    :   switchCaseLabel*
    ;
        
switchCaseLabel
    :   CASE^ expression COLON! blockStatement*
    ;
    
switchDefaultLabel
    :   DEFAULT^ COLON! blockStatement*
    ;
    
forInit
    :   localVariableDeclaration    ->  ^(FOR_INIT localVariableDeclaration)
    |   expressionList              ->  ^(FOR_INIT expressionList)
    |                               ->  ^(FOR_INIT)
    ;
    
forCondition
    :   expression?
        ->  ^(FOR_CONDITION expression?)
    ;
    
forUpdater
    :   expressionList?
        ->  ^(FOR_UPDATE expressionList?)
    ;

// EXPRESSIONS

parenthesizedExpression
    :   LPAREN expression RPAREN
        ->  ^(PARENTESIZED_EXPR[$LPAREN, "PARENTESIZED_EXPR"] expression)
    ;
    
expressionList
    :   expression (COMMA! expression)*
    
    ;

expression
    :   assignmentExpression
        ->  ^(EXPR assignmentExpression)
    ;

assignmentExpression
    :   conditionalExpression 
        (   (   ASSIGN^
            |   PLUS_ASSIGN^
            |   MINUS_ASSIGN^
            |   STAR_ASSIGN^
            |   DIV_ASSIGN^
            |   AND_ASSIGN^
            |   OR_ASSIGN^
            |   XOR_ASSIGN^
            |   MOD_ASSIGN^
            |   SHIFT_LEFT_ASSIGN^
            |   SHIFT_RIGHT_ASSIGN^
            |   BIT_SHIFT_RIGHT_ASSIGN^
        ) 
        assignmentExpression)?
    ;
    
conditionalExpression
    :   logicalOrExpression (QUESTION^ assignmentExpression COLON! conditionalExpression)?
    ;

logicalOrExpression
    :   logicalAndExpression (LOGICAL_OR^ logicalAndExpression)*
    ;

logicalAndExpression
    :   inclusiveOrExpression (LOGICAL_AND^ inclusiveOrExpression)*
    ;

inclusiveOrExpression
    :   exclusiveOrExpression (OR^ exclusiveOrExpression)*
    ;

exclusiveOrExpression
    :   andExpression (XOR^ andExpression)*
    ;

andExpression
    :   equalityExpression (AND^ equalityExpression)*
    ;

equalityExpression
    :   instanceOfExpression 
        (   (   EQUAL^
            |   NOT_EQUAL^
            ) 
            instanceOfExpression
        )*
    ;

instanceOfExpression
    :   relationalExpression (INSTANCEOF^ type)?
    ;

relationalExpression
    :   shiftExpression 
        (   (   LESS_OR_EQUAL^
            |   GREATER_OR_EQUAL^
            |   LESS_THAN^
            |   GREATER_THAN^
            )
            shiftExpression
        )*
    ;
    
shiftExpression
    :   additiveExpression
        (   (   BIT_SHIFT_RIGHT^
            |   SHIFT_RIGHT^
            |   SHIFT_LEFT^
            )
            additiveExpression
        )*
    ;

additiveExpression
    :   multiplicativeExpression
        (   (   PLUS^
            |   MINUS^
            )
            multiplicativeExpression
        )*
    ;

multiplicativeExpression
    :   unaryExpressionAll 
        (   (   STAR^
            |   DIV^
            |   MOD^
            )
            unaryExpressionAll
        )*
    ;
    
unaryExpressionAll
    :   PLUS unaryExpressionAll     ->  ^(UNARY_PLUS[$PLUS, "UNARY_PLUS"] unaryExpressionAll)
    |   MINUS unaryExpressionAll    ->  ^(UNARY_MINUS[$MINUS, "UNARY_MINUS"] unaryExpressionAll)
    |   INC postfixedExpression     ->  ^(PRE_INC[$INC, "PRE_INC"] postfixedExpression)
    |   DEC postfixedExpression     ->  ^(PRE_DEC[$DEC, "PRE_DEC"] postfixedExpression)
    |   unaryExpressionRest
    ;

unaryExpressionRest
    :   NOT unaryExpressionAll                          ->  ^(NOT unaryExpressionAll)
    |   LOGICAL_NOT unaryExpressionAll                  ->  ^(LOGICAL_NOT unaryExpressionAll)
    |   LPAREN simpleType RPAREN unaryExpressionAll     ->  ^(CAST_EXPR[$LPAREN, "CAST_EXPR"] simpleType unaryExpressionAll)
    |   LPAREN objectType RPAREN unaryExpressionRest    ->  ^(CAST_EXPR[$LPAREN, "CAST_EXPR"] objectType unaryExpressionRest)
    |   postfixedExpression
    ;
    
postfixedExpression
        // At first resolve the primary expression ...
    :   (   primaryExpression                       ->  primaryExpression
        )
        // ... and than the optional things that may follow a primary expression 0 or more times.
        (   outerDot=DOT            
        			// Note: generic type arguments are only valid for method 
        			// calls, i.e. if there is an argument list.                
            (   (   genericTypeArgumentListSimplified?  
                    IDENT                           ->  ^(DOT $postfixedExpression IDENT)
                ) 
                (   arguments                       ->  ^(METHOD_CALL $postfixedExpression genericTypeArgumentListSimplified? arguments)
                )?
            |   THIS                                ->  ^(DOT $postfixedExpression THIS)
            |   genericTypeArgumentListSimplified?
            	Super=SUPER arguments               ->  ^(SUPER_CONSTRUCTOR_CALL[$Super, "SUPER_CONSTRUCTOR_CALL"] $postfixedExpression genericTypeArgumentListSimplified? arguments)
            |   (   SUPER innerDot=DOT IDENT        ->  ^($innerDot ^($outerDot $postfixedExpression SUPER) IDENT)
                )
                (   arguments                       ->  ^(METHOD_CALL $postfixedExpression arguments)
                )?
            |   innerNewExpression                  ->  ^(DOT $postfixedExpression innerNewExpression)
            )
        |   LBRACK expression RBRACK                ->  ^(ARRAY_ELEMENT_ACCESS $postfixedExpression expression)
        )*
        // At the end there may follow a post increment/decrement.
        (   INC -> ^(POST_INC[$INC, "POST_INC"] $postfixedExpression)
        |   DEC -> ^(POST_DEC[$DEC, "POST_DEC"] $postfixedExpression)
        )?
    ;    
    
primaryExpression
    :   parenthesizedExpression
    |   literal
    |   newExpression
    |   qualifiedIdentExpression
    |   genericTypeArgumentListSimplified 
        (   SUPER
            (   arguments                               ->  ^(SUPER_CONSTRUCTOR_CALL[$SUPER, "SUPER_CONSTRUCTOR_CALL"] genericTypeArgumentListSimplified arguments)
            |   DOT IDENT arguments                     ->  ^(METHOD_CALL ^(DOT SUPER IDENT) genericTypeArgumentListSimplified arguments)
            )
        |   IDENT arguments                             ->  ^(METHOD_CALL IDENT genericTypeArgumentListSimplified arguments)
        |   THIS arguments                              ->  ^(THIS_CONSTRUCTOR_CALL[$THIS, "THIS_CONSTRUCTOR_CALL"] genericTypeArgumentListSimplified arguments)
        )
    |   (   THIS                                        ->  THIS
        )
        (   arguments                                   ->  ^(THIS_CONSTRUCTOR_CALL[$THIS, "THIS_CONSTRUCTOR_CALL"] arguments)
        )?
    |   SUPER arguments                                 ->  ^(SUPER_CONSTRUCTOR_CALL[$SUPER, "SUPER_CONSTRUCTOR_CALL"] arguments)
    |   (   SUPER DOT IDENT
        )
        (   arguments                                   ->  ^(METHOD_CALL ^(DOT SUPER IDENT) arguments)
        |                                               ->  ^(DOT SUPER IDENT)
        )
    |   (   primitiveType                               ->  primitiveType
        )
        (   arrayDeclarator                             ->  ^(arrayDeclarator $primaryExpression)   
        )* 
        DOT CLASS                                       ->  ^(DOT $primaryExpression CLASS)
    |   VOID DOT CLASS                                  ->  ^(DOT VOID CLASS)
    ;
    
qualifiedIdentExpression
        // The qualified identifier itself is the starting point for this rule.
    :   (   qualifiedIdentifier                             ->  qualifiedIdentifier
        )
        // And now comes the stuff that may follow the qualified identifier.
        (   (   arrayDeclarator                         ->  ^(arrayDeclarator $qualifiedIdentExpression)
            )+ 
            (   DOT CLASS                               ->  ^(DOT $qualifiedIdentExpression CLASS)
            )
        |   arguments                                   ->  ^(METHOD_CALL qualifiedIdentifier arguments)
        |   outerDot=DOT
            (   CLASS                                   ->  ^(DOT qualifiedIdentifier CLASS)
            |   genericTypeArgumentListSimplified 
                (   Super=SUPER arguments               ->  ^(SUPER_CONSTRUCTOR_CALL[$Super, "SUPER_CONSTRUCTOR_CALL"] qualifiedIdentifier genericTypeArgumentListSimplified arguments)
                |   SUPER innerDot=DOT IDENT arguments  ->  ^(METHOD_CALL ^($innerDot ^($outerDot qualifiedIdentifier SUPER) IDENT) genericTypeArgumentListSimplified arguments)
                |   IDENT arguments                     ->  ^(METHOD_CALL ^(DOT qualifiedIdentifier IDENT) genericTypeArgumentListSimplified arguments)
                )
            |   THIS                                    ->  ^(DOT qualifiedIdentifier THIS)
            |   Super=SUPER arguments                   ->  ^(SUPER_CONSTRUCTOR_CALL[$Super, "SUPER_CONSTRUCTOR_CALL"] qualifiedIdentifier arguments)
            |   innerNewExpression                      ->  ^(DOT qualifiedIdentifier innerNewExpression)
            )
        )?
    ;

newExpression
    :   NEW  
        (   primitiveType newArrayConstruction      // new static array of primitive type elements
            ->  ^(STATIC_ARRAY_CREATOR[$NEW, "STATIC_ARRAY_CREATOR"] primitiveType newArrayConstruction)
        |   genericTypeArgumentListSimplified? qualifiedTypeIdentSimplified
            (   newArrayConstruction                // new static array of object type reference elements
                ->  ^(STATIC_ARRAY_CREATOR[$NEW, "STATIC_ARRAY_CREATOR"] genericTypeArgumentListSimplified? qualifiedTypeIdentSimplified newArrayConstruction)
            |   arguments classBody?                // new object type via constructor invocation
                ->  ^(CLASS_CONSTRUCTOR_CALL[$NEW, "STATIC_ARRAY_CREATOR"] genericTypeArgumentListSimplified? qualifiedTypeIdentSimplified arguments classBody?)
            )
        )
    ;
    
innerNewExpression // something like 'InnerType innerType = outer.new InnerType();'
    :   NEW genericTypeArgumentListSimplified? IDENT arguments classBody?
        ->  ^(CLASS_CONSTRUCTOR_CALL[$NEW, "STATIC_ARRAY_CREATOR"] genericTypeArgumentListSimplified? IDENT arguments classBody?)
    ;

newArrayConstruction
    :   arrayDeclaratorList arrayInitializer
    |   LBRACK! expression RBRACK! (LBRACK! expression RBRACK!)* arrayDeclaratorList?
    ;

arguments
    :   LPAREN expressionList? RPAREN
        ->  ^(ARGUMENT_LIST[$LPAREN, "ARGUMENT_LIST"] expressionList?)
    ;

literal 
    :   HEX_LITERAL
    |   OCTAL_LITERAL
    |   DECIMAL_LITERAL
    |   FLOATING_POINT_LITERAL
    |   CHARACTER_LITERAL
    |   STRING_LITERAL
    |   TRUE
    |   FALSE
    |   NULL
    ;

// LEXER

HEX_LITERAL : HEX_PREFIX HEX_DIGIT+ INTEGER_TYPE_SUFFIX? ;

fragment
HEX_PREFIX : '0' ('x'|'X') ;

DECIMAL_LITERAL : ('0' | '1'..'9' '0'..'9'*) INTEGER_TYPE_SUFFIX? ;

OCTAL_LITERAL : '0' ('0'..'7')+ INTEGER_TYPE_SUFFIX? ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
INTEGER_TYPE_SUFFIX : ('l'|'L') ;

FLOATING_POINT_LITERAL
    :   DECIMAL_FP_LITERAL
    |   HEXADECIMAL_FP_LITERAL
    ;
    
fragment
DECIMAL_FP_LITERAL
    :   ('0'..'9')+ 
        (
            DOT ('0'..'9')* EXPONENT? FLOAT_TYPE_SUFFIX?
        |   EXPONENT FLOAT_TYPE_SUFFIX?
        |   FLOAT_TYPE_SUFFIX
        )
    |   DOT ('0'..'9')+ EXPONENT? FLOAT_TYPE_SUFFIX?
    ;
    
fragment
HEXADECIMAL_FP_LITERAL // example: 0xF.Fp3 (decimal: (15+15/16) + 2^3 => 127.5)
	:	HEX_PREFIX
		(
			// There're five possibilities of what can occur between the 
			// '0x/0X' and 'p/p':
			//
			// 1	nothing					example: 0xp10
			// 2	hexLiteral				example: 0xFp10
			// 3	hexLiteral.				example: 0xF.p10
			// 4	.hexLiteral				example: 0x.Fp10
			// 5	hexLiteral.hexLiteral  	example: 0x.F.F.p10
			HEX_DIGIT+ (DOT (HEX_DIGIT+)?)?
		|	DOT HEX_DIGIT+
		)?
		('p'|'P') SIGNED_INTEGER FLOAT_TYPE_SUFFIX?
	;

fragment
EXPONENT : ('e'|'E') SIGNED_INTEGER ;

fragment
SIGNED_INTEGER : ('+'|'-')? ('0'..'9')+ ;

fragment
FLOAT_TYPE_SUFFIX : ('f'|'F'|'d'|'D') ;

CHARACTER_LITERAL
    :   '\'' ( ESCAPE_SEQUENCE | ~('\''|'\\') ) '\''
    ;

STRING_LITERAL
    :  '"' ( ESCAPE_SEQUENCE | ~('\\'|'"') )* '"'
    ;

fragment
ESCAPE_SEQUENCE
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
OCTAL_ESCAPE
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

IDENT
    :   JAVA_ID_START (JAVA_ID_PART)*
    ;

fragment
JAVA_ID_START
    :  '\u0024'
    |  '\u0041'..'\u005a'
    |  '\u005f'
    |  '\u0061'..'\u007a'
    |  '\u00c0'..'\u00d6'
    |  '\u00d8'..'\u00f6'
    |  '\u00f8'..'\u00ff'
    |  '\u0100'..'\u1fff'
    |  '\u3040'..'\u318f'
    |  '\u3300'..'\u337f'
    |  '\u3400'..'\u3d2d'
    |  '\u4e00'..'\u9fff'
    |  '\uf900'..'\ufaff'
    ;

fragment
JAVA_ID_PART
    :  JAVA_ID_START
    |  '\u0030'..'\u0039'
    ;

NL  :  ('\r\n'|'\r'|'\n') 
    {   
        if (!mPreserveNewlineCharacters) {
            skip();
        } else {
            setText(NEW_LINE);
            $channel = HIDDEN;
        }
    }
    ;

WS  :  (' '|'\t'|'\u000C') 
    {   
        if (!mPreserveWhitespaces) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;

BLOCK_COMMENT // excluding javadoc comments
    :   (   '/*' ~'*' ( options {greedy=false;} : . )* '*/'
        |   '/**/'
        )
    {   
        if (!mPreserveBlockComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;

JAVADOC_COMMENT
    :   '/**' ( options {greedy=false;} : . )* '*/'
    {   
        if (!mPreserveJavaDocComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')*
    {   
        if (!mPreserveLineComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;
