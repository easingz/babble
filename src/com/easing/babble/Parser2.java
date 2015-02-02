// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

class Parser2 {
    private static final String TAG = "parser2";
    private Lexer mLexer;

    public Parser2(Lexer l) {
        assert l != null;
        mLexer = l;
    }

    public ProgramNode parse() {
        try {
            ProgramNode node = new ProgramNode(parseStatements);
            if (node.valid() && matchToken(TokenType.END)) {
                return node;
            } else {
                throw new ParseException("error, program should end with '\0'");
            }
        } catch (ParseException e) {
            Logger.d(TAG, e.getMessage());
        }
        return null;
    }

    private BlockNode parseBlock() throws ParseException {
        logStart("block");
        BlockNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && "{".equals(t.text)) {
            StmtsNode stmts = parseStatements();
            LstStmtNode lst = parseLastStatement();
            node = new CompoundBlockNode(stmts, lst);
            if (node.valid() && matchToken(TokenType.SIGN, "}")) {
                return node;
            }
        } else {
            mLexer.pushBack(t);
            node = new SingleStmtBlockNode(parseStatement());
            if (node.valid()) {
                return node;
            }
        }
        throw new ParseException("parsing block error at : " + t.loc.row + "," + t.loc.col);
    }

    private LstStmtNode parseLastStatement() throws ParseException {
        logStart("last statement");
        Token t = mLexer.next();
        LstStmtNode node;
        if (t.type == TokenType.KEYWORD && "return".equals(t.text)) {
            node = new LstStmtNode(parseExpr());
            if (node.valid() && matchStmtSeparator()) {
                return node;
            }
            throw new ParseException("parsing var continue error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private StmtsNode parseStatements() throws ParseException {
        logStart("statements");
        StmtsNode node;
        mLexer.pushBack(t);
        if (t.type == TokenType.END || t.type == TokenType.KEYWORD && "return".equals(t.text)) {
            // match epslon
        } else {
            StmtNode stmt = parseStatement();
            boolean sepa = matchStmtSeparator();
            StmtsNode stmts = parseStatements();
            if (sepa) {
                node = new StmtNode(stmt, stmts);
                if (node.valid()) {
                    return node;
                }
                throw new ParseException("parsing var continue error at "
                                     + t.loc.row + "," + t.loc.col);
            }
        }
        // match epslon
        return node;
    }

    private boolean matchStmtSeparator() throws ParseException {
        logStart("statement separator");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ";".equals(t.text)) {
            return true;
        }
        // match epslon
        mLexer.pushBack(t);
        return true;
    }

    private StmtNode parseStatement() throws ParseException {
        logStart("statement");
        StmtNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                Logger.d(TAG, "it's a function call");
                FuncCallNode funcCall= parseFunctionCall();
                MoreFuncCallNode moreFuncCall= parseMoreThanFuncCall();
                node = new FuncStmtNode(funcCall, moreFuncCall);
            } else {
                Logger.d(TAG, "it's an assignment");
                node = new AssignStmtNode(parseAssignment());
            }
        } else if (t.type == TokenType.KEYWORD) {
            // 'while', 'if', 'for', 'function', 'local'
            mLexer.pushBack(t);
            switch (t.text) {
            case "while" : node = new WhileNode(parseWhile());
            case "if" : node = new IfNode(parseIf());
            case "for" : node = new ForNode(parseFor());
            case "function" : node = new FuncDefNode(parseFunctionDef());
            case "local" : node = new LocalDeclNode(parseLocalDecl());
            default: //no-op
            }
        }
        if (node != null && node.valid()) {
            return node;
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing statement error at: " + t.loc.row + "," + t.loc.col);
    }

    private FuncCallNode parseFunctionCall() throws ParseException {
        logStart("function call");
        Token t = mLexer.next();
        FuncCallNode node;
        if (t.type == TokenType.IDENTIFIER) {
            Logger.d(TAG, "call fuction: " + t.text);
            IdNode id = new IdNode(t.text);
            ArgsNode args = parseArguments();
            node = new FuncCallNode(id, args);
            if (node.valid()) {
                return node;
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing function call error at: " + t.loc.row + "," + t.loc.col);
    }

    private ArgsNode parseArguments() throws ParseException {
        logStart("function arguments");
        ArgsNode node;
        boolean bracket = matchToken(TokenType.SIGN, "(");
        ExprListNode exprList = parseExprList();
        bracket = bracket & matchToken(TokenType.SIGN, ")");
        if (bracket && exprList != null) {
            node = new ArgsNode(exprList);
        } else {
            throw new ParseException("parsing arguments error.");
        }
        return node;
    }

    private ExprListNode parseExprList() throws ParseException {
        logStart("expression list");
        ExprNode expr = parseExpression();
        ExprContNode exprCont = parseExprContinue();
        ExprListNode node = new ExprListNode(expr, exprCont);
        if (node.valid()) {
            return node;
        }
        throw new ParseException("parsing expression list error.");
    }

    private ExprNode parseExpression() throws ParseException {
        logStart("expression");
        FactorNode factor = parseFactor();
        BinaryExprNode binary = parseBinaryExpr();
        // TODO(important): only left associative here
        ExprNode node = new ExprNode(factor, binary);
        if (node.valid()) {
            return node;
        }
        throw new ParseException("parsing expression error.");
    }

    private FactorNode parseFactor() throws ParseException {
        logStart("factor");
        FactorNode node;
        Token t = mLexer.next();
        switch (t.type) {
        case NIL:
            node = new NilNode();
            break;
        case FALSE:
            node = new FalseNode();
            break;
        case NUMBER:
            // TODO: only support integer now
            node = new NumberNode((Integer) t.val);
            break;
        case STRING:
            node = new StringNode(t.text);
            break;
        case KEYWORD:
            if ("function".equals(t.text)) {
                mLexer.pushBack(t);
                node = parseAnonymousFunction();
                break;
            } else {
                mLexer.pushBack(t);
                throw new ParseException("parsing factor error: keyword "
                                         + t.text + " not allowed.", t.loc);
            }
        case OPERATOR:
            mLexer.pushBack(t);
            node = parseUniqExpr();
            break;
        case SIGN:
            if ("{".equals(t.text)) {
                mLexer.pushBack(t);
                node = parseMap();
                break;
            }
            // maybe '(' else fall thru
        case IDENTIFIER:
            Token ahead = mLexer.next();
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                Logger.d(TAG, "it's a prefix expression");
                node = parsePrefixExpr();
            } else {
                Logger.d(TAG, "it's a variable");
                node = parseVar();
            }
            break;
        default:
            mLexer.pushBack(t);
        }
        if (node != null && node.valid()) {
            return node;
        } else {
            throw new ParseException("parsing factor error at "
                                     + t.loc.row + "," + t.loc.col);
        }
    }

    private UniqExprNode parseUniqExpr() throws ParseException {
        logStart("unique expression");
        Token t = mLexer.next();
        if ("-".equals(t.text) || "#".equals(t.text) || "not".equals(t.text)) {
            Logger.d(TAG, "got unique oeprator: " + t.text);
            UniqOpNode op = new UniqOpNode(t.text);
            ExprNode expr = parseExpression();
            UniqExprNode node = new UniqExprNode(op, expr);
            if (node.valid()) {
                return node;
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing unique expression error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private MapNode parseMap() throws ParseException {
        logStart("map");
        Token t = mLexer.next();
        if ("{".equals(t.text)) {
            FieldListNode fieldList = ParseFieldList();
            if(matchToken(TokenType.SIGN, "}")) {
                return new MapNode(fieldList);
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing map error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private AnonyFuncNode parseAnonymousFunction() throws ParseException {
        logStart("anonymous function");
        Token t = mLexer.next();
        if ("function".equals(t.text)) {
            return new AnonyFuncNode(parseFunctionBody());
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing anonymous function error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private PrefixExprNode ParsePrefixExpr() throws ParseException {
        logStart("prefix expression");
        PrefixExprNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                // only use the right value attribute of variable with function call start
                // creepy hack for LL grammar
                FuncCallNode funcCall= parseFunctionCall();
                if (funcCall != null) {
                    Var2Node var2 = parseVar2();
                    node = new FuncPrefixNode(funcCall, var2);
                    if (node.valid()) {
                        return node;
                    }
                }
            } else {
                node = new VarPrefixNode(parseVar());
                if (node.valid()) {
                    return node;
                }
            }
        } else if (t.type == TokenType.SIGN && "(".equals(t.text)) {
            Logger.d(TAG, "got a prefix expression starting with ( ");
            ExprNode expr = parseExpression();
            if (expr != null && matchToken(TokenType.SIGN, ")")) {
                node = new ExprPrefixNode(expr);
                if (node.valid()) {
                    return node;
                }
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing prefix expression error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private BinaryExprNode parseBinaryExpr() throws ParseException {
        logStart("binary expression");
        BinaryExprNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.OPERATOR) {
            BinaryOpNode op = new BinaryOpNode(t.text);
            ExprNode expr = parseExpression();
            node = new BinaryExprNode(op, expr);
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing binary expression error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private ExprContNode parseExprContinue() throws ParseException {
        logStart("more expressions");
        ExprContNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && t.text.equals(",")) {
            node = new ExprContNode(parseExprList());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing expression continue error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private AssignStmtNode parseAssignment() throws ParseException {
        logStart("assignment");
        AssignStmtNode node;
        VarListNode varList = parseVarList();
        if (varList != null && matchToken(TokenType.SIGN, "=")) {
            node = new AssignStmtNode(varList, parseExprList());
            if (node.valid()) {
                return node;
            }
        }
        throw new ParseException("parsing assignment error."
    }

    private VarListNode parseVarList() throws ParseException {
        logStart("variable list");
        VarListNode node;
        VarNode var = parseVar();
        if (var != null) {
            node = new VarListNode(var, parseVarContinue());
            if (node.valid()) {
                return node;
            }
        }
        throw new ParseException("parsing variable list error."
    }

    private VarNode parseVar() throws ParseException {
        logStart("variable");
        VarNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Logger.d(TAG, "got an identifier: " + t.text);
            IdNode id = new IdNode(t.text);
            node = new IdVarNode(parseVar2());
            if (node.valid()) {
                return node;
            }
        } else if (t.type == TokenType.SIGN && "(".equals(t.text)) {
            ExprNode expr = parseExpression();
            if (expr != null && matchToken(TokenType.SIGN, ")")) {
                node = new ExprVarNode(expr, parseVar1());
                if (node.valid()) {
                    return node;
                }
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing variable error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private Var2Node parseVar2() throws ParseException {
        logStart("optional variable of expression");
        Var2Node node;
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.SIGN && ("[".equals(t.text) || ".".equals(t.text))) {
            node = new Var2Node(parseVar1Node());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing var2 error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        return node;
    }

    private Var1Node parseVar1() throws ParseException {
        logStart("variable of expression");
        Var1Node node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN) {
            if ("[".equals(t.text)) {
                ExprNode expr = parseExpresion();
                if (expr != null && matchToken(TokenType.SIGN, "]")) {
                    Var2Node var2 = parseVar2();
                    node = new IndexVar1Node(expr, var2);
                    if (node.valid()) {
                        return node;
                    }
                }
            } else if (".".equals(t.text)) {
                Token id = mLexer.next();
                if (id.type == TokenType.IDENTIFIER) {
                    Var2Node var2 = parseVar2();
                    node = new MemberVar1Node(id.text, var2);
                    if (node.valid()) {
                        return node;
                    }
                }
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing variable of expression error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private VarContNode parseVarContinue() throws ParseException {
        logStart("more variables");
        VarContNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            node = new VarContNode(parseVarList());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing var continue error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private WhileNode parseWhile() throws ParseException {
        logStart("while statement");
        if (matchToken(TokenType.KEYWORD, "while")) {
            ExprNode expr = parseExpression();
            if (expr != null) {
                BlockNode block = parseBlock();
                WhileNode node = new WhileNode(expr, block);
                if (node.valid()) {
                    return node;
                }
            }
        }
        throw new ParseException("parsing while statement error."
    }

    private IfNode parseIf() throws ParseException {
        logStart("if statement");
        if (matchToken(TokenType.KEYWORD, "if")) {
            ExprNode expr = parseExpression();
            if (matchToken(TokenType.KEYWORD, "then")) {
                BlockNode block = parseBlock();
                ElifStmtNode elif = parseElifStatement();
                ElseStmtNode els = parseElseStatement();
                IfNode node = new IfNode(epxr, block ,elif, els);
                if (node.valid()) {
                    return node;
                }
            }
        }
        throw new ParseException("parsing if statement error. "
    }

    private ElifStmtNode parseElifStatement() throws ParseException {
        logStart("elif statement");
        ElifStmtNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.KEYWORD && "elif".equals(t.text)) {
            ExprNode expr = parseExpresssion();
            if (matchToken(TokenType.KEYWORD, "then")) {
                BlockNode block = parseBlock();
                ElifStmtNode elif = parseElifStatement();
                node = new ElifStmtNode(expr, block, elif);
                if (node.valid()) {
                    return node;
                }
            }
            throw new ParseException("parsing elif statement error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private ElseStmtNode parseElseStatement() throws ParseException {
        logStart("else statement");
        ElseStmtNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.KEYWORD && "else".equals(t.text)) {
            node = new ElseStmtNode(ParseBlock());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing else statement error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        / match epslon
        mLexer.pushBack(t);
        return node;
    }

    private ForNode parseFor() throws ParseException {
        logStart("for statement");
        ForNode node;
        Token t = mLexer.next();
        Token id = mLexer.next();
        if (t.type == TokenType.KEYWORD && "for".equals(t.text) && id.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            if (ahead.type == TokenType.SIGN && "=".equals(ahead.text)) {
                IdNode idNode = new IdNode(id.text);
                ExprNode expr1 = parseExpression();
                if (matchToken(TokenType.SIGN, ",")) {
                    ExprNode expr2 = parseExpression();
                    OptionalStepNode step = parseOptionalStep();
                    BlockNode block = parseBlock();
                    node = new RangeForNode(idNode, expr1, expr2, step, block);
                    if (node.valid()) {
                        return node;
                    }
                }
            } else if (ahead.type == TokenType.SIGN && ",".equals(ahead.text) ||
                       ahead.type == TokenType.KEYWORD && "in".equals(ahead.text)) {
                mLexer.pushBack(ahead);
                mLexer.pushBack(id);
                IdListNode idList = parseIdList();
                if (matchToken(TokenType.KEYWORD, "in")) {
                    ExprListNode exprList = parseExprList();
                    BlockNode block = parseBlock();
                    node = new TraverseForNode(idList, exprList, block);
                    if (node.valid()) {
                        return node;
                    }
                }
            }
        }
        mLexer.pushBack(id);
        mLexer.pushBack(t);
        throw new ParseException("parsing for error at "
                                 + t.loc.row + "," + t.loc.col);
     }

    private IdListNode parseIdList() throws ParseException {
        logStart("Identifier list");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            IdNode id = new IdNode(t.text);
            IdListNode node = new IdListNode(id, parseIdContinue());
            if (node.valid()) {
                return node;
            }
        }
        throw new ParseException("parsing id list error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private IdContNode parseIdContinue() throws ParseException {
        logStart("more identifiers");
        IdContNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            node = new IdContNode(parseIdList());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing id continue error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private OptionalStepNode parseOptionalStep() throws ParseException {
        logStart("optional step");
        OptionalStepNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            node = new OptionalStepNode(parseExpression());
            if (node.valid()) {
                return ndoe;
            }
            throw new ParseException("parsing optional step of for statment error at "
                                     + t.loc.row + "," + t.loc.col);
        }
        mLexer.pushBack(t);
        return node;
    }

    private FuncDefNode parseFunctionDef() throws ParseException {
        logStart("function defination");
        if (matchToken(TokenType.KEYWORD, "function")) {
            Token t = mLexer.next();
            if(t.type == TokenType.IDENTIFIERS) {
                IdNode id = new IdNode(t.text);
                FuncBodyNode body = pasreFunctionBody();
                FuncDefNode node = new FuncDefNode(id, body);
                if (node.valid()) {
                    return node;
                }
            }
        }
        throw new ParseException("parsing function body error.");
    }

    private FuncBodyNode parseFunctionBody() throws ParseException {
        logStart("function body");
        if (matchToken(TokenType.SIGN, "(")) {
            IdListNode parameters = parseIdList();
            if (idList != null && matchToken(TokenType.SIGN, ")")) {
                FuncBodyNode node = new FuncBodyNode(parameters, parseBlock());
                if (node.valid()) {
                    return node;
                }
            }
        }
        throw new ParseException("parsing function body error.");
    }

    private LocalDeclNode parseLocalDecl() throws ParseException {
        logStart("local declarition");
        Token t = mLexer.next();
        Token ahead = mLexer.next();
        mLexer.pushBack(ahead);
        if (t.type == TokenType.KEYWORD && "local".equals(t.text)) {
            if (ahead.type == TokenType.KEYWORD && "function".equals(t.text)) {
                LocalFuncNode node = new LocalFuncNode(parseFunctionDef());
                if (node.valid()) {
                    return node;
                }
            } else if (ahead.type == TokenType.IDENTIFIER) {
                IdListNode idList = parseIdList();
                if (idList != null && matchToken(TokenType.SIGN, "=")) {
                    ExprListNode exprList = parseExprList();
                    LocalVarNode node = new LocalVarNode(idList, exprList);
                    if (node.valid()) {
                        return node;
                    }
                }
            }
        }
        mLexer.pushBack(t);
        throw new ParseException("parsing local declaration error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private FieldListNode parseFieldList() throws ParseException {
        logStart("field list");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        FieldListNode node;
        if (t.type == TokenType.SIGN && "}".equals(t.text)) {
            // match epslon
            return node;
        } else {
            FieldNode field = parseField();
            if (field != null) {
                node = new FieldListNode(field, parseFieldContinue);
                if (node.valid()) {
                    return node;
                }
            }
        }
        throw new ParseException("parsing field list error at "
                                 + t.loc.row + "," + t.loc.col);
    }

    private FieldNode parseField() throws ParseException {
        logStart("field");
        FieldNode node;
        Token t = mLexer.next();
        Token ahead = mLexer.next();
        if (t.type == TokenType.IDENTIFIER && ahead.type == TokenType.SIGN &&
            "=".equals(ahead.text)) {
            Logger.d(TAG, "got a field key: " + t.text);
            ExprNode expr = parseExpression();
            node = new KeyValFieldNode(new KeyNode(t.text()), expr);
        } else {
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            node = new ValFieldNode(parseExpression);
        }
        if (node != null && node.valid()) {
            return node;
        }
        throw new ParseException("parsing field error at "
                                 + t.loc.row + "," + t.loc.col);        
    }

    private FieldContNode parseFieldContinue() throws ParseException {
        logStart("more fields");
        FieldContNode node;
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            node = new FieldContNode(parseFieldList());
            if (node.valid()) {
                return node;
            }
            throw new ParseException("parsing field error at "
                                 + t.loc.row + "," + t.loc.col);
        }
        // match epslon
        mLexer.pushBack(t);
        return node;
    }

    private MoreFuncCallNode parseMoreThanFuncCall() throws ParseException {
        logStart("more than function call");
        MoreFuncCallNode node;
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.SIGN && ("[".equals(t.text) || ".".equals(t.text))) {
            Var1Node var1 = parseVar1();
            VarContNode varCont = parseVarCont();
            if (matchToken(TokenType.SIGN, "=")) {
                ExprListNode exprList = parseExprList();
                node = new MoreFuncCallNode(var1, varCont, expList);
                if (node.valid()) {
                    return node;
                }
            }
            throw new ParseException("parsing more than func call error at "
                                     + t.loc.row + "," + t.loc.col);            
        }
        // match epslon
        return node;
    }

    private boolean matchToken(TokenType type) throws ParseException {
        Token t = mLexer.next();
        boolean ret = t.type == type;
        if (!ret) {
            mLexer.pushBack(t);
            logError("token", t.loc);
        }
        return ret;
    }

    private boolean matchToken(TokenType type, String text) throws ParseException {
        assert text != null;
        Logger.d(TAG, "trying to match : " + text);
        Token t = mLexer.next();
        boolean ret = t.type == type && t.text.equals(text);
        if (!ret) {
            mLexer.pushBack(t);
            logError("token " + t.text, t.loc);
        }
        return ret;
    }

    private void logStart(String s) {
        Logger.d(TAG, "Start parsing " + s);
    }

    private void logError(String s, Token.Location loc) {
        Logger.d(TAG, "error happens when parsing " + s
                 + " at row " + loc.row + " col " + loc.row);
    }
}
