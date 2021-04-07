/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.generators;

import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.openapi.cmd.Filter;
import org.ballerinalang.openapi.exception.BallerinaOpenApiException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.BACKTICK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PLUS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_STATEMENT;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static org.ballerinalang.generators.GeneratorUtils.buildUrl;
import static org.ballerinalang.generators.GeneratorUtils.convertOpenAPITypeToBallerina;
import static org.ballerinalang.generators.GeneratorUtils.escapeIdentifier;
import static org.ballerinalang.generators.GeneratorUtils.getBallerinaOpenApiType;

/**
 * This Util class use for generating ballerina client file according to given yaml file.
 */
public class BallerinaClientGenerator {
    public static Server server;
    public static Paths paths;
    public static Filter filters;

    public static SyntaxTree generateSyntaxTree(Path definitionPath, Filter filter)
            throws IOException, BallerinaOpenApiException, FormatterException {
        // Summaries OpenAPI details
        OpenAPI openAPI = getBallerinaOpenApiType(definitionPath);
        //Filter serverUrl
        List<Server> servers = openAPI.getServers();
        server = servers.get(0);

        paths = openAPI.getPaths();

        paths = setOperationId(paths);
        filters = filter;
        // 1. Load client template syntax tree
        SyntaxTree syntaxTree = null;
        // Create imports http and openapi
        ImportDeclarationNode importForHttp = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , GeneratorConstants.HTTP);
        NodeList<ImportDeclarationNode> imports = AbstractNodeFactory.createNodeList(importForHttp);
        ClassDefinitionNode classDefinitionNode = getClassDefinitionNode();

        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(imports,
                NodeFactory.createNodeList(classDefinitionNode), AbstractNodeFactory.createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        syntaxTree = SyntaxTree.from(textDocument);
        System.out.println(Formatter.format(syntaxTree.modifyWith(modulePartNode)));
        return syntaxTree.modifyWith(modulePartNode);
    }

    /**
     * Generate serverUrl for client default value.
     */
    private static String getServerURL(Server server) throws BallerinaOpenApiException {
        String serverURL;
        if (server != null) {
            if (server.getUrl() == null) {
                serverURL = "http://localhost:9090/v1";
            } else if (server.getVariables() != null) {
                ServerVariables variables = server.getVariables();
                URL url;
                String resolvedUrl = buildUrl(server.getUrl(), variables);
                try {
                    url = new URL(resolvedUrl);
                    serverURL = url.toString();
                } catch (MalformedURLException e) {
                    throw new BallerinaOpenApiException("Failed to read endpoint details of the server: " +
                            server.getUrl(), e);
                }
            } else {
                serverURL = server.getUrl();
            }
        } else {
            serverURL = "http://localhost:9090/v1";
        }
        return  serverURL;
    }

    /**
     * Generate Class definition Node.
     */
    private static ClassDefinitionNode getClassDefinitionNode() throws BallerinaOpenApiException {

        // Generate client class
        Token visibilityQualifier = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.PUBLIC);
        Token clientKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLIENT);
        NodeList<Token> classTypeQualifiers = NodeFactory.createNodeList(clientKeyWord);

        IdentifierToken classKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLASS);
        IdentifierToken className = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLIENT_CLASS);
        Token openBrace = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.OPEN_BRACE);
        //Fill the members
        List<Node> memberNodeList =  new ArrayList<>();
        //Create class field
        ObjectFieldNode fieldNode = getClassField();
        memberNodeList.add(fieldNode);
        //Create init function definition
        //Common Used
        NodeList<Token> qualifierList =
                NodeFactory.createNodeList(AbstractNodeFactory.createIdentifierToken(GeneratorConstants.PUBLIC));
        IdentifierToken functionKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.FUNCTION);
        IdentifierToken functionName = AbstractNodeFactory.createIdentifierToken("init");
        //Create function signature
        IdentifierToken openParen = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.OPEN_PRAN);
        //Add parameters
        List<Node> parameters  = new ArrayList<>();
        NodeList<AnnotationNode> annotationNodes = AbstractNodeFactory.createEmptyNodeList();
        BuiltinSimpleNameReferenceNode typeName = NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                AbstractNodeFactory.createIdentifierToken("string"));
        IdentifierToken paramName = AbstractNodeFactory.createIdentifierToken("serviceUrl");
        IdentifierToken equalToken = AbstractNodeFactory.createIdentifierToken("=");
        BasicLiteralNode expression = NodeFactory.createBasicLiteralNode(STRING_LITERAL,
                AbstractNodeFactory.createIdentifierToken('"' + getServerURL(server) + '"'));

        DefaultableParameterNode serviceUrl = NodeFactory.createDefaultableParameterNode(annotationNodes, typeName,
                paramName, equalToken, expression);
        IdentifierToken comma = AbstractNodeFactory.createIdentifierToken(",");
        parameters.add(serviceUrl);
        parameters.add(comma);

        QualifiedNameReferenceNode typeName1 =
                NodeFactory.createQualifiedNameReferenceNode(AbstractNodeFactory.createIdentifierToken(GeneratorConstants.HTTP),
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.COLON),
                        AbstractNodeFactory.createIdentifierToken("ClientConfiguration"));
        IdentifierToken paramName1 = AbstractNodeFactory.createIdentifierToken(" httpClientConfig");
        BasicLiteralNode expression1 = NodeFactory.createBasicLiteralNode(null,
                AbstractNodeFactory.createIdentifierToken(" {}"));

        DefaultableParameterNode clientConfig = NodeFactory.createDefaultableParameterNode(annotationNodes, typeName1
                , paramName1, equalToken, expression1);
        parameters.add(clientConfig);

        SeparatedNodeList<ParameterNode> parameterList = AbstractNodeFactory.createSeparatedNodeList(parameters);
        IdentifierToken closeParan = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLOSE_PRAN);

        //Return type
        IdentifierToken returnsKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.RETURN);
        OptionalTypeDescriptorNode type =
                NodeFactory.createOptionalTypeDescriptorNode(NodeFactory.createErrorTypeDescriptorNode(
                        AbstractNodeFactory.createIdentifierToken("error"), null),
                        AbstractNodeFactory.createIdentifierToken("?"));
        ReturnTypeDescriptorNode returnNode = NodeFactory.createReturnTypeDescriptorNode(returnsKeyWord,
                annotationNodes, type);

        //Create function signature
        FunctionSignatureNode functionSignatureNode = NodeFactory.createFunctionSignatureNode(openParen,
                parameterList, closeParan, returnNode);

        //Create function body
        Token openBraceFB = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.OPEN_BRACE);
        //Create Statement
        List<StatementNode> statements = new ArrayList<>();

        // Variable Declaration Node
        NodeList<AnnotationNode> annotationNodes1 = AbstractNodeFactory.createEmptyNodeList();
        // Create type Binding
        QualifiedNameReferenceNode typeBindingPattern = NodeFactory.createQualifiedNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.HTTP),
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.COLON),
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLIENT_CLASS));
        CaptureBindingPatternNode bindingPattern =
                NodeFactory.createCaptureBindingPatternNode(AbstractNodeFactory.createIdentifierToken("httpEp"));
        TypedBindingPatternNode typedBindingPatternNode =
                NodeFactory.createTypedBindingPatternNode(typeBindingPattern, bindingPattern);

        //Expression node
        Token newKeyWord = AbstractNodeFactory.createIdentifierToken("new");
        Token openParenArg = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.OPEN_PRAN);
        List<Node> argumentsList = new ArrayList<>();
        PositionalArgumentNode positionalArgumentNode01 =
                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken("serviceUrl")));
        Token comma1 = AbstractNodeFactory.createIdentifierToken(",");
        PositionalArgumentNode positionalArgumentNode02 =
                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken("httpClientConfig")));

        argumentsList.add(positionalArgumentNode01);
        argumentsList.add(comma1);
        argumentsList.add(positionalArgumentNode02);

        SeparatedNodeList<FunctionArgumentNode> arguments = NodeFactory.createSeparatedNodeList(argumentsList);
        Token closeParenArg = AbstractNodeFactory.createToken(CLOSE_PAREN_TOKEN);
        ParenthesizedArgList parenthesizedArgList = NodeFactory.createParenthesizedArgList(openParenArg, arguments,
                closeParenArg);
        ImplicitNewExpressionNode expressionNode = NodeFactory.createImplicitNewExpressionNode(newKeyWord,
                parenthesizedArgList);
        CheckExpressionNode initializer = NodeFactory.createCheckExpressionNode(null,
                AbstractNodeFactory.createToken(CHECK_KEYWORD), expressionNode);
        VariableDeclarationNode variableDeclarationNode = NodeFactory.createVariableDeclarationNode(annotationNodes,
                null, typedBindingPatternNode, AbstractNodeFactory.createToken(EQUAL_TOKEN),
                initializer, AbstractNodeFactory.createToken(SEMICOLON_TOKEN));
        statements.add(variableDeclarationNode);
        //Assigment
        FieldAccessExpressionNode varRef =
                NodeFactory.createFieldAccessExpressionNode(NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken("self")),
                        AbstractNodeFactory.createToken(DOT_TOKEN),
                        NodeFactory.createSimpleNameReferenceNode(
                                AbstractNodeFactory.createIdentifierToken("clientEp")));
        SimpleNameReferenceNode expr =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken("httpEp"));
        AssignmentStatementNode assignmentStatementNode = NodeFactory.createAssignmentStatementNode(varRef,
                AbstractNodeFactory.createToken(EQUAL_TOKEN), expr,
                AbstractNodeFactory.createToken(SEMICOLON_TOKEN));

        NodeList<StatementNode> statementList = NodeFactory.createNodeList(variableDeclarationNode,
                assignmentStatementNode);

        Token closeBraceFB = AbstractNodeFactory.createToken(CLOSE_BRACE_TOKEN);
        FunctionBodyNode functionBodyNode = NodeFactory.createFunctionBodyBlockNode(openBraceFB, null,
                statementList, closeBraceFB);
        FunctionDefinitionNode initFunctionNode = NodeFactory.createFunctionDefinitionNode(null, null,
                qualifierList,functionKeyWord, functionName, NodeFactory.createEmptyNodeList(), functionSignatureNode
                , functionBodyNode);

        memberNodeList.add(initFunctionNode);
        memberNodeList.addAll(createRemoteFunctions(paths, filters));

        return NodeFactory.createClassDefinitionNode(null, visibilityQualifier,
                classTypeQualifiers, classKeyWord, className, openBrace, NodeFactory.createNodeList(memberNodeList),
                AbstractNodeFactory.createToken(CLOSE_BRACE_TOKEN));
    }

    private static ObjectFieldNode getClassField() {

        Token visibilityQualifierAttribute = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.PUBLIC);
        NodeList<Token> qualifierList = AbstractNodeFactory.createEmptyNodeList();
        QualifiedNameReferenceNode typeName =
                NodeFactory.createQualifiedNameReferenceNode(AbstractNodeFactory.createIdentifierToken(
                        GeneratorConstants.HTTP),
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.COLON),
                        AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLIENT_CLASS));
        IdentifierToken fieldName = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLIENT_EP);
        return NodeFactory.createObjectFieldNode(null, visibilityQualifierAttribute,
                qualifierList, typeName, fieldName, null, null,
                AbstractNodeFactory.createIdentifierToken(GeneratorConstants.SEMICOLON));
    }

    private static Paths setOperationId(Paths paths) {

        Set<Map.Entry<String, PathItem>> entries = paths.entrySet();
        for (Map.Entry<String, PathItem> entry: entries) {
            PathItem pathItem = entry.getValue();
            int countMissId = 0;
            for (Operation operation : entry.getValue().readOperations()) {
                if (operation.getOperationId() == null) {
                    //simplify here with 1++
                    countMissId = countMissId + 1;
                }
            }

            if (pathItem.getGet() != null) {
                Operation getOp = pathItem.getGet();
                if (getOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "get");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    getOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPut() != null) {
                Operation putOp = pathItem.getPut();
                if (putOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "put");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    putOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPost() != null) {
                Operation postOp = pathItem.getPost();
                if (postOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "post");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    postOp.setOperationId(operationId);
                }
            }
            if (pathItem.getDelete() != null) {
                Operation deleteOp = pathItem.getDelete();
                if (deleteOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "delete");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    deleteOp.setOperationId(operationId);
                }
            }
            if (pathItem.getOptions() != null) {
                Operation optionOp = pathItem.getOptions();
                if (optionOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "options");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    optionOp.setOperationId(operationId);
                }
            }
            if (pathItem.getHead() != null) {
                Operation headOp = pathItem.getHead();
                if (headOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "head");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    headOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPatch() != null) {
                Operation patchOp = pathItem.getPatch();
                if (patchOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "patch");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    patchOp.setOperationId(operationId);
                }
            }
            if (pathItem.getTrace() != null) {
                Operation traceOp = pathItem.getTrace();
                if (traceOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "trace");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    traceOp.setOperationId(operationId);
                }
            }
        }
        return paths;
    }

    private static String getOperationId(String[] split, String method) {

        String operationId;
        String regEx="\\{([^}]*)\\}";
        Matcher matcher = Pattern.compile(regEx).matcher(split[split.length - 1]);
        if (matcher.matches()) {
            operationId = method + split[split.length - 2] + "By" + matcher.group(1);
        } else {
            operationId = method + split[split.length - 1];
        }
        return operationId;
    }

    private static List<FunctionDefinitionNode> createRemoteFunctions (Paths paths, Filter filter) {
        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();
        Set<Map.Entry<String, PathItem>> pathsItems = paths.entrySet();
        Iterator<Map.Entry<String, PathItem>> pathItr = pathsItems.iterator();
        while (pathItr.hasNext()) {
            Map.Entry<String, PathItem> path = pathItr.next();
            if (!path.getValue().readOperationsMap().isEmpty()) {
                Map<PathItem.HttpMethod, Operation> operationMap = path.getValue().readOperationsMap();
                for (Map.Entry<PathItem.HttpMethod, Operation> operation : operationMap.entrySet()) {
                    //Add filter availability
                    //1.Tag filter
                    //2.Operation filter
                    //3. Both tag and operation filter
                    List<String> filterTags = filter.getTags();
                    List<String> operationTags = operation.getValue().getTags();
                    List<String> filterOperations  = filter.getOperations();
                    if (!filterTags.isEmpty() || !filterOperations.isEmpty()) {
                        if (operationTags != null || ((!filterOperations.isEmpty())
                                && (operation.getValue().getOperationId() != null))) {
                            if (GeneratorUtils.hasTags(operationTags, filterTags) ||
                                    ((operation.getValue().getOperationId() != null) &&
                                            filterOperations.contains(operation.getValue().getOperationId().trim()))) {
                                // function call
                                FunctionDefinitionNode functionDefinitionNode = getFunctionDefinitionNode(path.getKey()
                                        , operation);
                                functionDefinitionNodeList.add(functionDefinitionNode);
                            }
                        }
                    } else {
                        // function call
                        FunctionDefinitionNode functionDefinitionNode = getFunctionDefinitionNode(path.getKey(), operation);
                        functionDefinitionNodeList.add(functionDefinitionNode);
                    }
                }
            }
        }
        return functionDefinitionNodeList;
    }

    private static FunctionDefinitionNode getFunctionDefinitionNode(String path, Map.Entry<PathItem.HttpMethod,
            Operation> operation) {
        //Create qualifier list
        NodeList<Token> qualifierList =
                AbstractNodeFactory.createNodeList(AbstractNodeFactory.createToken(REMOTE_KEYWORD));
        Token functionKeyWord = AbstractNodeFactory.createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = AbstractNodeFactory.createIdentifierToken(operation.getValue().getOperationId());
        NodeList<Node> relativeResourcePath = NodeFactory.createEmptyNodeList();

        FunctionSignatureNode functionSignatureNode = getFunctionSignatureNode(operation.getValue());

        // Create Function Body
        FunctionBodyNode functionBodyNode = getFunctionBodyNode(path, operation);

        FunctionDefinitionNode functionDefinitionNode = NodeFactory.createFunctionDefinitionNode(null, null,
                qualifierList, functionKeyWord, functionName, relativeResourcePath, functionSignatureNode,
                functionBodyNode);

        return functionDefinitionNode;
    }

    private static FunctionSignatureNode getFunctionSignatureNode(Operation operation) {

        // Create Function Signature
        Token openParen = AbstractNodeFactory.createToken(OPEN_PAREN_TOKEN);
        // Create Parameters - function with parameters
        // Function RequestBody
        List<Node> parameterList =  new ArrayList<>();
        NodeList<AnnotationNode> annotationNodes = NodeFactory.createEmptyNodeList();
        Token comma =  NodeFactory.createToken(COMMA_TOKEN);
        if (operation.getParameters() != null) {
            setFunctionParameters(operation, parameterList, annotationNodes, comma);
            if (parameterList.size() >= 2) {
                parameterList.remove(parameterList.size()-1);
            }

        }
        SeparatedNodeList<ParameterNode> parameters = AbstractNodeFactory.createSeparatedNodeList(parameterList);
        Token closeParen = AbstractNodeFactory.createToken(CLOSE_PAREN_TOKEN);
        //Create Return type - function with response
        Token returnsKeyWord = AbstractNodeFactory.createToken(RETURNS_KEYWORD);
        //Type Always Union
        // Default
        ReturnTypeDescriptorNode returnTypeDescriptorNode = NodeFactory.createReturnTypeDescriptorNode(returnsKeyWord,
                annotationNodes, NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                        AbstractNodeFactory.createIdentifierToken("http:Response | error?")));

        return NodeFactory.createFunctionSignatureNode(openParen, parameters,
                closeParen, returnTypeDescriptorNode);
    }

    private static void setFunctionParameters(Operation operation, List<Node> parameterList,
                                              NodeList<AnnotationNode> annotationNodes, Token comma) {

        List<Parameter> parameters = operation.getParameters();
        for (Parameter parameter: parameters) {
            String in = parameter.getIn();
            if (in.equals("path")) {
                setPathParameters(parameterList, parameter);
                parameterList.add(comma);
            } else if (in.equals("query")) {
                setQueryParameters(parameterList, parameter);
                parameterList.add(comma);
            } else if (in.equals("header")) {

            }
        }
    }

    private static void setQueryParameters(List<Node> parameterList,Parameter parameter) {
        NodeList<AnnotationNode> annotationNodes = NodeFactory.createEmptyNodeList();
        TypeDescriptorNode typeName;
        String paramType = convertOpenAPITypeToBallerina(parameter.getSchema().getType().trim());
        if (parameter.getSchema().getType().trim().equals("array")) {
            ArraySchema arraySchema = (ArraySchema) parameter.getSchema();
            String itemType = arraySchema.getItems().getType();
            if (itemType.equals("string") | itemType.equals("integer") | itemType.equals("boolean")
                    | itemType.equals("float") | itemType.equals("decimal")) {
                //can make function
                paramType = convertOpenAPITypeToBallerina(itemType) + "[]";
            }
        }
        if (parameter.getRequired()) {
             typeName = NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken(paramType));
        } else {
             typeName = NodeFactory.createOptionalTypeDescriptorNode(
                    NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken(paramType)),
                    AbstractNodeFactory.createToken(QUESTION_MARK_TOKEN));
        }
        IdentifierToken paramName =
                AbstractNodeFactory.createIdentifierToken(escapeIdentifier(parameter.getName().trim()));
        RequiredParameterNode queryParam = NodeFactory.createRequiredParameterNode(annotationNodes,
                typeName, paramName);
        parameterList.add(queryParam);
    }

    private static void setPathParameters(List<Node> parameterList, Parameter parameter) {
        NodeList<AnnotationNode> annotationNodes = NodeFactory.createEmptyNodeList();

        BuiltinSimpleNameReferenceNode typeName = NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                AbstractNodeFactory
                        .createIdentifierToken(convertOpenAPITypeToBallerina(parameter.getSchema().getType().trim())));
        IdentifierToken paramName =
                AbstractNodeFactory.createIdentifierToken(escapeIdentifier(parameter.getName().trim()));
        RequiredParameterNode pathParam = NodeFactory.createRequiredParameterNode(annotationNodes,
                typeName, paramName);
        parameterList.add(pathParam);
    }

    private static FunctionBodyNode getFunctionBodyNode(String path,
                                                        Map.Entry<PathItem.HttpMethod, Operation> operation) {
        Token openBrace = AbstractNodeFactory.createToken(OPEN_BRACE_TOKEN);
        NodeList<AnnotationNode> annotationNodes = AbstractNodeFactory.createEmptyNodeList();
        // Create statements
        List<StatementNode> statementsList =  new ArrayList<>();
        // -- create variable declaration
        //string path - common
        TypedBindingPatternNode typedBindingPatternNode =
                NodeFactory.createTypedBindingPatternNode(NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken("string ")),
                        NodeFactory.createCaptureBindingPatternNode(
                                AbstractNodeFactory.createIdentifierToken("path")));

        Token equalToken = AbstractNodeFactory.createToken(EQUAL_TOKEN);

        // Create initializer
        Token type = AbstractNodeFactory.createToken(STRING_KEYWORD);
        Token startBacktick = AbstractNodeFactory.createToken(BACKTICK_TOKEN);
        //content  should decide with /pet and /pet/{pet}
        if (path.contains("{")) {
            path = path.replaceAll("[{]", "\\${");
        }
        //Handel optional query parameter
        List<StatementNode> queryStatement = new ArrayList<>();
        if (operation.getValue().getParameters() != null) {
            List<Parameter> parameters = operation.getValue().getParameters();
            int queryParamCount = 0;
            for (Parameter parameter: parameters) {
                if (parameter.getIn().trim().equals("query")) {
                    queryParamCount = queryParamCount + 1;
                    if (parameter.getRequired()) {
                        String queryParam = getQueryParamBindingString(queryParamCount, parameter);
                        path = path + queryParam;
                    } else {
                        IfElseStatementNode ifElseStatementNode = getIfElseStatementNode(queryParamCount, parameter);
                        queryStatement.add(ifElseStatementNode);
                    }
                }
            }
        }
        NodeList<Node> content = NodeFactory.createNodeList(NodeFactory.createLiteralValueToken(null, path,
                AbstractNodeFactory.createEmptyMinutiaeList(), AbstractNodeFactory.createEmptyMinutiaeList()));
        Token endBacktick = AbstractNodeFactory.createToken(BACKTICK_TOKEN);
        TemplateExpressionNode initializer = NodeFactory.createTemplateExpressionNode(null, type, startBacktick,
                content, endBacktick);
        Token semicolon = AbstractNodeFactory.createToken(SEMICOLON_TOKEN);

        VariableDeclarationNode pathInt = NodeFactory.createVariableDeclarationNode(annotationNodes,null,
                typedBindingPatternNode, equalToken, initializer, semicolon);

        statementsList.add(pathInt);
        statementsList.addAll(queryStatement);


        //create typeBind for return
        //default bind
        String responseType = "http:Response";
        SimpleNameReferenceNode resTypeBind =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken(responseType));
        CaptureBindingPatternNode bindingPattern =
                NodeFactory.createCaptureBindingPatternNode(AbstractNodeFactory.createIdentifierToken("response"));
        TypedBindingPatternNode resTypedBindingPatternNode = NodeFactory.createTypedBindingPatternNode(resTypeBind,
                bindingPattern);

        //Need to focus check expression node with initializer
        Token check = AbstractNodeFactory.createToken(CHECK_KEYWORD);

        FieldAccessExpressionNode fieldExpr =
                NodeFactory.createFieldAccessExpressionNode(NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken("self")),
                        AbstractNodeFactory.createToken(DOT_TOKEN),
                        NodeFactory.createSimpleNameReferenceNode(
                                AbstractNodeFactory.createIdentifierToken("clientEp")));
        Token rightArrowToken = AbstractNodeFactory.createToken(RIGHT_ARROW_TOKEN);
        //Method name
        SimpleNameReferenceNode methodName =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken(
                        operation.getKey().name().trim().toLowerCase(Locale.ENGLISH)));
        Token openParen = AbstractNodeFactory.createToken(OPEN_PAREN_TOKEN);
        //Argument Nodes
        List<Node> argumentsNodes = new ArrayList<>();
        PositionalArgumentNode positionalArgumentNode =
                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken("path")));
        argumentsNodes.add(positionalArgumentNode);
        Token comma = AbstractNodeFactory.createToken(COMMA_TOKEN);
        argumentsNodes.add(comma);
        SimpleNameReferenceNode argumentNode =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken("targetType"));
        SimpleNameReferenceNode expression =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken("http:Response"));
        NamedArgumentNode targetTypeNode = NodeFactory.createNamedArgumentNode(argumentNode, equalToken, expression);
        argumentsNodes.add(targetTypeNode);
        SeparatedNodeList<FunctionArgumentNode> argumentNodeList = NodeFactory.createSeparatedNodeList(argumentsNodes);
        Token closeParen = AbstractNodeFactory.createToken(CLOSE_PAREN_TOKEN);
        RemoteMethodCallActionNode actionNode = NodeFactory.createRemoteMethodCallActionNode(fieldExpr,
                rightArrowToken,methodName, openParen, argumentNodeList, closeParen);
        CheckExpressionNode initRes = NodeFactory.createCheckExpressionNode(null, check, actionNode);
        VariableDeclarationNode checkVariable = NodeFactory.createVariableDeclarationNode(annotationNodes, null,
                resTypedBindingPatternNode, equalToken, initRes, semicolon);
        statementsList.add(checkVariable);
        //Return Variable
        Token returnKeyWord = AbstractNodeFactory.createToken(RETURN_STATEMENT);
        SimpleNameReferenceNode returnType =
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken("response"));

        ReturnStatementNode returnStatementNode = NodeFactory.createReturnStatementNode(returnKeyWord, returnType,
                semicolon);
        statementsList.add(returnStatementNode);

        //Create statements
        NodeList<StatementNode> statements = NodeFactory.createNodeList(statementsList);
        Token closeBrace = NodeFactory.createToken(CLOSE_BRACE_TOKEN);
        return NodeFactory.createFunctionBodyBlockNode(openBrace, null, statements,
                closeBrace);
    }

    private static IfElseStatementNode getIfElseStatementNode(int queryParamCount, Parameter parameter) {

        //Create IfElse statement
        Token ifKeyWord = AbstractNodeFactory.createToken(IF_KEYWORD);
        SimpleNameReferenceNode queryParam = NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken(escapeIdentifier(parameter.getName())));

        Token isKeyWord = AbstractNodeFactory.createToken(IS_KEYWORD);
        //Temporary assign to null while handlin other data
        TypeTestExpressionNode conditionNode = null;
        if (parameter.getSchema() instanceof ArraySchema) {
            ArraySchema schema = (ArraySchema) parameter.getSchema();
            String itemType = schema.getItems().getType();
            if (itemType.equals("string") | itemType.equals("integer") | itemType.equals("boolean")
                    | itemType.equals("float") | itemType.equals("decimal")) {
                BuiltinSimpleNameReferenceNode queryParamType =
                        NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                                AbstractNodeFactory.createIdentifierToken(
                                        convertOpenAPITypeToBallerina(itemType) + "[]"));
                conditionNode = NodeFactory.createTypeTestExpressionNode(queryParam, isKeyWord,
                        queryParamType);
            } else {

            }
        } else if (!(parameter.getSchema() instanceof ObjectSchema)) {
            BuiltinSimpleNameReferenceNode queryParamType = NodeFactory.createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken(convertOpenAPITypeToBallerina(
                            parameter.getSchema().getType().trim())));

            conditionNode = NodeFactory.createTypeTestExpressionNode(queryParam, isKeyWord, queryParamType);
        }
        BracedExpressionNode conditionStatement = NodeFactory.createBracedExpressionNode(null,
                AbstractNodeFactory.createToken(OPEN_PAREN_TOKEN), conditionNode,
                AbstractNodeFactory.createToken(CLOSE_PAREN_TOKEN));

        //If Body handle
        SimpleNameReferenceNode lhsExpr = NodeFactory.createSimpleNameReferenceNode(
                AbstractNodeFactory.createIdentifierToken("path"));
        Token operator = AbstractNodeFactory.createToken(PLUS_TOKEN);

        String contentPath = getQueryParamBindingString(queryParamCount, parameter);
        SimpleNameReferenceNode pathContent = NodeFactory.createSimpleNameReferenceNode(
                        AbstractNodeFactory.createIdentifierToken(contentPath));

        TemplateExpressionNode rhsExpr = NodeFactory.createTemplateExpressionNode(null, null,
                AbstractNodeFactory.createToken(BACKTICK_TOKEN),
                NodeFactory.createNodeList(pathContent),
                AbstractNodeFactory.createToken(BACKTICK_TOKEN));

        BinaryExpressionNode binaryExpr = NodeFactory.createBinaryExpressionNode(null, lhsExpr, operator, rhsExpr);
        AssignmentStatementNode ifBodyStatement = NodeFactory.createAssignmentStatementNode(
                        NodeFactory.createSimpleNameReferenceNode(
                                AbstractNodeFactory.createIdentifierToken("path")),
                                AbstractNodeFactory.createToken(EQUAL_TOKEN), binaryExpr,
                                AbstractNodeFactory.createToken(SEMICOLON_TOKEN));

        NodeList<StatementNode> ifBodyContent = NodeFactory.createNodeList(ifBodyStatement);
        BlockStatementNode ifBlock = NodeFactory.createBlockStatementNode(
                AbstractNodeFactory.createToken(OPEN_BRACE_TOKEN), ifBodyContent,
                AbstractNodeFactory.createToken(CLOSE_BRACE_TOKEN));

        return NodeFactory.createIfElseStatementNode(ifKeyWord, conditionStatement, ifBlock, null);
    }

    private static String getQueryParamBindingString(int queryParamCount, Parameter parameter) {

        String queryParam;
        if (queryParamCount == 1) {
            queryParam =
                    "?" + parameter.getName().trim() + "=${" + escapeIdentifier(parameter.getName().trim()) + "}";
        } else {
            queryParam =
                    "&" + parameter.getName().trim() + "=${" + escapeIdentifier(parameter.getName().trim()) + "}";
        }
        return queryParam;
    }
}
