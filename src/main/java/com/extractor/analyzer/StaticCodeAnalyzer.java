package com.extractor.analyzer;

import com.extractor.model.CodeIssue;
import com.extractor.model.Component;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.CtScanner;

/**
 * Analyzes Java code patterns using Spoon to detect common issues.
 */
public class StaticCodeAnalyzer {

    public StaticCodeAnalyzer() {
    }

    public void analyzeType(CtType<?> type, Component component) {
        // Check Package Naming Convention
        checkPackageNaming(type, component);

        type.accept(new CtScanner() {

            @Override
            public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
                super.visitCtBinaryOperator(operator);

                // Check for String comparison with == or !=
                if (operator.getKind() == BinaryOperatorKind.EQ || operator.getKind() == BinaryOperatorKind.NE) {
                    if (isString(operator.getLeftHandOperand()) || isString(operator.getRightHandOperand())) {
                        component.addCodeIssue(CodeIssue.builder()
                                .type(CodeIssue.Type.BUG_PATTERN)
                                .severity(CodeIssue.Severity.WARNING)
                                .message("Potential String comparison using " +
                                        (operator.getKind() == BinaryOperatorKind.EQ ? "==" : "!=") +
                                        ". Use .equals() instead.")
                                .line(getSafeLine(operator))
                                .pattern("string_comparison_operator")
                                .build());
                    }
                }
            }

            @Override
            public void visitCtCatch(CtCatch catchBlock) {
                super.visitCtCatch(catchBlock);

                // Check for empty catch blocks
                CtBlock<?> body = catchBlock.getBody();
                if (body != null && body.getStatements().isEmpty()) {
                    component.addCodeIssue(CodeIssue.builder()
                            .type(CodeIssue.Type.BUG_PATTERN)
                            .severity(CodeIssue.Severity.WARNING)
                            .message("Empty catch block detected. Exceptions should not be swallowed.")
                            .line(getSafeLine(catchBlock))
                            .pattern("empty_catch_block")
                            .build());
                }

                // Check for catching generic Exception
                if (catchBlock.getParameter() != null && catchBlock.getParameter().getType() != null) {
                    String exceptionType = catchBlock.getParameter().getType().getQualifiedName();
                    if ("java.lang.Exception".equals(exceptionType) || "java.lang.Throwable".equals(exceptionType)) {
                        component.addCodeIssue(CodeIssue.builder()
                                .type(CodeIssue.Type.BUG_PATTERN)
                                .severity(CodeIssue.Severity.INFO)
                                .message("Catching generic Exception or Throwable is generally discouraged.")
                                .line(getSafeLine(catchBlock))
                                .pattern("generic_exception_catch")
                                .build());
                    }
                }
            }

            @Override
            public <T> void visitCtInvocation(CtInvocation<T> invocation) {
                super.visitCtInvocation(invocation);

                if (invocation.getExecutable() == null)
                    return;

                // Check for System.out.println / System.err.println
                if (invocation.getTarget() != null) {
                    String target = invocation.getTarget().toString();
                    String method = invocation.getExecutable().getSimpleName();
                    if (("System.out".equals(target) || "System.err".equals(target)) &&
                            (method.startsWith("print"))) {
                        component.addCodeIssue(CodeIssue.builder()
                                .type(CodeIssue.Type.CODE_STYLE)
                                .severity(CodeIssue.Severity.INFO)
                                .message("Direct use of System.out/err. Use a logger instead.")
                                .line(getSafeLine(invocation))
                                .pattern("system_out_println")
                                .build());
                    }
                }

                // Check for hardcoded string comparisons in .equals() or .contains()
                String method = invocation.getExecutable().getSimpleName();
                if (("equals".equals(method) || "contains".equals(method) || "equalsIgnoreCase".equals(method)) &&
                        !invocation.getArguments().isEmpty()) {
                    CtExpression<?> arg = invocation.getArguments().get(0);
                    if (arg instanceof CtLiteral && ((CtLiteral<?>) arg).getValue() instanceof String) {
                        component.addCodeIssue(CodeIssue.builder()
                                .type(CodeIssue.Type.CODE_STYLE)
                                .severity(CodeIssue.Severity.INFO)
                                .message("Comparison with hardcoded String literal: \""
                                        + ((CtLiteral<?>) arg).getValue() + "\". Consider using a constant.")
                                .line(getSafeLine(invocation))
                                .pattern("hardcoded_string_comparison")
                                .build());
                    }
                }
            }
        });
    }

    private void checkPackageNaming(CtType<?> type, Component component) {
        CtPackage pkg = type.getPackage();
        if (pkg == null)
            return;

        String fqn = pkg.getQualifiedName();
        if (fqn == null || fqn.isEmpty())
            return;

        String[] parts = fqn.split("\\.");
        for (String part : parts) {
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                component.addCodeIssue(CodeIssue.builder()
                        .type(CodeIssue.Type.CODE_STYLE)
                        .severity(CodeIssue.Severity.INFO)
                        .message("Package name component \"" + part
                                + "\" is capitalized. Java convention recommends lowercase package names.")
                        .line(1) // Class level
                        .pattern("capitalized_package_name")
                        .build());
                break; // One warning per component is enough
            }
        }
    }

    private boolean isString(CtExpression<?> expression) {
        if (expression == null || expression.getType() == null)
            return false;
        String qName = expression.getType().getQualifiedName();
        return "java.lang.String".equals(qName) || "String".equals(qName);
    }

    private int getSafeLine(CtElement element) {
        if (element != null && element.getPosition() != null && element.getPosition().isValidPosition()) {
            return element.getPosition().getLine();
        }
        return 0;
    }
}
