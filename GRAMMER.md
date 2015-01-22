This file gives the target grammer of Babble programming language. Since the language is first attempt to write my own, it'll be simple and stupid.

	 program ::= statements End

	 statements ::= statement stmt_separator statements | null

	 last_statement ::= 'return' expression | null

	 stmt_separator ::= ';' | null

	 block ::= '{' statements last_statement '}' |
		   statement

	 statement ::= variable_list '=' expression_list |
		       function_call |
		       'while' expression block |
		       if expression then block elif_statement else_statement |
		       for Identifier '=' expression ',' expression optional_step block |
		       for Identifiler_list in expression_list block |
		       function_def | 'local' function_def |
		       'local' identifier_list '=' expresssion_list

	 elif_statement ::= 'elif' expression then block elif_statement | null

	 else_statement ::= 'else' block | null

	 var ::= Identifier |
		 prefix_expression '[' expression ']' |
		 prefix_expression '.' Identifier

	 variable_list ::= var var_continue

	 var_continue ::= ',' var | null

	 expression_list ::= expression expression_continue

	 expression_continue ::= ',' expression | null

	 identifier_list ::= Identifier identifier_continue

	 identifier_continue ::= ',' Identifier | null

	 function ::= 'function' function_body

	 function_def ::= 'function' Identifier function_body

	 function_body ::= '(' identifier_list ')' block

	 expression ::= Nil | False | True | Number | String | function |
			prefix_expression | map | expression binary_op expression |
		        uniq_op expression

	 prefix_expression ::= var | function_call | '(' expression ')'

	 function_call ::= var arguments

	 arguments ::= '(' expression_list ')' | map

	 map ::= '{' field_list '}'

	 field_list ::= field filed_continue| null

	 field ::= Identifier '=' expression | expression

	 field_continue ::= ';' field | null

	 binary_op ::= '+' | '-' | '*' | '/' | '^' | '%' | '..' | '<' | '<=' | '>' | '>=' | '==' |
		       '~=' | and | or

	 uniq_op ::= '-' | '#' | not