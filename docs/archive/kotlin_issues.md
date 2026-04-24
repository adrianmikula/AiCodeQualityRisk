

Found the root cause! JavaParser returns:

Kotlin: stringLiteralCount: 0 (nothing parsed!)
Java: stringLiteralCount: 1 (correct)
JavaParser can't parse Kotlin syntax properly. When analyzing Kotlin code, it doesn't fail - it just returns empty metrics because no Java AST nodes are found.

I'll add a regex-based fallback for Kotlin files when AST parsing returns mostly zeros:

